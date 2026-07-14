package com.sheyingtong.controller;

import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.dto.response.PhotoScoreResponse;
import com.sheyingtong.entity.PhotoAnalysis;
import com.sheyingtong.entity.User;
import com.sheyingtong.service.PhotoService;
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

@RestController
@RequestMapping("/api/photo")
@RequiredArgsConstructor
public class PhotoController {
    
    private final PhotoService photoService;
    
    @PostMapping("/score")
    public ApiResponse<PhotoScoreResponse> scorePhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        
        if (file.isEmpty()) {
            throw new RuntimeException("请选择要上传的照片");
        }
        
        String username = getCurrentUsername();
        PhotoScoreResponse response = photoService.analyzePhoto(file, description, username);
        return ApiResponse.success(response);
    }
    
    @GetMapping("/history")
    public ApiResponse<Page<PhotoAnalysis>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        String username = getCurrentUsername();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PhotoAnalysis> history = photoService.getHistory(username, pageable);
        return ApiResponse.success(history);
    }
    
    @GetMapping("/detail/{id}")
    public ApiResponse<PhotoScoreResponse> getDetail(@PathVariable Long id) {
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
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAnalysis(@PathVariable Long id) {
        PhotoAnalysis analysis = photoService.getAnalysisById(id);
        if (analysis == null) {
            return ApiResponse.error("记录不存在");
        }
        
        photoService.deleteAnalysis(id);
        return ApiResponse.success(null);
    }
    
    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) {
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
