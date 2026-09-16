package com.sheyingtong.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sheyingtong.config.AiAsyncProperties;
import com.sheyingtong.dto.response.PhotoScoreResponse;
import com.sheyingtong.entity.AiBizType;
import com.sheyingtong.entity.AiTask;
import com.sheyingtong.entity.PhotoAnalysis;
import com.sheyingtong.service.AIService;
import com.sheyingtong.service.AiTaskExecutor;
import com.sheyingtong.service.AiTaskStateService;
import com.sheyingtong.service.ImageCompressor;
import com.sheyingtong.service.PhotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 照片打分任务消费者（消息驱动 + 就地执行共用同一份处理逻辑，见 AiTaskExecutor）
 * ----------------------------------------------------------------
 * 处理流水线：
 *  1. 抢占（claim）：PENDING/FAILED → RUNNING；抢不到（已被处理/次数用尽）直接跳过
 *  2. 读图 → ImageCompressor 压缩 → AIService 调 Qwen-VL（**全程无事务、无数据库连接**）
 *  3. 成功：PhotoService.persistScore 写 photo_analysis → markSuccess(taskId, bizId, resultJson)
 *  4. 失败：
 *     - 非最后一次尝试 → markFailed + **抛异常** → 交给 RabbitMQ RetryInterceptor 指数退避重试
 *     - 最后一次尝试   → 按 app.ai.async.degrade-on-failure 决定写兜底(DEGRADED)还是标失败(FAILED)，
 *                        然后同样**抛异常** → 重试耗尽的消息被转发进死信队列，作为审计记录
 *       （任务此时已是终态，从死信队列重放也不会被抢占，天然幂等）
 *
 * 为什么第 4 步无论降级与否都要抛：Recoverer 只有在监听器抛异常时才会转发死信，
 * "用尽了全部重试"这件事本身就是需要被运维看到的信号。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoScoreConsumer implements AiTaskExecutor {

    private final AiTaskStateService aiTaskStateService;
    private final AIService aiService;
    private final PhotoService photoService;
    private final ImageCompressor imageCompressor;
    private final AiAsyncProperties aiAsyncProperties;
    private final ObjectMapper objectMapper;

    @Override
    public AiBizType bizType() {
        return AiBizType.PHOTO_SCORE;
    }

    @RabbitListener(queues = RabbitNames.PHOTO_SCORE_QUEUE)
    public void onMessage(AiTaskMessage message) {
        if (message == null || message.taskId() == null) {
            log.warn("[photo-score] 收到空消息，直接确认丢弃");
            return;
        }
        execute(message.taskId());
    }

    @Override
    public void execute(Long taskId) {
        AiTask task = aiTaskStateService.claim(taskId);
        if (task == null) {
            // 幂等闸门挡下的重复消息：已被处理 / 已终态 / 次数用尽
            log.info("[photo-score] 任务抢占失败，跳过: taskId={}（可能已被处理或达到重试上限）", taskId);
            return;
        }

        boolean lastAttempt = task.getAttemptCount() >= aiAsyncProperties.getMaxAttempts();

        try {
            byte[] rawImage = Files.readAllBytes(Paths.get(task.getFilePath()));
            byte[] compressed = imageCompressor.compress(rawImage);
            log.info("[photo-score] 开始 AI 打分: taskId={}, 尝试={}/{}", taskId, task.getAttemptCount(), aiAsyncProperties.getMaxAttempts());

            String aiText = aiService.analyzePhoto(compressed, task.getDescription());
            Map<String, Object> aiResult = photoService.parseAIResponse(aiText);

            PhotoAnalysis saved = photoService.persistScore(task, aiResult);
            PhotoScoreResponse response = photoService.toResponse(saved);

            aiTaskStateService.markSuccess(taskId, saved.getId(), toJson(response));
            log.info("[photo-score] 打分成功: taskId={}, 记录ID={}, 评分={}", taskId, saved.getId(), saved.getScore());
        } catch (Exception e) {
            if (lastAttempt) {
                settleAfterRetriesExhausted(taskId, task, e);
            } else {
                log.warn("[photo-score] 本轮尝试失败，交给消息层重试: taskId={}, 原因={}", taskId, summarize(e));
                aiTaskStateService.markFailed(taskId, summarize(e));
            }
            // 必须把异常抛给 MQ 容器：非末次触发退避重试，末次触发恢复器转发死信
            throw wrap(e);
        }
    }

    /** 重试全部耗尽：按配置降级（写 mock 标 DEGRADED）或失败（标 FAILED） */
    private void settleAfterRetriesExhausted(Long taskId, AiTask task, Exception original) {
        if (aiAsyncProperties.isDegradeOnFailure()) {
            try {
                PhotoAnalysis saved = photoService.persistMockScore(task);
                PhotoScoreResponse response = photoService.toResponse(saved);
                aiTaskStateService.markDegraded(taskId, saved.getId(), toJson(response), summarize(original));
                log.warn("[photo-score] 重试耗尽，已写入降级结果: taskId={}, 记录ID={}, 原因={}",
                        taskId, saved.getId(), summarize(original));
                return;
            } catch (Exception degradeError) {
                log.error("[photo-score] 降级写入也失败，标记为 FAILED: taskId={}", taskId, degradeError);
            }
        }
        aiTaskStateService.markFailed(taskId, summarize(original));
        log.error("[photo-score] 重试耗尽，任务失败: taskId={}, 原因={}", taskId, summarize(original));
    }

    private String toJson(PhotoScoreResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("[photo-score] 序列化打分结果失败: taskId={}", response, e);
            return "{}";
        }
    }

    private String summarize(Throwable e) {
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return message.length() > 300 ? message.substring(0, 300) : message;
    }

    private RuntimeException wrap(Exception e) {
        return e instanceof RuntimeException re ? re : new RuntimeException("照片打分任务执行失败", e);
    }
}