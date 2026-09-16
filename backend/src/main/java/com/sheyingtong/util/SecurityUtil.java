package com.sheyingtong.util;

import com.sheyingtong.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具：从 SecurityContext 取 JWT 过滤器注入的 User principal
 * 未登录（匿名 principal 为字符串）时返回 null
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        return null;
    }
}
