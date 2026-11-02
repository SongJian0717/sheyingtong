package com.sheyingtong.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
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
    private static final String SUMMARY_KEY_PREFIX = "sheyingtong:chat:summary:";
    private static final long DEFAULT_TTL_HOURS = 24;

    /** 条数上限（原策略，保留作为第一道闸） */
    private static final int MAX_MESSAGES = 20;

    /**
     * 输入侧 token 预算：只约束记忆本身，system prompt + 工具定义 + RAG 片段另算。
     * ⚠️ 为什么必须加这道闸：条数和成本无关 —— 一条带图消息按 qwen-vl 的分辨率计价约 4.3k tokens，
     *    顶十几条文字；只按 20 条裁，长会话的输入会悄悄涨上去。
     */
    private static final int MAX_TOKENS = 8000;

    /** 预算再紧，最近这么多条也不丢（保证"刚说的话"一定在上下文里） */
    private static final int MIN_RETAINED_MESSAGES = 6;

    /** 摘要长度上限：摘要自己不能变成新的负担 */
    private static final int MAX_SUMMARY_CHARS = 800;

    /**
     * 攒够这么多条被丢弃的消息，才触发一次摘要。
     * ⚠️ 为什么必须设门槛：会话满了以后每轮只丢 1 条，若"丢一条就摘一次"，
     *    长会话会变成**每轮多一次模型调用**（成本翻倍）。攒 4 条才摘，
     *    相当于每 2~4 轮一次，摘要最多滞后 3 条消息 —— 兜底记忆，滞后可接受。
     */
    private static final int MIN_EVICTED_BATCH = 4;

    /** 各会话"已丢但还没摘要"的消息，攒够 MIN_EVICTED_BATCH 就发事件 */
    private final Map<String, List<ChatMessage>> pendingEvictions = new ConcurrentHashMap<>();

    private final QwenTokenEstimator tokenEstimator = new QwenTokenEstimator();

    /** 无 Redis 时的摘要兜底（与 fallbackMemory 同生命周期） */
    private final Map<String, String> fallbackSummary = new ConcurrentHashMap<>();

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

                List<ChatMessage> evicted = trim(messages);
                saveMessages(sessionId, messages);

                if (!evicted.isEmpty()) {
                    // 异步摘要：不阻塞模型回调线程（这里同步调模型会拖慢 SSE 的 [DONE]）
                    publishEvicted(sessionId, evicted);
                }
            }

            @Override
            public List<ChatMessage> messages() {
                return getMessages(sessionId);
            }

            @Override
            public void clear() {
                deleteMessages(sessionId);
                deleteSummary(sessionId);
                memoryCache.remove(sessionId);
            }
        };
    }

    // ===================== 记忆裁剪（条数 + token 双上限） =====================

    /**
     * 裁剪记忆，返回被丢掉的消息（交给摘要服务压缩）。三道闸门按顺序：
     *  ① 旧 SystemMessage 直接清掉 —— AiServices 每轮写一份，读取侧本来只用最后一份，
     *     留着纯占条数和 token（实测 3 轮后请求里就出现 3 份 system prompt）
     *  ② 条数上限 MAX_MESSAGES
     *  ③ token 预算 MAX_TOKENS：从最老的开始丢，但最近 MIN_RETAINED_MESSAGES 条不动
     */
    private List<ChatMessage> trim(List<ChatMessage> messages) {
        List<ChatMessage> evicted = new ArrayList<>();

        int lastSystemIndex = -1;
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) instanceof SystemMessage) {
                lastSystemIndex = i;
            }
        }
        for (int i = lastSystemIndex - 1; i >= 0; i--) {
            if (messages.get(i) instanceof SystemMessage) {
                messages.remove(i);   // 旧指令，不进摘要
            }
        }

        while (messages.size() > MAX_MESSAGES) {
            evicted.add(messages.remove(0));
        }

        int totalTokens = tokenEstimator.estimateTokenCountInMessages(messages);
        while (totalTokens > MAX_TOKENS && messages.size() > MIN_RETAINED_MESSAGES) {
            ChatMessage dropped = messages.remove(0);
            totalTokens -= tokenEstimator.estimateTokenCountInMessage(dropped);
            evicted.add(dropped);
        }

        if (!evicted.isEmpty()) {
            log.info("记忆裁剪: 剩余 {} 条 / 估算 {} tokens，丢弃 {} 条交给摘要",
                    messages.size(), totalTokens, evicted.size());
        }
        return evicted;
    }

    private void publishEvicted(String sessionId, List<ChatMessage> evicted) {
        if (applicationContext == null) {
            return;
        }
        List<ChatMessage> pending = pendingEvictions.computeIfAbsent(sessionId, key -> new ArrayList<>());
        List<ChatMessage> batch;
        synchronized (pending) {
            pending.addAll(evicted);
            if (pending.size() < MIN_EVICTED_BATCH) {
                return;
            }
            batch = List.copyOf(pending);
            pending.clear();
        }
        try {
            applicationContext.publishEvent(new MemoryEvictedEvent(sessionId, batch));
        } catch (Exception e) {
            log.warn("发布记忆裁剪事件失败（不影响主流程）: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    // ===================== 较早对话的摘要（与被裁剪的记忆同 TTL） =====================

    /** 读"较早对话的摘要"，没有则返回 null */
    public String getSummary(Object sessionId) {
        String key = String.valueOf(sessionId);
        StringRedisTemplate tpl = tryGetRedisTemplate();
        if (tpl == null) {
            return fallbackSummary.get(key);
        }
        try {
            String value = tpl.opsForValue().get(SUMMARY_KEY_PREFIX + key);
            return (value == null || value.isBlank()) ? null : value;
        } catch (Exception e) {
            log.warn("读取会话摘要失败，本轮忽略摘要: sessionId={}, error={}", key, e.getMessage());
            return fallbackSummary.get(key);
        }
    }

    /** 写摘要（超长截断）；空摘要等于删除 */
    public void saveSummary(Object sessionId, String summary) {
        String key = String.valueOf(sessionId);
        if (summary == null || summary.isBlank()) {
            deleteSummary(sessionId);
            return;
        }
        String text = summary.length() > MAX_SUMMARY_CHARS
                ? summary.substring(0, MAX_SUMMARY_CHARS)
                : summary;

        StringRedisTemplate tpl = tryGetRedisTemplate();
        if (tpl == null) {
            fallbackSummary.put(key, text);
            return;
        }
        try {
            tpl.opsForValue().set(SUMMARY_KEY_PREFIX + key, text, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("保存会话摘要失败，降级写本机内存: sessionId={}, error={}", key, e.getMessage());
            fallbackSummary.put(key, text);
        }
    }

    public void deleteSummary(Object sessionId) {
        String key = String.valueOf(sessionId);
        fallbackSummary.remove(key);
        StringRedisTemplate tpl = tryGetRedisTemplate();
        if (tpl == null) {
            return;
        }
        try {
            tpl.delete(SUMMARY_KEY_PREFIX + key);
        } catch (Exception e) {
            log.warn("删除会话摘要失败: sessionId={}, error={}", key, e.getMessage());
        }
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
