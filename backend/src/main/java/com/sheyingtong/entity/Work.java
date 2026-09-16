package com.sheyingtong.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 摄影作品（社区模块核心表）
 * ----------------------------------------------------------------
 * 发布源 = AI 打分记录(photo_analysis)：作品本身不复制图片文件，
 * 图片走现有公开接口 /api/photo/image/{photoAnalysisId}，
 * AI 六维评语走 /api/photo/detail/{photoAnalysisId}。
 *
 * 计数一致性：likes_count / comments_count 为冗余计数列，
 * 点赞/评论/取消都在同一事务内 增删明细 + 同步 ±1，读侧不再 COUNT。
 *
 * 索引设计（面试可讲）：
 *  - uk_works_analysis(photo_analysis_id)：一篇打分只能发布一篇作品，并发下防重复发布
 *  - idx_works_user(user_id)：用户主页/我的作品列表，二级索引叶子含主键，单用户天然按 id 有序
 *  - idx_works_latest(is_public,id)：广场"最新"游标页 WHERE is_public=1 AND id<:cursor
 *                                     ORDER BY id DESC，复合索引直接按序扫描，无 filesort
 *  - idx_works_rank(is_public,likes_count,id)：热门榜 ORDER BY likes_count DESC, id DESC
 *                                               定位 is_public=1 后按键序取 Top-N
 */
@Entity
@Table(name = "works",
        uniqueConstraints = @UniqueConstraint(name = "uk_works_analysis", columnNames = "photo_analysis_id"),
        indexes = {
                @Index(name = "idx_works_user", columnList = "user_id"),
                @Index(name = "idx_works_latest", columnList = "is_public,id"),
                @Index(name = "idx_works_rank", columnList = "is_public,likes_count,id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "photo_analysis_id", nullable = false)
    private Long photoAnalysisId;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name = "is_public", nullable = false, columnDefinition = "TINYINT(1) NOT NULL DEFAULT 1")
    private Boolean isPublic = true;

    @Builder.Default
    @Column(name = "likes_count", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    private Integer likesCount = 0;

    @Builder.Default
    @Column(name = "comments_count", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    private Integer commentsCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
