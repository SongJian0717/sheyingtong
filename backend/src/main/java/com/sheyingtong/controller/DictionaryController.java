package com.sheyingtong.controller;

import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.entity.PhotographyTerm;
import com.sheyingtong.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dictionary")
@RequiredArgsConstructor
public class DictionaryController {
    
    private final DictionaryService dictionaryService;
    
    @PostMapping("/search")
    public ApiResponse<Page<PhotographyTerm>> search(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        int topK = request.containsKey("topK") ? ((Number) request.get("topK")).intValue() : 5;
        
        Pageable pageable = PageRequest.of(0, topK, Sort.by(Sort.Direction.ASC, "term"));
        Page<PhotographyTerm> terms = dictionaryService.searchTerms(query, pageable);
        return ApiResponse.success(terms);
    }
    
    @GetMapping("/term/{term}")
    public ApiResponse<PhotographyTerm> getTermDetail(@PathVariable String term) {
        PhotographyTerm photographyTerm = dictionaryService.getTermDetail(term);
        return ApiResponse.success(photographyTerm);
    }
    
    @GetMapping("/terms")
    public ApiResponse<Page<PhotographyTerm>> getTerms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "term"));
        Page<PhotographyTerm> terms = dictionaryService.getTerms(category, pageable);
        return ApiResponse.success(terms);
    }
    
    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        List<String> categories = dictionaryService.getCategories();
        return ApiResponse.success(categories);
    }
    
    @GetMapping("/category-count")
    public ApiResponse<Map<String, Long>> getCategoryCount() {
        Map<String, Long> count = dictionaryService.getCategoryCount();
        return ApiResponse.success(count);
    }
}
