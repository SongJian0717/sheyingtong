package com.sheyingtong.controller;

import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.entity.Gear;
import com.sheyingtong.entity.GearCategory;
import com.sheyingtong.service.GearService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gear")
@RequiredArgsConstructor
public class GearController {
    
    private final GearService gearService;
    
    @PostMapping("/recommend")
    public ApiResponse<Map<String, Object>> getRecommendations(@RequestBody Map<String, Object> request) {
        BigDecimal minBudget = request.get("minBudget") != null ? 
                new BigDecimal(request.get("minBudget").toString()) : null;
        BigDecimal maxBudget = request.get("maxBudget") != null ? 
                new BigDecimal(request.get("maxBudget").toString()) : null;
        String subject = request.get("subject") != null ? request.get("subject").toString() : "";
        String level = request.get("level") != null ? request.get("level").toString() : "";
        String brand = request.get("brand") != null ? request.get("brand").toString() : "";
        String type = request.get("type") != null ? request.get("type").toString() : "";
        
        Map<String, Object> result = gearService.getRecommendations(minBudget, maxBudget, subject, level, brand, type);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/search")
    public ApiResponse<Page<Gear>> searchGear(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "rating"));
        Page<Gear> gears = gearService.searchGear(keyword, categoryId, minPrice, maxPrice, pageable);
        return ApiResponse.success(gears);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<Gear> getGearDetail(@PathVariable Long id) {
        Gear gear = gearService.getGearDetail(id);
        return ApiResponse.success(gear);
    }
    
    @GetMapping("/categories")
    public ApiResponse<List<GearCategory>> getCategories() {
        List<GearCategory> categories = gearService.getCategories();
        return ApiResponse.success(categories);
    }
    
    @GetMapping("/brands")
    public ApiResponse<List<String>> getBrands() {
        List<String> brands = gearService.getBrands();
        return ApiResponse.success(brands);
    }
    
    @GetMapping("/popular")
    public ApiResponse<List<Gear>> getPopularGear() {
        List<Gear> popular = gearService.getPopularGear();
        return ApiResponse.success(popular);
    }
}
