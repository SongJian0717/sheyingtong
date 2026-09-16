package com.sheyingtong.service;

import com.sheyingtong.dto.request.CommentRequest;
import com.sheyingtong.dto.request.PublishWorkRequest;
import com.sheyingtong.dto.response.*;
import com.sheyingtong.entity.*;
import com.sheyingtong.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 摄影社区核心服务：作品发布/广场/详情、点赞、评论、关注、粉丝、关注动态 Feed
 * ----------------------------------------------------------------
 * 面试可讲的工程点（详见 docs/社区模块-MySQL设计要点.md）：
 *  1. 冗余计数一致性：点赞/评论明细与 works.likes_count/comments_count 在同一事务内
 *     增删明细 + ±1，避免"明细和数字对不上"，读侧零 COUNT
 *  2. 分页全部用自增主键 id 游标（id < cursor 倒序取一页），规避深分页 OFFSET 慢查询；
 *     热门榜是 Top-N 直查（规模小，无需游标）
 *  3. 唯一索引（uk）是防重复点赞/关注/发布的并发最后防线
 *  4. 评论列表批量预取 parent 评论，避免逐条查库的 N+1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final WorkRepository workRepository;
    private final FollowRepository followRepository;
    private final WorkLikeRepository workLikeRepository;
    private final WorkCommentRepository workCommentRepository;
    private final PhotoAnalysisRepository photoAnalysisRepository;
    private final UserRepository userRepository;

    // ==================== 作品发布 / 删除 ====================

    @Transactional
    public WorkResponse publish(User actor, PublishWorkRequest request) {
        PhotoAnalysis analysis = photoAnalysisRepository.findById(request.getPhotoAnalysisId())
                .orElseThrow(() -> new RuntimeException("照片打分记录不存在"));
        if (analysis.getUser() == null || !analysis.getUser().getId().equals(actor.getId())) {
            throw new RuntimeException("发布失败，只能发布自己的打分照片");
        }
        if (workRepository.existsByPhotoAnalysisId(request.getPhotoAnalysisId())) {
            throw new RuntimeException("发布失败，该照片已发布过");
        }

        Work work = Work.builder()
                .user(userRepository.getReferenceById(actor.getId()))
                .photoAnalysisId(request.getPhotoAnalysisId())
                .title(request.getTitle().trim())
                .content(request.getContent())
                .isPublic(true)
                .build();
        workRepository.save(work);
        log.info("用户 {} 发布作品 #{}（打分记录 #{}）", actor.getUsername(), work.getId(), work.getPhotoAnalysisId());
        return toWorkResponse(work);
    }

    @Transactional
    public void deleteWork(User actor, Long workId) {
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new RuntimeException("作品不存在"));
        if (!work.getUser().getId().equals(actor.getId())) {
            throw new RuntimeException("删除失败，只能删除自己的作品");
        }
        workCommentRepository.deleteByWorkId(workId);
        workLikeRepository.deleteByWorkId(workId);
        workRepository.delete(work);
        log.info("用户 {} 删除作品 #{}", actor.getUsername(), workId);
    }

    // ==================== 作品查询 ====================

    /**
     * 作品广场（公开作品）
     * latest：id 游标分页；hot：likes_count Top-N
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<WorkResponse> pagePublicWorks(String sort, Long cursor, int size) {
        int pageSize = normalizeSize(size);
        List<Work> works;
        Long nextCursor = null;
        boolean hasMore = false;

        if ("hot".equalsIgnoreCase(sort)) {
            works = workRepository.findHotPublic(PageRequest.of(0, pageSize));
        } else {
            List<Work> fetched = (cursor == null)
                    ? workRepository.findLatestPublic(PageRequest.of(0, pageSize + 1))
                    : workRepository.findLatestPublicBefore(cursor, PageRequest.of(0, pageSize + 1));
            hasMore = fetched.size() > pageSize;
            works = hasMore ? fetched.subList(0, pageSize) : fetched;
            if (!works.isEmpty()) {
                nextCursor = works.get(works.size() - 1).getId();
            }
        }
        return CursorPageResponse.<WorkResponse>builder()
                .items(works.stream().map(this::toWorkResponse).collect(Collectors.toList()))
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    /**
     * 我的作品（个人中心管理用）
     */
    @Transactional(readOnly = true)
    public List<WorkResponse> listMyWorks(User actor) {
        List<Work> works = workRepository.findUserWorks(actor.getId(),
                PageRequest.of(0, MAX_PAGE_SIZE));
        return works.stream().map(this::toWorkResponse).collect(Collectors.toList());
    }

    /**
     * 指定用户的公开作品（他人主页作品墙，id 游标分页）
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<WorkResponse> pageUserWorks(Long userId, Long cursor, int size) {
        int pageSize = normalizeSize(size);
        List<Work> fetched = (cursor == null)
                ? workRepository.findPublicUserWorks(userId, PageRequest.of(0, pageSize + 1))
                : workRepository.findPublicUserWorksBefore(userId, cursor, PageRequest.of(0, pageSize + 1));
        boolean hasMore = fetched.size() > pageSize;
        List<Work> works = hasMore ? fetched.subList(0, pageSize) : fetched;
        Long nextCursor = works.isEmpty() ? null : works.get(works.size() - 1).getId();
        return CursorPageResponse.<WorkResponse>builder()
                .items(works.stream().map(this::toWorkResponse).collect(Collectors.toList()))
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    /**
     * 作品详情（私密作品仅作者本人可见，对他人等同于不存在，避免泄露）
     */
    @Transactional(readOnly = true)
    public WorkDetailResponse getWorkDetail(Long workId, User viewer) {
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new RuntimeException("作品不存在"));
        boolean owner = viewer != null && work.getUser().getId().equals(viewer.getId());
        if (!work.getIsPublic() && !owner) {
            throw new RuntimeException("作品不存在");
        }
        boolean liked = viewer != null
                && workLikeRepository.existsByWorkIdAndUserId(workId, viewer.getId());

        return WorkDetailResponse.builder()
                .id(work.getId())
                .photoAnalysisId(work.getPhotoAnalysisId())
                .title(work.getTitle())
                .content(work.getContent())
                .likesCount(work.getLikesCount())
                .commentsCount(work.getCommentsCount())
                .liked(liked)
                .createdAt(work.getCreatedAt())
                .author(toAuthor(work.getUser()))
                .build();
    }

    /**
     * 关注动态 Feed：我关注的人发布的公开作品
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<WorkResponse> pageFollowFeed(User actor, Long cursor, int size) {
        int pageSize = normalizeSize(size);
        List<Work> fetched = workRepository.findFollowFeed(actor.getId(), cursor, PageRequest.of(0, pageSize + 1));
        boolean hasMore = fetched.size() > pageSize;
        List<Work> works = hasMore ? fetched.subList(0, pageSize) : fetched;
        Long nextCursor = works.isEmpty() ? null : works.get(works.size() - 1).getId();
        return CursorPageResponse.<WorkResponse>builder()
                .items(works.stream().map(this::toWorkResponse).collect(Collectors.toList()))
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    // ==================== 点赞 ====================

    @Transactional
    public LikeResponse like(User actor, Long workId) {
        Work work = getPublicWork(workId);
        if (work.getUser().getId().equals(actor.getId())) {
            throw new RuntimeException("点赞失败，不能给自己的作品点赞");
        }
        boolean already = workLikeRepository.existsByWorkIdAndUserId(workId, actor.getId());
        if (!already) {
            // 明细插入 + 计数 +1 同一事务；uk_like(user_id, work_id) 兜底并发重复
            workLikeRepository.save(WorkLike.builder()
                    .work(work)
                    .user(userRepository.getReferenceById(actor.getId()))
                    .build());
            work.setLikesCount(work.getLikesCount() + 1);
        }
        return LikeResponse.builder().liked(true).likesCount(work.getLikesCount()).build();
    }

    @Transactional
    public LikeResponse unlike(User actor, Long workId) {
        Work work = getPublicWork(workId);
        Optional<WorkLike> like = workLikeRepository.findByWorkIdAndUserId(workId, actor.getId());
        if (like.isPresent()) {
            workLikeRepository.delete(like.get());
            work.setLikesCount(Math.max(0, work.getLikesCount() - 1));
        }
        return LikeResponse.builder().liked(false).likesCount(work.getLikesCount()).build();
    }

    // ==================== 评论 ====================

    @Transactional
    public CommentResponse addComment(User actor, Long workId, CommentRequest request) {
        Work work = getPublicWork(workId);
        Long parentId = normalizeParentId(workId, request.getParentId());
        WorkComment comment = WorkComment.builder()
                .work(work)
                .user(userRepository.getReferenceById(actor.getId()))
                .parentId(parentId)
                .content(request.getContent().trim())
                .build();
        workCommentRepository.save(comment);
        work.setCommentsCount(work.getCommentsCount() + 1);
        return toCommentResponse(comment);
    }

    /**
     * 只支持一层楼中楼：如果回复的目标本身是子评论，则自动归并到它的顶楼 parent 下
     */
    private Long normalizeParentId(Long workId, Long requestParentId) {
        if (requestParentId == null) {
            return null;
        }
        WorkComment parent = workCommentRepository.findById(requestParentId)
                .orElseThrow(() -> new RuntimeException("评论失败，回复的评论不存在"));
        if (!parent.getWork().getId().equals(workId)) {
            throw new RuntimeException("评论失败，回复的评论不存在");
        }
        return parent.getParentId() != null ? parent.getParentId() : parent.getId();
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<CommentResponse> pageComments(Long workId, Long cursor, int size) {
        getPublicWork(workId);
        int pageSize = normalizeSize(size);
        List<WorkComment> fetched = (cursor == null)
                ? workCommentRepository.findByWorkIdOrderByIdDesc(workId, PageRequest.of(0, pageSize + 1))
                : workCommentRepository.findByWorkIdAndIdLessThanOrderByIdDesc(workId, cursor, PageRequest.of(0, pageSize + 1));
        boolean hasMore = fetched.size() > pageSize;
        List<WorkComment> comments = hasMore ? fetched.subList(0, pageSize) : fetched;
        Long nextCursor = comments.isEmpty() ? null : comments.get(comments.size() - 1).getId();
        return CursorPageResponse.<CommentResponse>builder()
                .items(toCommentResponses(comments))
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    @Transactional
    public void deleteComment(User actor, Long commentId) {
        WorkComment comment = workCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        Work work = comment.getWork();
        boolean isCommenter = comment.getUser().getId().equals(actor.getId());
        boolean isWorkOwner = work.getUser().getId().equals(actor.getId());
        if (!isCommenter && !isWorkOwner) {
            throw new RuntimeException("删除失败，只能删除自己的评论");
        }

        int removed = 1;
        if (comment.getParentId() == null) {
            // 删除顶楼评论时连带删除其子回复
            List<WorkComment> children = workCommentRepository.findByParentId(commentId);
            if (!children.isEmpty()) {
                workCommentRepository.deleteAll(children);
                removed += children.size();
            }
        }
        workCommentRepository.delete(comment);
        work.setCommentsCount(Math.max(0, work.getCommentsCount() - removed));
    }

    // ==================== 关注 ====================

    @Transactional
    public FollowResponse follow(User actor, Long targetUserId) {
        if (actor.getId().equals(targetUserId)) {
            throw new RuntimeException("关注失败，不能关注自己");
        }
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!followRepository.existsByFollowerIdAndFolloweeId(actor.getId(), targetUserId)) {
            // uk_follow 唯一索引兜底并发重复关注
            followRepository.save(Follow.builder()
                    .follower(userRepository.getReferenceById(actor.getId()))
                    .followee(userRepository.getReferenceById(targetUserId))
                    .build());
        }
        return FollowResponse.builder()
                .following(true)
                .followersCount(followRepository.countByFolloweeId(targetUserId))
                .build();
    }

    @Transactional
    public FollowResponse unfollow(User actor, Long targetUserId) {
        followRepository.findByFollowerIdAndFolloweeId(actor.getId(), targetUserId)
                .ifPresent(followRepository::delete);
        return FollowResponse.builder()
                .following(false)
                .followersCount(followRepository.countByFolloweeId(targetUserId))
                .build();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId, User viewer) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return buildUserProfile(user, viewer);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileByUsername(String username, User viewer) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return buildUserProfile(user, viewer);
    }

    private UserProfileResponse buildUserProfile(User user, User viewer) {
        Long userId = user.getId();
        boolean followed = viewer != null
                && followRepository.existsByFollowerIdAndFolloweeId(viewer.getId(), userId);
        return UserProfileResponse.builder()
                .id(userId)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .worksCount(workRepository.countPublicWorks(userId))
                .followersCount(followRepository.countByFolloweeId(userId))
                .followingCount(followRepository.countByFollowerId(userId))
                .joinedDate(user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate().toString() : null)
                .followedByMe(followed)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AuthorInfo> listFollowers(Long userId, int size) {
        return toAuthorList(followRepository.findFollowerUsers(userId, PageRequest.of(0, normalizeSize(size))));
    }

    @Transactional(readOnly = true)
    public List<AuthorInfo> listFollowing(Long userId, int size) {
        return toAuthorList(followRepository.findFollowingUsers(userId, PageRequest.of(0, normalizeSize(size))));
    }

    // ==================== 内部方法 ====================

    /** 仅允许读取/操作公开作品 */
    private Work getPublicWork(Long workId) {
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new RuntimeException("作品不存在"));
        if (!work.getIsPublic()) {
            throw new RuntimeException("作品不存在");
        }
        return work;
    }

    private WorkResponse toWorkResponse(Work work) {
        return WorkResponse.builder()
                .id(work.getId())
                .photoAnalysisId(work.getPhotoAnalysisId())
                .title(work.getTitle())
                .content(work.getContent())
                .likesCount(work.getLikesCount())
                .commentsCount(work.getCommentsCount())
                .createdAt(work.getCreatedAt())
                .author(toAuthor(work.getUser()))
                .build();
    }

    private List<CommentResponse> toCommentResponses(List<WorkComment> comments) {
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量预取 parent 评论作者昵称，避免逐条 N+1
        Set<Long> parentIds = comments.stream()
                .map(WorkComment::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> parentAuthorMap = parentIds.isEmpty()
                ? Collections.emptyMap()
                : workCommentRepository.findAllById(parentIds).stream()
                        .collect(Collectors.toMap(WorkComment::getId, WorkComment::getUser));
        return comments.stream()
                .map(c -> toCommentResponse(c, parentAuthorMap.get(c.getParentId())))
                .collect(Collectors.toList());
    }

    private CommentResponse toCommentResponse(WorkComment comment) {
        return toCommentResponse(comment, null);
    }

    private CommentResponse toCommentResponse(WorkComment comment, User replyTo) {
        return CommentResponse.builder()
                .id(comment.getId())
                .workId(comment.getWork().getId())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .author(toAuthor(comment.getUser()))
                .replyToName(replyTo == null ? null : displayName(replyTo))
                .build();
    }

    private List<AuthorInfo> toAuthorList(List<User> users) {
        return users.stream().map(this::toAuthor).collect(Collectors.toList());
    }

    private AuthorInfo toAuthor(User user) {
        if (user == null) {
            return null;
        }
        return AuthorInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        return user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname() : user.getUsername();
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
