package com.sheyingtong.repository;

import com.sheyingtong.entity.WorkLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkLikeRepository extends JpaRepository<WorkLike, Long> {

    /** 判重：同用户同作品只能赞一次（写路径先查，uk_like 唯一索引兜底并发） */
    @Query("""
            select case when count(l) > 0 then true else false end
            from WorkLike l where l.work.id = :workId and l.user.id = :userId
            """)
    boolean existsByWorkIdAndUserId(@Param("workId") Long workId, @Param("userId") Long userId);

    @Query("select l from WorkLike l where l.work.id = :workId and l.user.id = :userId")
    Optional<WorkLike> findByWorkIdAndUserId(@Param("workId") Long workId, @Param("userId") Long userId);

    /** 删除作品时级联清空其点赞（调用方处于 @Transactional 事务内） */
    @Modifying
    @Query("delete from WorkLike l where l.work.id = :workId")
    void deleteByWorkId(@Param("workId") Long workId);
}
