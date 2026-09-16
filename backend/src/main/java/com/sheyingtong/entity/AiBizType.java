package com.sheyingtong.entity;

import com.sheyingtong.mq.RabbitNames;

/**
 * AI 任务业务类型
 * ----------------------------------------------------------------
 * 决定这条任务最终把结果写进哪张业务表（photo_analysis / retouch_records），
 * 以及投递到哪条队列。新增一类 AI 任务时在这里加一个枚举值 + 一条路由即可。
 */
public enum AiBizType {

    /** 照片 6 维度打分 → 结果写 photo_analysis */
    PHOTO_SCORE(RabbitNames.PHOTO_SCORE_ROUTING_KEY),

    /** 修图建议（8 大模块） → 结果写 retouch_records */
    RETOUCH(RabbitNames.RETOUCH_ROUTING_KEY);

    private final String routingKey;

    AiBizType(String routingKey) {
        this.routingKey = routingKey;
    }

    /** 投递到交换机时使用的路由键 */
    public String routingKey() {
        return routingKey;
    }

    public static AiBizType of(String name) {
        return AiBizType.valueOf(name);
    }
}
