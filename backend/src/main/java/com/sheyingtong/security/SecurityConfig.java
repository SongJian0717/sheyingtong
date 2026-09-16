package com.sheyingtong.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 6 核心安全配置类
 * ----------------------------------------------------------------
 * 【设计思路】基于 JWT 无状态 Token 的前后端分离鉴权方案
 *  1) 关闭 CSRF：纯 JSON API + JWT 放在 Header，天然免疫 CSRF
 *  2) 启用 CORS：放行 Vite 开发端口(5173~5176)，线上改为读配置文件
 *  3) STATELESS 会话：服务端不存 HttpSession，完全靠 JWT claims 识别用户
 *  4) URL 权限分层：公开接口(词典/器材/静态图) permitAll，用户相关接口 authenticated
 *  5) Filter 顺序：JwtAuthenticationFilter 插入在 UsernamePasswordAuthenticationFilter 之前
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * 构建 Spring Security 过滤器链（核心）
     * ----------------------------------------------------------------
     * 鉴权决策：
     *  permitAll 层 - 公开只读：登录注册/词典查询/器材推荐/历史照片缩略图（不暴露隐私）
     *  authenticated 层 - 需登录：上传头像/AI聊天/照片打分/修图建议/删除历史
     *  anyRequest() 兜底 permitAll - 留给 RAG 搜索等新接口（生产环境建议改为 denyAll）
     *
     * @param http Spring Security 安全构建器
     * @return 装配完成的 SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Step 1：关闭 CSRF - 纯 API + JWT Header，不存在表单提交场景下的跨站伪造
                .csrf(AbstractHttpConfigurer::disable)
                // Step 2：启用 CORS，使用下方 corsConfigurationSource() 的规则
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Step 3：无状态会话，每个请求独立鉴权，不依赖 JSESSIONID cookie
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Step 4：URL 权限分层（从上到下匹配，顺序敏感，具体在前，兜底在后）
                .authorizeHttpRequests(auth -> auth
                        // ---------- Swagger / OpenAPI 文档静态资源 ----------
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()
                        // ---------- 无需登录的公开接口 ----------
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/dictionary/**").permitAll()
                        .requestMatchers("/api/gear/**").permitAll()
                        .requestMatchers("/api/photo/image/**").permitAll()
                        .requestMatchers("/api/retouch/image/**").permitAll()
                        // ---------- 必须登录的接口 ----------
                        .requestMatchers("/api/auth/user").authenticated()
                        .requestMatchers("/api/auth/user/**").authenticated()
                        .requestMatchers("/api/upload/**").authenticated()
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/photo/score").authenticated()
                        .requestMatchers("/api/photo/history").authenticated()
                        // DELETE 操作必须带 JWT，防止脚本批量删除用户数据
                        .requestMatchers(HttpMethod.DELETE, "/api/photo/**").authenticated()
                        .requestMatchers("/api/retouch/suggestions").authenticated()
                        .requestMatchers("/api/retouch/history").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/retouch/**").authenticated()
                        // 异步任务状态查询/重试：必须登录，且控制器内还会校验任务归属，
                        // 否则任何登录用户都能靠遍历 taskId 拿到别人的评分结果
                        .requestMatchers("/api/ai/tasks/**").authenticated()
                        // ---------- 摄影社区：写操作需登录（读操作公开，见下方 permitAll）----------
                        .requestMatchers("/api/works/mine").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/works", "/api/works/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/works/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").authenticated()
                        .requestMatchers("/api/feed").authenticated()
                        // 社区公开读：作品广场/详情/评论、用户主页/作品墙/粉丝关注列表
                        .requestMatchers(HttpMethod.GET, "/api/works", "/api/works/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/**").permitAll()
                        // 兜底：剩余 URL 放行（如 /api/rag/**）
                        .anyRequest().permitAll()
                )
                // Step 5：在账号密码过滤器之前插入 JWT 过滤器，先校验 Token
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    /**
     * BCrypt 密码哈希编码器
     * 单向不可逆，强度 cost=10 默认，登录时用 matches() 比对明文与数据库 hash 值
     * 相同明文多次 hash 结果不同（含随机salt），防止彩虹表反查
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 暴露 AuthenticationManager Bean 给 AuthService.login() 调用
     * Spring Security 6 默认不公开注入，需要通过 AuthenticationConfiguration 手动导出
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * CORS 跨域配置
     * 开发阶段放行 localhost:5173~5176 共4个Vite端口（多项目并行调试场景）
     * 生产部署：建议改为读取配置文件 app.cors.origins，严格限制域名
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的前端域名：默认Vite 5173 + 预留 5174~5176 多开场景
        config.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        // 允许前端请求携带 Cookie/Authorization Header（JWT 必须开）
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
