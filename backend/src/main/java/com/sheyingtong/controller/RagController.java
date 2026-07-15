package com.sheyingtong.controller;

import com.sheyingtong.dto.response.ApiResponse;
import com.sheyingtong.service.ChromaRagService;
import com.sheyingtong.service.RagContentRetriever;
import com.sheyingtong.service.RagService;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "RAG知识库管理", description = "摄影知识RAG向量库检索测试、全量同步触发、初始化状态查询接口。向量数据持久化存储于ChromaDB（8002端口）")
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Slf4j
public class RagController {

    private final RagContentRetriever ragContentRetriever;
    private final RagService ragService;
    private final ChromaRagService chromaRagService;

    @Operation(summary = "RAG向量检索测试", description = "输入查询文本，通过Chroma向量库+余弦相似度检索最相关的摄影术语片段，返回Top-K结果。用于调试RAG链路是否正常。【公开接口】")
    @PostMapping("/search")
    public ApiResponse<List<Map<String, Object>>> search(
            @Parameter(description = "{query: 要检索的文本，如\"什么是三分法构图？\"}", required = true)
            @RequestBody Map<String, String> request) {
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

    @Operation(summary = "触发RAG全量同步", description = "强制从MySQL photography_terms表读取全部术语，重新分片并向量化写入ChromaDB。会先检查初始化状态，幂等操作。【需登录认证】")
    @PostMapping("/sync")
    public ApiResponse<Void> sync() {
        ragService.syncKnowledgeBase();
        return ApiResponse.success(null);
    }

    @Operation(summary = "查询RAG初始化状态", description = "返回向量库的初始化进度：是否启动、是否完成、已向量化文档数、初始化错误信息。用于前端展示加载状态。【公开接口】")
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
