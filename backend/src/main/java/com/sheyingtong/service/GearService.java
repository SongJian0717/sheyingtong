package com.sheyingtong.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sheyingtong.config.CacheProperties;
import com.sheyingtong.entity.Gear;
import com.sheyingtong.entity.GearCategory;
import com.sheyingtong.repository.GearCategoryRepository;
import com.sheyingtong.repository.GearRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 器材选购：产品库只有 36 条、分类 6 条，且没有任何写入口，同样属于静态知识库。
 * 整表缓存后在内存完成搜索/筛选/排序/分页，AI 工具链（{@link PhotographyTools}）复用同一份缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GearService {

    private static final TypeReference<List<Gear>> GEAR_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<GearCategory>> CATEGORY_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> BRANDS_TYPE = new TypeReference<>() {
    };

    /** 与原 SQL 的 ORDER BY rating DESC 一致；同分时按 id 升序保证翻页结果稳定 */
    private static final Comparator<Gear> BY_RATING_DESC =
            Comparator.comparing(Gear::getRating).reversed().thenComparing(Gear::getId);

    private final GearRepository gearRepository;
    private final GearCategoryRepository categoryRepository;
    private final CacheService cacheService;
    private final CacheProperties cacheProperties;

    public Page<Gear> searchGear(String keyword, Long categoryId, String brand,
                                  BigDecimal minPrice, BigDecimal maxPrice,
                                  Pageable pageable) {
        List<Gear> matched = allGear().stream()
                .filter(gear -> keyword == null
                        || containsIgnoreCase(gear.getBrand(), keyword)
                        || containsIgnoreCase(gear.getModel(), keyword))
                .filter(gear -> categoryId == null
                        || (gear.getCategory() != null && categoryId.equals(gear.getCategory().getId())))
                .filter(gear -> brand == null || containsIgnoreCase(gear.getBrand(), brand))
                .filter(gear -> minPrice == null || gear.getPrice().compareTo(minPrice) >= 0)
                .filter(gear -> maxPrice == null || gear.getPrice().compareTo(maxPrice) <= 0)
                .sorted(BY_RATING_DESC)
                .toList();
        return pageOf(matched, pageable);
    }

    public Gear getGearDetail(Long id) {
        return allGear().stream()
                .filter(gear -> id != null && id.equals(gear.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("器材不存在"));
    }

    public Map<String, Object> getRecommendations(BigDecimal minBudget, BigDecimal maxBudget,
                                                   String subject, String level, String brand, String type) {
        List<Gear> allGear = allGear();
        List<Gear> filteredGear = allGear.stream()
                .filter(g -> minBudget == null || g.getPrice().compareTo(minBudget) >= 0)
                .filter(g -> maxBudget == null || g.getPrice().compareTo(maxBudget) <= 0)
                .filter(g -> brand == null || brand.isEmpty() || g.getBrand().contains(brand))
                .filter(g -> type == null || type.isEmpty() || (g.getCategory() != null && g.getCategory().getName().contains(type)))
                .toList();
        
        if (!subject.isEmpty()) {
            String lowerSubject = subject.toLowerCase();
            filteredGear = filteredGear.stream()
                    .filter(g -> {
                        String model = g.getModel().toLowerCase();
                        String desc = g.getDescription() != null ? g.getDescription().toLowerCase() : "";
                        if (lowerSubject.contains("人像")) {
                            return model.contains("85") || model.contains("50") || model.contains("portrait") ||
                                   desc.contains("人像") || desc.contains("portrait") ||
                                   (g.getCategory() != null && g.getCategory().getName().contains("相机"));
                        }
                        if (lowerSubject.contains("风光") || lowerSubject.contains("风景")) {
                            return model.contains("wide") || model.contains("24") || model.contains("16") ||
                                   desc.contains("风光") || desc.contains("风景") ||
                                   (g.getCategory() != null && g.getCategory().getName().contains("相机"));
                        }
                        if (lowerSubject.contains("运动")) {
                            return model.contains("sports") || desc.contains("运动") || desc.contains("连拍") ||
                                   desc.contains("对焦");
                        }
                        if (lowerSubject.contains("视频")) {
                            return model.contains("video") || desc.contains("视频") ||
                                   desc.contains("4K") || desc.contains("录制");
                        }
                        return true;
                    })
                    .toList();
        }
        
        if (!level.isEmpty()) {
            if (level.contains("入门")) {
                filteredGear = filteredGear.stream()
                        .filter(g -> g.getPrice().compareTo(BigDecimal.valueOf(8000)) <= 0)
                        .toList();
            } else if (level.contains("专业")) {
                filteredGear = filteredGear.stream()
                        .filter(g -> g.getPrice().compareTo(BigDecimal.valueOf(15000)) >= 0)
                        .toList();
            }
        }
        
        List<Gear> recommendations = filteredGear.stream()
                .sorted((a, b) -> b.getRating().compareTo(a.getRating()))
                .limit(5)
                .toList();
        
        String summary = generateSummary(recommendations, subject, level, minBudget, maxBudget);
        
        return Map.of(
                "recommendations", recommendations,
                "summary", summary
        );
    }
    
    private String generateSummary(List<Gear> recommendations, String subject, String level,
                                   BigDecimal minBudget, BigDecimal maxBudget) {
        if (recommendations.isEmpty()) {
            return "暂无符合条件的器材推荐，请尝试调整筛选条件。";
        }
        
        StringBuilder summary = new StringBuilder("综合考虑你的需求，");
        
        if (!level.isEmpty()) {
            if (level.contains("入门")) {
                summary.append("作为入门用户，");
            } else if (level.contains("专业")) {
                summary.append("作为专业摄影师，");
            }
        }
        
        if (!subject.isEmpty()) {
            summary.append("拍摄").append(subject).append("题材，");
        }
        
        if (minBudget != null && maxBudget != null) {
            summary.append("预算在").append(minBudget).append("-").append(maxBudget).append("元之间，");
        }
        
        Gear topGear = recommendations.get(0);
        summary.append("推荐").append(topGear.getBrand()).append(topGear.getModel())
                .append("作为首选。");
        
        if (recommendations.size() > 1) {
            summary.append("同时也可以考虑").append(recommendations.get(1).getBrand())
                    .append(recommendations.get(1).getModel())
                    .append("等其他选项。");
        }
        
        return summary.toString();
    }
    
    public List<GearCategory> getCategories() {
        List<GearCategory> categories = cacheService.getOrLoad(
                CacheKeys.GEAR_CATEGORIES,
                cacheProperties.getGearTtl(),
                CATEGORY_TYPE,
                this::loadCategoriesFromDatabase);
        return categories == null ? List.of() : categories;
    }
    
    public List<String> getBrands() {
        List<String> brands = cacheService.getOrLoad(
                CacheKeys.GEAR_BRANDS,
                cacheProperties.getGearTtl(),
                BRANDS_TYPE,
                this::loadBrandsFromDatabase);
        return brands == null ? List.of() : brands;
    }
    
    public List<Gear> getPopularGear() {
        return allGear().stream()
                .sorted(BY_RATING_DESC)
                .limit(6)
                .toList();
    }

    // ===================== 供 AI 工具链复用 =====================

    /** 对应原 gearRepository.searchByKeyword：品牌/型号/简介任一命中 */
    public List<Gear> searchByKeyword(String keyword) {
        if (keyword == null) {
            return List.of();
        }
        String trimmed = keyword.trim();
        return allGear().stream()
                .filter(gear -> containsIgnoreCase(gear.getBrand(), trimmed)
                        || containsIgnoreCase(gear.getModel(), trimmed)
                        || containsIgnoreCase(gear.getDescription(), trimmed))
                .toList();
    }

    /** 对应原 gearRepository.findByPriceRange：价格区间内按价格升序 */
    public List<Gear> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return allGear().stream()
                .filter(gear -> minPrice == null || gear.getPrice().compareTo(minPrice) >= 0)
                .filter(gear -> maxPrice == null || gear.getPrice().compareTo(maxPrice) <= 0)
                .sorted(Comparator.comparing(Gear::getPrice))
                .toList();
    }

    // ===================== 缓存载入 =====================

    private List<Gear> allGear() {
        List<Gear> gears = cacheService.getOrLoad(
                CacheKeys.GEAR_ALL,
                cacheProperties.getGearTtl(),
                GEAR_TYPE,
                this::loadGearFromDatabase);
        return gears == null ? List.of() : gears;
    }

    private List<Gear> loadGearFromDatabase() {
        log.info("[gear] 器材缓存未命中，回源加载全表");
        // 按 id 升序缓存：与改造前 findAll() 的自然顺序一致，让"同评分器材"的先后顺序保持稳定
        return new ArrayList<>(gearRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
    }
    
    private List<GearCategory> loadCategoriesFromDatabase() {
        log.info("[gear] 分类缓存未命中，回源加载全表");
        return new ArrayList<>(categoryRepository.findAll());
    }

    private List<String> loadBrandsFromDatabase() {
        log.info("[gear] 品牌缓存未命中，回源加载去重品牌");
        return new ArrayList<>(gearRepository.findDistinctBrands());
    }

    // ===================== 内存分页/匹配 =====================

    private static <T> Page<T> pageOf(List<T> source, Pageable pageable) {
        int from = (int) Math.min(pageable.getOffset(), source.size());
        int to = (int) Math.min((long) from + pageable.getPageSize(), source.size());
        return new PageImpl<>(source.subList(from, to), pageable, source.size());
    }

    private static boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }
}
