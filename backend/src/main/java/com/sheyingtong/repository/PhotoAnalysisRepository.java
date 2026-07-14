package com.sheyingtong.repository;

import com.sheyingtong.entity.PhotoAnalysis;
import com.sheyingtong.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoAnalysisRepository extends JpaRepository<PhotoAnalysis, Long> {
    
    Page<PhotoAnalysis> findByUser(User user, Pageable pageable);
    
    int countByUser(User user);
}
