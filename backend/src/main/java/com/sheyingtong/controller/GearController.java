package com.sheyingtong.controller;

import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.entity.Gear;
import com.sheyingtong.entity.GearCategory;
import com.sheyingtong.service.GearService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "器材选购", description = "摄影器材智能推荐、搜索、分类浏览，覆盖机身/镜头/三脚架/灯光/滤镜等36+款产品库")
@RestController
@RequestMapping("/api/gear")
@RequiredArgsConstructor
public class GearController {
    
    private final GearService gearService;
    
    @Operation(summary = "AI智能器材推荐", description = "根据预算区间、拍摄题材、用户级别、品牌偏好、器材类型多维条件，返回个性化推荐清单+选购建议。【公开接口】")
    @PostMapping("/recommend")
    public ApiResponse<Map<String, Object>> getRecommendations(
            @Parameter(description = "推荐条件：minBudget/maxBudget/subject/level/brand/type", required = true)
            @RequestBody Map<String, Object> request) {
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
    
    @Operation(summary = "搜索器材", description = "支持关键词+分类+品牌+价格区间的组合条件查询，按评分降序分页返回。【公开接口】")
    @GetMapping("/search")
    public ApiResponse<Page<Gear>> searchGear(
            @Parameter(description = "关键词（匹配器材名称和描述）") @RequestParam(required = false) String keyword,
            @Parameter(description = "器材分类ID（1=机身,2=镜头,3=三脚架...）") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "品牌英文名，如 Sony/Canon/Nikon/Fujifilm") @RequestParam(required = false) String brand,
            @Parameter(description = "最低价格") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "rating"));
        Page<Gear> gears = gearService.searchGear(keyword, categoryId, brand, minPrice, maxPrice, pageable);
        return ApiResponse.success(gears);
    }
    
    @Operation(summary = "获取器材详情", description = "通过ID获取单款器材的完整规格、AI点评、评分、评测数量、图片等。【公开接口】")
    @GetMapping("/{id}")
    public ApiResponse<Gear> getGearDetail(
            @Parameter(description = "器材ID（1~36）", required = true)
            @PathVariable Long id) {
        Gear gear = gearService.getGearDetail(id);
        return ApiResponse.success(gear);
    }
    
    @Operation(summary = "获取器材分类列表", description = "返回所有器材分类（7大类：机身/镜头/三脚架等），含分类名称、描述、图标。【公开接口】")
    @GetMapping("/categories")
    public ApiResponse<List<GearCategory>> getCategories() {
        List<GearCategory> categories = gearService.getCategories();
        return ApiResponse.success(categories);
    }
    
    @Operation(summary = "获取所有品牌列表", description = "返回器材库中的所有品牌（去重），用于下拉框筛选。【公开接口】")
    @GetMapping("/brands")
    public ApiResponse<List<String>> getBrands() {
        List<String> brands = gearService.getBrands();
        return ApiResponse.success(brands);
    }
    
    @Operation(summary = "热门器材榜单", description = "返回评分最高的6款热门器材，用于首页推荐展示。【公开接口】")
    @GetMapping("/popular")
    public ApiResponse<List<Gear>> getPopularGear() {
        List<Gear> popular = gearService.getPopularGear();
        return ApiResponse.success(popular);
    }
}
