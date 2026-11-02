package com.sheyingtong.service;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.TokenCountEstimator;

import java.util.List;

/**
 * Qwen 系的 token 估算器（用于"记忆预算"，不追求精确）。
 * ----------------------------------------------------------------
 * 为什么不用 LangChain4j 自带的 OpenAiTokenCountEstimator（实测）：
 *    new OpenAiTokenCountEstimator("gpt-4o")        → OK
 *    new OpenAiTokenCountEstimator("qwen-plus")     → IllegalArgumentException: Model 'qwen-plus' is unknown to jtokkit
 *    new OpenAiTokenCountEstimator("qwen3-vl-plus") → 同上
 * 它只认 OpenAI/jtokkit 内置的模型名，qwen 必须自己估。同一句中文它给 23 tokens，
 * 下面这套公式给 29（汉字 1、全角标点 1、英文 4 字符 1），**偏保守**，用来卡预算正合适。
 */
public class QwenTokenEstimator implements TokenCountEstimator {

    /**
     * 图片 token：qwen-vl 按像素计，约 w×h/784。
     * 1832×1832 ≈ 4279，取 4300；再大的图会被模型侧缩放，这个常量当上限用。
     */
    private static final int IMAGE_TOKENS = 4300;

    /** 每条消息的 role/分隔符结构开销 */
    private static final int PER_MESSAGE_OVERHEAD = 4;

    @Override
    public int estimateTokenCountInText(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjk = 0;
        int ascii = 0;
        int other = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCjk(c)) {
                cjk++;
            } else if (c < 128) {
                ascii++;
            } else {
                other++;
            }
        }
        // 汉字≈1 token/字，全角标点≈1，英文数字≈4 字符/token
        return cjk + other + (int) Math.ceil(ascii / 4.0);
    }

    @Override
    public int estimateTokenCountInMessage(ChatMessage message) {
        if (message == null) {
            return 0;
        }
        int total = PER_MESSAGE_OVERHEAD;
        if (message instanceof UserMessage userMessage) {
            for (Content content : userMessage.contents()) {
                if (content instanceof TextContent textContent) {
                    total += estimateTokenCountInText(textContent.text());
                } else if (content instanceof ImageContent) {
                    total += IMAGE_TOKENS;
                }
            }
        } else if (message instanceof AiMessage aiMessage) {
            total += estimateTokenCountInText(aiMessage.text());
            List<ToolExecutionRequest> requests = aiMessage.toolExecutionRequests();
            if (requests != null) {
                for (ToolExecutionRequest request : requests) {
                    total += estimateTokenCountInText(request.arguments());
                }
            }
        } else if (message instanceof SystemMessage systemMessage) {
            total += estimateTokenCountInText(systemMessage.text());
        } else if (message instanceof ToolExecutionResultMessage toolResult) {
            total += estimateTokenCountInText(toolResult.text());
        } else {
            total += estimateTokenCountInText(message.toString());
        }
        return total;
    }

    @Override
    public int estimateTokenCountInMessages(Iterable<ChatMessage> messages) {
        int total = 0;
        for (ChatMessage message : messages) {
            total += estimateTokenCountInMessage(message);
        }
        return total;
    }

    private static boolean isCjk(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF)      // 基本汉字
                || (c >= 0x3400 && c <= 0x4DBF)  // 扩展 A
                || (c >= 0x3000 && c <= 0x303F)  // 中文标点
                || (c >= 0xFF00 && c <= 0xFFEF); // 全角字符
    }
}