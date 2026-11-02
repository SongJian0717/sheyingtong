package com.sheyingtong.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 较早对话的滚动摘要 —— 记忆被裁剪时的"兜底记忆"。
 * ----------------------------------------------------------------
 * 触发：只有 RedisChatMemoryStore.trim() 真的丢了消息、且攒够一批才发 MemoryEvictedEvent，
 *      不是每轮都跑（会话满了以后每轮只丢 1 条，逐条摘会让长会话每轮多一次模型调用）。
 * 线程：@Async + @EventListener，跑在 app-async 线程池里 —— 不能在 add() 里同步调模型，
 *      那个方法在模型回调线程上，会拖慢 SSE 的 [DONE]。
 * 落库：摘要存在 sheyingtong:chat:summary:{sessionId}，与被裁剪的记忆同 TTL（24h）；
 *      读取侧由 SanitizingChatMemory 拼到 system prompt 前面。
 * 模型：用主模型（qwen-turbo 在本账号被限流实测 429，不折腾），输出上限 400，成本可忽略。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSummaryService {

    /** 交给摘要模型的素材上限（字符），防止素材本身爆掉 */
    private static final int MAX_DIALOGUE_CHARS = 6000;

    /** 摘要目标：150~300 字（写进 prompt，也是人工核对时的标准） */
    private static final String PROMPT_TEMPLATE = """
            把下面这段较早的对话压缩成中文摘要，供后续对话继续使用。

            %s【新丢弃的对话】
            %s

            要求：
            - 150~300 字，一段连贯的话，不要分点、不要加小标题
            - 必须保留：用户的目标与已确认的结论、涉及的器材/场景/参数、我给出的关键建议、尚未解决的问题
            - 不要写"用户问了一个问题"这类空话，直接写事实
            - 只输出摘要正文，不要任何解释或前后缀
            """;

    private final RedisChatMemoryStore chatMemoryStore;

    @Value("${qwen.api-key}")
    private String apiKey;

    @Value("${qwen.model:qwen-plus}")
    private String modelName;

    @Value("${qwen.timeout:180000}")
    private int timeout;

    private ChatModel summarizer;

    @PostConstruct
    void init() {
        summarizer = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .timeout(Duration.ofMillis(timeout))
                .temperature(0.3)
                .maxTokens(400)
                .build();
        log.info("摘要模型初始化完成: model={}（仅在记忆被裁剪时调用）", modelName);
    }

    @Async
    @EventListener
    public void onMemoryEvicted(MemoryEvictedEvent event) {
        try {
            String dialogue = render(event.getEvictedMessages());
            if (dialogue.isBlank()) {
                return;
            }
            String previous = chatMemoryStore.getSummary(event.getSessionId());
            String summary = summarize(previous, dialogue);
            if (summary == null || summary.isBlank()) {
                log.warn("摘要模型返回空内容，保留原摘要: sessionId={}", event.getSessionId());
                return;
            }
            chatMemoryStore.saveSummary(event.getSessionId(), summary.trim());
            log.info("会话摘要已更新: sessionId={}, 长度={}", event.getSessionId(), summary.trim().length());
        } catch (Exception e) {
            // 摘要只是"锦上添花"，失败绝不影响正常对话
            log.warn("生成会话摘要失败（忽略）: sessionId={}, error={}", event.getSessionId(), e.getMessage());
        }
    }

    /** 把被丢掉的一批消息渲染成纯文本；图片只留占位，不重复消耗 token */
    String render(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage message : messages) {
            String role;
            String text;
            if (message instanceof UserMessage userMessage) {
                role = "用户";
                text = renderUser(userMessage);
            } else if (message instanceof AiMessage aiMessage) {
                role = "助手";
                text = aiMessage.text();
            } else if (message instanceof ToolExecutionResultMessage toolResult) {
                role = "工具";
                text = toolResult.text();
            } else {
                continue;
            }
            if (text == null || text.isBlank()) {
                continue;
            }
            sb.append(role).append(": ")
                    .append(text.replaceAll("\\s+", " ").trim())
                    .append('\n');
        }
        return sb.length() > MAX_DIALOGUE_CHARS ? sb.substring(0, MAX_DIALOGUE_CHARS) : sb.toString();
    }

    private static String renderUser(UserMessage userMessage) {
        StringBuilder sb = new StringBuilder();
        for (Content content : userMessage.contents()) {
            if (content instanceof TextContent textContent) {
                sb.append(textContent.text());
            } else if (content instanceof ImageContent) {
                sb.append("[用户上传了一张图片]");
            }
        }
        return sb.toString();
    }

    String summarize(String previousSummary, String dialogue) {
        String previousBlock = (previousSummary == null || previousSummary.isBlank())
                ? ""
                : "【已有摘要（在其基础上续写并去重，不要重复已有内容）】\n" + previousSummary + "\n\n";
        String prompt = PROMPT_TEMPLATE.formatted(previousBlock, dialogue);
        String answer = summarizer.chat(prompt);
        return answer == null ? null : answer.replace("\r", "").trim();
    }
}