package com.sheyingtong.config;

import com.sheyingtong.repository.PhotographyTermRepository;
import com.sheyingtong.service.ChromaRagService;
import com.sheyingtong.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动钩子 2/2 · RAG 知识库自动同步器
 * ----------------------------------------------------------------
 * 执行顺序：@Order(2)，晚于 SeedDataRunner；data.sql 跑完 → 摄影术语已落 MySQL。
 * 作用：检测到 Chroma 向量库为空或条目数少于 MySQL 词条数，
 *       自动启动后台虚拟线程执行 ragService.syncKnowledgeBase()，
 *       让「新设备 clone 代码 → 启动」后，RAG 检索增强也自动就绪，
 *       不需要用户再去 curl POST /api/rag/sync。
 *
 * 触发条件（同时满足）：
 *   ① app.rag.auto-sync = true  (默认 true，生产可关)
 *   ② MySQL photography_terms 表非空 (>= 1 条)
 *   ③ Chroma 当前向量数 = 0  OR  向量数 < MySQL 词条数 * 0.9
 *      （允许小幅度差异，比如某条 embedding 失败时不至于每次启动都全量重 sync）
 *
 * 异常降级：
 *   - Chroma / vector-service 没启动 → catch 住异常 → 打印 WARN 日志，
 *     不阻塞主流程启动；后续用户手动调 /api/rag/sync 即可。
 *   - sync 时间较长（200条 1~3 分钟）→ 放在虚拟 Thread 里异步执行，
 *     不阻塞 Spring Boot 的 "Started application in X seconds" 日志。
 */
@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class RagAutoSyncRunner implements ApplicationRunner {

    private final ChromaRagService chromaRagService;
    private final PhotographyTermRepository photographyTermRepository;
    private final RagService ragService;

    /**
     * 生产可关：application.yml 里 app.rag.auto-sync=false，
     * 避免每次发布上线都触发全量 embedding 成本开销。
     */
    @Value("${app.rag.auto-sync:true}")
    private boolean autoSyncEnabled;

    /**
     * 修复乱码/脏数据的应急开关：application.yml 里 app.rag.force-reset-on-start=true，
     * 下次启动就会先 DELETE 整个 photography_dictionary 集合，再从 MySQL 全量重同步。
     * 用完记得改回 false，避免每次启动都全量重算 embedding 烧 Token / 浪费时间。
     * 触发条件：true  → 每次启动无条件删集合重建
     *          false（默认） → 仅当向量数=0 或 比词条数少 10%+ 才同步
     */
    @Value("${app.rag.force-reset-on-start:false}")
    private boolean forceResetOnStart;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoSyncEnabled) {
            log.info("【RAG自动同步】配置 app.rag.auto-sync=false，跳过自动同步");
            return;
        }

        // Step 0：如果开启了强制重建（修复乱码时用）→ 先删掉整个旧集合
        if (forceResetOnStart) {
            log.warn("================================================================");
            log.warn("  ⚠️  app.rag.force-reset-on-start = true，即将删除 Chroma 集合后重建");
            log.warn("       （用于修复向量乱码、向量哈希不匹配、检索结果异常等情况）");
            log.warn("================================================================");
            try {
                chromaRagService.resetCollection();
                log.info("【RAG自动同步】已删除旧 photography_dictionary 集合，准备全量重建");
            } catch (Exception e) {
                log.warn("【RAG自动同步】删除旧集合失败（可能集合不存在，忽略即可）：{}", e.getMessage());
            }
        }

        // Step 1：看 MySQL 有没有词条数（刚启动新机器还没写 data.sql 可能为 0）
        long termCount = photographyTermRepository.count();
        if (termCount == 0) {
            log.warn("【RAG自动同步】MySQL photography_terms 表为空 → 跳过自动同步"
                    + "（等 data.sql 跑完或手动 curl POST /api/rag/sync）");
            return;
        }

        // Step 2：查询 Chroma 向量数（Chroma/vector 还没起会异常，直接降级）
        // 注：ChromaRagService.getDocumentCount() 返回的是 int（不是 Long）
        int chromaCount;
        try {
            chromaCount = chromaRagService.getDocumentCount();
        } catch (Exception e) {
            log.warn("【RAG自动同步】无法连接 ChromaDB({})，暂时跳过自动向量化；"
                            + "等 Chroma/vector-service 启动完成后可手动 curl POST /api/rag/sync 触发",
                    e.getMessage().split("\\R", 2)[0]);
            return;
        }

        // Step 3：判断是否要同步（空库 或 向量数差了 10%+）
        // termCount 是 long，转 int 时先限一下 int 最大值，避免 20 亿条以上溢出（个人项目不会到）
        int termCountInt = termCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) termCount;
        boolean needSync = (chromaCount == 0) || (chromaCount < (int) (termCountInt * 0.9));
        if (!needSync) {
            log.info("【RAG自动同步】跳过：MySQL词条数={}，Chroma向量数={}，两者匹配",
                    termCount, chromaCount);
            return;
        }

        log.info("================================================================");
        log.info("  📚 【RAG自动同步】检测到词条与向量不匹配，触发全量同步");
        log.info("       - MySQL photography_terms : {} 条", termCount);
        log.info("       - Chroma 当前向量数       : {} 条", chromaCount);
        log.info("       - 同步方式                : 后台守护线程异步执行（不阻塞启动）");
        log.info("       - 预计耗时                : 200条 约 1~3 分钟，视 embedding 服务速度");
        log.info("================================================================");

        // Step 4：异步执行 sync，不阻塞 Spring Boot "Started application in Xs" 的启动日志
        //         注：JDK 17 没有 Thread.ofVirtual()，用普通守护线程；生产用 JDK 21 可换虚拟线程
        Thread syncThread = new Thread(() -> {
            long t0 = System.currentTimeMillis();
            try {
                ragService.syncKnowledgeBase();
                // sync 完成后再读一次 Chroma 条数（返回 int，不是 Long）
                int newCount = 0;
                try {
                    newCount = chromaRagService.getDocumentCount();
                } catch (Exception ignored) {
                }
                log.info("================================================================");
                log.info("  ✅ 【RAG自动同步】完成！耗时 {} ms，Chroma 当前向量数：{}",
                        System.currentTimeMillis() - t0, newCount);
                log.info("       - 现在可以测试 AI 问答（含 % 字符不会误解码）");
                log.info("       - curl /api/rag/status 可查看状态");
                log.info("================================================================");
            } catch (Exception e) {
                log.error("【RAG自动同步】执行失败：{}；可稍后手动 curl POST /api/rag/sync 重试",
                        e.getMessage(), e);
            }
        }, "rag-auto-sync");
        syncThread.setDaemon(true);   // 守护线程：主进程退出时自动跟着结束，不会卡 JVM 关闭
        syncThread.start();
    }
}
