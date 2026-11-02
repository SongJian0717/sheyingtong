package com.sheyingtong.service;

import dev.langchain4j.data.message.ChatMessage;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 记忆被裁剪时发布的事件：带上"被丢掉的这段对话"，交给摘要服务压缩成摘要。
 *
 * 为什么用事件而不是直接调用：
 *   ① RedisChatMemoryStore 不能在 add() 里同步调模型 —— 那个方法跑在模型回调线程上，
 *      会拖慢 onCompleteResponse，也就是 SSE 的 [DONE]；
 *   ② 存储层直接注入 ChatSummaryService 会形成循环依赖（摘要服务要反过来写摘要进存储）。
 * 事件 + @Async 监听最干净。
 *
 * 注意：不是"丢一条发一次"。发布方攒够 MIN_EVICTED_BATCH 条才发一批
 * （会话满了以后每轮只丢 1 条，逐条发会让长会话变成每轮多一次模型调用）。
 */
public class MemoryEvictedEvent extends ApplicationEvent {

    private final String sessionId;
    private final List<ChatMessage> evictedMessages;

    public MemoryEvictedEvent(String sessionId, List<ChatMessage> evictedMessages) {
        super(sessionId);
        this.sessionId = sessionId;
        this.evictedMessages = evictedMessages;
    }

    /** 会话标识（与 Redis key 的 sessionId 一致） */
    public String getSessionId() {
        return sessionId;
    }

    /** 一批被丢掉的消息（不含被清掉的旧 SystemMessage） */
    public List<ChatMessage> getEvictedMessages() {
        return evictedMessages;
    }
}