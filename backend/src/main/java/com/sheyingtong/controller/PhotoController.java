package com.sheyingtong.controller;

import com.sheyingtong.dto.response.AiTaskSubmitResponse;
import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.dto.response.PhotoScoreResponse;
import com.sheyingtong.entity.PhotoAnalysis;
import com.sheyingtong.entity.User;
import com.sheyingtong.service.PhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.util.Map;

@Tag(name = "AI照片打分", description = "上传照片进行AI六维度评分（构图/光线/色彩/对焦/内容/建议），基于Qwen-VL-Flash多模态模型")
@RestController
@RequestMapping("/api/photo")
@RequiredArgsConstructor
public class PhotoController {
    
    private final PhotoService photoService;
    
    @Operation(summary = "AI照片打分（异步提交）", description = "上传照片后立即返回任务ID，AI 六维度评分在后台异步执行，前端轮询 GET /api/ai/tasks/{taskId} 取结果。每个维度80-120字详细评语。【需登录】")
    @PostMapping("/score")
    public ApiResponse<AiTaskSubmitResponse> scorePhoto(
            @Parameter(description = "照片文件（jpg/png/webp）", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "可选：用户对照片的补充描述，帮助AI更精准评价")
            @RequestParam(value = "description", required = false) String description) {
        
        if (file.isEmpty()) {
            throw new RuntimeException("请选择要上传的照片");
        }
        
        String username = getCurrentUsername();
        AiTaskSubmitResponse response = photoService.submitScore(file, description, username);
        return ApiResponse.success(response);
    }
    
    @Operation(summary = "获取打分历史记录", description = "分页返回当前用户的所有照片打分记录（按时间倒序）。【需登录】")
    @GetMapping("/history")
    public ApiResponse<Page<PhotoAnalysis>> getHistory(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        
        String username = getCurrentUsername();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PhotoAnalysis> history = photoService.getHistory(username, pageable);
        return ApiResponse.success(history);
    }
    
    @Operation(summary = "获取打分详情", description = "根据记录ID获取单条打分的完整六维度评分和建议详情。【公开接口】")
    @GetMapping("/detail/{id}")
    public ApiResponse<PhotoScoreResponse> getDetail(
            @Parameter(description = "打分记录ID", required = true)
            @PathVariable Long id) {
        PhotoAnalysis analysis = photoService.getAnalysisById(id);
        if (analysis == null) {
            return ApiResponse.error("记录不存在");
        }
        
        PhotoScoreResponse response = PhotoScoreResponse.builder()
                .score(analysis.getScore())
                .analysis(parseAnalysis(analysis.getAnalysis()))
                .suggestions(analysis.getSuggestions())
                .build();
        
        return ApiResponse.success(response);
    }
    
    private Map<String, Object> parseAnalysis(String analysisJson) {
        if (analysisJson == null || analysisJson.isEmpty()) {
            return Map.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(analysisJson, 
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
    
    @Operation(summary = "删除打分记录", description = "删除指定ID的照片打分记录（同时删除磁盘上的图片文件）。【需登录】")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAnalysis(
            @Parameter(description = "打分记录ID", required = true)
            @PathVariable Long id) {
        PhotoAnalysis analysis = photoService.getAnalysisById(id);
        if (analysis == null) {
            return ApiResponse.error("记录不存在");
        }
        
        photoService.deleteAnalysis(id);
        return ApiResponse.success(null);
    }
    
    @Operation(summary = "获取打分照片原图", description = "返回原始上传的照片二进制流（内联显示）。用于历史记录卡片展示缩略图。【公开接口】")
    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> getImage(
            @Parameter(description = "打分记录ID", required = true)
            @PathVariable Long id) {
        PhotoAnalysis analysis = photoService.getAnalysisById(id);
        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }
        
        File file = new File(analysis.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = new FileSystemResource(file);
        
        String fileName = analysis.getFileName();
        String contentType = determineContentType(fileName);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "inline; filename=\"" + URLEncoder.encode(fileName) + "\"")
                .body(resource);
    }
    
    private String determineContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !(authentication.getPrincipal() instanceof String)) {
            User user = (User) authentication.getPrincipal();
            return user.getUsername();
        }
        return null;
    }
}
