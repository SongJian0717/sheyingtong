package com.sheyingtong.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final ChromaRagService chromaRagService;

    public void syncKnowledgeBase() {
        log.info("触发知识库同步（委托给ChromaRagService）");
        chromaRagService.syncKnowledgeBase();
    }
}