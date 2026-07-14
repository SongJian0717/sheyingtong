package com.sheyingtong.service;

import com.sheyingtong.entity.ChatMessage;
import com.sheyingtong.entity.ChatSession;
import com.sheyingtong.repository.ChatMessageRepository;
import com.sheyingtong.repository.ChatSessionRepository;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * 聊天服务（核心业务）：整合 LLM + Function Calling + Redis 会话记忆 + RAG 知识库
 * ----------------------------------------------------------------
 * 【架构分层】
 *  非流式 /sendMessage         → PhotographyAssistant（LangChain4j AiServices）
 *                                 自动带工具调用、Redis 记忆、RAG 召回 → 一次性返回完整文本
 *  流式 /message/stream        → StreamingChatModel（独立实例）
 *                                 不带工具/记忆/RAG（为了性能直接 Flux sink.emitNext 推送 token）
 *  多模态 /message/image       → Qwen-VL-Plus 多模态模型
 *                                 图片 Base64 + 用户提示词 → 分析后按 char 粒度逐字输出模拟流式
 *
 * 【工具集成】PhotographyTools 3 个函数：查词典/曝光三角计算/器材查询
 * 【记忆集成】RedisChatMemoryStore → 每个 sessionId 独立记忆，TTL 自动过期
 * 【检索集成】RagContentRetriever → ChromaDB Top-K 相似摄影知识拼接进 prompt
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final PhotographyTools photographyTools;
    private final RedisChatMemoryStore redisChatMemoryStore;
    private final RagContentRetriever ragContentRetriever;

    @Value("${qwen.api-key}")
    private String apiKey;

    @Value("${qwen.model:qwen-turbo}")
    private String modelName;

    @Value("${qwen.timeout:180000}")
    private int timeout;

    private PhotographyAssistant assistant;
    private StreamingChatModel streamingChatModel;

    /**
     * 依赖注入完成后的初始化方法（Spring Bean 生命周期 @PostConstruct）
     * ----------------------------------------------------------------
     * 做两件事：
     *  Step 1：创建两份 LLM 客户端实例（一份同步/一份流式）
     *    - ChatModel            → 给 PhotographyAssistant 用，支持 Function Calling / 记忆 / RAG
     *    - StreamingChatModel   → 给 sendMessageStream 用，低延迟 emit token
     *  Step 2：通过 LangChain4j AiServices.builder() 组装 PhotographyAssistant
     *    - .chatModel()         : 挂载模型实例
     *    - .tools()             : 注册 PhotographyTools 3 个 @Tool（词典查询/曝光计算/曝光三角）
     *    - .chatMemoryProvider(): 注入 RedisChatMemoryStore 提供的 per-session 记忆（带 TTL 自动过期）
     *    - .contentRetriever()  : 注入 RagContentRetriever 做摄影知识 Top-K 召回（增强 RAG）
     *
     * 端点：DashScope OpenAI 兼容模式 https://dashscope.aliyuncs.com/compatible-mode/v1
     * 参数：temperature=0.7（中等创造性）/ topP=0.8 / maxTokens=4096（避免超长回答）
     */
    @PostConstruct
    public void init() {
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .timeout(Duration.ofMillis(timeout))
                .temperature(0.7)
                .topP(0.8)
                .maxTokens(4096)
                .build();

        streamingChatModel = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .timeout(Duration.ofMillis(timeout))
                .temperature(0.7)
                .topP(0.8)
                .maxTokens(4096)
                .build();

        assistant = AiServices.builder(PhotographyAssistant.class)
                .chatModel(chatModel)
                .tools(photographyTools)
                .chatMemoryProvider(redisChatMemoryStore::getChatMemory)
                .contentRetriever(ragContentRetriever)
                .build();

        log.info("PhotographyAssistant初始化完成，已集成工具调用、Redis会话记忆和RAG知识库");
    }

    @Transactional
    public ChatSession createSession(Long userId, String title) {
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .title(title != null && !title.isEmpty() ? title : "新对话")
                .active(true)
                .build();

        session = sessionRepository.save(session);
        log.info("创建聊天会话: userId={}, sessionId={}", userId, session.getId());

        return session;
    }

    @Transactional(readOnly = true)
    public ChatSession getSession(Long sessionId, Long userId) {
        return sessionRepository.findById(sessionId)
                .filter(session -> session.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("会话不存在或无权访问"));
    }

    @Transactional(readOnly = true)
    public List<ChatSession> getSessions(Long userId) {
        return sessionRepository.findByUserIdAndActiveTrueOrderByUpdatedAtDesc(userId);
    }

    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = getSession(sessionId, userId);
        session.setActive(false);
        sessionRepository.save(session);

        messageRepository.deleteBySessionId(sessionId);
        log.info("删除聊天会话: sessionId={}", sessionId);
    }

    @Transactional
    public String sendMessage(Long sessionId, Long userId, String message) {
        ChatSession session = getSession(sessionId, userId);

        saveMessage(sessionId, "user", message, null);

        String response = chatWithModel(sessionId, message);

        saveMessage(sessionId, "assistant", response, null);

        session.setTitle(generateTitle(sessionId));
        sessionRepository.save(session);

        log.info("消息发送完成: sessionId={}, messageLength={}, responseLength={}",
                sessionId, message.length(), response.length());

        return response;
    }

    /**
     * SSE 流式消息核心实现：Flux<Sinks.Many> + 后台线程 + StreamingChatModel
     * ----------------------------------------------------------------
     * 执行流程：
     *  Step 1：校验会话归属 + 持久化用户消息（saveMessage 落 MySQL）
     *  Step 2：创建 reactor Sinks.Many unicast 单播发布器
     *          （onBackpressureBuffer 背压：消费者慢时先缓存，不丢 token）
     *  Step 3：启动独立线程（避免阻塞 HTTP 请求线程）
     *          → 调用 streamingChatModel.chat(prompt, StreamingChatResponseHandler)
     *          → onPartialResponse : 每来一个 token 就 sink.tryEmitNext() 推给前端
     *          → onCompleteResponse: 调用 sink.tryEmitComplete()，并把完整回答落 MySQL
     *          → onError           : sink.tryEmitError(error)，前端 xhr onerror 回调触发
     *  Step 4：立即返回 sink.asFlux()（此时 HTTP 200 已响应，后续 SSE chunk 持续推送）
     *
     * 【降级说明】流式链路不使用 PhotographyAssistant（因为 LangChain4j streaming AiServices 对
     *             Function Calling 支持不稳定），所以流式问答不带工具调用/Redis 记忆/RAG 能力。
     */
    @Transactional
    public Flux<String> sendMessageStream(Long sessionId, Long userId, String message) {
        ChatSession session = getSession(sessionId, userId);

        saveMessage(sessionId, "user", message, null);

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        StringBuilder fullResponse = new StringBuilder();

        String systemPrompt = """
            你是一位专业的摄影AI助手，精通摄影技术、器材推荐、构图技巧等。
            
            你的职责：
            1. 提供专业、实用的摄影建议
            2. 解释摄影术语和概念
            3. 回答用户关于摄影的各种问题
            
            回答要求：
            - 回答要简洁明了，使用中文
            - 使用专业摄影术语，但也要通俗易懂
            """;

        new Thread(() -> {
            try {
                streamingChatModel.chat(systemPrompt + "\n\n用户问题: " + message,
                        new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
                            @Override
                            public void onPartialResponse(String partialResponse) {
                                if (partialResponse != null && !partialResponse.isEmpty()) {
                                    fullResponse.append(partialResponse);
                                    sink.tryEmitNext(partialResponse);
                                }
                            }

                            @Override
                            public void onCompleteResponse(
                                    dev.langchain4j.model.chat.response.ChatResponse completeResponse) {
                                sink.tryEmitComplete();

                                saveMessage(sessionId, "assistant", fullResponse.toString(), null);

                                session.setTitle(generateTitle(sessionId));
                                sessionRepository.save(session);

                                log.info("流式消息发送完成: sessionId={}, messageLength={}, responseLength={}",
                                        sessionId, message.length(), fullResponse.length());
                            }

                            @Override
                            public void onError(Throwable error) {
                                log.error("流式聊天失败: sessionId={}, error={}", sessionId, error.getMessage(), error);
                                sink.tryEmitError(error);
                            }
                        });
            } catch (Exception e) {
                log.error("流式聊天失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
                sink.tryEmitError(e);
            }
        }).start();

        return sink.asFlux();
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Long sessionId, Long userId) {
        getSession(sessionId, userId);
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    private void saveMessage(Long sessionId, String role, String content, String toolResult) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .toolResult(toolResult)
                .build();
        messageRepository.save(message);
    }

    private String chatWithModel(Long sessionId, String userMessage) {
        try {
            log.info("开始聊天: sessionId={}, message={}", sessionId, userMessage);
            String response = assistant.chat(sessionId, userMessage);
            log.info("聊天响应: sessionId={}, responseLength={}, response={}", sessionId, response.length(),
                    response.length() > 200 ? response.substring(0, 200) + "..." : response);
            return response;
        } catch (Exception e) {
            log.error("聊天失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return "抱歉，我现在无法回答您的问题，请稍后再试。错误信息：" + e.getMessage();
        }
    }

    private String generateTitle(Long sessionId) {
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        if (messages.isEmpty()) {
            return "新对话";
        }

        String firstUserMessage = messages.stream()
                .filter(m -> "user".equals(m.getRole()))
                .findFirst()
                .map(ChatMessage::getContent)
                .orElse("新对话");

        if (firstUserMessage.length() <= 20) {
            return firstUserMessage;
        }

        return firstUserMessage.substring(0, 20) + "...";
    }

    @Transactional
    public ChatSession createNewSession(Long userId) {
        return createSession(userId, null);
    }

    /**
     * 多模态（图文）消息接口：用户上传图片 + 提问 → Qwen-VL-Plus → 逐字模拟流式输出
     * ----------------------------------------------------------------
     * 注意：Qwen-VL-Plus 目前官方 Java SDK 不支持真正 StreamingChatModel，
     *       所以先用同步拿到完整回答，再 char 粒度每 30ms emit 一个字符模拟打字机效果。
     * 执行流程：
     *  Step 1：图片 MultipartFile.getBytes() → Base64 编码
     *  Step 2：临时构建 Qwen-VL-Plus ChatModel（vl 模型名和 turbo 不同，每次调用独立实例）
     *  Step 3：构造 LangChain4j UserMessage = ImageContent + TextContent 多模态消息
     *  Step 4：vlModel.chat() 同步一次性拿到完整回答
     *  Step 5：String.toCharArray() 循环，每 30ms 调 sink.tryEmitNext(String.valueOf(c))
     *          → 模拟真实流式打字机效果，提高用户感知体验
     *  Step 6：tryEmitComplete() + saveMessage() 持久化回答
     */
    @Transactional
    public Flux<String> sendMessageWithImage(Long sessionId, Long userId, String content, MultipartFile image) {
        ChatSession session = getSession(sessionId, userId);

        saveMessage(sessionId, "user", content, null);

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        StringBuilder fullResponse = new StringBuilder();

        new Thread(() -> {
            try {
                byte[] imageBytes = image.getBytes();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                
                String systemPrompt = """
                    你是一位专业的摄影AI助手，精通摄影技术、构图技巧、后期处理等。
                    
                    请分析这张照片，并根据用户的问题提供专业的摄影建议。
                    """;
                
                String userPrompt = "用户问题：" + content;

                ChatModel vlModel = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("qwen-vl-plus")
                        .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                        .timeout(Duration.ofMillis(timeout))
                        .temperature(0.7)
                        .topP(0.8)
                        .maxTokens(4096)
                        .build();

                ImageContent imageContent = ImageContent.from("data:image/jpeg;base64," + base64Image);
                TextContent textContent = TextContent.from(userPrompt);
                UserMessage userMessage = UserMessage.from(imageContent, textContent);

                log.info("开始图片分析: sessionId={}, contentLength={}, imageSize={}",
                        sessionId, content.length(), imageBytes.length);

                ChatResponse response = vlModel.chat(List.of(
                        SystemMessage.from(systemPrompt),
                        userMessage
                ));

                String text = response.aiMessage().text();
                log.info("图片分析完成: sessionId={}, responseLength={}", sessionId, text.length());

                char[] chars = text.toCharArray();
                for (char c : chars) {
                    String chunk = String.valueOf(c);
                    fullResponse.append(chunk);
                    sink.tryEmitNext(chunk);
                    Thread.sleep(30);
                }

                sink.tryEmitComplete();

                saveMessage(sessionId, "assistant", fullResponse.toString(), null);

                session.setTitle(generateTitle(sessionId));
                sessionRepository.save(session);

                log.info("图片消息发送完成: sessionId={}, contentLength={}, responseLength={}",
                        sessionId, content.length(), fullResponse.length());

            } catch (IOException e) {
                log.error("读取图片失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
                sink.tryEmitError(e);
            } catch (Exception e) {
                log.error("图片分析失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
                sink.tryEmitError(e);
            }
        }).start();

        return sink.asFlux();
    }

    public String testToolQueryDictionary(String term) {
        return photographyTools.queryDictionary(term);
    }

    public String testToolCalculateExposure(String aperture, Integer iso) {
        return photographyTools.calculateExposure(aperture, iso);
    }

    public String testToolExplainExposureTriangle() {
        return photographyTools.explainExposureTriangle();
    }
}
