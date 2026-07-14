package com.sheyingtong.config;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class LocalEmbeddingModel implements EmbeddingModel {

    private final String serviceUrl;
    private final RestTemplate restTemplate;

    public LocalEmbeddingModel(String serviceUrl) {
        this.serviceUrl = serviceUrl;
        this.restTemplate = new RestTemplate();
        log.info("LocalEmbeddingModel initialized with service URL: {}", serviceUrl);
    }

    @Override
    public Response<Embedding> embed(String text) {
        Response<List<Embedding>> response = embedAll(List.of(TextSegment.from(text)));
        List<Embedding> embeddings = response.content();
        return embeddings == null || embeddings.isEmpty() ? Response.from(null) : Response.from(embeddings.get(0));
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        try {
            String url = serviceUrl + "/embed";
            List<String> texts = textSegments.stream()
                    .map(TextSegment::text)
                    .toList();
            Map<String, Object> request = Map.of("texts", texts);
            
            log.debug("Calling local embedding service: {}, texts count: {}", url, texts.size());
            
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            
            if (response != null && response.containsKey("embeddings")) {
                List<List<Double>> embeddingData = (List<List<Double>>) response.get("embeddings");
                List<Embedding> result = new ArrayList<>();
                for (List<Double> embedding : embeddingData) {
                    float[] floatArray = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        floatArray[i] = embedding.get(i).floatValue();
                    }
                    result.add(Embedding.from(floatArray));
                }
                return Response.from(result);
            } else {
                log.warn("Embedding service returned invalid response: {}", response);
                return Response.from(List.of());
            }
        } catch (Exception e) {
            log.error("Failed to call local embedding service: {}", e.getMessage());
            return Response.from(List.of());
        }
    }
}