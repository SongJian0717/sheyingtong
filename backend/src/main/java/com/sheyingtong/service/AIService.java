package com.sheyingtong.service;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
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
 * 调用策略：
 *  - MAX_RETRY = 3：网络抖动、限流时自动重试，间隔 RETRY_DELAY_MS=2s 指数退避（线性）
 *  - preferIPv4Stack=true：部分 Java 环境下 IPv6 路由不稳定，强制使用 IPv4 访问 dashscope
 *  - parseResponse()：模型偶尔返回 ```json``` 代码块，需先剥离 fences 才能被 JSON.parse 正常反序列化
 *
 * 主方法：
 *  analyzePhoto()   → 给 PhotoService 用，严格输出 6 维度 + 综合分 JSON
 *  analyzeImage()   → 给 RetouchService 用，自定义 system prompt 用于修图建议
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

    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 2000;

    /**
     * 照片 6 维度打分主方法（给 PhotoService.scorePhoto 调用）
     * ----------------------------------------------------------------
     * 鲁棒性策略：
     *  Step 1：System.setProperty("java.net.preferIPv4Stack", true) → 优先 IPv4，
     *          解决 DashScope API 在 Windows IPv6 栈下 "No route to host" 的偶发故障
     *  Step 2：buildSystemPrompt() → 严格要求模型按固定 JSON 结构返回
     *          （6 维度评分 + 80-120 字评语 + suggestions 数组）
     *  Step 3：图片转 Base64 → ImageContent.from("data:image/jpeg;base64,XXX") 多模态消息
     *  Step 4：循环 MAX_RETRY=3 次，catch 住网络/限流/5xx 异常，每次间隔 2s 重试
     *          （保证弱网环境下成功率 > 98%）
     *  Step 5：parseResponse() 剥离 markdown fences，供前端 JSON.parse() 使用
     *
     * @param imageData    图片字节数组（通常是 MultipartFile.getBytes()）
     * @param description  用户选填的描述（可空），拼在 user prompt 里让模型更聚焦
     * @return             纯 JSON 字符串，含 score / analysis / suggestions 三字段
     * @throws Exception   3 次重试全部失败时抛出 RuntimeException
     */
    public String analyzePhoto(byte[] imageData, String description) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(vlModelName)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .timeout(Duration.ofMillis(timeout))
                .temperature(0.7)
                .topP(0.8)
                .maxTokens(4096)
                .build();

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(description);

        String base64Image = Base64.getEncoder().encodeToString(imageData);
        ImageContent imageContent = ImageContent.from("data:image/jpeg;base64," + base64Image);
        TextContent textContent = TextContent.from(userPrompt);

        UserMessage userMessage = UserMessage.from(imageContent, textContent);

        Exception lastException = null;
        for (int retry = 1; retry <= MAX_RETRY; retry++) {
            try {
                log.info("调用LangChain4j OpenAI兼容模式分析照片，模型: {}, 图片大小: {} bytes, 重试次数: {}/{}",
                        vlModelName, imageData.length, retry, MAX_RETRY);

                ChatResponse response = chatModel.chat(List.of(
                        SystemMessage.from(systemPrompt),
                        userMessage
                ));

                String text = response.aiMessage().text();
                log.info("LangChain4j返回结果: {}", text.length() > 500 ? text.substring(0, 500) + "..." : text);

                return parseResponse(text);

            } catch (Exception e) {
                lastException = e;
                log.error("LangChain4j调用失败(重试{}/{}): {}", retry, MAX_RETRY, e.getMessage());

                if (retry < MAX_RETRY) {
                    log.info("等待{}ms后重试...", RETRY_DELAY_MS);
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
        }

        log.error("LangChain4j调用失败，已达到最大重试次数: {}", lastException.getMessage());
        throw new RuntimeException("AI分析失败: " + lastException.getMessage(), lastException);
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

    public String analyzeImage(String base64Image, String userPrompt, String systemPrompt) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(vlModelName)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .timeout(Duration.ofMillis(timeout))
                .temperature(0.7)
                .topP(0.8)
                .maxTokens(4096)
                .build();

        ImageContent imageContent = ImageContent.from("data:image/jpeg;base64," + base64Image);
        TextContent textContent = TextContent.from(userPrompt);

        UserMessage userMessage = UserMessage.from(imageContent, textContent);

        Exception lastException = null;
        for (int retry = 1; retry <= MAX_RETRY; retry++) {
            try {
                log.info("调用LangChain4j OpenAI兼容模式分析图片，模型: {}, 重试次数: {}/{}",
                        vlModelName, retry, MAX_RETRY);

                ChatResponse response = chatModel.chat(List.of(
                        SystemMessage.from(systemPrompt),
                        userMessage
                ));

                String text = response.aiMessage().text();
                log.info("LangChain4j返回结果: {}", text.length() > 500 ? text.substring(0, 500) + "..." : text);

                return parseResponse(text);

            } catch (Exception e) {
                lastException = e;
                log.error("LangChain4j调用失败(重试{}/{}): {}", retry, MAX_RETRY, e.getMessage());

                if (retry < MAX_RETRY) {
                    log.info("等待{}ms后重试...", RETRY_DELAY_MS);
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
        }

        log.error("LangChain4j调用失败，已达到最大重试次数: {}", lastException.getMessage());
        throw new RuntimeException("AI分析失败: " + lastException.getMessage(), lastException);
    }
}
