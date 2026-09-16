package com.sheyingtong.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * AI 异步任务表（照片打分 / 修图建议 / 以后的 RAG 向量化共用）
 * ----------------------------------------------------------------
 * 为什么单开一张表，而不是给 photo_analysis 加 status 列：
 *  1. 任务生命周期（排队/重试/失败原因）与业务结果（评分/修图参数）是两件事；
 *  2. photo_analysis.score 是 NOT NULL，提交时还没结果，硬塞进去就得开 nullable
 *     （而 ddl-auto: update 不会帮你把已有列改成可空，还得手写 DDL）；
 *  3. 一张表能同时服务以后新增的 AI 任务类型，且天然承载 attempt_count /
 *     error_message / 各阶段时间戳，可观测性直接就有了。
 *
 * 与消息的关系：MQ 消息体里只有 taskId（见 mq/AiTaskMessage），本表是唯一事实源。
 *
 * 状态流转见 {@link AiTaskStatus}；抢占闸门见 AiTaskRepository.claimForRunning。
 */
@Entity
@Table(name = "ai_task", indexes = {
        // 查"我的任务"（按用户 + 时间倒序）
        @Index(name = "idx_ai_task_user_created", columnList = "user_id, created_at"),
        // 排查"卡在 RUNNING 的任务"
        @Index(name = "idx_ai_task_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** 业务类型：决定投递路由键与结果写哪张业务表 */
    @Enumerated(EnumType.STRING)
    @Column(name = "biz_type", nullable = false, length = 32)
    private AiBizType bizType;

    /** 当前状态（PENDING/RUNNING/SUCCESS/DEGRADED/FAILED） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AiTaskStatus status;

    /** 待分析图片在磁盘上的路径（提交阶段就已落盘，消费者据此读图） */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_name", length = 200)
    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 结果业务行 id（photo_analysis.id / retouch_records.id），终态后回填 */
    @Column(name = "biz_id")
    private Long bizId;

    /**
     * 结果负载 JSON，原样返回给前端。
     * 之所以把结果也存一份：修图建议的完整响应（8 大模块）并没有全部落到
     * retouch_records 表（只存了 objective 与 steps），光靠业务表重建不出来；
     * 存 JSON 后轮询端点对两种业务类型都能返回与原来同步接口完全一致的结构，
     * 前端渲染代码不用改。
     */
    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    /** 已被消费的次数，抢占时 +1；也是 MQ 重试与手动重试的次数闸门 */
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    /** 最后一次失败原因，成功后被清空 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 首次被消费者抢占的时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 进入终态的时间 */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    void applyDefaults() {
        if (status == null) {
            status = AiTaskStatus.PENDING;
        }
        if (attemptCount == null) {
            attemptCount = 0;
        }
    }

    /** 是否已进入终态（终态不可再被抢占） */
    public boolean isTerminal() {
        return status == AiTaskStatus.SUCCESS
                || status == AiTaskStatus.DEGRADED
                || status == AiTaskStatus.FAILED;
    }
}
