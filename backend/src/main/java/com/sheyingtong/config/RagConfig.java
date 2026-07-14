package com.sheyingtong.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzh.BgeSmallZhEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RagConfig {

    @Value("${embedding.local-service-enabled:true}")
    private boolean localServiceEnabled;

    @Value("${embedding.local-service-url:http://localhost:8001}")
    private String localServiceUrl;

    @Bean
    public EmbeddingModel embeddingModel() {
        if (localServiceEnabled) {
            log.info("初始化本地Python向量服务: {}", localServiceUrl);
            return new LocalEmbeddingModel(localServiceUrl);
        } else {
            log.info("初始化BGE-small-zh嵌入模型（中文）");
            return new BgeSmallZhEmbeddingModel();
        }
    }
}