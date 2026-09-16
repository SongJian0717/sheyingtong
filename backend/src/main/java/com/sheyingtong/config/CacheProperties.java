package com.sheyingtong.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    /** 全局开关：false 时所有缓存读写直接短路，便于做"开/关缓存"对比压测 */
    private boolean enabled = true;

    /** 统一 key 命名空间，与聊天记忆的 sheyingtong: 保持一致 */
    private String keyPrefix = "sheyingtong:cache:";

    /** TTL 随机抖动比例，防同一时刻大批 key 集体过期造成雪崩 */
    private double jitterRatio = 0.1;

    /** 空值占位缓存 TTL，防穿透 */
    private Duration nullTtl = Duration.ofSeconds(30);

    /** Tier A：词典术语全量（静态数据，无写入口） */
    private Duration dictTtl = Duration.ofHours(12);

    /** Tier A：器材全量（静态数据，无写入口） */
    private Duration gearTtl = Duration.ofHours(12);
}
