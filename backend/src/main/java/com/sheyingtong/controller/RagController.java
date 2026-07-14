package com.sheyingtong.controller;

import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.service.ChromaRagService;
import com.sheyingtong.service.RagContentRetriever;
import com.sheyingtong.service.RagService;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Slf4j
public class RagController {

    private final RagContentRetriever ragContentRetriever;
    private final RagService ragService;
    private final ChromaRagService chromaRagService;

    @PostMapping("/search")
    public ApiResponse<List<Map<String, Object>>> search(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        log.info("RAG测试检索: {}", query);

        Query ragQuery = Query.from(query);
        List<Content> results = ragContentRetriever.retrieve(ragQuery);

        List<Map<String, Object>> formattedResults = results.stream()
                .map(content -> Map.of(
                        "content", content.toString(),
                        "metadata", content.metadata()
                ))
                .collect(Collectors.toList());

        log.info("RAG检索返回 {} 条结果", formattedResults.size());
        return ApiResponse.success(formattedResults);
    }

    @PostMapping("/sync")
    public ApiResponse<Void> sync() {
        ragService.syncKnowledgeBase();
        return ApiResponse.success(null);
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("initialized", chromaRagService.isInitialized());
        status.put("initializationStarted", chromaRagService.isInitializationStarted());
        status.put("documentCount", chromaRagService.getDocumentCount());
        status.put("initializationError", chromaRagService.getInitializationError());
        return ApiResponse.success(status);
    }
}
