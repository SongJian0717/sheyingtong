package com.sheyingtong.controller;

import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.entity.ChatMessage;
import com.sheyingtong.entity.ChatSession;
import com.sheyingtong.entity.User;
import com.sheyingtong.repository.UserRepository;
import com.sheyingtong.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * AI 聊天控制器（REST API 入口）
 * ----------------------------------------------------------------
 * 三大类能力：
 *  1) 会话管理 - 新增/查询/删除聊天会话（逻辑删除 active=false）
 *  2) 消息发送 - 同步回复（/message）+ SSE 流式（/message/stream）+ 图片多模态（/message/image）
 *  3) 工具调试 - /test-tool 手动触发 LangChain4j Function Calling，验证工具输出
 *
 * 权限：全部接口需要 authenticated（SecurityConfig L72 .requestMatchers("/api/chat/**")）
 */
@Tag(name = "AI聊天助手", description = "摄影AI智能对话接口，支持会话管理、同步回复、SSE流式打字机、多模态图片问答、工具调用调试。【全部需登录】")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    /**
     * 从 Principal 安全地提取 userId
     * ----------------------------------------------------------------
     * 【历史坑】JwtAuthenticationFilter 把整个 User 对象放在 principal 位置，
     *          而 Spring Security 默认 principal.getName() 返回的是 User.toString()，
     *          并非 username 字符串，直接 username 查询会查不到用户抛 500。
     * 【修复策略】三级兜底：
     *   1) 优先 instanceof User 直接取 id（0 查询，性能最优）
     *   2) 其次 instanceof String 用 username 查一次数据库
     *   3) 最后 principal.getName() 兜底（兼容其他认证方式）
     */
    private Long getUserId(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("用户未登录");
        }
        if (principal instanceof Authentication auth) {
            Object principalObj = auth.getPrincipal();
            if (principalObj instanceof User user) {
                return user.getId();
            }
            if (principalObj instanceof String username) {
                return userRepository.findByUsername(username)
                        .map(User::getId)
                        .orElseThrow(() -> new RuntimeException("用户未找到"));
            }
        }
        String name = principal.getName();
        return userRepository.findByUsername(name)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("用户未找到: " + name));
    }

    @Operation(summary = "创建聊天会话", description = "新建一个AI聊天会话，可指定初始标题。返回包含sessionId的会话对象用于后续发消息。【需登录】")
    @PostMapping("/session")
    public ApiResponse<ChatSession> createSession(Principal principal, 
                                                  @Parameter(description = "可选：title=会话初始标题")
                                                  @RequestBody(required = false) Map<String, String> request) {
        Long userId = getUserId(principal);
        String title = request != null ? request.get("title") : null;
        ChatSession session = chatService.createSession(userId, title);
        return ApiResponse.success(session);
    }

    @Operation(summary = "获取我的会话列表", description = "返回当前用户的所有聊天会话（按时间倒序，逻辑删除的active=false不返回）。【需登录】")
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSession>> getSessions(Principal principal) {
        Long userId = getUserId(principal);
        List<ChatSession> sessions = chatService.getSessions(userId);
        return ApiResponse.success(sessions);
    }

    @Operation(summary = "获取单个会话详情", description = "根据sessionId查询会话元数据（标题、创建时间等）。【需登录】")
    @GetMapping("/session/{sessionId}")
    public ApiResponse<ChatSession> getSession(Principal principal,
                                               @Parameter(description = "会话ID", required = true)
                                               @PathVariable Long sessionId) {
        Long userId = getUserId(principal);
        ChatSession session = chatService.getSession(sessionId, userId);
        return ApiResponse.success(session);
    }

    @Operation(summary = "删除聊天会话", description = "逻辑删除会话（active=false），会话消息保留但列表不再显示。【需登录】")
    @DeleteMapping("/session/{sessionId}")
    public ApiResponse<Void> deleteSession(Principal principal,
                                           @Parameter(description = "会话ID", required = true)
                                           @PathVariable Long sessionId) {
        Long userId = getUserId(principal);
        chatService.deleteSession(sessionId, userId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取会话历史消息", description = "返回指定会话下的全部历史聊天消息（用户提问+AI回复）。【需登录】")
    @GetMapping("/session/{sessionId}/messages")
    public ApiResponse<List<ChatMessage>> getMessages(Principal principal,
                                                      @Parameter(description = "会话ID", required = true)
                                                      @PathVariable Long sessionId) {
        Long userId = getUserId(principal);
        List<ChatMessage> messages = chatService.getMessages(sessionId, userId);
        return ApiResponse.success(messages);
    }

    @Operation(summary = "发送消息（同步回复）", description = "向指定会话发送用户消息，AI完整生成回复后一次性返回。支持Function Calling + RAG检索 + Redis记忆。【需登录】")
    @PostMapping("/session/{sessionId}/message")
    public ApiResponse<Map<String, Object>> sendMessage(Principal principal,
                                                        @Parameter(description = "会话ID", required = true)
                                                        @PathVariable Long sessionId,
                                                        @Parameter(description = "请求体：{content: 用户提问文本}", required = true)
                                                        @RequestBody Map<String, String> request) {
        Long userId = getUserId(principal);
        String message = request.get("content");
        
        if (message == null || message.trim().isEmpty()) {
            return ApiResponse.error("消息内容不能为空");
        }
        
        log.info("收到聊天消息: sessionId={}, userId={}, contentLength={}", 
                sessionId, userId, message.length());
        
        String response = chatService.sendMessage(sessionId, userId, message);
        
        return ApiResponse.success(Map.of(
                "response", response,
                "sessionId", sessionId
        ));
    }

    /**
     * SSE 流式消息接口（打字机效果）
     * ----------------------------------------------------------------
     * 特点：
     *  - produces = TEXT_EVENT_STREAM_VALUE → Content-Type: text/event-stream
     *  - 返回 reactor Flux<String>，每个 emit 的 token 作为一个 SSE chunk 推到前端
     *  - 前端用 XMLHttpRequest onprogress 逐字解析（chat.js sendMessageStream）
     * 注意：
     *  - 流式接口不使用 PhotographyAssistant（Function Calling 不支持 streaming）
     *    而是直接调用 StreamingChatModel，不走工具调用/Redis记忆/RAG链路
     *  - 空消息直接返回 Flux.error IllegalArgumentException，由统一异常处理映射成 400
     */
    @Operation(summary = "发送消息（SSE流式打字机）", description = "流式返回AI回复token，实现打字机效果。Content-Type: text/event-stream，返回Flux<SSE chunk>。【需登录】")
    @PostMapping(value = "/session/{sessionId}/message/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sendMessageStream(Principal principal,
                                          @Parameter(description = "会话ID", required = true)
                                          @PathVariable Long sessionId,
                                          @Parameter(description = "{content: 提问文本}", required = true)
                                          @RequestBody Map<String, String> request) {
        Long userId = getUserId(principal);
        String message = request.get("content");
        
        if (message == null || message.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("消息内容不能为空"));
        }
        
        log.info("收到流式聊天消息: sessionId={}, userId={}, contentLength={}", 
                sessionId, userId, message.length());
        
        return chatService.sendMessageStream(sessionId, userId, message);
    }

    @Operation(summary = "快速新建会话", description = "不指定标题，创建一个默认标题（按首条消息自动命名）的新会话。【需登录】")
    @PostMapping("/new-session")
    public ApiResponse<ChatSession> createNewSession(Principal principal) {
        Long userId = getUserId(principal);
        ChatSession session = chatService.createNewSession(userId);
        return ApiResponse.success(session);
    }

    /**
     * 多模态消息接口：用户上传图片 + 文字问题 → 流式返回 AI 分析
     * ----------------------------------------------------------------
     * 参数：
     *  - content：文字提问（RequestParam 表单字段）
     *  - image：MultipartFile 二进制图片（浏览器端 new FormData append("image", file)）
     * 实现：ChatService.sendMessageWithImage() 内部使用 Qwen-VL-Plus 多模态模型
     */
    @Operation(summary = "多模态图片问答（SSE流式）", description = "上传一张图片 + 文字提问，AI基于图片内容进行分析回复（FormData表单提交）。使用Qwen-VL-Flash多模态模型。【需登录】")
    @PostMapping(value = "/session/{sessionId}/message/image", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sendMessageWithImage(Principal principal,
                                             @Parameter(description = "会话ID", required = true)
                                             @PathVariable Long sessionId,
                                             @Parameter(description = "针对图片的文字提问", required = true)
                                             @RequestParam String content,
                                             @Parameter(description = "图片文件（jpg/png/webp）", required = true)
                                             @RequestParam MultipartFile image) {
        Long userId = getUserId(principal);
        
        log.info("收到图片消息: sessionId={}, userId={}, content={}, imageSize={}", 
                sessionId, userId, content, image.getSize());
        
        return chatService.sendMessageWithImage(sessionId, userId, content, image);
    }

    @Operation(summary = "工具调用调试接口", description = "手动触发LangChain4j Function Calling工具，验证工具输出。支持：queryDictionary、calculateExposure、explainExposureTriangle。")
    @PostMapping("/test-tool")
    public ApiResponse<String> testTool(
            @Parameter(description = "{tool: 工具名, param: 参数字符串}", required = true)
            @RequestBody Map<String, String> request) {
        String toolName = request.get("tool");
        String param = request.get("param");
        
        log.info("测试工具调用: tool={}, param={}", toolName, param);
        
        String result = "";
        if ("queryDictionary".equals(toolName)) {
            result = chatService.testToolQueryDictionary(param);
        } else if ("calculateExposure".equals(toolName)) {
            result = chatService.testToolCalculateExposure(param, 100);
        } else if ("explainExposureTriangle".equals(toolName)) {
            result = chatService.testToolExplainExposureTriangle();
        } else {
            result = "未知工具: " + toolName;
        }
        
        return ApiResponse.success(result);
    }
}