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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success("注册成功", null);
    }
    
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }
    
    @GetMapping("/user")
    public ApiResponse<LoginResponse.UserInfo> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        LoginResponse.UserInfo userInfo = authService.getUserInfo(user.getUsername());
        return ApiResponse.success(userInfo);
    }
    
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        SecurityContextHolder.getContext().setAuthentication(null);
        return ApiResponse.success("退出成功", null);
    }
    
    @PutMapping("/user")
    public ApiResponse<LoginResponse.UserInfo> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        LoginResponse.UserInfo userInfo = authService.updateUserInfo(user.getUsername(), request);
        return ApiResponse.success(userInfo);
    }
    
    @PostMapping("/user/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        authService.changePassword(user.getUsername(), request);
        return ApiResponse.success("密码修改成功", null);
    }
    
    @GetMapping("/user/statistics")
    public ApiResponse<UserStatisticsResponse> getUserStatistics() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        UserStatisticsResponse statistics = authService.getUserStatistics(user.getUsername());
        return ApiResponse.success(statistics);
    }
}
