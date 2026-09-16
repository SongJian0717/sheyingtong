package com.sheyingtong.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sheyingtong.mq.RabbitNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑与消费端策略声明
 * ----------------------------------------------------------------
 * 设计说明见 docs/RabbitMQ异步化设计方案.md
 *
 * 拓扑：
 * <pre>
 *   sheyingtong.ai (topic)
 *     ├─ ai.photo.score    ▶ sheyingtong.q.ai.photo-score
 *     └─ ai.retouch.suggest▶ sheyingtong.q.ai.retouch
 *
 *   sheyingtong.ai.dlx (topic) ▶ sheyingtong.q.ai.dlq
 * </pre>
 *
 * 队列与交换机都是 durable，且消息默认 persistent → broker 重启后未消费任务不丢。
 * 消费者拿到的消息体只有 taskId（见 AiTaskMessage），靠 DB 状态机做幂等。
 */
@Configuration
@Slf4j
public class RabbitConfig {

    // ==================== 交换机 ====================

    /** 业务交换机：topic 类型，便于以后用路由键前缀扩展新的 AI 任务类型 */
    @Bean
    public TopicExchange aiExchange() {
        return new TopicExchange(RabbitNames.AI_EXCHANGE, true, false);
    }

    /** 死信交换机 */
    @Bean
    public TopicExchange aiDeadLetterExchange() {
        return new TopicExchange(RabbitNames.AI_DLX, true, false);
    }

    // ==================== 业务队列 ====================

    /**
     * 照片打分队列。
     * 配 x-dead-letter-exchange：消息被 reject 且不重回队列时（消费端彻底放弃）自动进死信，
     * 避免失败消息无限重投把 CPU 和日志打满。
     */
    @Bean
    public Queue photoScoreQueue() {
        return QueueBuilder.durable(RabbitNames.PHOTO_SCORE_QUEUE)
                .deadLetterExchange(RabbitNames.AI_DLX)
                .deadLetterRoutingKey(RabbitNames.AI_DLQ_ROUTING_KEY)
                .build();
    }

