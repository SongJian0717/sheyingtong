package com.sheyingtong.service;

import com.sheyingtong.entity.Gear;
import com.sheyingtong.entity.PhotographyTerm;
import com.sheyingtong.repository.GearRepository;
import com.sheyingtong.repository.PhotographyTermRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChromaRagService {

    private final PhotographyTermRepository termRepository;
    private final GearRepository gearRepository;
    private final EmbeddingModel embeddingModel;

    private final RestTemplate restTemplate = new RestTemplate();

    // ⚠️ 不再硬编码，统一从 application.yml 的 chroma.host / chroma.port / collection-name 读取
    //    之前硬编码 http://localhost:8002 与配置 8000 不一致，导致 RAG 后端一直连不上向量库
    @Value("${chroma.host:127.0.0.1}")
    private String chromaHost;

    @Value("${chroma.port:8002}")
    private int chromaPort;

    @Value("${chroma.collection-name:photography_dictionary}")
    private String collectionName;

    /** Build base URL lazily (after @Value fields injected) */
    private String chromaUrl() {
        return "http://" + chromaHost + ":" + chromaPort;
    }

    private volatile boolean initialized = false;
    private volatile boolean initializationStarted = false;
    private volatile int documentCount = 0;
    private volatile String initializationError = null;

    @Async
    @EventListener(ContextRefreshedEvent.class)
    public void initAsync() {
        if (initializationStarted) {
            return;
        }
        initializationStarted = true;
        
        log.info("========== 异步初始化Chroma RAG知识库开始 ==========");
        long startTime = System.currentTimeMillis();
        
        try {
            syncKnowledgeBase();
            initialized = true;
            long endTime = System.currentTimeMillis();
            log.info("========== Chroma RAG知识库异步初始化完成，耗时: {}ms ==========", endTime - startTime);
        } catch (Exception e) {
            initializationError = e.getMessage();
            log.error("Chroma RAG知识库异步初始化失败: {}", e.getMessage(), e);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isInitializationStarted() {
        return initializationStarted;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public String getInitializationError() {
        return initializationError;
    }

    public void syncKnowledgeBase() {
        List<PhotographyTerm> terms = termRepository.findAll();
        List<Gear> gears = gearRepository.findAll();

        log.info("发现 {} 个摄影术语, {} 个器材数据", terms.size(), gears.size());

        int totalSegments = 0;
        for (PhotographyTerm term : terms) {
            totalSegments += ingestTerm(term);
        }

        for (Gear gear : gears) {
            totalSegments += ingestGear(gear);
        }

        totalSegments += ingestMarkdownFile("photography-basics.md");
        totalSegments += ingestMarkdownFile("lens-guide.md");

        documentCount = totalSegments;
        log.info("共向量化 {} 个文本段", totalSegments);
    }

    private int ingestTerm(PhotographyTerm term) {
        int count = 0;

        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("type", "term");
        metadataMap.put("category", term.getCategory());
        metadataMap.put("term", term.getTerm());
        metadataMap.put("id", String.valueOf(term.getId()));

        String fullContent = String.format("【%s】\n分类: %s\n定义: %s\n详细解释: %s\n示例: %s",
                term.getTerm(),
                term.getCategory(),
                term.getDefinition(),
                term.getExplanation(),
                term.getExamples());

        count += addToChroma(fullContent, metadataMap);

        if (term.getDefinition() != null && !term.getDefinition().isEmpty()) {
            Map<String, String> defMeta = new HashMap<>();
            defMeta.put("type", "term_definition");
            defMeta.put("term", term.getTerm());
            count += addToChroma(term.getTerm() + " - 定义: " + term.getDefinition(), defMeta);
        }

        if (term.getExplanation() != null && !term.getExplanation().isEmpty()) {
            Map<String, String> expMeta = new HashMap<>();
            expMeta.put("type", "term_explanation");
            expMeta.put("term", term.getTerm());
            count += addToChroma(term.getTerm() + " - 解释: " + term.getExplanation(), expMeta);
        }

        if (term.getExamples() != null && !term.getExamples().isEmpty()) {
            Map<String, String> exMeta = new HashMap<>();
            exMeta.put("type", "term_example");
            exMeta.put("term", term.getTerm());
            count += addToChroma(term.getTerm() + " - 示例: " + term.getExamples(), exMeta);
        }

        log.info("已向量化术语[{}]: {} 个片段", term.getTerm(), count);
        return count;
    }

    private int ingestGear(Gear gear) {
        String categoryName = gear.getCategory() != null ? gear.getCategory().getName() : "其他";

        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("type", "gear");
        metadataMap.put("category", categoryName);
        metadataMap.put("brand", gear.getBrand());
        metadataMap.put("model", gear.getModel());
        metadataMap.put("id", String.valueOf(gear.getId()));

        String fullContent = String.format("【%s %s】\n分类: %s\n价格: ¥%s\n描述: %s\n像素: %s\n连拍: %s\n视频: %s\n对焦: %s\n规格: %s",
                gear.getBrand(),
                gear.getModel(),
                categoryName,
                gear.getPrice(),
                gear.getDescription(),
                gear.getPixel(),
                gear.getContinuousShooting(),
                gear.getVideo(),
                gear.getFocus(),
                gear.getSpecifications());

        log.info("向量化器材[{} {}]", gear.getBrand(), gear.getModel());
        return addToChroma(fullContent, metadataMap);
    }

    private int ingestMarkdownFile(String fileName) {
        log.info("开始向量化Markdown文件: {}", fileName);
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            java.io.InputStream inputStream = classLoader.getResourceAsStream(fileName);
            if (inputStream == null) {
                log.warn("Markdown文件不存在: {}", fileName);
                return 0;
            }

            StringBuilder content = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            String[] chapters = content.toString().split("\n## ");
            int count = 0;
            for (String chapter : chapters) {
                if (chapter.trim().isEmpty()) continue;
                
                String[] parts = chapter.split("\n", 2);
                String title = parts[0].trim();
                String body = parts.length > 1 ? parts[1].trim() : "";

                Map<String, String> metadataMap = new HashMap<>();
                metadataMap.put("type", "markdown");
                metadataMap.put("chapter", title);

                if (!body.isEmpty()) {
                    String[] paragraphs = body.split("\n\n");
                    for (String paragraph : paragraphs) {
                        paragraph = paragraph.trim();
                        if (paragraph.isEmpty() || paragraph.startsWith("###")) continue;
                        count += addToChroma("【" + title + "】\n" + paragraph, metadataMap);
                    }
                }
            }

            log.info("Markdown文件向量化完成: {}，共 {} 个片段", fileName, count);
            return count;
        } catch (Exception e) {
            log.error("向量化Markdown文件失败: {}", e.getMessage());
            return 0;
        }
    }

    private String generateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256算法不可用", e);
            return UUID.randomUUID().toString();
        }
    }

    private int addToChroma(String content, Map<String, String> metadata) {
        try {
            String contentHash = generateContentHash(content);
            
            Embedding embedding = embeddingModel.embed(content).content();
            if (embedding == null) {
                log.warn("Embedding为空，跳过: {}", content.substring(0, Math.min(50, content.length())));
                return 0;
            }

            List<Double> embeddingList = new ArrayList<>();
            for (float f : embedding.vector()) {
                embeddingList.add((double) f);
            }

            Map<String, Object> request = new HashMap<>();
            request.put("ids", List.of(contentHash));
            request.put("documents", List.of(content));
            request.put("embeddings", List.of(embeddingList));
            request.put("metadatas", List.of(metadata));

            String url = chromaUrl() + "/collections/" + collectionName + "/add";
            restTemplate.postForObject(url, request, Map.class);
            return 1;
        } catch (Exception e) {
            log.error("添加到Chroma失败: {}", e.getMessage());
            return 0;
        }
    }

    public List<TextSegment> search(String query, int maxResults) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            if (queryEmbedding == null) {
                return List.of();
            }

            List<Double> embeddingList = new ArrayList<>();
            for (float f : queryEmbedding.vector()) {
                embeddingList.add((double) f);
            }

            Map<String, Object> request = new HashMap<>();
            request.put("query_embeddings", List.of(embeddingList));
            request.put("n_results", maxResults);
            request.put("include", List.of("documents", "metadatas", "distances"));

            String url = chromaUrl() + "/collections/" + collectionName + "/query";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> result = response.getBody();
            if (result == null) {
                return List.of();
            }

            List<?> documents = (List<?>) result.get("documents");
            List<?> metadatas = (List<?>) result.get("metadatas");

            List<TextSegment> segments = new ArrayList<>();
            if (documents != null && !documents.isEmpty()) {
                List<?> firstDoc = (List<?>) documents.get(0);
                List<?> firstMeta = (List<?>) metadatas.get(0);

                for (int i = 0; i < firstDoc.size(); i++) {
                    String document = (String) firstDoc.get(i);
                    
                    Map<String, Object> metaMap = (Map<String, Object>) firstMeta.get(i);
                    Metadata metadata = Metadata.from(metaMap);
                    TextSegment segment = TextSegment.from(document, metadata);
                    segments.add(segment);
                }
            }

            return segments;
        } catch (Exception e) {
            log.error("Chroma搜索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 重置（删除）整个 photography_dictionary 向量集合。
     * 触发场景：
     *   · 修复向量乱码：MySQL中文被双重编码后写入Chroma，检索到的上下文也是乱码
     *   · 数据大规模变动后哈希不再匹配
     *   · 向量数与MySQL词条数不匹配的极端情况
     *
     * 注意：删除后 ragService.syncKnowledgeBase() 会从 MySQL 重新 embedding 重建；
     *       50 条术语约 30~60 秒，期间检索结果为空属正常现象。
     */
    public void resetCollection() {
        String url = chromaUrl() + "/collections/" + collectionName;
        try {
            restTemplate.delete(url);
            log.info("Chroma 集合 [{}] 已删除（重置成功，等待重建）", collectionName);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound nfe) {
            // 集合本来就不存在 → 不用管，下次sync会自动创建
            log.warn("Chroma 集合 [{}] 本来就不存在（无需重置）", collectionName);
        } catch (Exception e) {
            log.error("删除 Chroma 集合失败：{}", e.getMessage());
            // 不抛出：让 syncKnowledgeBase() 继续尝试，万一 Chroma 服务重启也会因为向量数=0 自动重同步
        }
    }
}