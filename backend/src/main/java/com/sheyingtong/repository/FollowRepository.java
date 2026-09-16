package com.sheyingtong.repository;

import com.sheyingtong.entity.Follow;
import com.sheyingtong.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    /**
     * 是否已关注。显式 JPQL（不用 FollowId 派生）：
     * Follow 只有 follower/followee 关联对象，没有 followerId 标量属性。
     * 走 uk_follow(follower_id, followee_id) 唯一索引。
     */
    @Query("""
            select case when count(f) > 0 then true else false end
            from Follow f where f.follower.id = :followerId and f.followee.id = :followeeId
            """)
    boolean existsByFollowerIdAndFolloweeId(@Param("followerId") Long followerId,
                                            @Param("followeeId") Long followeeId);

    @Query("select f from Follow f where f.follower.id = :followerId and f.followee.id = :followeeId")
    Optional<Follow> findByFollowerIdAndFolloweeId(@Param("followerId") Long followerId,
                                                   @Param("followeeId") Long followeeId);

    @Query("select count(f) from Follow f where f.follower.id = :followerId")
    long countByFollowerId(@Param("followerId") Long followerId);

    @Query("select count(f) from Follow f where f.followee.id = :followeeId")
    long countByFolloweeId(@Param("followeeId") Long followeeId);

    /**
     * 我关注的人（按关注时间倒序）。Follow.id 递增方向即关注先后。
     */
    @Query("select f.followee from Follow f where f.follower.id = :userId order by f.id desc")
    List<User> findFollowingUsers(@Param("userId") Long userId, Pageable pageable);

    /**
     * 关注我的人（粉丝列表，按关注时间倒序）。
     */
    @Query("select f.follower from Follow f where f.followee.id = :userId order by f.id desc")
    List<User> findFollowerUsers(@Param("userId") Long userId, Pageable pageable);
}
