package com.sheyingtong.service;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * 多模态 AI 服务：照片 6 维度打分 + 通用图片分析
 * ----------------------------------------------------------------
 * 模型：阿里云 DashScope OpenAI 兼容模式 → Qwen-VL-Plus（视觉语言大模型）
 *
 * 调用策略（异步化改造后）：
 *  - **单次尝试，失败即抛**。重试已经上移到消息层（RabbitMQ 的 RetryInterceptor，
 *    指数退避 + 重试耗尽进死信队列），消费者会重新调用本方法。
 *    改造前这里是 MAX_RETRY=3、固定间隔 2s 的内部重试；如果和消息层的重试叠加，
 *    最坏情况会变成 3×3=9 次 × 180s 超时 ≈ 27 分钟，所以必须只留一层。
 *  - **共享 ChatModel 实例**：改造前每次调用都 new 一个 OpenAiChatModel，
 *    等于每次新建 HTTP 客户端、重建连接池、重新 TLS 握手。现在整个进程复用一个。
 *  - preferIPv4Stack=true：部分 Java 环境下 IPv6 路由不稳定，强制 IPv4 访问 dashscope；
 *    这是全局 JVM 属性，只需在启动时设置一次（原来是每次调用都设，纯属重复的全局副作用）。
 *  - parseResponse()：模型偶尔返回 ```json``` 代码块，需先剥离 fences 才能正常反序列化
 *
 * 主方法：
 *  analyzePhoto()   → 给照片打分任务用，严格输出 6 维度 + 综合分 JSON
 *  analyzeImage()   → 给修图建议任务用，自定义 system prompt

 */
@Service
@Slf4j
public class AIService {

    @Value("${qwen.api-key}")
    private String apiKey;

    @Value("${qwen.vl-model:qwen-vl-plus}")
    private String vlModelName;

    @Value("${qwen.timeout:180000}")
    private int timeout;

    /** 全进程共享的模型实例（延迟创建，见 chatModel()） */
    private volatile ChatModel sharedChatModel;

