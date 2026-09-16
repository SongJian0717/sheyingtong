package com.sheyingtong.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 点赞明细表
 * ----------------------------------------------------------------
 * 开关用 INSERT/DELETE 物理增删（不用 0/1 状态列）：
 *   - 每次点赞可追溯、唯一索引 uk_like 在并发下防同一人重复赞
 *   - 计数不靠这张表，靠 works.likes_count 冗余列（事务内 ±1）
 *
 * 索引：
 *  - uk_like(user_id, work_id)：唯一索引防重复赞 + "我是否赞过"查询
 *  - idx_like_work(work_id)：作品被谁赞过 / 删除作品时批量清理
 */
@Entity
@Table(name = "work_like",
        uniqueConstraints = @UniqueConstraint(name = "uk_like", columnNames = {"user_id", "work_id"}),
        indexes = @Index(name = "idx_like_work", columnList = "work_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id", nullable = false)
    private Work work;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
