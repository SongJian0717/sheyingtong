package com.sheyingtong.service;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * 多模态（看图）助手：独立接口，因为它绑定另一个模型（qwen3-vl-plus），
 * 且 system prompt 与文字助手完全不同（看图分析 vs 工具路由）。
 *
 * 三个入口对应三种情形：
 *   · chatWithImage    —— 用户本轮上传了新图片（/message/image）
 *   · chatText         —— 带图会话里的纯文字追问：图片由记忆读侧补水提供（见 SanitizingChatMemory）
 *   · chatTextSync     —— 同上，供非流式 /sendMessage 使用
 *
 * ⚠️ chatWithImage 的两个参数都必须标 @UserMessage —— 这是 AiServices 认定"内容参数"的唯一方式（均实测）：
 *   · 只标 @V   → 被当成模板变量，图片根本不会拼进消息
 *   · 完全不标  → 过不了参数校验，抛 IllegalConfigurationException
 */
public interface PhotographyVisionAssistant {

    /**
     * 放在接口常量里，三个方法共用：@SystemMessage 的值必须是编译期常量，文本块常量正好满足。
     */
    String SYSTEM_MESSAGE = """
        你是一位专业的摄影AI助手，擅长看图分析：构图、用光、色彩、曝光与后期方向。

        要求：
        - 先客观描述图片里真实可见的内容，再针对用户的问题给出建议
        - 用户没有明确提问时，主动点评这张照片最值得改进的一两点
        - 回答具体、可操作，使用中文，不要空泛套话
        - 只依据图片里真实可见的内容作答，不要编造画面里没有的元素

        关于历史图片：对话历史里可能保留了之前上传的图片，也可能只剩下"[图片：…]"这样的文字占位。
        用户追问"刚才那张图 / 上一张图"时，优先依据历史里能看到的图片或已有的文字描述回答，
        不要回答"我看不到之前的图片"；确实没有任何依据时，才说明需要重新上传。
        """;

    @SystemMessage(SYSTEM_MESSAGE)
    TokenStream chatWithImage(@MemoryId Long sessionId,
                              @UserMessage String question,
                              @UserMessage Content image);

    @SystemMessage(SYSTEM_MESSAGE)
    TokenStream chatText(@MemoryId Long sessionId, @UserMessage String question);

    @SystemMessage(SYSTEM_MESSAGE)
    String chatTextSync(@MemoryId Long sessionId, @UserMessage String question);
}