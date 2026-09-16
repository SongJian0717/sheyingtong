package com.sheyingtong.service;

import com.sheyingtong.dto.request.ChangePasswordRequest;
import com.sheyingtong.dto.request.LoginRequest;
import com.sheyingtong.dto.request.RegisterRequest;
import com.sheyingtong.dto.request.UpdateUserRequest;
import com.sheyingtong.dto.response.LoginResponse;
import com.sheyingtong.dto.response.UserStatisticsResponse;
import com.sheyingtong.repository.ChatSessionRepository;
import com.sheyingtong.repository.PhotoAnalysisRepository;
import com.sheyingtong.repository.RetouchRecordRepository;
import com.sheyingtong.repository.FollowRepository;
import com.sheyingtong.repository.WorkRepository;
import com.sheyingtong.entity.User;
import com.sheyingtong.repository.UserRepository;
import com.sheyingtong.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PhotoAnalysisRepository photoAnalysisRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final RetouchRecordRepository retouchRecordRepository;
    private final WorkRepository workRepository;
    private final FollowRepository followRepository;
    
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getUsername())
                .enabled(true)
                .build();
        
        userRepository.save(user);
    }
    
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        if (!user.getEnabled()) {
            throw new RuntimeException("账号已被禁用");
        }
        
        String token = jwtUtil.generateToken(user.getUsername());
        
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
        
        return LoginResponse.builder()
                .token(token)
                .user(userInfo)
                .build();
    }
    
    public LoginResponse.UserInfo getUserInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }
    
    @Transactional
    public LoginResponse.UserInfo updateUserInfo(String username, UpdateUserRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            user.setNickname(request.getNickname());
        }
        
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("邮箱已被使用");
            }
            user.setEmail(request.getEmail());
        }
        
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        
        user = userRepository.save(user);
        
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }
    
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码不正确");
        }
        
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
    
    public UserStatisticsResponse getUserStatistics(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        int totalScoreRecords = photoAnalysisRepository.countByUser(user);
        int totalChatSessions = chatSessionRepository.countByUserId(user.getId());
        int totalRetouchRecords = retouchRecordRepository.countByUser(user);
        
        return UserStatisticsResponse.builder()
                .totalPhotos(totalScoreRecords + totalRetouchRecords)
                .totalChatSessions(totalChatSessions)
                .totalRetouchRecords(totalRetouchRecords)
                .totalScoreRecords(totalScoreRecords)
                .joinDate(user.getCreatedAt().toLocalDate().toString())
                .worksCount(workRepository.countPublicWorks(user.getId()))
                .followersCount(followRepository.countByFolloweeId(user.getId()))
                .followingCount(followRepository.countByFollowerId(user.getId()))
                .build();
    }
}
