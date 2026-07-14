package com.sheyingtong.repository;

import com.sheyingtong.entity.PhotographyTerm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotographyTermRepository extends JpaRepository<PhotographyTerm, Long> {
    
    Optional<PhotographyTerm> findByTerm(String term);
    
    Page<PhotographyTerm> findByCategory(String category, Pageable pageable);
    
    @Query("SELECT t FROM PhotographyTerm t WHERE " +
           "t.term LIKE %:keyword% OR " +
           "t.definition LIKE %:keyword% OR " +
           "t.explanation LIKE %:keyword%")
    Page<PhotographyTerm> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    List<PhotographyTerm> findByCategory(String category);
    
    @Query("SELECT DISTINCT t.category FROM PhotographyTerm t ORDER BY t.category")
    List<String> findDistinctCategories();
}
