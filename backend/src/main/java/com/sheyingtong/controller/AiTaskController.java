package com.sheyingtong.controller;

import com.sheyingtong.dto.response.AiTaskStatusResponse;
import com.sheyingtong.dto.response.AiTaskSubmitResponse;
import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.entity.User;
import com.sheyingtong.service.AiTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 异步任务查询/重试接口
 * ----------------------------------------------------------------
 * 提交入口仍在原来的业务接口上（POST /api/photo/score、POST /api/retouch/suggestions），
 * 它们现在立即返回 taskId；本控制器负责"拿 taskId 换结果"。
 *
 * 两个接口都做归属校验：只有任务所属用户能查看/重试自己的任务，
 * 否则任何登录用户都能靠遍历 id 拿到别人的评分结果。
 */
@RestController
@RequestMapping("/api/ai/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI 异步任务", description = "照片打分 / 修图建议的异步任务状态查询与重试")
public class AiTaskController {

    private final AiTaskService aiTaskService;

    /**
     * 查询任务状态与结果（前端轮询）
     * GET /api/ai/tasks/{taskId}
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "查询异步任务状态与结果")
    public ApiResponse<AiTaskStatusResponse> getTaskStatus(@PathVariable Long taskId) {
        String username = currentUsername();
        return ApiResponse.success(aiTaskService.getStatus(taskId, username));
    }

    /**
     * 重试失败/降级的任务（复用已上传的原图，无需再次上传）
     * POST /api/ai/tasks/{taskId}/retry
     */
    @PostMapping("/{taskId}/retry")
    @Operation(summary = "重试失败的异步任务")
    public ApiResponse<AiTaskSubmitResponse> retryTask(@PathVariable Long taskId) {
        String username = currentUsername();
        log.info("[ai-task] 用户 {} 请求重试任务 {}", username, taskId);
        return ApiResponse.success(aiTaskService.retry(taskId, username));
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new RuntimeException("未登录或登录已过期");
        }
        return user.getUsername();
    }
}
