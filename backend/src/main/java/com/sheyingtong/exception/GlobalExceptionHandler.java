package com.sheyingtong.exception;

import com.sheyingtong.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring 全局异常处理器（@RestControllerAdvice 织入所有 @RestController）
 * ----------------------------------------------------------------
 * 异常分层策略（从上到下匹配，粒度细 → 粗）：
 *  1) MethodArgumentNotValidException  →  @Valid 校验失败，返回字段级错误 Map
 *  2) ConstraintViolationException     →  方法级参数校验（@Min/@NotBlank 等）
 *  3) MaxUploadSizeExceededException   →  文件上传超过 spring.servlet.multipart.max-file-size
 *  4) RuntimeException                  →  业务异常，用 message 关键词判断 400/500
 *  5) Exception（兜底）                  →  所有未捕获异常，记录完整堆栈并返回具体 message
 *
 * 设计目标：无论哪一层抛异常，前端都一定收到 ApiResponse 结构，不会暴露 Tomcat 错误页
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * RuntimeException 处理：语义关键词区分 400/500
     * ----------------------------------------------------------------
     * （项目早期没有自定义 BusinessException，所以用关键词判断）
     *  - message 包含 "不存在/错误/禁用/已被/失败" → 客户端错误，400
     *  - 其他一律 → 服务端错误，500 + 通用"服务器内部错误"避免泄露堆栈
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();
        log.error("RuntimeException: {}", message, ex);
        if (message != null && (message.contains("不存在") || message.contains("错误") || 
            message.contains("禁用") || message.contains("已被") || message.contains("失败"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, message));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误"));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage
                ));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .code(400)
                        .message("参数校验失败")
                        .data(errors)
                        .build());
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .code(400)
                        .message("参数校验失败")
                        .data(errors)
                        .build());
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "上传文件大小超过限制（最大50MB）"));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("未捕获异常: {}", ex.getMessage(), ex);
        String detail = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误: " + detail));
    }
}
