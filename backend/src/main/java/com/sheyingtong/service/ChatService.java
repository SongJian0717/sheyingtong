package com.sheyingtong.service;

import com.sheyingtong.entity.ChatMessage;
import com.sheyingtong.entity.ChatSession;
import com.sheyingtong.repository.ChatMessageRepository;
import com.sheyingtong.repository.ChatSessionRepository;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
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
 *  非流式 /sendMessage         → assistant.chat()（LangChain4j AiServices）
 *                                 工具调用 + Redis 记忆 + RAG 召回 → 一次性返回完整文本
 *  流式 /message/stream        → assistant.chatStream()（同一个 AiServices 实例，TokenStream）
 *                                 同一套工具 / 记忆 / RAG，token 经 Flux sink 直推前端
 *                                 ⚠️ 若记忆最后 RECENT_IMAGE_WINDOW 条消息内有图片（刚发过图的会话），
 *                                    本轮改走 visionAssistant.chatText()，让模型看到记忆里补水的那张图
 *  多模态 /message/image       → visionAssistant.chatWithImage()（独立 VL 模型实例，真流式）
 *                                 图片 Base64 随当轮消息发出，并保留在 Redis 记忆里
 *
 * 【图片记忆】写入时保留图片 base64（否则追问时无图可用）；读取时按链路裁剪：
 *            文字链路 → 全部换成文字占位（qwen-plus 收 image 内容会直接报错）
 *            VL 链路  → 只保留最近 KEEP_IMAGES 张（历史图每轮都要重新上传，实测每张约 +0.9s）
 *
 * 【工具集成】PhotographyTools 6 个 @Tool：查词典/器材搜索/预算器材/曝光计算/摄影技巧/曝光三角
 * 【记忆集成】RedisChatMemoryStore → 每个 sessionId 独立记忆，TTL 自动过期
 *            SanitizingChatMemory → 只保留最后一份 SystemMessage + 按链路裁剪图片
 * 【检索集成】RagContentRetriever → ChromaDB Top-K 相似摄影知识拼接进 prompt
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    /**
     * 读取侧保留最近几张**历史**图片的 base64（更早的换成文字占位）。
     * 设 1 覆盖"追问上一张图"这个主场景，同时把每轮请求体压在"一张图"的量级 ——
     * 实测单个 1.5MB 图（base64 1.96MB）：1 张 2.75s / 3 张 4.47s / 6 张 7.46s，每张约 +0.9s 且每轮重复计费。
     * ⚠️ 只约束历史：上传新图的那一轮，本轮图片是在请求组装之后才写进记忆的
     *    （实测顺序 add(SYSTEM) → messages() → add(USER)），框架会把它追加进请求，
     *    所以那一轮实际带 KEEP_IMAGES + 1 张（新图 + 最近的历史图，顺带支持"和上一张比"）。
     * 想全部保留就调大；想让模型完全看不到历史图就设 0。
     */
    private static final int KEEP_IMAGES = 1;

    /**
     * 记忆最后 N 条消息内有图片时，纯文字追问也交给 VL 链路。
     * 必要性：前端只把图片挂在发送那一条消息上（发完即清空），之后都是纯文字；
     * 若仍走文字链路（qwen-plus 非 VL 模型），模型根本收不到图，只能回答"我看不到之前的图片"。
     * 窗口取 6 ≈ 图片轮之后再追问 2 轮仍然能看图，之后再问就回到普通文字链路。
     */
    private static final int RECENT_IMAGE_WINDOW = 6;

    /**
     * SSE 显式结束标记（OpenAI/Anthropic 同类约定）。
     * 前端见到它立刻收尾，**不再依赖"连接被关闭"来结束流** ——
     * 实测 Vite dev 代理会吞掉上游的结束信号（直连 8080 正常在 0.4s 收尾，
     * 经 5173 代理则连接永不关闭），前端 xhr.onload 不触发，界面就永远停在"AI正在思考"。
     */
    private static final String SSE_DONE = "[DONE]";

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final PhotographyTools photographyTools;
    private final RedisChatMemoryStore redisChatMemoryStore;
    private final RagContentRetriever ragContentRetriever;

    @Value("${qwen.api-key}")
    private String apiKey;

    /** 默认模型与 application.yml 的 qwen.model 保持一致（qwen-turbo 在本账号被限流，见该处注释） */
    @Value("${qwen.model:qwen-plus}")
    private String modelName;

    /** VL 模型名（图片链路）。qwen-vl-plus / qwen-vl-max 已下线，实测 qwen3-vl-plus 可用且支持真流式 */
    @Value("${qwen.vl-model:qwen3-vl-plus}")
    private String vlModelName;

    @Value("${qwen.timeout:180000}")
    private int timeout;

    /** 文字链路：同步 + 流式共用同一个 AiServices 实例（同时挂 chatModel 与 streamingChatModel） */
    private PhotographyAssistant assistant;

    /** 图片链路：独立的 VL 模型 AiServices 实例；记忆与文字链路共享同一份底层存储 */
    private PhotographyVisionAssistant visionAssistant;

    private StreamingChatModel streamingChatModel;

    /**
     * 依赖注入完成后的初始化方法（Spring Bean 生命周期 @PostConstruct）
     * ----------------------------------------------------------------
     * 做两件事：
     *  Step 1：创建两份 LLM 客户端实例（一份同步/一份流式）
     *    - ChatModel            → 供 assistant.chat() 同步链路使用
     *    - StreamingChatModel   → 供 assistant.chatStream() 流式链路使用
     *  Step 2：组装唯一的 assistant 实例，两条链路共用
     *    - .chatModel()         : 见上
     *    - .streamingChatModel(): 见上
     *    - .tools()             : 注册 PhotographyTools 6 个 @Tool
     *    - .chatMemoryProvider(): 注入 RedisChatMemoryStore 的 per-session 记忆（TTL 自动过期），
     *                             外层包 SanitizingChatMemory（只保留最后一份 SystemMessage + 按链路裁剪图片）
     *    - .contentRetriever()  : 注入 RagContentRetriever 做摄影知识 Top-K 召回（增强 RAG）
     *
     * 【真机验证结论】qwen 的 OpenAI 兼容流式接口会返回 tool_calls，LangChain4j 能执行 @Tool、
     * 回灌工具结果并继续生成（实测 qwen-plus：3 次模型调用、2 次工具执行、参数绑定正确）。
     * 所以流式链路与同步链路装配完全一致，不再需要"流式降级"这类特例。
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

        // 单实例同时挂 chatModel 与 streamingChatModel：/message 与 /message/stream 共用
        // 同一套 @Tool、Redis 记忆与 RAG 召回，不存在两条链路配置漂移的可能
        // 记忆读取侧策略 TEXT_ONLY：图片一律换成文字占位（qwen-plus 收 image 内容会直接报错）
        assistant = AiServices.builder(PhotographyAssistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .tools(photographyTools)
                .chatMemoryProvider(sessionId -> new SanitizingChatMemory(
                        redisChatMemoryStore.getChatMemory(sessionId),
                        SanitizingChatMemory.ImagePolicy.TEXT_ONLY, 0,
                        () -> redisChatMemoryStore.getSummary(sessionId)))
                .contentRetriever(ragContentRetriever)
                .build();

        log.info("AiServices 初始化完成：文字链路（工具 + Redis 记忆 + RAG）");

        // 图片链路：独立的 VL 模型实例（qwen3-vl-plus 支持真流式 + 看图），
        // 记忆复用同一份底层存储 → 图文在同一会话里上下文连续；
        // 读取侧策略 KEEP_RECENT：只把最近 KEEP_IMAGES 张图的 base64 递给模型，更早的换成文字占位
        ChatModel vlChatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(vlModelName)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .timeout(Duration.ofMillis(timeout))
                .temperature(0.7)
                .topP(0.8)
                .maxTokens(4096)
                .build();

        StreamingChatModel vlStreamingModel = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(vlModelName)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .timeout(Duration.ofMillis(timeout))
                .temperature(0.7)
                .topP(0.8)
                .maxTokens(4096)
                .build();

        visionAssistant = AiServices.builder(PhotographyVisionAssistant.class)
                .chatModel(vlChatModel)
                .streamingChatModel(vlStreamingModel)
                .chatMemoryProvider(sessionId -> new SanitizingChatMemory(
                        redisChatMemoryStore.getChatMemory(sessionId),
                        SanitizingChatMemory.ImagePolicy.KEEP_RECENT, KEEP_IMAGES,
                        () -> redisChatMemoryStore.getSummary(sessionId)))
                .build();

        log.info("视觉助手初始化完成：VL 模型 = {}，读取侧保留最近 {} 张图，带图会话的文字追问窗口 = {} 条消息",
                vlModelName, KEEP_IMAGES, RECENT_IMAGE_WINDOW);
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
     * SSE 流式消息核心实现：Flux<Sinks.Many> + 后台线程 + assistant.chatStream（AiServices / TokenStream）
     * ----------------------------------------------------------------
     * 执行流程：
     *  Step 1：校验会话归属 + 持久化用户消息（saveMessage 落 MySQL）
     *  Step 2：创建 reactor Sinks.Many unicast 单播发布器
     *          （onBackpressureBuffer 背压：消费者慢时先缓存，不丢 token）
     *  Step 3：启动独立线程（避免阻塞 HTTP 请求线程）
     *          → 调用 assistant.chatStream(sessionId, message)
     *          → onPartialResponse : 每来一个 token 就 sink.tryEmitNext() 推给前端
     *          → onCompleteResponse: 调用 sink.tryEmitComplete()，并把完整回答落 MySQL
     *          → onError           : sink.tryEmitError(error)，前端 xhr onerror 回调触发
     *  Step 4：立即返回 sink.asFlux()（此时 HTTP 200 已响应，后续 SSE chunk 持续推送）
     *
     * 【上下文装配】与同步链路共用同一个 AiServices 实例：@Tool、Redis 记忆、RAG 召回完全一致，
     *             记忆写回由框架自动完成（既不需要手工 chatMemory.add，也不需要手工拼 prompt）。
     *
     * ⚠️ 下面这个 @Transactional 覆盖不到模型回调：回调跑在模型自己的线程上，
     *    而方法返回时事务就已提交（此处事务实际只罩住 getSession 与 saveMessage(user)）。
     */
    @Transactional
    public Flux<String> sendMessageStream(Long sessionId, Long userId, String message) {
        ChatSession session = getSession(sessionId, userId);

        saveMessage(sessionId, "user", message, null);

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        StringBuilder fullResponse = new StringBuilder();

        // 带图会话里的纯文字追问交给 VL 链路：图片不在本轮消息里，而是由记忆读取侧补水提供
        boolean followUpOnImage = hasRecentImage(sessionId);
        log.info("流式链路选择: sessionId={}, 走 {} 链路", sessionId, followUpOnImage ? "VL（追问上一张图）" : "文字");

        new Thread(() -> {
            try {
                TokenStream stream = followUpOnImage
                        ? visionAssistant.chatText(sessionId, message)
                        : assistant.chatStream(sessionId, message);

                stream
                        .onPartialResponse(partialResponse -> {
                            if (partialResponse != null && !partialResponse.isEmpty()) {
                                fullResponse.append(partialResponse);
                                sink.tryEmitNext(partialResponse);
                            }
                        })
                        .onCompleteResponse(completeResponse -> {
                            // 先发结束标记再 complete：前端据此收尾，不依赖连接关闭
                            sink.tryEmitNext(SSE_DONE);
                            sink.tryEmitComplete();

                            saveMessage(sessionId, "assistant", fullResponse.toString(), null);

                            session.setTitle(generateTitle(sessionId));
                            sessionRepository.save(session);

                            log.info("流式消息发送完成: sessionId={}, messageLength={}, responseLength={}",
                                    sessionId, message.length(), fullResponse.length());
                        })
                        .onError(error -> {
                            log.error("流式聊天失败: sessionId={}, error={}", sessionId, error.getMessage(), error);
                            // 同样补一个结束标记：否则前端会一直卡在"正在思考"
                            sink.tryEmitNext(SSE_DONE);
                            sink.tryEmitError(error);
                        })
                        // start() 必须放在最后：前面的调用只是在注册回调，start() 才真正发起请求
                        .start();
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

    /**
     * 记忆里是否有"还新鲜"的图片 —— 决定纯文字追问走哪条链路。
     * ----------------------------------------------------------------
     * 前端只把图片挂在发送那一条消息上（发完即清空 uploadedImage），后续追问都是纯文字。
     * 若这些追问仍走文字链路（qwen-plus，非 VL 模型），模型收不到图，只能回答
     * "我无法查看或回忆之前对话中发送的图片"（实测原话）—— 图片记忆等于白留。
     * 所以在最后 RECENT_IMAGE_WINDOW 条消息里找到图片时，把这一轮交给 VL 链路，
     * 图片由 SanitizingChatMemory 的读取侧补水提供。
     */
    private boolean hasRecentImage(Long sessionId) {
        List<dev.langchain4j.data.message.ChatMessage> stored =
                redisChatMemoryStore.getChatMemory(sessionId).messages();
        int from = Math.max(0, stored.size() - RECENT_IMAGE_WINDOW);
        for (int i = stored.size() - 1; i >= from; i--) {
            if (stored.get(i) instanceof UserMessage userMessage
                    && userMessage.contents().stream().anyMatch(content -> content instanceof ImageContent)) {
                return true;
            }
        }
        return false;
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
            // 与流式链路同一套判断：带图会话里的纯文字追问交给 VL 链路，避免两条链路行为不一致
            boolean followUpOnImage = hasRecentImage(sessionId);
            log.info("同步链路选择: sessionId={}, 走 {} 链路, message={}",
                    sessionId, followUpOnImage ? "VL（追问上一张图）" : "文字", userMessage);
            String response = followUpOnImage
                    ? visionAssistant.chatTextSync(sessionId, userMessage)
                    : assistant.chat(sessionId, userMessage);
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
     * 多模态（图文）消息接口：用户上传图片 + 提问 → qwen3-vl-plus（真流式）
     * ----------------------------------------------------------------
     * 与文字链路同构：走 AiServices，Redis 记忆自动读写、图文上下文连续。
     * 两点差别：
     *   · 独立 VL 模型实例（qwen3-vl-plus）—— 一个 AiServices 只能挂一套模型
     *   · 记忆读取侧保留最近 KEEP_IMAGES 张图（文字链路那边是全换占位）
     * 执行流程：
     *  Step 1：图片 MultipartFile.getBytes() → Base64 data URL
     *  Step 2：启动独立线程，调用 visionAssistant.chatWithImage(sessionId, 问题, 图片)
     *  Step 3：onPartialResponse → sink.tryEmitNext()（真流式，不再是 30ms/字的模拟）
     *  Step 4：onCompleteResponse → tryEmitComplete() + 落库 + 更新标题
     *
     * ⚠️ @Transactional 覆盖不到模型回调所在的线程（方法返回时事务就已提交）。
     *    图片会留在记忆里供追问使用，超出 KEEP_IMAGES 的旧图在读取时换成文字占位。
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
                Content imageContent = ImageContent.from(
                        "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes));

                log.info("开始图片分析: sessionId={}, contentLength={}, imageSize={}",
                        sessionId, content.length(), imageBytes.length);

                visionAssistant.chatWithImage(sessionId, content, imageContent)
                        .onPartialResponse(partialResponse -> {
                            if (partialResponse != null && !partialResponse.isEmpty()) {
                                fullResponse.append(partialResponse);
                                sink.tryEmitNext(partialResponse);
                            }
                        })
                        .onCompleteResponse(completeResponse -> {
                            sink.tryEmitNext(SSE_DONE);
                            sink.tryEmitComplete();

                            saveMessage(sessionId, "assistant", fullResponse.toString(), null);

                            session.setTitle(generateTitle(sessionId));
                            sessionRepository.save(session);

                            log.info("图片消息发送完成: sessionId={}, contentLength={}, responseLength={}",
                                    sessionId, content.length(), fullResponse.length());
                        })
                        .onError(error -> {
                            log.error("图片分析失败: sessionId={}, error={}", sessionId, error.getMessage(), error);
                            sink.tryEmitNext(SSE_DONE);
                            sink.tryEmitError(error);
                        })
                        // start() 必须放在最后：前面的调用只是在注册回调
                        .start();

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
