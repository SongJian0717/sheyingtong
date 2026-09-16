package com.sheyingtong.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sheyingtong.config.CacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 显式缓存层（Cache Aside），不使用 {@code @Cacheable}：
 * key 规范、TTL 抖动、降级开关、命中率观测都在这里显式实现，便于排查与讲解。
 *
 * <p>降级策略沿用 {@link RedisChatMemoryStore}：{@link StringRedisTemplate} 惰性获取，
 * Redis 不可达时不抛错、不阻塞启动，直接跳过缓存查库，可用性不受影响。
 */
@Slf4j
@Component
public class CacheService implements ApplicationContextAware {

    /** 空值占位符：命中它说明"数据库里查过了没有"，用于防穿透 */
    private static final String NULL_SENTINEL = "__NULL__";

    /** 每累计这么多次访问打一行命中率汇总 */
    private static final long STATS_LOG_INTERVAL = 20;

    private final CacheProperties properties;
    private final ObjectMapper cacheMapper;

    private ApplicationContext applicationContext;

    /** 首次成功获取后缓存下来；null 表示尚未获取成功 */
    private volatile StringRedisTemplate redisTemplateCached = null;

    /** 一旦确认 Redis 不可用，本 JVM 运行期内不再重试，避免每次请求都等超时 */
    private volatile boolean redisDisabled = false;

    private final AtomicLong accesses = new AtomicLong();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong redisErrors = new AtomicLong();

    public CacheService(CacheProperties properties) {
        this.properties = properties;
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 缓存体内的时间一律 ISO 字符串，用一份独立的 Mapper 与接口层解耦：
        // 接口层把 LocalDateTime 输出成数组（前端已为此写了兼容函数），而数组形式反序列化回
        // LocalDateTime 会失败，缓存若沿用数组格式，命中后重建对象就会抛异常。
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.cacheMapper = mapper;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    // ===================== 对外 API =====================

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /** 读缓存：未命中或 Redis 不可用时返回 empty；命中空值占位符同样返回 empty（但已算命中） */
    public <T> Optional<T> get(String key, TypeReference<T> type) {
        return Optional.ofNullable(read(key, type).value());
    }

    /**
     * 读不到就回源加载并写入缓存。
     * 约定：加载结果为 null → 写空值占位符（防穿透）；加载结果为空集合 → 不写缓存（防把"还没初始化好"的空状态固化 12 小时）。
     */
    public <T> T getOrLoad(String key, Duration ttl, TypeReference<T> type, Supplier<T> loader) {
        ReadResult<T> cached = read(key, type);
        if (cached.found()) {
            return cached.value();
        }
        T value = loader.get();
        if (value == null) {
            putNull(key);
            return null;
        }
        if (value instanceof Collection<?> collection && collection.isEmpty()) {
            log.debug("[cache] 回源结果为空集合，跳过写缓存: key={}", fullKey(key));
            return value;
        }
        put(key, value, ttl);
        return value;
    }

    public <T> void put(String key, T value, Duration ttl) {
        StringRedisTemplate template = tryGetRedisTemplate();
        if (template == null) {
            return;
        }
        String fullKey = fullKey(key);
        try {
            template.opsForValue().set(fullKey, cacheMapper.writeValueAsString(value), jittered(ttl));
        } catch (Exception e) {
            handleRedisError("写缓存失败（不影响本次请求）", fullKey, e);
        }
    }

    /** 写路径的失效入口：删 key 失败只记日志，绝不因缓存问题让业务写失败 */
    public void delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        StringRedisTemplate template = tryGetRedisTemplate();
        if (template == null) {
            return;
        }
        for (String key : keys) {
            if (key == null || key.isEmpty()) {
                continue;
            }
            String fullKey = fullKey(key);
            try {
                template.delete(fullKey);
            } catch (RuntimeException e) {
                handleRedisError("删除缓存失败（业务不受影响）", fullKey, e);
            }
        }
    }

    // ===================== 内部实现 =====================

