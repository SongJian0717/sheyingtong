package com.sheyingtong.controller;

import com.sheyingtong.dto.request.CommentRequest;
import com.sheyingtong.dto.request.PublishWorkRequest;
import com.sheyingtong.dto.response.*;
import com.sheyingtong.entity.User;
import com.sheyingtong.service.CommunityService;
import com.sheyingtong.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 作品接口：发布/广场/详情/我的作品 + 点赞 + 评论
 */
@Tag(name = "摄影社区-作品", description = "作品发布、广场浏览、详情、点赞、评论（发布/点赞/评论需登录）")
@RestController
@RequestMapping("/api/works")
@RequiredArgsConstructor
public class WorkController {

    private final CommunityService communityService;

    @Operation(summary = "作品广场", description = "公开作品列表。sort=latest(默认，id游标分页) 或 hot(点赞Top-N)；cursor 为上一页返回的 nextCursor")
    @GetMapping
    public ApiResponse<CursorPageResponse<WorkResponse>> pageWorks(
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(communityService.pagePublicWorks(sort, cursor, size));
    }

    @Operation(summary = "我的作品", description = "当前登录用户发布过的作品列表。【需登录】")
    @GetMapping("/mine")
    public ApiResponse<java.util.List<WorkResponse>> myWorks() {
        User actor = SecurityUtil.getCurrentUser();
        return ApiResponse.success(communityService.listMyWorks(actor));
    }

    @Operation(summary = "作品详情", description = "作品信息 + 当前登录用户是否已点赞。私密作品仅作者可见")
    @GetMapping("/{id}")
    public ApiResponse<WorkDetailResponse> workDetail(@PathVariable Long id) {
        User viewer = SecurityUtil.getCurrentUser();
        return ApiResponse.success(communityService.getWorkDetail(id, viewer));
    }

    @Operation(summary = "发布作品", description = "把本人一条 AI 打分记录(photoAnalysisId)发布为公开作品。【需登录】")
    @PostMapping
    public ApiResponse<WorkResponse> publish(@Valid @RequestBody PublishWorkRequest request) {
        User actor = SecurityUtil.getCurrentUser();
        return ApiResponse.success(communityService.publish(actor, request));
    }

    @Operation(summary = "删除作品", description = "删除本人作品，同时清空其点赞与评论记录。【需登录】")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWork(@PathVariable Long id) {
        communityService.deleteWork(SecurityUtil.getCurrentUser(), id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "点赞作品", description = "重复点赞幂等返回当前计数。【需登录】")
    @PostMapping("/{id}/like")
    public ApiResponse<LikeResponse> like(@PathVariable Long id) {
        return ApiResponse.success(communityService.like(SecurityUtil.getCurrentUser(), id));
    }

    @Operation(summary = "取消点赞", description = "未点赞时幂等返回当前计数。【需登录】")
    @DeleteMapping("/{id}/like")
    public ApiResponse<LikeResponse> unlike(@PathVariable Long id) {
        return ApiResponse.success(communityService.unlike(SecurityUtil.getCurrentUser(), id));
    }

    @Operation(summary = "作品评论列表", description = "id 游标分页，返回最新在前")
    @GetMapping("/{id}/comments")
    public ApiResponse<CursorPageResponse<CommentResponse>> comments(
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(communityService.pageComments(id, cursor, size));
    }

    @Operation(summary = "发表评论", description = "content 必填；parentId 指定回复某条评论（仅一层楼中楼）。【需登录】")
    @PostMapping("/{id}/comments")
    public ApiResponse<CommentResponse> addComment(@PathVariable Long id,
                                                   @Valid @RequestBody CommentRequest request) {
        return ApiResponse.success(communityService.addComment(SecurityUtil.getCurrentUser(), id, request));
    }

    @Operation(summary = "删除评论", description = "本人评论或本作品作者可删；删顶楼评论连带删子回复并同步扣减计数。【需登录】")
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        communityService.deleteComment(SecurityUtil.getCurrentUser(), commentId);
        return ApiResponse.success(null);
    }
}