    /**
     * 全局 JVM 网络属性只需设置一次。
     * 原实现在每次 AI 调用里都重复设置（每次调用两次全局 setProperty），既无意义也是全局副作用。
     */
    @PostConstruct
    void applyNetworkPreferences() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
    }

    /**
     * 获取共享的 ChatModel；没有就创建（双重检查锁）。
     * ----------------------------------------------------------------
     * 刻意做成**延迟创建**而不是在 @PostConstruct 里建：
     * api-key 缺失或配置错误时，问题应该暴露在调用处（能被消费者的兜底逻辑接住），
     * 而不是让整个应用启动失败。这也是项目里 Redis/CacheService 的既有思路。
     */
    private ChatModel chatModel() {
        ChatModel local = sharedChatModel;
        if (local == null) {
            synchronized (this) {
                local = sharedChatModel;
                if (local == null) {
                    local = OpenAiChatModel.builder()
                            .apiKey(apiKey)
                            .modelName(vlModelName)
                            .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                            .timeout(Duration.ofMillis(timeout))
                            .temperature(0.7)
                            .topP(0.8)
                            .maxTokens(4096)
                            .build();
                    sharedChatModel = local;
                    log.info("[ai] 已创建共享 ChatModel: model={}, timeout={}ms（整进程复用，不再每次调用重建连接）",
                            vlModelName, timeout);
                }
            }
        }
        return local;
    }

    /**
     * 照片 6 维度打分主方法（由照片打分消费者调用）
     * ----------------------------------------------------------------
     *  Step 1：buildSystemPrompt() → 严格要求模型按固定 JSON 结构返回
     *          （6 维度评分 + 80-120 字评语 + suggestions 数组）
     *  Step 2：图片转 Base64 → ImageContent.from("data:image/jpeg;base64,XXX") 多模态消息
     *  Step 3：单次调用共享的 ChatModel（失败即抛，重试交给消息层的 RetryInterceptor）
     *  Step 4：parseResponse() 剥离 markdown fences，供 JSON 反序列化使用
     *
     * @param imageData    图片字节数组（已压缩）
     * @param description  用户选填的描述（可空），拼在 user prompt 里让模型更聚焦
     * @return             纯 JSON 字符串，含 score / analysis / suggestions 三字段
     * @throws Exception   调用失败时抛出，由消费者决定重试（抛给 MQ 层）还是降级
     */
    public String analyzePhoto(byte[] imageData, String description) throws Exception {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(description);

        String base64Image = Base64.getEncoder().encodeToString(imageData);
        ImageContent imageContent = ImageContent.from("data:image/jpeg;base64," + base64Image);
        TextContent textContent = TextContent.from(userPrompt);

        UserMessage userMessage = UserMessage.from(imageContent, textContent);

        try {
            log.info("调用 Qwen-VL 分析照片，模型: {}, 图片大小: {} bytes", vlModelName, imageData.length);

            ChatResponse response = chatModel().chat(List.of(
                    SystemMessage.from(systemPrompt),
                    userMessage
            ));

            String text = response.aiMessage().text();
            log.info("Qwen-VL 返回结果: {}", text.length() > 500 ? text.substring(0, 500) + "..." : text);

            return parseResponse(text);
        } catch (Exception e) {
            log.error("Qwen-VL 调用失败: {}", e.getMessage());
            throw new RuntimeException("AI分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 剥离 LLM 返回的 markdown fences（```json ... ```），保证 JSON.parse 不报错
     * ----------------------------------------------------------------
     * 现象：Qwen/VL 模型有时会自作主张把 JSON 用 markdown 代码块包起来，
     *       导致前端 JSON.parse() 报 Unexpected token '`'。
     * 处理：
     *  - 开头 ```json / ``` → 去掉前 3/7 个字符
     *  - 结尾 ```           → 去掉后 3 个字符
     *  - 最后 trim() 清除前后空格/换行
     */
    private String parseResponse(String response) {
        String text = response.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }

    private String buildSystemPrompt() {
        return """
                你是专业摄影评分专家，请根据上传的照片进行分析。

                评分维度：
                1. 构图(composition)：画面布局、主体位置、构图法则
                2. 光线(lighting)：曝光准确性、明暗对比、光影层次
                3. 色彩(color)：色彩搭配、白平衡、饱和度
                4. 对焦(focus)：清晰度、景深控制、焦点准确性
                5. 内容(content)：趣味性、内涵、叙事能力、主题表达
                6. 建议(suggestion)：具体修改建议及原因（完美时为"这张照片非常出色，无需修改！"）

                返回JSON格式：
                {
                    "score": 综合评分(60-100),
                    "analysis": {
                        "composition": {"score": 分数, "comment": "评语80-120字"},
                        "lighting": {"score": 分数, "comment": "评语80-120字"},
                        "color": {"score": 分数, "comment": "评语80-120字"},
                        "focus": {"score": 分数, "comment": "评语80-120字"},
                        "content": {"score": 分数, "comment": "评语80-120字"},
                        "suggestion": {"score": 分数, "comment": "评语80-120字"}
                    },
                    "suggestions": ["改进建议1（60-80字）", "改进建议2（60-80字）"]
                }

                要求：
                - 评语80-120字，结合照片细节分析
                - 使用专业摄影术语，通俗易懂
                - 纯JSON格式，不要markdown
                - suggestions字段为字符串数组

                评分标准：
                - 构图、光线、色彩、对焦：60-100
                - 内容：60-100
                - 建议：完美100分，有改进空间80-95分，有问题60-79分
                """;
    }

    private String buildUserPrompt(String description) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("请分析这张照片，每个维度评语80-120字。\n");

        if (description != null && !description.isEmpty()) {
            userPrompt.append("用户描述：").append(description).append("\n");
        }

        return userPrompt.toString();
    }

    /**
     * 通用图片分析（由修图建议消费者调用，自定义 system/user prompt）。
     * 与 analyzePhoto 同样是单次尝试、共享 ChatModel，重试交给消息层。
     *
     * @param base64Image 已压缩图片的 Base64（不含 data: 前缀）
     */
    public String analyzeImage(String base64Image, String userPrompt, String systemPrompt) throws Exception {
        ImageContent imageContent = ImageContent.from("data:image/jpeg;base64," + base64Image);
        TextContent textContent = TextContent.from(userPrompt);

        UserMessage userMessage = UserMessage.from(imageContent, textContent);

        try {
            log.info("调用 Qwen-VL 分析图片（修图建议），模型: {}", vlModelName);

            ChatResponse response = chatModel().chat(List.of(
                    SystemMessage.from(systemPrompt),
                    userMessage
            ));

            String text = response.aiMessage().text();
            log.info("Qwen-VL 返回结果: {}", text.length() > 500 ? text.substring(0, 500) + "..." : text);

            return parseResponse(text);
        } catch (Exception e) {
            log.error("Qwen-VL 调用失败（修图建议）: {}", e.getMessage());
            throw new RuntimeException("AI分析失败: " + e.getMessage(), e);
        }
    }
}
