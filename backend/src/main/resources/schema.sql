-- =====================================================================
-- 社影通 · Schema 初始化脚本（schema.sql）
-- 执行时机：Spring Boot 启动时，Hibernate ddl-auto=update 建表完成后，
--           data.sql 之前执行（由 defer-datasource-initialization=true 保证）
-- 主要作用：
--   1. 确保 sheyingtong 数据库及所有表使用 utf8mb4_unicode_ci 字符集
--      （utf8mb4 支持完整 4 字节 UTF-8，包括所有中文、emoji、生僻字）
--
-- ⚠️ 注意：本脚本的所有 ALTER 语句在「表不存在」时会报错，
--    因此 application.yml 配置了 sql.init.continue-on-error=true，
--    报错会被忽略，后续 data.sql 仍能正常执行。
--    （首次启动建表时 Hibernate ddl-auto=update 会用连接字符集 utf8mb4
--     创建新表，所以本身就是 utf8mb4；ALTER 只对旧机器的旧表生效。）
-- =====================================================================

-- ---------- 1. 设置当前数据库字符集 ----------
ALTER DATABASE sheyingtong CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- 2. 修复所有业务表字符集（首次启动表不存在时会报错，已忽略）----------
-- 表：users（用户表，含 avatar Base64 头像，需用 utf8mb4）
ALTER TABLE users CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 表：photography_terms（摄影词典，中文主体）
ALTER TABLE photography_terms CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 表：gear（器材库）
ALTER TABLE gear CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 表：gear_categories（器材分类）
ALTER TABLE gear_categories CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 表：chat_sessions（AI 问答会话元数据）
ALTER TABLE chat_sessions CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 表：chat_messages（AI 问答消息内容）
ALTER TABLE chat_messages CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 表：photo_analyses（照片打分结果）
ALTER TABLE photo_analyses CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 表：retouch_suggestions（修图建议）
ALTER TABLE retouch_suggestions CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================================
-- 【重要】关于旧乱码数据的清理
-- =====================================================================
-- ⚠️ 由于 data.sql 使用 INSERT IGNORE，如果主键 id=1~50 已经存在
--    （哪怕 term 字段值是 ?????? 乱码），会直接跳过不更新，
--    导致修复字符集后页面看到的还是旧的乱码。
--
-- 👉 清理方法（只需执行一次！以后不要再跑，否则会丢失你手动新增的术语）：
--
--    方案 A · 一键脚本（最推荐，自动停止后端+改字符集+清表+重启）：
--      在项目根目录打开 PowerShell，运行：
--        powershell -ExecutionPolicy Bypass -File .\fix-dictionary-encoding.ps1
--
--    方案 B · 手动命令行清理：
--      在 PowerShell / CMD 执行：
--        mysql -uroot -p123456 sheyingtong -e "TRUNCATE photography_terms;"
--      然后重启后端 → data.sql 会自动重新插入 50 条干净术语。
--
--    方案 C · 图形化工具清理：
--      打开 Navicat / DBeaver / Workbench → 连 sheyingtong →
--      右键 photography_terms 表 → 截断表(TRUNCATE) → 确认 →
--      重启 Spring Boot 后端即可。
--
-- =====================================================================
-- 修复后验证（任选其一）：
--   1. 打开 http://localhost:5173/dictionary ，点分类下拉应该看到中文术语
--   2. 进 MySQL 执行：SELECT id, term FROM photography_terms LIMIT 5;
--      → 应该返回：三分法构图、黄金分割构图、引导线构图、框架式构图、对称构图
--   3. 如果还是 ?????? 或乱码，执行：
--        SHOW CREATE TABLE photography_terms;
--      看最后一行是不是 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
-- =====================================================================
