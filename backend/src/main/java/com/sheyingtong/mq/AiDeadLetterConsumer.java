package com.sheyingtong.mq;

import com.sheyingtong.service.AiTaskStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 死信队列收尾消费者
 * ----------------------------------------------------------------
 * 监听 sheyingtong.q.ai.dlq，用专用无重试容器工厂 aiDlqListenerContainerFactory——
 * 这里再被驳回也会死在 DLQ 里，不会形成死信环。
 *
 * 能到 DLQ 的消息 = 业务队列重试耗尽仍失败的 AI 任务。正常情况下消费者已经把它
 * 标记成终态（FAILED/DEGRADED），这里的职责是：
 *  1. ERROR 日志（带上 RepublishMessageRecoverer 写入消息头的最后异常栈）→ 运维审计
 *  2. 兜底再调一次 markFailed：万一消费者进程在标记终态前崩溃、消息又恰好被驳回，
 *     这里把任务落成 FAILED，避免 PENDING/RUNNING 卡死
 *
 * 为什么不会误伤：任务终态后 claim 幂等闸门（status IN PENDING/FAILED 且
 * attempt_count < max）过不了，重投也不可能被业务消费者抢走。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiDeadLetterConsumer {

    private static final int MAX_LOG_LEN = 2000;

    private final AiTaskStateService aiTaskStateService;

    @RabbitListener(queues = RabbitNames.AI_DLQ, containerFactory = "aiDlqListenerContainerFactory")
    public void onDeadLetter(@Payload AiTaskMessage message, Message amqpMessage) {
        Long taskId = message != null ? message.taskId() : null;
        String reason = extractError(amqpMessage);

        log.error("[mq] 死信消息收到（AI 任务重试耗尽）: taskId={}, 原因摘要: {}", taskId, reason);

        if (taskId != null) {
            // ⚠️ 用"已有结果则不动"的版本：消费者重试耗尽时已把任务定格成
            // DEGRADED（降级结果已写入），这里不能把它覆盖成 FAILED 抹掉结果
            try {
                aiTaskStateService.markFailedIfNoResult(taskId, "超过最大重试次数: " + summarize(reason));
            } catch (Exception e) {
                log.error("[mq] 死信收尾标记 FAILED 失败: taskId={}", taskId, e);
            }
        }
    }

    /** RepublishMessageRecoverer 会把"最后一次异常堆栈"写进 x-exception-stacktrace 头 */
    private String extractError(Message message) {
        if (message == null) {
            return "未知（消息为空）";
        }
        Object stacktrace = message.getMessageProperties().getHeader("x-exception-stacktrace");
        if (stacktrace != null && !stacktrace.toString().isBlank()) {
            String s = stacktrace.toString();
            return s.length() > MAX_LOG_LEN ? s.substring(0, MAX_LOG_LEN) : s;
        }
        Object reason = message.getMessageProperties().getHeader("x-exception-message");
        return reason != null ? reason.toString() : "无异常头（消息可能在转发前已被处理）";
    }

    private String summarize(String s) {
        return s != null && s.length() > 300 ? s.substring(0, 300) : s;
    }
}