package com.sheyingtong.controller;

import com.sheyingtong.dto.request.ChangePasswordRequest;
import com.sheyingtong.dto.request.LoginRequest;
import com.sheyingtong.dto.request.RegisterRequest;
import com.sheyingtong.dto.request.UpdateUserRequest;
import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.dto.response.LoginResponse;
import com.sheyingtong.dto.response.UserStatisticsResponse;
import com.sheyingtong.entity.User;
import com.sheyingtong.service.AuthService;
import com.sheyingtong.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "用户注册、登录、个人信息、密码修改、统计数据等用户账户相关接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    
    @Operation(summary = "用户注册", description = "创建新用户账户，用户名和邮箱需全局唯一，密码需6-32位，保存前使用BCrypt加密。")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数校验失败或用户名/邮箱已存在")
    })
    @PostMapping("/register")
    public ApiResponse<Void> register(
            @Parameter(description = "注册请求体（用户名、邮箱、密码、昵称）", required = true)
            @Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success("注册成功", null);
    }
    
    @Operation(summary = "用户登录", description = "使用用户名+密码登录，认证通过后返回 JWT Bearer Token，用于后续接口鉴权。")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功，返回token和用户信息",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Parameter(description = "登录请求体（用户名、密码）", required = true)
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }
    
    @Operation(summary = "获取当前用户信息", description = "从 JWT Token 解析当前登录用户，返回用户基本资料（含头像Base64）。【需登录】")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "返回用户信息",
                    content = @Content(schema = @Schema(implementation = LoginResponse.UserInfo.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或Token无效")
    })
    @GetMapping("/user")
    public ApiResponse<LoginResponse.UserInfo> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        LoginResponse.UserInfo userInfo = authService.getUserInfo(user.getUsername());
        return ApiResponse.success(userInfo);
    }
    
    @Operation(summary = "用户退出登录", description = "清除当前 SecurityContext 中的认证信息（前端应同时删除本地保存的Token）。【需登录】")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        SecurityContextHolder.getContext().setAuthentication(null);
        return ApiResponse.success("退出成功", null);
    }
    
    @Operation(summary = "更新用户信息", description = "修改当前用户的昵称、头像、简介等资料，返回更新后的完整用户信息。【需登录】")
    @PutMapping("/user")
    public ApiResponse<LoginResponse.UserInfo> updateUserInfo(
            @Parameter(description = "更新请求体（昵称、头像、简介等）", required = true)
            @Valid @RequestBody UpdateUserRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        LoginResponse.UserInfo userInfo = authService.updateUserInfo(user.getUsername(), request);
        return ApiResponse.success(userInfo);
    }
    
    @Operation(summary = "修改用户密码", description = "校验原密码通过后，更新为新密码（BCrypt加密存储）。【需登录】")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "密码修改成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "原密码错误或新密码不符合规则")
    })
    @PostMapping("/user/change-password")
    public ApiResponse<Void> changePassword(
            @Parameter(description = "修改密码请求体（oldPassword + newPassword）", required = true)
            @Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        authService.changePassword(user.getUsername(), request);
        return ApiResponse.success("密码修改成功", null);
    }
    
    @Operation(summary = "获取用户统计数据", description = "返回当前用户的照片打分次数、修图次数、AI聊天对话数等统计指标。【需登录】")
    @GetMapping("/user/statistics")
    public ApiResponse<UserStatisticsResponse> getUserStatistics() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        UserStatisticsResponse statistics = authService.getUserStatistics(user.getUsername());
        return ApiResponse.success(statistics);
    }
}
