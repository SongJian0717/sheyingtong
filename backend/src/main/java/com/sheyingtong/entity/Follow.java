package com.sheyingtong.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 关注关系表
 * ----------------------------------------------------------------
 * follower_id = 主动关注的人，followee_id = 被关注的人。
 *
 * 索引设计（面试可讲）：
 *  - uk_follow(follower_id, followee_id)：唯一索引兜底防重复关注（并发最后一层防线），
 *    同时其最左前缀 (follower_id) 可支撑"我关注了谁"与 Feed 子查询
 *  - idx_follow_followee(followee_id)：粉丝列表/粉丝数（倒查"谁关注了我"）
 */
@Entity
@Table(name = "follow",
        uniqueConstraints = @UniqueConstraint(name = "uk_follow", columnNames = {"follower_id", "followee_id"}),
        indexes = @Index(name = "idx_follow_followee", columnList = "followee_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followee_id", nullable = false)
    private User followee;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
