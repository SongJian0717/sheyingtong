package com.sheyingtong.service;

import com.sheyingtong.entity.AiBizType;

/**
 * AI 任务执行器：一类业务对应一个实现
 * ----------------------------------------------------------------
 * 实现者同时是 MQ 消费者（{@code @RabbitListener}）与就地执行入口：
 *  - 正常路径：消息到达 → 容器调用 {@code execute(taskId)}
 *  - 降级路径：broker 不可达或 app.ai.async.enabled=false 时，
 *    提交请求的线程直接调用同一个 {@code execute(taskId)}
 *
 * 两条路径共用同一份处理逻辑（含抢占闸门），所以"异步"和"降级同步"
 * 不会出现行为差异 —— 只有"在哪个线程跑"不同。
 *
 * 新增一类 AI 任务（例如以后的 RAG 向量化）只需：
 *  1. AiBizType 加一个枚举值 + 路由键；
 *  2. RabbitConfig 加一条 Queue + Binding；
 *  3. 写一个本接口的实现。
 *  不需要动 AiTaskService / 状态机 / 控制器。
 */
public interface AiTaskExecutor {

    /** 本执行器负责的业务类型 */
    AiBizType bizType();

    /**
     * 执行任务：内部自己完成"抢占 → 调 AI → 写结果 → 落终态"。
     * 抢占失败（已被处理/次数用尽）时应静默返回，不要抛异常。
     */
    void execute(Long taskId);
}
