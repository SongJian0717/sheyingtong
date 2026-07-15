package com.sheyingtong.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisChatMemoryStore implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private static final String KEY_PREFIX = "sheyingtong:chat:memory:";
    private static final long DEFAULT_TTL_HOURS = 24;
    private static final int MAX_MESSAGES = 20;

    /** Per-session ChatMemory instance cache (works for BOTH backends) */
    private final Map<String, ChatMemory> memoryCache = new ConcurrentHashMap<>();

    /**
     * In-Memory fallback store used when:
     *   1) Redis is not installed / not running on localhost:6379
     *   2) Redis connection fails / times out
     *   3) StringRedisTemplate bean cannot be created (BeanCreationException)
     *
     * ⚠️ Without this fallback, Spring Boot startup FAILS if Redis is down:
     *    StringRedisTemplate -> LettuceConnectionFactory -> connection ping = startup crash
     */
    private final Map<String, List<ChatMessage>> fallbackMemory = new ConcurrentHashMap<>();

    /** Cached StringRedisTemplate instance; null until first successful lookup */
    private volatile StringRedisTemplate redisTemplateCached = null;
    /** After one failure, we permanently fall back for this JVM run to avoid retry storms */
    private volatile boolean redisDisabled = false;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Lazy lookup of StringRedisTemplate.
     * IMPORTANT: we do NOT inject this via constructor/field because Spring Boot will
     * FAIL during ApplicationContext startup if Redis is unreachable (bean creation fails).
     * Instead we look it up lazily on first use and swallow any exception, returning null.
     */
    private StringRedisTemplate tryGetRedisTemplate() {
        if (redisDisabled) return null;
        if (redisTemplateCached != null) return redisTemplateCached;
        try {
            StringRedisTemplate tpl = applicationContext.getBean(StringRedisTemplate.class);
            // Quick connectivity check (cheap GET on a non-existent key) - if this throws, Redis is down
            try {
                tpl.opsForValue().get(KEY_PREFIX + "_ping_" + System.currentTimeMillis());
            } catch (Exception pingEx) {
                log.warn("Redis installed but unreachable, falling back to in-memory chat memory for this run. " +
                         "Install/start Redis if you want cross-restart session TTL persistence. Cause: {}", pingEx.getMessage());
                redisDisabled = true;
                return null;
            }
            redisTemplateCached = tpl;
            return tpl;
        } catch (BeansException be) {
            log.info("Redis not configured (StringRedisTemplate bean missing), using in-memory chat memory fallback. " +
                     "Install & start Redis on localhost:6379 for persistent TTL-based session memory.");
            redisDisabled = true;
            return null;
        }
    }

    public ChatMemory getChatMemory(Object sessionId) {
        String key = String.valueOf(sessionId);
        return memoryCache.computeIfAbsent(key, this::createChatMemory);
    }

    private ChatMemory createChatMemory(final String sessionId) {
        return new ChatMemory() {
            @Override
            public Object id() {
                return sessionId;
            }

            @Override
            public void add(ChatMessage message) {
                List<ChatMessage> messages = getMessages(sessionId);
                messages.add(message);
                while (messages.size() > MAX_MESSAGES) {
                    messages.remove(0);
                }
                saveMessages(sessionId, messages);
            }

            @Override
            public List<ChatMessage> messages() {
                return getMessages(sessionId);
            }

            @Override
            public void clear() {
                deleteMessages(sessionId);
                memoryCache.remove(sessionId);
            }
        };
    }

    // ===================== Dual-backend storage helpers =====================

    private List<ChatMessage> getMessages(String sessionId) {
        StringRedisTemplate tpl = tryGetRedisTemplate();
        if (tpl != null) {
            return getMessagesFromRedis(tpl, sessionId);
        }
        return new ArrayList<>(fallbackMemory.getOrDefault(sessionId, Collections.emptyList()));
    }

    private void saveMessages(String sessionId, List<ChatMessage> messages) {
        StringRedisTemplate tpl = tryGetRedisTemplate();
        if (tpl != null) {
            saveMessagesToRedis(tpl, sessionId, messages);
        } else {
            fallbackMemory.put(sessionId, new ArrayList<>(messages));
        }
    }

    private void deleteMessages(String sessionId) {
        StringRedisTemplate tpl = tryGetRedisTemplate();
        if (tpl != null) {
            deleteMessagesFromRedis(tpl, sessionId);
        } else {
            fallbackMemory.remove(sessionId);
        }
    }

    // ===================== Redis backend (safe wrappers) =====================

    private List<ChatMessage> getMessagesFromRedis(StringRedisTemplate tpl, String sessionId) {
        String key = getKey(sessionId);
        try {
            String json = tpl.opsForValue().get(key);
            if (json == null || json.isEmpty()) {
                return new ArrayList<>();
            }
            return ChatMessageDeserializer.messagesFromJson(json);
        } catch (Exception e) {
            log.error("读取Redis会话记忆失败，返回空列表: sessionId={}, error={}", sessionId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveMessagesToRedis(StringRedisTemplate tpl, String sessionId, List<ChatMessage> messages) {
        String key = getKey(sessionId);
        try {
            String json = ChatMessageSerializer.messagesToJson(messages);
            tpl.opsForValue().set(key, json, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("保存Redis会话记忆失败（消息未丢失，降级写入本机内存）: sessionId={}, error={}", sessionId, e.getMessage());
            fallbackMemory.put(sessionId, new ArrayList<>(messages));
        }
    }

    private void deleteMessagesFromRedis(StringRedisTemplate tpl, String sessionId) {
        try {
            tpl.delete(getKey(sessionId));
        } catch (Exception e) {
            log.error("删除Redis会话记忆失败: sessionId={}, error={}", sessionId, e.getMessage());
            fallbackMemory.remove(sessionId);
        }
    }

    private String getKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
