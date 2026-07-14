package com.sheyingtong.repository;

import com.sheyingtong.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
    
    List<ChatSession> findByUserIdAndActiveTrueOrderByUpdatedAtDesc(Long userId);
    
    int countByUserId(Long userId);
}