    /** 修图建议队列（与打分队列同构，独立队列便于两者分别调并发） */
    @Bean
    public Queue retouchQueue() {
        return QueueBuilder.durable(RabbitNames.RETOUCH_QUEUE)
                .deadLetterExchange(RabbitNames.AI_DLX)
                .deadLetterRoutingKey(RabbitNames.AI_DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * 死信队列。
     * ⚠️ 刻意不给它配 dead-letter-exchange：否则死信队列里的消息再次失败会形成死信环。
     */
    @Bean
    public Queue aiDeadLetterQueue() {
        return QueueBuilder.durable(RabbitNames.AI_DLQ).build();
    }

    // ==================== 绑定 ====================

    @Bean
    public Binding photoScoreBinding(Queue photoScoreQueue, TopicExchange aiExchange) {
        return BindingBuilder.bind(photoScoreQueue).to(aiExchange).with(RabbitNames.PHOTO_SCORE_ROUTING_KEY);
    }

    @Bean
    public Binding retouchBinding(Queue retouchQueue, TopicExchange aiExchange) {
        return BindingBuilder.bind(retouchQueue).to(aiExchange).with(RabbitNames.RETOUCH_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding(Queue aiDeadLetterQueue, TopicExchange aiDeadLetterExchange) {
        return BindingBuilder.bind(aiDeadLetterQueue).to(aiDeadLetterExchange).with(RabbitNames.AI_DLQ_ROUTING_KEY);
    }

    // ==================== 序列化 ====================

    /**
     * 消息体 JSON 序列化器。
     * ----------------------------------------------------------------
     * ⚠️ 两个刻意的选择：
     *
     * 1. 用**独立的 ObjectMapper**，不复用容器里那个（对齐 CacheService 的做法）。
     *    本项目接口层的 ObjectMapper 把 LocalDateTime 序列化成数组，MQ 消息不该被
     *    这种"面向 HTTP 的历史行为"绑住。
     *
     * 2. setTypePrecedence(INFERRED)：**完全忽略消息头里的 __TypeId__**，
     *    只按 @RabbitListener 方法参数声明类型反序列化。默认行为（TYPE_ID 优先）
     *    允许发送方通过消息头指定要实例化的类，是一个反序列化攻击面；
     *    改成 INFERRED 后这个面从根上消失了。
     */
    @Bean
    public MessageConverter aiMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(mapper);
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    // ==================== 管理端 ====================

    /**
     * RabbitTemplate（发布侧）。
     * ----------------------------------------------------------------
     * 手动定义并注册 confirm / return 回调，把"消息到底进没进队列"变成可观测的日志。
     *  ⚠️ 只做观测，**不做落库补偿**（见设计方案"明确不做"）：
     *     任务事实源在 DB，一条 confirm 失败不代表任务没机会执行——
     *     投递失败路径已经由 AiTaskService 的 fallback 兜底，这里多落一条补偿记录
     *     只会和既有降级逻辑抢"谁说了算"。
     *
     * confirm（publisher-confirm-type=correlated 已在连接工厂上生效）：
     *   ack=false 表示消息没被 broker 接受（队列/交换机不存在、权限不足等）。
     * return（mandatory=true）：
     *   交换机接受了但按路由键找不到任何队列时，消息被退回这里。
     */
    @Bean
    public RabbitTemplate aiRabbitTemplate(ConnectionFactory connectionFactory, MessageConverter aiMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(aiMessageConverter);
        template.setMandatory(true);

        template.setConfirmCallback((c, ack, cause) -> {
            if (!ack) {
                String correlationId = c != null && c.getId() != null ? c.getId() : "unknown";
                log.error("[mq] 消息未被 broker 确认（可能未进队列）: correlationId={}, cause={}",
                        correlationId, cause == null ? "无原因" : cause);
            } else {
                log.debug("[mq] 消息已确认: {}", c != null ? c.getId() : "unknown");
            }
        });

        template.setReturnsCallback(returned -> log.error(
                "[mq] 消息不可路由已被退回: exchange={}, routingKey={}, replyText={}, body类={}",
                returned.getExchange(), returned.getRoutingKey(),
                returned.getReplyText(),
                returned.getMessage() != null && returned.getMessage().getBody() != null
                        ? new String(returned.getMessage().getBody()).length() : 0));
        return template;
    }

    /**
     * RabbitAdmin 负责在连接建立后把上面声明的 exchange/queue/binding 推到 broker。
     * ⚠️ ignoreDeclarationExceptions(true)：broker 没起或权限不足时只记日志，
     *    不让应用启动失败 —— 这是"broker 挂了不影响启动"的前提。
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setIgnoreDeclarationExceptions(true);
        return admin;
    }

    // ==================== 消费端容器工厂 ====================

    /**
     * 默认容器工厂：带"指数退避重试 + 重试耗尽转发死信"的拦截器。
     * ----------------------------------------------------------------
     * 用 SimpleRabbitListenerContainerFactoryConfigurer 把 application.yml 里
     * spring.rabbitmq.listener.simple.* 的配置（prefetch / concurrency / 手动确认等）
     * 全部应用上，避免自定义工厂后 yml 里的配置静默失效。
     *
     * 为什么用 stateless 重试（而不是 stateful）：
     *   stateless 的重试在同一个投递内就地完成，每次重试都会重新走一遍监听器方法，
     *   也就重新走一次 DB 的抢占闸门。配合"FAILED 可被重新抢占"的设计，
     *   每次重试都能重新把任务从 FAILED 抢成 RUNNING —— 语义正好对上，
     *   而且不需要 broker 把消息 requeue 回来（stateful 需要）。
     *
     * 重试次数在这里统一控制（app.ai.async.max-attempts），
     * 因此 AIService 内部不再自己重试，避免 3×3=9 次 × 180s 的乘数爆炸。
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter aiMessageConverter,
            RabbitTemplate rabbitTemplate,
            AiAsyncProperties props) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(aiMessageConverter);

        long maxBackoff = props.getRetryMaxBackoff().toMillis();
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(props.getMaxAttempts())
                .backOffOptions(props.getRetryBackoff().toMillis(),
                        props.getRetryBackoffMultiplier(),
                        maxBackoff)
                // 重试全部耗尽 → 把原消息（含异常堆栈头）转发到死信交换机，由 AiDeadLetterConsumer 收尾
                .recoverer(new RepublishMessageRecoverer(rabbitTemplate, RabbitNames.AI_DLX, RabbitNames.AI_DLQ_ROUTING_KEY))
                .build());

        log.info("[mq] 消费端容器工厂已装配：maxAttempts={}, 退避 {}ms ×{}, 上限 {}ms",
                props.getMaxAttempts(), props.getRetryBackoff().toMillis(),
                props.getRetryBackoffMultiplier(), maxBackoff);
        return factory;
    }

    /**
     * 死信队列专用容器工厂：⚠️ 不带重试拦截器。
     * 否则死信消费者一旦抛异常，又会被重试并再次转发进死信，形成死信环。
     */
    @Bean
    public SimpleRabbitListenerContainerFactory aiDlqListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter aiMessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(aiMessageConverter);
        return factory;
    }
}
