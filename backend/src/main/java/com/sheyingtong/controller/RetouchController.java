package com.sheyingtong.controller;

import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.dto.response.RetouchResponse;
import com.sheyingtong.entity.RetouchRecord;
import com.sheyingtong.entity.User;
import com.sheyingtong.service.RetouchService;
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

@RestController
@RequestMapping("/api/retouch")
@RequiredArgsConstructor
public class RetouchController {
    
    private final RetouchService retouchService;
    
    @PostMapping("/suggestions")
    public ApiResponse<RetouchResponse> getSuggestions(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        
        if (file.isEmpty()) {
            throw new RuntimeException("请选择要上传的照片");
        }
        
        String username = getCurrentUsername();
        RetouchResponse response = retouchService.getRetouchSuggestions(file, description, username);
        return ApiResponse.success(response);
    }
    
    @GetMapping("/history")
    public ApiResponse<Page<RetouchRecord>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        String username = getCurrentUsername();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RetouchRecord> history = retouchService.getHistory(username, pageable);
        return ApiResponse.success(history);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id) {
        RetouchRecord record = retouchService.getRecordById(id);
        if (record == null) {
            return ApiResponse.error("记录不存在");
        }
        
        retouchService.deleteRecord(id);
        return ApiResponse.success(null);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<RetouchResponse> getDetail(@PathVariable Long id) {
        RetouchRecord record = retouchService.getRecordById(id);
        if (record == null) {
            return ApiResponse.error("记录不存在");
        }
        
        RetouchResponse response = retouchService.getRetouchDetail(record);
        return ApiResponse.success(response);
    }
    
    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) {
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
