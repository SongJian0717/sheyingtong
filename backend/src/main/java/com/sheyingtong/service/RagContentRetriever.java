package com.sheyingtong.service;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.DefaultContent;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagContentRetriever implements ContentRetriever {

    private final ChromaRagService chromaRagService;

    @Override
    public List<Content> retrieve(Query query) {
        log.info("RAG检索: {}", query.text());
        
        if (!chromaRagService.isInitialized()) {
            log.warn("RAG知识库尚未初始化完成，跳过检索");
            return List.of();
        }
        
        try {
            List<TextSegment> segments = chromaRagService.search(query.text(), 5);
            log.info("RAG检索完成，返回 {} 条结果", segments.size());
            
            List<Content> results = new ArrayList<>();
            for (TextSegment segment : segments) {
                results.add(new DefaultContent(segment));
            }
            return results;
        } catch (Exception e) {
            log.warn("RAG检索失败: {}", e.getMessage());
            return List.of();
        }
    }
}