package com.sheyingtong.mq;

import com.sheyingtong.config.AiAsyncProperties;
import com.sheyingtong.entity.AiTask;
import com.sheyingtong.repository.AiTaskRepository;
import com.sheyingtong.service.AiTaskStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动恢复：重整"孤儿 RUNNING"任务
 * ----------------------------------------------------------------
 * 场景：消费者正在调 AI（任务 status=RUNNING）时进程被 kill / 崩溃，
 * RabbitMQ 检测到连接断开会把未 ack 的消息重投，但抢占闸门只认
 * PENDING/FAILED —— RUNNING 任务抢不动 → 永久卡死，前端永远转圈。
 *
 * 解决：应用启动完成后，把遗留的 RUNNING 任务重置回 PENDING 并重新投递
 * 一次消息，让消费者把它们抢起来续跑完。AI 调用本来就有 180s 超时，
 * "启动时还在 RUNNING"只可能是上一轮进程非正常终止，不会误伤正常流转。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiTaskStartupRecovery implements ApplicationRunner {

    private final AiTaskStateService aiTaskStateService;
    private final AiTaskRepository aiTaskRepository;
    private final AiTaskProducer aiTaskProducer;
    private final AiAsyncProperties aiAsyncProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!aiAsyncProperties.isRecoverInterruptedOnStartup()) {
            log.info("[ai-task] 启动恢复已关闭（app.ai.async.recover-interrupted-on-startup=false），跳过孤儿任务检查");
            return;
        }

        List<Long> recovered = aiTaskStateService.recoverInterruptedRunning();
        if (recovered.isEmpty()) {
            log.info("[ai-task] 启动恢复：无孤儿 RUNNING 任务");
            return;
        }

        int republished = 0;
        for (Long taskId : recovered) {
            try {
                AiTask task = aiTaskRepository.findById(taskId).orElse(null);
                if (task == null) {
                    continue;
                }
                aiTaskProducer.publish(task);
                republished++;
            } catch (Exception e) {
                log.error("[ai-task] 启动恢复重投失败: taskId={}", taskId, e);
            }
        }
        log.warn("[ai-task] 启动恢复完成：重置 {} 个孤儿任务并重投 {} 条消息", recovered.size(), republished);
    }
}