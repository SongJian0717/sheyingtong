package com.sheyingtong.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("摄影通 (SheYingTong) API 文档")
                        .version("1.0.0")
                        .description("AI 驱动的摄影学习平台后端接口文档。提供用户认证、AI 照片打分、修图建议、智能聊天助手、摄影词典 RAG 检索、器材选购推荐等核心能力。\n\n" +
                                "**鉴权说明**：本项目使用 JWT Bearer Token 无状态鉴权。\n" +
                                "- 调用 `/api/auth/login` 或 `/api/auth/register` 获取 `token`\n" +
                                "- 在需要认证的接口请求头中添加：`Authorization: Bearer <token>`\n\n" +
                                "**主要模块**：\n" +
                                "- 认证模块：注册、登录、用户信息、修改密码、统计数据\n" +
                                "- 照片打分：上传图片 → AI 六维度评分（构图/光线/色彩/对焦/内容/建议）\n" +
                                "- 修图建议：上传图片 → AI 生成 Lightroom/Capture One 风格调色参数\n" +
                                "- AI 聊天：摄影知识问答（含 Function Calling + RAG + 多模态图片输入）\n" +
                                "- 摄影词典：术语查询、分类浏览、RAG 向量检索\n" +
                                "- 器材选购：按预算/题材/级别/品牌智能推荐器材")
                        .contact(new Contact()
                                .name("摄影通开发团队")
                                .url("http://localhost:5173")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("本地开发环境"),
                        new Server().url("http://127.0.0.1:8080").description("本地开发环境 (IPv4)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("请输入 JWT Token，格式：`Bearer <token>`。可通过 `/api/auth/login` 接口获取。")
                        ));
    }
}
