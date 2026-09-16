package com.sheyingtong.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sheyingtong.config.AiAsyncProperties;
import com.sheyingtong.dto.response.AiTaskStatusResponse;
import com.sheyingtong.dto.response.AiTaskSubmitResponse;
import com.sheyingtong.entity.AiBizType;
import com.sheyingtong.entity.AiTask;
import com.sheyingtong.entity.AiTaskStatus;
import com.sheyingtong.entity.User;
import com.sheyingtong.exception.AiServiceUnavailableException;
import com.sheyingtong.mq.AiTaskProducer;
import com.sheyingtong.repository.AiTaskRepository;
import com.sheyingtong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 异步任务的对外门面（提交 / 查状态 / 重试）
 * ----------------------------------------------------------------
 * 提交接口的全部工作就三步，都在毫秒级：
 *   1. 落盘（调用方已完成）
 *   2. 建一条 ai_task(PENDING) 记录 —— 短事务
 *   3. 投一条只含 taskId 的消息 —— 立即返回
 * 图片压缩、Qwen-VL 调用全部发生在消费者线程，**期间不持有任何数据库事务与连接**。
 * 这是本次改造的核心收益（改造前这些活都在 @Transactional 里干）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiTaskService {

    private final AiTaskStateService aiTaskStateService;
    private final AiTaskProducer aiTaskProducer;
    private final AiTaskRepository aiTaskRepository;
    private final UserRepository userRepository;
    private final AiAsyncProperties aiAsyncProperties;
    private final ObjectMapper objectMapper;

    /**
     * 全部 AI 任务执行器的**延迟**来源。
     * ⚠️ 不能用构造期 List<AiTaskExecutor> 直接注入：执行器(消费者) → 领域服务(PhotoService/
     * RetouchService) → AiTaskService 是一个闭环，构造期解析会触发 Spring 的循环依赖报错。
     * 用 ObjectProvider 把解析延后到 executeInline 真正发生（请求线程、应用已启动完成），
     * 此时所有单例（含 AOP 代理）都已就绪，既解开环又不会拿到未完成的早期引用。
     */
    private final ObjectProvider<List<AiTaskExecutor>> executorsProvider;

    // ==================== 提交 ====================

    /**
     * 提交一个 AI 任务并立即返回（不等待 AI 结果）。
     *
     * @param bizType     业务类型
     * @param filePath    已落盘的图片路径
     * @param fileName    原始文件名
     * @param description 用户描述（可空）
     * @param username    当前登录用户名
     */
    public AiTaskSubmitResponse submit(AiBizType bizType, String filePath, String fileName,
                                       String description, String username) {
        User user = resolveUser(username);
        AiTask task = aiTaskStateService.create(bizType, filePath, fileName, description, user);

        boolean inline = dispatch(task);
        AiTask fresh = reload(task);

        log.info("[ai-task] 提交完成: taskId={}, bizType={}, status={}, 就地执行={}",
                fresh.getId(), fresh.getBizType(), fresh.getStatus(), inline);

        return AiTaskSubmitResponse.builder()
                .taskId(fresh.getId())
                .bizType(fresh.getBizType().name())
                .status(fresh.getStatus().name())
                .degradedToSync(inline)
                .build();
    }

    // ==================== 查状态 ====================

    /**
     * 查询任务状态与结果（前端轮询入口）。
     * @Transactional(readOnly=true) 是必要的：下面要访问 task.getUser() 这个 LAZY 关联做归属校验，
     * 没有事务会抛 LazyInitializationException。
     */
    @Transactional(readOnly = true)
    public AiTaskStatusResponse getStatus(Long taskId, String username) {
        AiTask task = aiTaskRepository.findById(taskId)
                // ⚠️ "不存在"与"无权查看"合并成同一个提示，避免通过状态码差异探测他人 taskId 是否存在
                .orElseThrow(() -> new RuntimeException("任务不存在或无权查看"));

        if (task.getUser() == null || !task.getUser().getUsername().equals(username)) {
            throw new RuntimeException("任务不存在或无权查看");
        }

        return AiTaskStatusResponse.builder()
                .taskId(task.getId())
                .bizType(task.getBizType().name())
                .status(task.getStatus().name())
                // ⚠️ FAILED 有两种：中间态（MQ 退避重试间隙，attempt_count 未耗尽）与终态（重试耗尽）。
                //    不能照搬 task.isTerminal()（它把 FAILED 一律算终态）——否则第一次尝试失败
                //    前端就停轮询，永远等不到重试最终写出的 DEGRADED 结果。
                .terminal(isTerminalAfterRetries(task))
                .retryable(isRetryable(task))
                .bizId(task.getBizId())
                .errorMessage(task.getErrorMessage())
                .result(readResult(task))
                .build();
    }

    /**
     * 终态判定（区分"短暂的 FAILED"与"重试耗尽的 FAILED"）。
     * FAILED 在 attempt_count 未达上限前只是"本轮尝试失败"的中间态，
     * MQ 退避结束后会被消费者重新抢成 RUNNING；只有 attempts 耗尽才算结束。
     */
    private boolean isTerminalAfterRetries(AiTask task) {
        if (task.getStatus() == AiTaskStatus.SUCCESS || task.getStatus() == AiTaskStatus.DEGRADED) {
            return true;
        }
        if (task.getStatus() == AiTaskStatus.FAILED) {
            return task.getAttemptCount() >= aiAsyncProperties.getMaxAttempts();
        }
        return false;
    }

    // ==================== 手动重试 ====================

    /**
     * 手动重试失败/降级的任务：重置状态 → 重新投递（复用磁盘上的原图，不需要用户再上传一次）。
     */
    public AiTaskSubmitResponse retry(Long taskId, String username) {
        AiTask task = aiTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在或无权查看"));

        if (task.getUser() == null || !task.getUser().getUsername().equals(username)) {
            throw new RuntimeException("任务不存在或无权查看");
        }
        if (!isRetryable(task)) {
            throw new RuntimeException("当前状态不可重试，任务已被处理或正在处理中");
        }

        // 带条件 UPDATE：并发点两次重试只有一个能改成 PENDING，另一个在这里被挡住
        if (!aiTaskStateService.resetForRetry(taskId)) {
            throw new RuntimeException("重试失败，任务状态已变化，请刷新后重试");
        }

        AiTask reset = reload(task);
        boolean inline = dispatch(reset);
        AiTask fresh = reload(reset);

        return AiTaskSubmitResponse.builder()
                .taskId(fresh.getId())
                .bizType(fresh.getBizType().name())
                .status(fresh.getStatus().name())
                .degradedToSync(inline)
                .build();
    }

    // ==================== 内部实现 ====================

    /**
     * 投递消息；broker 不可达或开关关闭时按配置降级为"就地同步执行"。
     *
     * @return true 表示走了就地执行（没有经过 MQ）
     */
    private boolean dispatch(AiTask task) {
        if (!aiAsyncProperties.isEnabled()) {
            log.info("[ai-task] app.ai.async.enabled=false，跳过投递，就地执行: taskId={}", task.getId());
            safeExecuteInline(task);
            return true;
        }

        try {
            aiTaskProducer.publish(task);
            return false;
        } catch (AmqpException e) {
            if (!aiAsyncProperties.isFallbackToSync()) {
                log.error("[ai-task] 消息投递失败且未开启同步降级: taskId={}", task.getId(), e);
                aiTaskStateService.markFailed(task.getId(), "消息队列不可用: " + e.getMessage());
                throw new AiServiceUnavailableException("AI 任务队列暂时不可用，请稍后重试", e);
            }
            // 降级：慢但可用（请求会挂到 AI 返回为止），而不是直接告诉用户"功能坏了"
            log.warn("[ai-task] broker 不可达，降级为就地同步执行: taskId={}, 原因={}", task.getId(), e.getMessage());
            safeExecuteInline(task);
            return true;
        }
    }

    /**
     * 就地执行并吞掉异常：执行器内部已经把任务标记成终态（FAILED / DEGRADED，
     * 见 PhotoScoreConsumer/RetouchConsumer 的 settleAfterRetriesExhausted），
     * 这里再往外抛只会让提交接口变成 500，前端反而拿不到 taskId 去展示/重试。
     * 提交响应会带上任务此时的终态，前端据此显示失败原因 + 重试按钮。
     */
    private void safeExecuteInline(AiTask task) {
        try {
            executeInline(task);
        } catch (Exception e) {
            log.error("[ai-task] 就地同步执行失败，任务已标记终态（可从前端重试）: taskId={}", task.getId(), e);
        }
    }

    private void executeInline(AiTask task) {
        AiTaskExecutor executor = executorsProvider.getObject().stream()
                .filter(e -> e.bizType() == task.getBizType())
                .findFirst()
                .orElse(null);
        if (executor == null) {
            throw new IllegalStateException("没有为 " + task.getBizType() + " 注册 AI 任务执行器");
        }
        executor.execute(task.getId());
    }

    /**
     * 终态之外的一个判定：FAILED 与 DEGRADED 允许重试，其余状态不允许。
     */
    private boolean isRetryable(AiTask task) {
        return task.getStatus() == AiTaskStatus.FAILED || task.getStatus() == AiTaskStatus.DEGRADED;
    }

    /** 把 result_json 解析回对象返回给前端；解析不了就返回 null，不让脏数据把轮询接口打挂 */
    private Object readResult(AiTask task) {
        if (task.getStatus() != AiTaskStatus.SUCCESS && task.getStatus() != AiTaskStatus.DEGRADED) {
            return null;
        }
        if (task.getResultJson() == null || task.getResultJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(task.getResultJson());
        } catch (Exception e) {
            log.error("[ai-task] result_json 解析失败: taskId={}", task.getId(), e);
            return null;
        }
    }

    private AiTask reload(AiTask task) {
        return aiTaskRepository.findById(task.getId()).orElse(task);
    }

    private User resolveUser(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }
}
