package com.sheyingtong.security;

import com.sheyingtong.entity.User;
import com.sheyingtong.repository.UserRepository;
import com.sheyingtong.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * JWT 鉴权过滤器（核心链路，OncePerRequestFilter 保证每个请求只走一次）
 * ----------------------------------------------------------------
 * 执行顺序：DispatcherServlet 之前 → 在 UsernamePasswordAuthenticationFilter 之前插入
 * 处理流程：
 *   1) 从请求头取 "Authorization: Bearer xxx"
 *   2) 解析 JWT → 拿到 username claims
 *   3) 查数据库验证用户存在 + Token 未过期
 *   4) 把 User 对象作为 principal 放进 SecurityContext
 *   5) 后续 Controller 可直接通过 Authentication.getPrincipal() 拿 User 实例
 *
 * 【关键设计】把 User 对象整体作为 principal，而不只放 username：
 *            ChatController / PhotoController 等不用再查一次数据库，减少 N+1
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    
    /**
     * 核心过滤器方法：从请求中解析 JWT → 验证 → 注入 SecurityContext
     * ----------------------------------------------------------------
     * 快速失败：没有 Authorization 头或不以 "Bearer " 开头 → 直接放行，交给下一个过滤器
     * 异常降级：任何解析/校验异常（过期、签名错、格式错）→ 记 debug 日志，不中断请求
     *          （因为 permitAll 的接口即使没 Token 也应该能访问）
     *
     * @param request  HTTP 请求（取 Authorization header）
     * @param response HTTP 响应（不修改，交给业务层处理）
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        
        // Step 1：快速失败 - 无 JWT 头直接放行，后续由 Security 权限规则决定是否 403
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Step 2：截取 "Bearer " 后的真正 Token（从 index=7 开始）
        jwt = authHeader.substring(7);
        
        try {
            // Step 3：解析 JWT payload → 拿 sub 字段 = 用户名
            username = jwtUtil.extractUsername(jwt);
            
            // Step 4：只在 SecurityContext 还没认证过时才注入（避免重复工作）
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<User> userOptional = userRepository.findByUsername(username);
                
                // Step 5：双重校验 - 用户存在 + Token 有效（签名+未过期）
                if (userOptional.isPresent() && jwtUtil.validateToken(jwt, username)) {
                    User user = userOptional.get();
                    
                    // Step 6：账号必须启用（预留软删除/禁用功能）
                    if (user.getEnabled()) {
                        // Step 7：把 User 对象直接放到 principal 位置
                        // （这里放整个 User 实体，而不是 username 字符串，
                        //  这样 ChatController.getUserId 可以直接强转 User 取 id，不用再查 DB）
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            // JWT 解析/过期/签名失败一律只记 debug 日志，不会抛异常给用户
            // （因为请求可能命中的是 permitAll 接口，即使 token 坏也应该能访问）
            logger.debug("JWT validation failed: " + e.getMessage());
        }
        
        // Step 8：无论鉴权是否成功，最终都必须调用 doFilter 放行到下一环
        filterChain.doFilter(request, response);
    }
}
