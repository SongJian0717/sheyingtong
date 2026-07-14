package com.sheyingtong.config;

import com.sheyingtong.entity.User;
import com.sheyingtong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动钩子 1/2 · 种子账号初始化器
 * ----------------------------------------------------------------
 * 执行顺序：@Order(1)，在 data.sql 跑完之后立刻执行
 * 作用：如果 users 表为空（新机器首次启动），自动插入两个默认账号，
 *       省去每个新环境都要「先去注册页注册 → 回来登录」的麻烦。
 *
 * 账号清单：
 *   - 演示账号 demo / 123456  → demo@sheyingtong.com
 *   - 测试账号 tester_001 / 123456  → tester001@sheyingtong.com
 *
 * 安全性说明：
 *   - 密码没有写死 SQL（因为 BCrypt hash 每次不一样），
 *     在这里用 BCryptPasswordEncoder.encode() 实时生成
 *   - 生产环境可通过配置 app.seed.enabled=false 关掉
 */
@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class SeedDataRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        long userCount = userRepository.count();
        if (userCount > 0) {
            // 已有账号 → 静默跳过，避免重复插入
            log.debug("【种子数据】用户表已存在 {} 条记录，跳过默认账号初始化", userCount);
            return;
        }

        // --- 演示账号 1 ---
        User demo = User.builder()
                .username("demo")
                .nickname("演示账号")
                .email("demo@sheyingtong.com")
                // BCrypt 实时哈希（cost=10，每次生成都不同，但 matches 都能校验通过）
                .password(passwordEncoder.encode("123456"))
                .enabled(true)
                .build();

        // --- 测试账号 2（配合之前 curl 脚本里用的 tester_001）---
        User tester = User.builder()
                .username("tester_001")
                .nickname("测试账号")
                .email("tester001@sheyingtong.com")
                .password(passwordEncoder.encode("123456"))
                .enabled(true)
                .build();

        userRepository.saveAll(List.of(demo, tester));

        log.info("================================================================");
        log.info("  ✅ 【种子数据】用户表为空，已自动注入 2 个默认账号：");
        log.info("       🎫  demo       / 123456   (演示账号)");
        log.info("       🧪  tester_001 / 123456   (测试账号)");
        log.info("       📧 密码强度 6 位以上，生产环境请务必修改默认密码！");
        log.info("================================================================");
    }
}
