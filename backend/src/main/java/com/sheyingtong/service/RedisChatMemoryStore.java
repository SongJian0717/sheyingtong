package com.sheyingtong.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.memory.ChatMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemoryStore {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "sheyingtong:chat:memory:";
    private static final long DEFAULT_TTL_HOURS = 24;
    private static final int MAX_MESSAGES = 20;

    private final Map<String, ChatMemory> memoryCache = new ConcurrentHashMap<>();

    public ChatMemory getChatMemory(Object sessionId) {
        String key = String.valueOf(sessionId);
        return memoryCache.computeIfAbsent(key, k -> createChatMemory(k));
    }

    private ChatMemory createChatMemory(String sessionId) {
        return new ChatMemory() {
            @Override
            public Object id() {
                return sessionId;
            }

            @Override
            public void add(ChatMessage message) {
                List<ChatMessage> messages = getMessagesFromRedis(sessionId);
                messages.add(message);
                while (messages.size() > MAX_MESSAGES) {
                    messages.remove(0);
                }
                saveMessagesToRedis(sessionId, messages);
                log.debug("添加消息到Redis会话记忆: sessionId={}, messageType={}", sessionId, message.getClass().getSimpleName());
            }

            @Override
            public List<ChatMessage> messages() {
                List<ChatMessage> messages = getMessagesFromRedis(sessionId);
                log.debug("从Redis读取会话记忆: sessionId={}, messageCount={}", sessionId, messages.size());
                return messages;
            }

            @Override
            public void clear() {
                deleteMessagesFromRedis(sessionId);
                memoryCache.remove(sessionId);
                log.debug("清除Redis会话记忆: sessionId={}", sessionId);
            }
        };
    }

    private List<ChatMessage> getMessagesFromRedis(String sessionId) {
        String key = getKey(sessionId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return ChatMessageDeserializer.messagesFromJson(json);
        } catch (Exception e) {
            log.error("读取Redis会话记忆失败: sessionId={}, error={}", sessionId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveMessagesToRedis(String sessionId, List<ChatMessage> messages) {
        String key = getKey(sessionId);
        try {
            String json = ChatMessageSerializer.messagesToJson(messages);
            redisTemplate.opsForValue().set(key, json, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
            log.debug("保存Redis会话记忆: sessionId={}, messageCount={}", sessionId, messages.size());
        } catch (Exception e) {
            log.error("保存Redis会话记忆失败: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    private void deleteMessagesFromRedis(String sessionId) {
        String key = getKey(sessionId);
        redisTemplate.delete(key);
    }

    private String getKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
