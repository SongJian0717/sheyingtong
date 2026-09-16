package com.sheyingtong.service;

/**
 * 缓存 key 的统一出口（写在 {@link CacheService} 的命名空间之下，key 本身不带前缀）。
 * 写路径失效时引用这里，避免 key 字符串散落在各个 Service 里对不上。
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    /** Tier A：词典术语全量（含分类/关键字检索所需字段） */
    public static final String DICT_TERMS_ALL = "dict:terms:all";

    /** Tier A：器材全量 */
    public static final String GEAR_ALL = "gear:all";

    /** Tier A：器材分类全量 */
    public static final String GEAR_CATEGORIES = "gear:categories";

    /** Tier A：器材品牌去重列表（DISTINCT + 排序交给数据库，避免与 MySQL 排序规则产生差异） */
    public static final String GEAR_BRANDS = "gear:brands";
}
