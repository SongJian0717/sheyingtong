package com.sheyingtong.service;

import com.sheyingtong.entity.PhotographyTerm;
import com.sheyingtong.repository.PhotographyTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictionaryService {
    
    private final PhotographyTermRepository termRepository;
    
    public Page<PhotographyTerm> searchTerms(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return termRepository.findAll(pageable);
        }
        return termRepository.searchByKeyword(query.trim(), pageable);
    }
    
    public PhotographyTerm getTermDetail(String term) {
        return termRepository.findByTerm(term)
                .orElseThrow(() -> new RuntimeException("术语不存在"));
    }
    
    public Page<PhotographyTerm> getTerms(String category, Pageable pageable) {
        if (category == null || category.trim().isEmpty()) {
            return termRepository.findAll(pageable);
        }
        return termRepository.findByCategory(category.trim(), pageable);
    }
    
    public List<String> getCategories() {
        return termRepository.findDistinctCategories();
    }
    
    public Map<String, Long> getCategoryCount() {
        List<PhotographyTerm> allTerms = termRepository.findAll();
        return allTerms.stream()
                .collect(Collectors.groupingBy(
                        PhotographyTerm::getCategory,
                        Collectors.counting()
                ));
    }
}
