package com.sheyingtong.mq;

import com.sheyingtong.entity.AiTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * AI 任务消息生产者
 * ----------------------------------------------------------------
 * 只做一件事：把 taskId 投到业务交换机，由路由键决定进哪条队列。
 *
 * ⚠️ 这里**不吞异常**：broker 不可达时把 AmqpException 抛给调用方
 * （AiTaskService），由它决定"就地同步执行"降级还是直接报错。
 * 生产者的职责边界是"投递"，降级策略属于业务决策，不放在这一层。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiTaskProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 投递任务。
     * correlationData 带 taskId，配合 publisher-confirm-type=correlated，
     * RabbitTemplate 的 confirm 回调能对到具体是哪条任务没进队列（见 RabbitConfig）。
     *
     * @throws org.springframework.amqp.AmqpException broker 不可达 / 交换机不存在等
     */
    public void publish(AiTask task) {
        AiTaskMessage message = new AiTaskMessage(task.getId(), task.getBizType().name());
        rabbitTemplate.convertAndSend(
                RabbitNames.AI_EXCHANGE,
                task.getBizType().routingKey(),
                message,
                new CorrelationData("ai-task-" + task.getId()));
        log.info("[mq] 任务已投递: taskId={}, bizType={}, routingKey={}",
                task.getId(), task.getBizType(), task.getBizType().routingKey());
    }
}
