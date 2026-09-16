package com.sheyingtong.repository;

import com.sheyingtong.entity.AiTask;
import com.sheyingtong.entity.AiTaskStatus;
import com.sheyingtong.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AiTaskRepository extends JpaRepository<AiTask, Long> {

    List<AiTask> findTop20ByUserOrderByIdDesc(User user);

    /**
     * 启动恢复用：找出上一次进程崩溃时遗留的 RUNNING 任务
     * （消费者正在调 AI 但进程被杀，任务停在 RUNNING、消息已被重投）。
     */
    List<AiTask> findByStatus(AiTaskStatus status);

    /**
     * 抢占闸门 —— 整个异步方案幂等性的唯一实现点
     * ----------------------------------------------------------------
     * 一条带条件的 UPDATE 同时解决三件事：
     *
     * 1. **幂等**：只有 PENDING / FAILED 能被抢。已 SUCCESS/DEGRADED 的任务抢不动
     *    → 消息重复投递、broker 重投、并发消费都只会有一条走到最后。
     * 2. **并发互斥**：两个消费者同时拿到同一条消息时，数据库行锁保证只有一个
     *    更新成功（返回 1），另一个返回 0 直接跳过。不需要分布式锁。
     * 3. **重试次数闸门**：attemptCount &lt; maxAttempts 才允许抢，
     *    与 MQ 层的 RetryInterceptor 上限同源，避免重试乘数爆炸。
     *
     * ⚠️ 调用方必须在一个独立的、立即提交的事务里调用它（见 AiTaskStateService.claim），
     * 否则"抢占"会随外层事务一起回滚，互斥就失效了。
     *
     * clearAutomatically=true：批量 UPDATE 绕过持久化上下文，清掉它以便随后
     * findById 读到的是数据库里的最新状态（attemptCount 已 +1）。
     *
     * @return 影响行数；1 = 抢占成功，0 = 已被处理或次数用尽
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AiTask t
               SET t.status = :running,
                   t.attemptCount = t.attemptCount + 1,
                   t.startedAt = :now,
                   t.errorMessage = null
             WHERE t.id = :id
               AND t.status IN :claimable
               AND t.attemptCount < :maxAttempts
            """)
    int claimForRunning(@Param("id") Long id,
                        @Param("running") AiTaskStatus running,
                        @Param("claimable") Collection<AiTaskStatus> claimable,
                        @Param("maxAttempts") int maxAttempts,
                        @Param("now") LocalDateTime now);

    /**
     * 手动重试：把已失败/已降级的任务重置为 PENDING 并**清零尝试次数**。
     * ----------------------------------------------------------------
     * 必须清零，否则前一次已经耗尽 maxAttempts 的任务重新投递后会被抢占闸门
     * 直接挡掉（attemptCount &gt;= maxAttempts），重试按钮就成了摆设。
     * 语义上也说得通：手动重试是"新的一组尝试"。
     *
     * 同样是带条件的 UPDATE，所以并发点两次重试也只会有一个成功。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AiTask t
               SET t.status = :pending,
                   t.attemptCount = 0,
                   t.errorMessage = null,
                   t.startedAt = null,
                   t.finishedAt = null
             WHERE t.id = :id
               AND t.status IN :retryable
            """)
    int resetForRetry(@Param("id") Long id,
                      @Param("pending") AiTaskStatus pending,
                      @Param("retryable") Collection<AiTaskStatus> retryable);
}
