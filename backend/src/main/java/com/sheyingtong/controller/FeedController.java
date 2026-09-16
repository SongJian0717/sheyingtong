package com.sheyingtong.controller;

import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.dto.response.CursorPageResponse;
import com.sheyingtong.dto.response.WorkResponse;
import com.sheyingtong.entity.User;
import com.sheyingtong.service.CommunityService;
import com.sheyingtong.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关注动态 Feed：我关注的人发布的最新公开作品
 */
@Tag(name = "摄影社区-Feed", description = "关注动态时间线（需登录）")
@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final CommunityService communityService;

    @Operation(summary = "关注动态", description = "我关注的人发布的作品，id 游标分页。【需登录】")
    @GetMapping
    public ApiResponse<CursorPageResponse<WorkResponse>> feed(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtil.getCurrentUser();
        return ApiResponse.success(communityService.pageFollowFeed(actor, cursor, size));
    }
}
