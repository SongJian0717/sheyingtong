package com.sheyingtong.service;

import com.sheyingtong.entity.Gear;
import com.sheyingtong.entity.GearCategory;
import com.sheyingtong.repository.GearCategoryRepository;
import com.sheyingtong.repository.GearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GearService {
    
    private final GearRepository gearRepository;
    private final GearCategoryRepository categoryRepository;
    
    public Page<Gear> searchGear(String keyword, Long categoryId, 
                                  BigDecimal minPrice, BigDecimal maxPrice, 
                                  Pageable pageable) {
        return gearRepository.search(keyword, categoryId, minPrice, maxPrice, pageable);
    }
    
    public Gear getGearDetail(Long id) {
        return gearRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("器材不存在"));
    }
    
    public Map<String, Object> getRecommendations(BigDecimal minBudget, BigDecimal maxBudget,
                                                   String subject, String level, String brand, String type) {
        List<Gear> allGear = gearRepository.findAll();
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
        return categoryRepository.findAll();
    }
    
    public List<String> getBrands() {
        return gearRepository.findDistinctBrands();
    }
    
    public List<Gear> getPopularGear() {
        return gearRepository.findTop6ByOrderByRatingDesc();
    }
}