    private <T> ReadResult<T> read(String key, TypeReference<T> type) {
        StringRedisTemplate template = tryGetRedisTemplate();
        String fullKey = fullKey(key);
        if (template == null) {
            countMiss();
            return ReadResult.miss();
        }
        String json;
        try {
            json = template.opsForValue().get(fullKey);
        } catch (RuntimeException e) {
            handleRedisError("读缓存失败，本次降级为直查数据库", fullKey, e);
            countMiss();
            return ReadResult.miss();
        }
        if (json == null) {
            countMiss();
            log.debug("[cache] MISS {}", fullKey);
            return ReadResult.miss();
        }
        if (NULL_SENTINEL.equals(json)) {
            countHit();
            log.debug("[cache] HIT(null) {}", fullKey);
            return new ReadResult<>(true, null);
        }
        try {
            T value = cacheMapper.readValue(json, type);
            countHit();
            log.debug("[cache] HIT {}", fullKey);
            return new ReadResult<>(true, value);
        } catch (Exception e) {
            // 反序列化失败通常是缓存体结构变了：删掉脏数据，下次请求回源重建
            log.warn("[cache] 缓存体反序列化失败，已删除待重建: key={}, error={}", fullKey, e.getMessage());
            try {
                template.delete(fullKey);
            } catch (RuntimeException ignored) {
                // 删不掉就算了：后续写入会覆盖它
            }
            countMiss();
            return ReadResult.miss();
        }
    }

    private void putNull(String key) {
        StringRedisTemplate template = tryGetRedisTemplate();
        if (template == null) {
            return;
        }
        String fullKey = fullKey(key);
        try {
            template.opsForValue().set(fullKey, NULL_SENTINEL, properties.getNullTtl());
        } catch (RuntimeException e) {
            handleRedisError("写空值占位符失败（不影响本次请求）", fullKey, e);
        }
    }

    /**
     * 惰性获取 StringRedisTemplate。
     * 刻意不注入构造器：Redis 不可达时 Spring Boot 创建该 Bean 会失败，进而拖垮整个启动。
     * 这里首次使用时才查找，失败就静默降级。
     */
    private StringRedisTemplate tryGetRedisTemplate() {
        if (!properties.isEnabled() || redisDisabled) {
            return null;
        }
        StringRedisTemplate cached = redisTemplateCached;
        if (cached != null) {
            return cached;
        }
        try {
            StringRedisTemplate template = applicationContext.getBean(StringRedisTemplate.class);
            try {
                template.opsForValue().get(properties.getKeyPrefix() + "_ping_" + System.currentTimeMillis());
            } catch (RuntimeException pingError) {
                redisDisabled = true;
                log.warn("Redis 不可达，本次运行期内停用缓存（接口继续直查数据库，可用性不受影响）。cause={}",
                        pingError.getMessage());
                return null;
            }
            redisTemplateCached = template;
            return template;
        } catch (BeansException beanError) {
            redisDisabled = true;
            log.info("未配置 Redis（StringRedisTemplate 缺失），本次运行期内停用缓存，接口直查数据库。");
            return null;
        }
    }

    private void handleRedisError(String message, String key, Exception error) {
        redisErrors.incrementAndGet();
        if (isConnectivityError(error)) {
            redisDisabled = true;
            log.warn("{}（已停用缓存，后续请求直查数据库）: key={}, cause={}", message, key, error.getMessage());
        } else {
            log.warn("{}: key={}, error={}", message, key, error.getMessage());
        }
    }

    /** 只对"连接不上/超时"这类错误停用缓存；序列化等问题不该把整个缓存层关掉 */
    private static boolean isConnectivityError(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof RedisConnectionFailureException || cause instanceof java.io.IOException) {
                return true;
            }
            String simpleName = cause.getClass().getSimpleName();
            if ("RedisConnectionException".equals(simpleName)
                    || "RedisCommandTimeoutException".equals(simpleName)
                    || "QueryTimeoutException".equals(simpleName)) {
                return true;
            }
        }
        return false;
    }

    /** 实际 TTL = 基准 × (1 - jitter ~ 1 + jitter)，避免同一批 key 同时失效 */
    private Duration jittered(Duration base) {
        double ratio = properties.getJitterRatio();
        if (ratio <= 0 || base == null) {
            return base;
        }
        double factor = 1.0 - ratio + ThreadLocalRandom.current().nextDouble() * ratio * 2;
        return Duration.ofMillis(Math.max(1000L, (long) (base.toMillis() * factor)));
    }

    private String fullKey(String key) {
        return properties.getKeyPrefix() + key;
    }

    private void countHit() {
        hits.incrementAndGet();
        maybeLogStats();
    }

    private void countMiss() {
        misses.incrementAndGet();
        maybeLogStats();
    }

    private void maybeLogStats() {
        long total = accesses.incrementAndGet();
        if (total % STATS_LOG_INTERVAL != 0) {
            return;
        }
        log.info("[cache] 命中率汇总: 访问 {} 次, 命中 {}, 未命中 {}, 命中率 {}%, Redis 错误 {} 次",
                total, hits.get(), misses.get(), hits.get() * 100 / Math.max(1, total), redisErrors.get());
    }

    private record ReadResult<T>(boolean found, T value) {
        static <T> ReadResult<T> miss() {
            return new ReadResult<>(false, null);
        }
    }
}
