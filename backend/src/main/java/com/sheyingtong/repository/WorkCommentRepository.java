package com.sheyingtong.repository;

import com.sheyingtong.entity.WorkComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkCommentRepository extends JpaRepository<WorkComment, Long> {

    /**
     * 评论列表首页（按 id 倒序 = 最新在前）。
     * 显式 JPQL（不用 findByWorkId 派生）：实体只有 work/user 关联对象、没有 workId 标量属性，
     * 派生方法解析属性名会失败；同时可精确写 c.id 排序走 (work_id,id) 复合索引。
     */
    @Query("select c from WorkComment c where c.work.id = :workId order by c.id desc")
    List<WorkComment> findByWorkIdOrderByIdDesc(@Param("workId") Long workId, Pageable pageable);

    @Query("""
            select c from WorkComment c
            where c.work.id = :workId and c.id < :cursor
            order by c.id desc
            """)
    List<WorkComment> findByWorkIdAndIdLessThanOrderByIdDesc(@Param("workId") Long workId,
                                                             @Param("cursor") Long cursor,
                                                             Pageable pageable);

    @Query("select count(c) from WorkComment c where c.work.id = :workId")
    long countByWorkId(@Param("workId") Long workId);

    /** 删除作品时级联清空其评论（调用方处于 @Transactional 事务内） */
    @Modifying
    @Query("delete from WorkComment c where c.work.id = :workId")
    void deleteByWorkId(@Param("workId") Long workId);

    /** 删除顶楼评论时连带删除其子回复 */
    List<WorkComment> findByParentId(Long parentId);
}
