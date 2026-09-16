package com.sheyingtong.controller;

import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.dto.response.AuthorInfo;
import com.sheyingtong.dto.response.CursorPageResponse;
import com.sheyingtong.dto.response.FollowResponse;
import com.sheyingtong.dto.response.UserProfileResponse;
import com.sheyingtong.dto.response.WorkResponse;
import com.sheyingtong.entity.User;
import com.sheyingtong.service.CommunityService;
import com.sheyingtong.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户社交接口：公开主页 / 作品墙 / 粉丝关注列表 / 关注与取关
 */
@Tag(name = "摄影社区-用户", description = "用户公开主页、作品墙、粉丝/关注列表、关注取关（关注/取关需登录）")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserSocialController {

    private final CommunityService communityService;

    @Operation(summary = "用户公开主页", description = "用户信息 + 作品/粉丝/关注数 + 当前登录者是否已关注")
    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> profile(@PathVariable Long userId) {
        return ApiResponse.success(communityService.getUserProfile(userId, SecurityUtil.getCurrentUser()));
    }

    @Operation(summary = "按用户名查公开主页", description = "前端 /user/:username 路由用，返回主页信息（含 id）")
    @GetMapping("/username/{username}")
    public ApiResponse<UserProfileResponse> profileByUsername(@PathVariable String username) {
        return ApiResponse.success(communityService.getUserProfileByUsername(username, SecurityUtil.getCurrentUser()));
    }

    @Operation(summary = "用户作品墙", description = "该用户公开作品，id 游标分页")
    @GetMapping("/{userId}/works")
    public ApiResponse<CursorPageResponse<WorkResponse>> userWorks(
            @PathVariable Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(communityService.pageUserWorks(userId, cursor, size));
    }

    @Operation(summary = "粉丝列表", description = "关注该用户的人（按关注时间倒序，最多50）")
    @GetMapping("/{userId}/followers")
    public ApiResponse<List<AuthorInfo>> followers(@PathVariable Long userId,
                                                   @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(communityService.listFollowers(userId, size));
    }

    @Operation(summary = "关注列表", description = "该用户关注的人（按关注时间倒序，最多50）")
    @GetMapping("/{userId}/following")
    public ApiResponse<List<AuthorInfo>> following(@PathVariable Long userId,
                                                   @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(communityService.listFollowing(userId, size));
    }

    @Operation(summary = "关注用户", description = "重复关注幂等返回当前粉丝数。【需登录】")
    @PostMapping("/{userId}/follow")
    public ApiResponse<FollowResponse> follow(@PathVariable Long userId) {
        return ApiResponse.success(communityService.follow(SecurityUtil.getCurrentUser(), userId));
    }

    @Operation(summary = "取消关注", description = "未关注时幂等返回当前粉丝数。【需登录】")
    @DeleteMapping("/{userId}/follow")
    public ApiResponse<FollowResponse> unfollow(@PathVariable Long userId) {
        return ApiResponse.success(communityService.unfollow(SecurityUtil.getCurrentUser(), userId));
    }
}
