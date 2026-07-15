package com.sheyingtong.controller;

import com.sheyingtong.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Tag(name = "文件上传", description = "通用文件上传接口，目前支持用户头像上传（jpg/png/gif≤5MB），后续可扩展作品图等场景")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final String UPLOAD_DIR = "./uploads/avatars";

    @Operation(summary = "上传用户头像", description = "将头像图片保存到服务器本地（./uploads/avatars），返回可访问的URL路径（配合WebConfig静态资源映射）。支持jpg/png/gif，最大5MB。【需登录】")
    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(
            @Parameter(description = "头像图片文件（jpg/jpeg/png/gif）", required = true)
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error("请选择要上传的文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isValidImageExtension(originalFilename)) {
            return ApiResponse.error("只支持jpg、jpeg、png、gif格式的图片");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            return ApiResponse.error("图片大小不能超过5MB");
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(newFilename);

            Files.copy(file.getInputStream(), filePath);

            String avatarUrl = "/uploads/avatars/" + newFilename;
            return ApiResponse.success(avatarUrl);

        } catch (IOException e) {
            return ApiResponse.error("文件上传失败");
        }
    }

    private boolean isValidImageExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")
                || lowerFilename.endsWith(".png") || lowerFilename.endsWith(".gif");
    }
}