package com.sheyingtong.service;

import com.sheyingtong.config.AiAsyncProperties;
import com.sheyingtong.entity.AiBizType;
import com.sheyingtong.entity.AiTask;
import com.sheyingtong.entity.AiTaskStatus;
import com.sheyingtong.entity.User;
import com.sheyingtong.repository.AiTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * AI 任务的状态流转服务（事务边界集中在这里）
 * ----------------------------------------------------------------
 * ⚠️ 为什么必须单独一个 Bean，而不是把 @Transactional 方法写在 AiTaskService / 消费者里：
 *
 *   Spring 的 @Transactional 靠 AOP 代理生效。**同类内部的方法自调用不走代理**，
 *   注解会被静默忽略，事务根本不开 —— 这是本项目里最容易踩、又最难发现的坑
 *   （不会报错，只是"看起来加了事务但没生效"）。把这些方法抽到独立 Bean 里，
 *   调用方只能通过代理进入，事务就一定生效。
 *
 * 另一个关键点：**每个状态变更都是一个短事务、立即提交**。
 * 消费者在两次调用之间（跑压缩、调 AI 的那几十秒）没有任何事务和数据库连接，
 * 这正是本次改造要解决的核心问题（改造前 @Transactional 包住了整个 AI 调用）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiTaskStateService {

    private final AiTaskRepository aiTaskRepository;
    private final AiAsyncProperties aiAsyncProperties;

    /** 可被抢占的状态：终态抢不动，FAILED 允许重新抢占（MQ 重试与手动重试都靠这条） */
    private static final List<AiTaskStatus> CLAIMABLE =
            List.of(AiTaskStatus.PENDING, AiTaskStatus.FAILED);

    /** 允许手动重试的状态 */
    private static final List<AiTaskStatus> RETRYABLE =
            List.of(AiTaskStatus.FAILED, AiTaskStatus.DEGRADED);

    /**
     * 新建任务（短事务）。提交接口只做"落盘 + 建这条记录 + 投消息"三件事。
     */
    @Transactional
    public AiTask create(AiBizType bizType,
                         String filePath, String fileName, String description,
                         User user) {
        AiTask task = AiTask.builder()
                .bizType(bizType)
                .status(AiTaskStatus.PENDING)
                .filePath(filePath)
                .fileName(fileName)
                .description(description)
                .user(user)
                .attemptCount(0)
                .build();
        return aiTaskRepository.save(task);
    }

    /**
     * 抢占任务（短事务，立即提交）。
     *
     * @return 抢占成功返回**重新读出**的任务（attemptCount 已 +1，供判断是否最后一次尝试）；
     *         返回 null 表示没抢到（已被别的消费者处理 / 已终态 / 次数用尽）→ 调用方直接 ack 跳过
     */
    @Transactional
    public AiTask claim(Long taskId) {
        int rows = aiTaskRepository.claimForRunning(
                taskId,
                AiTaskStatus.RUNNING,
                CLAIMABLE,
                aiAsyncProperties.getMaxAttempts(),
                LocalDateTime.now());

        if (rows == 0) {
            return null;
        }
        // 批量 UPDATE 后持久化上下文已被清空（clearAutomatically），这里读到的是 DB 最新值
        AiTask task = aiTaskRepository.findById(taskId).orElse(null);
        if (task != null) {
            // ⚠️ 在事务内提前初始化 LAZY 的 user：消费者全程在事务外处理，
            //    不初始化的话 post-commit 再访问 task.getUser() 会抛 LazyInitializationException
            org.hibernate.Hibernate.initialize(task.getUser());
        }
        return task;
    }

    /** 成功拿到真实 AI 结果 */
    @Transactional
    public void markSuccess(Long taskId, Long bizId, String resultJson) {
        update(taskId, task -> {
            task.setStatus(AiTaskStatus.SUCCESS);
            task.setBizId(bizId);
            task.setResultJson(resultJson);
            task.setErrorMessage(null);
            task.setFinishedAt(LocalDateTime.now());
        });
    }

    /** 没拿到 AI 结果，但写入了兜底数据（结果可用但不可信） */
    @Transactional
    public void markDegraded(Long taskId, Long bizId, String resultJson, String reason) {
        update(taskId, task -> {
            task.setStatus(AiTaskStatus.DEGRADED);
            task.setBizId(bizId);
            task.setResultJson(resultJson);
            task.setErrorMessage(reason);
            task.setFinishedAt(LocalDateTime.now());
        });
    }

    /**
     * 本次尝试失败，标记为 FAILED。
     * 注意 FAILED **不是**终态意义上的"结束"：MQ 重试与手动重试都能把它重新抢成 RUNNING。
     * errorMessage 记录本次失败原因，供前端展示与排查。
     */
    @Transactional
    public void markFailed(Long taskId, String reason) {
        update(taskId, task -> {
            task.setStatus(AiTaskStatus.FAILED);
            task.setErrorMessage(reason);
            task.setFinishedAt(LocalDateTime.now());
        });
    }

    /**
     * DLQ 收尾用的兜底标记失败：**只在任务还没有结果时**才改成 FAILED。
     * ----------------------------------------------------------------
     * 消费者正常路径在重试耗尽时已经把任务定格为 SUCCESS / DEGRADED（写了 mock 降级
     * 结果）或 FAILED（见 PhotoScoreConsumer/RetouchConsumer 的 settleAfterRetriesExhausted），
     * 所以消息到死信队列时任务通常已是终态。
     *
     * 这个兜底只补一种缝隙：消费者进程在标记终态**之前**崩溃、消息又恰好被驳回。
     * 此时任务可能还停在 RUNNING / PENDING，需要收尾成 FAILED。
     *
     * 为什么必须先查状态再动手：如果任务已经是 DEGRADED（降级结果已写入），
     * 再被这里无条件改成 FAILED，degrade-on-failure 保住"页面永远不空"的成果就
     * 白写了——前端会看到一个"纯失败"而不是"降级结果 + 重试按钮"。
     */
    @Transactional
    public void markFailedIfNoResult(Long taskId, String reason) {
        AiTask task = aiTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        if (task.getStatus() == AiTaskStatus.SUCCESS || task.getStatus() == AiTaskStatus.DEGRADED) {
            log.info("[ai-task] 任务已有结果，跳过 DLQ 兜底覆盖: taskId={}, status={}", taskId, task.getStatus());
            return;
        }
        markFailed(taskId, reason);
    }

    /**
     * 手动重试：重置为 PENDING 并清零尝试次数。
     *
     * @return true = 重置成功可以重新投递；false = 当前状态不允许重试（已在处理中或已完成）
     */
    @Transactional
    public boolean resetForRetry(Long taskId) {
        int rows = aiTaskRepository.resetForRetry(taskId, AiTaskStatus.PENDING, RETRYABLE);
        return rows > 0;
    }

    /**
     * 启动时恢复"孤儿 RUNNING"任务：上一次进程在 AI 调用中途死掉/被杀，任务停在
     * RUNNING，broker 重投的消息又被抢占闸门（只认 PENDING/FAILED）挡下——任务
     * 就此卡死。恢复方式：把这些任务重置回 PENDING 并清掉 attempts，让随后
     * 重新投递的消息能把它们抢起来续跑完（见 AiTaskStartupRecovery）。
     *
     * @return 被恢复（重置回 PENDING）的任务 id 列表，调用方需为它们重新投递消息
     */
    @Transactional
    public List<Long> recoverInterruptedRunning() {
        List<AiTask> stuck = aiTaskRepository.findByStatus(AiTaskStatus.RUNNING);
        if (stuck.isEmpty()) {
            return List.of();
        }
        List<Long> recovered = new java.util.ArrayList<>(stuck.size());
        for (AiTask task : stuck) {
            task.setStatus(AiTaskStatus.PENDING);
            task.setAttemptCount(0);
            task.setErrorMessage("消费者中断，启动时已重置回待处理");
            task.setStartedAt(null);
            task.setFinishedAt(null);
            aiTaskRepository.save(task);
            recovered.add(task.getId());
            log.warn("[ai-task] 启动恢复孤儿任务: taskId={}, bizType={}", task.getId(), task.getBizType());
        }
        return recovered;
    }

    private void update(Long taskId, Consumer<AiTask> mutator) {
        AiTask task = aiTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("[ai-task] 任务不存在，跳过状态更新: taskId={}", taskId);
            return;
        }
        mutator.accept(task);
        aiTaskRepository.save(task);
    }
}
