package com.sheyingtong.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 作品评论表（支持一层楼中楼）
 * ----------------------------------------------------------------
 * parent_id 指向同表另一条评论（回复谁）。删除父评论时会连带删除其子回复，
 * 并一次性扣减 works.comments_count，避免计数漂移。
 *
 * 索引：
 *  - idx_comment_work(work_id, id)：作品评论列表（按主键 id 倒序即发布时间倒序），
 *    复合索引让"按作品过滤 + 排序"一次索引扫描即可
 *  - idx_comment_user(user_id)：用户发过的评论 / 账号清理
 */
@Entity
@Table(name = "work_comment",
        indexes = {
                @Index(name = "idx_comment_work", columnList = "work_id,id"),
                @Index(name = "idx_comment_user", columnList = "user_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id", nullable = false)
    private Work work;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 500)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
