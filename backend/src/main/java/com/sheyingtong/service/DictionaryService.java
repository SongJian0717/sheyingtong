package com.sheyingtong.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sheyingtong.config.CacheProperties;
import com.sheyingtong.entity.PhotographyTerm;
import com.sheyingtong.repository.PhotographyTermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 摄影词典：术语表只有 50 条且没有任何写入口，属于典型的静态知识库。
 * 因此整表缓存一次，分页/分类过滤/关键字匹配全部在内存完成——既省掉每次请求的 SQL，
 * 也避开了 Spring {@code Page} 对象进缓存的序列化麻烦。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryService {

    private static final TypeReference<List<PhotographyTerm>> TERMS_TYPE = new TypeReference<>() {
    };

    private final PhotographyTermRepository termRepository;
    private final CacheService cacheService;
    private final CacheProperties cacheProperties;

    public Page<PhotographyTerm> searchTerms(String query, Pageable pageable) {
        List<PhotographyTerm> all = allTerms();
        if (query == null || query.trim().isEmpty()) {
            return pageOf(all, pageable);
        }
        String keyword = query.trim().toLowerCase(Locale.ROOT);
        List<PhotographyTerm> matched = all.stream()
                .filter(term -> containsIgnoreCase(term.getTerm(), keyword)
                        || containsIgnoreCase(term.getDefinition(), keyword)
                        || containsIgnoreCase(term.getExplanation(), keyword))
                .toList();
        return pageOf(matched, pageable);
    }

    public PhotographyTerm getTermDetail(String term) {
        return findTerm(term).orElseThrow(() -> new RuntimeException("术语不存在"));
    }

    /** 供 AI 工具链复用同一份缓存；未找到返回 empty 而不是抛异常 */
    public Optional<PhotographyTerm> findTerm(String term) {
        if (term == null) {
            return Optional.empty();
        }
        return allTerms().stream()
                .filter(candidate -> term.equals(candidate.getTerm()))
                .findFirst();
    }

    public Page<PhotographyTerm> getTerms(String category, Pageable pageable) {
        List<PhotographyTerm> all = allTerms();
        if (category == null || category.trim().isEmpty()) {
            return pageOf(all, pageable);
        }
        String target = category.trim();
        List<PhotographyTerm> filtered = all.stream()
                .filter(term -> target.equals(term.getCategory()))
                .toList();
        return pageOf(filtered, pageable);
    }

    public List<String> getCategories() {
        return allTerms().stream()
                .map(PhotographyTerm::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    public Map<String, Long> getCategoryCount() {
        return allTerms().stream()
                .filter(term -> term.getCategory() != null)
                .collect(Collectors.groupingBy(PhotographyTerm::getCategory, Collectors.counting()));
    }

    // ===================== 缓存载入 =====================

    private List<PhotographyTerm> allTerms() {
        List<PhotographyTerm> terms = cacheService.getOrLoad(
                CacheKeys.DICT_TERMS_ALL,
                cacheProperties.getDictTtl(),
                TERMS_TYPE,
                this::loadTermsFromDatabase);
        return terms == null ? List.of() : terms;
    }

    private List<PhotographyTerm> loadTermsFromDatabase() {
        log.info("[dict] 术语缓存未命中，回源加载全表");
        // 交给数据库按 term 升序排好再缓存：与改造前各接口 ORDER BY t.term 的排序结果
        // 完全一致，避免 Java 侧排序与 MySQL 排序规则（utf8mb4_unicode_ci）产生差异
        return new ArrayList<>(termRepository.findAll(Sort.by(Sort.Direction.ASC, "term")));
    }

    // ===================== 内存分页/匹配 =====================

    private static Page<PhotographyTerm> pageOf(List<PhotographyTerm> source, Pageable pageable) {
        int from = (int) Math.min(pageable.getOffset(), source.size());
        int to = (int) Math.min((long) from + pageable.getPageSize(), source.size());
        return new PageImpl<>(source.subList(from, to), pageable, source.size());
    }

    /** 关键字已预先小写化；MySQL 的 utf8mb4_unicode_ci 排序规则本身就是大小写不敏感 */
    private static boolean containsIgnoreCase(String source, String lowerKeyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }
}
