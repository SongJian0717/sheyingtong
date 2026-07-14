package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体包装类（前后端契约）
 * ----------------------------------------------------------------
 * 所有 Controller 返回都用该类，前端 request.js L43 会自动解包取 data 字段：
 *   - 成功：{ "code": 200, "message": "success", "data": <T> }
 *   - 失败：{ "code": 400/401/403/500, "message": "错误原因", "data": null }
 *
 * code 规范：200 成功；4xx 客户端问题（参数/鉴权/权限）；5xx 服务端问题
 *
 * @param <T> 响应体 data 字段的泛型类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    private int code;
    
    private String message;
    
    private T data;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .build();
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }
    
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .code(500)
                .message(message)
                .data(null)
                .build();
    }
}
