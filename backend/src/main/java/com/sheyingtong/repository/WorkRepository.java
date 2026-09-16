package com.sheyingtong.repository;

import com.sheyingtong.entity.Work;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 作品 Repository
 * ----------------------------------------------------------------
 * 分页统一用自增主键 id 做游标（id 递增方向 ≈ 发布时间，且稳定无并列）：
 *   id < :cursor 倒序取一页 —— 避免深分页 OFFSET 越来越慢（面试可讲的优化点）
 */
@Repository
public interface WorkRepository extends JpaRepository<Work, Long> {

    // ---- 唯一性 / 归属 ----
    boolean existsByPhotoAnalysisId(Long photoAnalysisId);

    Optional<Work> findByPhotoAnalysisId(Long photoAnalysisId);

    // ---- 作品列表（按 id 倒序 = 最新在前，走主键/二级索引免 filesort）----
    @Query("select w from Work w where w.user.id = :userId order by w.id desc")
    List<Work> findUserWorks(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select w from Work w
            where w.user.id = :userId and w.isPublic = true
            order by w.id desc
            """)
    List<Work> findPublicUserWorks(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select w from Work w
            where w.user.id = :userId and w.isPublic = true and w.id < :cursor
            order by w.id desc
            """)
    List<Work> findPublicUserWorksBefore(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    @Query("select w from Work w where w.isPublic = true order by w.id desc")
    List<Work> findLatestPublic(Pageable pageable);

    @Query("""
            select w from Work w
            where w.isPublic = true and w.id < :cursor
            order by w.id desc
            """)
    List<Work> findLatestPublicBefore(@Param("cursor") Long cursor, Pageable pageable);

    // ---- 热门榜：likes_count 降序，同分按新作品优先（Top-N，走 idx_works_rank）----
    @Query("select w from Work w where w.isPublic = true order by w.likesCount desc, w.id desc")
    List<Work> findHotPublic(Pageable pageable);

    // ---- 统计 ----
    @Query("select count(w) from Work w where w.user.id = :userId")
    long countUserWorks(@Param("userId") Long userId);

    @Query("select count(w) from Work w where w.user.id = :userId and w.isPublic = true")
    long countPublicWorks(@Param("userId") Long userId);

    // ---- 关注动态 Feed：我关注的人发布的公开作品 ----
    @Query("""
            select w from Work w
            where w.isPublic = true
              and w.user.id in (select f.followee.id from Follow f where f.follower.id = :userId)
              and (:cursor is null or w.id < :cursor)
            order by w.id desc
            """)
    List<Work> findFollowFeed(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);
}
