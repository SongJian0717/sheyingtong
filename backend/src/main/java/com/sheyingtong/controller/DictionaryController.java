package com.sheyingtong.controller;

import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.entity.PhotographyTerm;
import com.sheyingtong.service.DictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "摄影词典", description = "摄影术语的搜索、浏览、分类查询等功能，数据来源为内置50+专业术语及RAG向量知识库")
@RestController
@RequestMapping("/api/dictionary")
@RequiredArgsConstructor
public class DictionaryController {
    
    private final DictionaryService dictionaryService;
    
    @Operation(summary = "搜索术语", description = "按关键词模糊匹配术语标题和定义，返回分页结果（按术语名升序排列）。【公开接口】")
    @PostMapping("/search")
    public ApiResponse<Page<PhotographyTerm>> search(
            @Parameter(description = "搜索请求：query=关键词，topK=返回数量（默认5）", required = true)
            @RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        int topK = request.containsKey("topK") ? ((Number) request.get("topK")).intValue() : 5;
        
        Pageable pageable = PageRequest.of(0, topK, Sort.by(Sort.Direction.ASC, "term"));
        Page<PhotographyTerm> terms = dictionaryService.searchTerms(query, pageable);
        return ApiResponse.success(terms);
    }
    
    @Operation(summary = "获取术语详情", description = "通过术语名称（精确匹配）获取完整释义，包含：定义、深入解释、示例、关联术语。【公开接口】")
    @GetMapping("/term/{term}")
    public ApiResponse<PhotographyTerm> getTermDetail(
            @Parameter(description = "术语名称，如 \"ISO感光度\"、\"Bokeh焦外虚化\"", required = true)
            @PathVariable String term) {
        PhotographyTerm photographyTerm = dictionaryService.getTermDetail(term);
        return ApiResponse.success(photographyTerm);
    }
    
    @Operation(summary = "分页查询术语列表", description = "支持按分类筛选 + 分页遍历全部术语，用于词典页面浏览。【公开接口】")
    @GetMapping("/terms")
    public ApiResponse<Page<PhotographyTerm>> getTerms(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "可选：按分类过滤（如\"构图法则\"、\"对焦与镜头\"）") @RequestParam(required = false) String category) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "term"));
        Page<PhotographyTerm> terms = dictionaryService.getTerms(category, pageable);
        return ApiResponse.success(terms);
    }
    
    @Operation(summary = "获取所有分类", description = "返回词典中所有术语分类的列表（去重）。【公开接口】")
    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        List<String> categories = dictionaryService.getCategories();
        return ApiResponse.success(categories);
    }
    
    @Operation(summary = "获取分类术语数量", description = "返回每个分类下的术语数量统计（Map：分类名→条数）。【公开接口】")
    @GetMapping("/category-count")
    public ApiResponse<Map<String, Long>> getCategoryCount() {
        Map<String, Long> count = dictionaryService.getCategoryCount();
        return ApiResponse.success(count);
    }
}
