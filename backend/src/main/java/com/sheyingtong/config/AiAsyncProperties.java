package com.sheyingtong.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 异步任务配置（对应 application.yml 的 app.ai.async）
 * ----------------------------------------------------------------
 * 设计说明见 docs/RabbitMQ异步化设计方案.md
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.ai.async")
public class AiAsyncProperties {

    /**
     * 总开关。
     * false = 不投消息，请求线程同步执行到底（即本次改造之前的行为）。
     * 用途：① 做"开/关异步"的对比压测；② 出问题时的应急回退开关。
     */
    private boolean enabled = true;

    /**
     * broker 不可达（AmqpConnectException 等）时是否就地同步执行。
     * true  = 慢但可用（请求会挂到 AI 返回，但功能正常）
     * false = 返回 503，让前端明确知道"现在提交不了"
     * 与 Redis 侧"连不上就跳过缓存直查 DB"的降级哲学保持一致。
     */
    private boolean fallbackToSync = true;

    /** 单条消息最多被消费几次（条件更新闸门用 attempt_count 卡这个上限） */
    private int maxAttempts = 3;

    /**
     * 最后一次尝试仍失败时怎么办。
     * true  = 写入兜底 mock 结果并把任务标记为 DEGRADED（保留"页面永远不空"的产品保证）
     * false = 直接把任务标记为 FAILED（无结果，前端展示失败 + 重试）
     * 两种情况下失败消息都会进死信队列，作为"用尽了全部重试"的审计记录。
     */
    private boolean degradeOnFailure = true;

    /** 重试指数退避的初始间隔 */
    private Duration retryBackoff = Duration.ofSeconds(5);

    /** 退避倍数（5s → 10s → 20s…） */
    private double retryBackoffMultiplier = 2.0;

    /** 退避间隔上限，防止倍数放大后重试间隔过长 */
    private Duration retryMaxBackoff = Duration.ofSeconds(60);

    /**
     * 启动时是否恢复"孤儿 RUNNING 任务"：上一次进程在 AI 调用中途崩溃/被杀，
     * 任务停在 RUNNING 而消息已被重投（只认 PENDING/FAILED 的抢占闸门挡不住它）。
     * true  = 启动完成后把这类任务重置回 PENDING 并重新投递，让消费者续跑完（默认）
     * false = 不做恢复，RUNNING 任务只能靠人工干预
     */
    private boolean recoverInterruptedOnStartup = true;
}
