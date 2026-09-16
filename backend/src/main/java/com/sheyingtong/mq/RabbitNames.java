package com.sheyingtong.mq;

/**
 * RabbitMQ 命名常量（交换机 / 队列 / 路由键）
 * ----------------------------------------------------------------
 * 集中一处，避免字符串散落在生产者和消费者里各写一份而写错。
 * 命名沿用本项目 Redis 侧 "sheyingtong:" 的统一命名空间习惯。
 *
 * 拓扑形状：
 * <pre>
 *   sheyingtong.ai (topic)  ──ai.photo.score──▶  sheyingtong.q.ai.photo-score
 *                           ──ai.retouch.suggest▶ sheyingtong.q.ai.retouch
 *         │ 两个业务队列都配了 x-dead-letter-exchange
 *         ▼
 *   sheyingtong.ai.dlx (topic) ──ai.dead──▶ sheyingtong.q.ai.dlq
 * </pre>
 *
 * 设计意图：一个交换机 + 每类任务一条独立队列（而不是一类任务一个交换机），
 * 这样以后加"RAG 向量化"只需加一条队列和一个路由键，不用动已有的队列与消费者。
 */
public final class RabbitNames {

    private RabbitNames() {
    }

    /** 业务交换机（topic 类型，便于用路由键前缀做分类扩展） */
    public static final String AI_EXCHANGE = "sheyingtong.ai";

    /** 死信交换机 */
    public static final String AI_DLX = "sheyingtong.ai.dlx";

    /** 照片打分任务队列 */
    public static final String PHOTO_SCORE_QUEUE = "sheyingtong.q.ai.photo-score";
    public static final String PHOTO_SCORE_ROUTING_KEY = "ai.photo.score";

    /** 修图建议任务队列 */
    public static final String RETOUCH_QUEUE = "sheyingtong.q.ai.retouch";
    public static final String RETOUCH_ROUTING_KEY = "ai.retouch.suggest";

    /** 死信队列：重试耗尽的消息落到这里，由 AiDeadLetterConsumer 记录并标记任务失败 */
    public static final String AI_DLQ = "sheyingtong.q.ai.dlq";
    public static final String AI_DLQ_ROUTING_KEY = "ai.dead";
}
