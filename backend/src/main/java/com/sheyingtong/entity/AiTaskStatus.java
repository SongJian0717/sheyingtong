package com.sheyingtong.entity;

/**
 * AI 任务状态机
 * ----------------------------------------------------------------
 * <pre>
 *   PENDING ──抢占──▶ RUNNING ──成功──▶ SUCCESS      （终态，不可再被抢占）
 *                        │
 *                        ├──AI 失败但写了兜底数据──▶ DEGRADED  （终态，可手动重试）
 *                        └──彻底失败（重试耗尽）────▶ FAILED    （可被 MQ 重投或手动重试重新抢占）
 * </pre>
 *
 * 关键设计：只有 PENDING 与 FAILED 能被"抢占"（见 AiTaskRepository.claimForRunning）。
 *  - SUCCESS / DEGRADED 是终态 → 重复投递、并发消费都被挡在闸门外，这就是幂等的实现方式；
 *  - FAILED 允许重新抢占 → MQ 层重试与前端"重试"按钮**共用同一个机制**，不需要第二套代码。
 *
 * 为什么要有 DEGRADED（而不是直接 FAILED）：
 *  改造前 AI 失败会静默返回 mock 的随机 60~95 分（PhotoService.generateMockAIResult），
 *  用户看到的是一个像真结果的东西。DEGRADED 保留"页面永远不空"的产品保证，
 *  但把降级这件事显式化 —— 前端可以据此标注"本次为降级结果"并提供重试。
 */
public enum AiTaskStatus {

    /** 已提交，等待消费者处理 */
    PENDING,

    /** 已被某个消费者抢占，正在调用 AI */
    RUNNING,

    /** 成功拿到真实 AI 结果 */
    SUCCESS,

    /** 未拿到 AI 结果，已写入兜底（mock）数据，结果可用但不可信 */
    DEGRADED,

    /** 彻底失败，无结果；可被重投或手动重试 */
    FAILED
}
