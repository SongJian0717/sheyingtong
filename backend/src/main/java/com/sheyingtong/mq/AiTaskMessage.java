package com.sheyingtong.mq;

import java.io.Serializable;

/**
 * AI 异步任务消息体
 * ----------------------------------------------------------------
 * ⚠️ 刻意只放 taskId 与 bizType，不放图片字节、不放 Base64、不放业务参数。
 *
 * 理由（这是本方案的核心约定）：
 *  1. 消息小 → 不占 broker 内存与磁盘，MB 级图片塞进消息是反模式；
 *  2. 消息不过期 → 重投时读到的永远是最新状态（DB 是唯一事实源），不会出现
 *     "消息里带着 3 分钟前快照" 的脏数据问题；
 *  3. 天然幂等 → 消费者只凭 taskId 去 DB 抢占，重复投递不会产生第二条结果。
 *
 * 消费者拿 taskId 回 DB 取 filePath / description / user 等上下文。
 *
 * @param taskId  ai_task 表主键
 * @param bizType 业务类型，取值见 {@link AiBizType}，仅用于日志与断言
 */
public record AiTaskMessage(Long taskId, String bizType) implements Serializable {
}
