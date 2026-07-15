package com.sheyingtong.controller;

import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.dto.response.RetouchResponse;
import com.sheyingtong.entity.RetouchRecord;
import com.sheyingtong.entity.User;
import com.sheyingtong.service.RetouchService;
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

@Tag(name = "AI修图建议", description = "上传照片获取AI生成的Lightroom风格修图调色参数建议（曝光/对比度/白平衡/HSL等），含历史记录管理")
@RestController
@RequestMapping("/api/retouch")
@RequiredArgsConstructor
public class RetouchController {
    
    private final RetouchService retouchService;
    
    @Operation(summary = "AI修图建议", description = "上传照片后由AI分析并给出Lightroom/Capture One风格的调色参数建议：曝光、对比度、高光/阴影、白平衡、HSL色相、饱和度、明度、清晰度、锐化等。【需登录】")
    @PostMapping("/suggestions")
    public ApiResponse<RetouchResponse> getSuggestions(
            @Parameter(description = "照片文件（jpg/png/webp）", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "可选：用户想要的修图风格描述（如\"日系清新\"、\"电影感\"）")
            @RequestParam(value = "description", required = false) String description) {
        
        if (file.isEmpty()) {
            throw new RuntimeException("请选择要上传的照片");
        }
        
        String username = getCurrentUsername();
        RetouchResponse response = retouchService.getRetouchSuggestions(file, description, username);
        return ApiResponse.success(response);
    }
    
    @Operation(summary = "获取修图历史记录", description = "分页返回当前用户的所有AI修图建议记录（按时间倒序）。【需登录】")
    @GetMapping("/history")
    public ApiResponse<Page<RetouchRecord>> getHistory(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        
        String username = getCurrentUsername();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RetouchRecord> history = retouchService.getHistory(username, pageable);
        return ApiResponse.success(history);
    }
    
    @Operation(summary = "删除修图记录", description = "删除指定ID的修图建议记录（同时删除磁盘上的图片文件）。【需登录】")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRecord(
            @Parameter(description = "修图记录ID", required = true)
            @PathVariable Long id) {
        RetouchRecord record = retouchService.getRecordById(id);
        if (record == null) {
            return ApiResponse.error("记录不存在");
        }
        
        retouchService.deleteRecord(id);
        return ApiResponse.success(null);
    }
    
    @Operation(summary = "获取修图详情", description = "根据记录ID获取单条修图建议的完整参数详情。【公开接口】")
    @GetMapping("/{id}")
    public ApiResponse<RetouchResponse> getDetail(
            @Parameter(description = "修图记录ID", required = true)
            @PathVariable Long id) {
        RetouchRecord record = retouchService.getRecordById(id);
        if (record == null) {
            return ApiResponse.error("记录不存在");
        }
        
        RetouchResponse response = retouchService.getRetouchDetail(record);
        return ApiResponse.success(response);
    }
    
    @Operation(summary = "获取修图照片原图", description = "返回上传的原始照片二进制流（内联显示）。用于修图历史记录卡片展示缩略图。【公开接口】")
    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> getImage(
            @Parameter(description = "修图记录ID", required = true)
            @PathVariable Long id) {
        RetouchRecord record = retouchService.getRecordById(id);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        
        File file = new File(record.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = new FileSystemResource(file);
        
        String fileName = record.getFileName();
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
