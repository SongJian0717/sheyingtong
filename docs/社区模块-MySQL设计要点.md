# 摄影社区模块 · MySQL 表设计与索引优化要点

> 面向简历 / 面试讲解用。本文是"社影通（AI 摄影平台）"上扩展的**摄影社区**子系统的数据库设计复盘：
> 4 张新表 + 冗余计数 + 唯一索引兜底 + 自增主键游标分页。全部基于 **MySQL 8 + InnoDB**，
> 由 Spring Boot JPA（`ddl-auto: update`）自动建表，下方 DDL 为期望的目标结构。

---

## 1. 模块职责与表关系

社区模块新增能力：**发布作品、作品广场、关注动态 Feed、点赞、评论、关注/粉丝**。

复用现有能力，不复制文件、不新增文件服务器：

- 作品封面图 = 已有公开图片接口 `/api/photo/image/{photoAnalysisId}`
- 作品 AI 六维评分摘要 = 已有公开打分详情 `/api/photo/detail/{photoAnalysisId}`
- `works.photo_analysis_id` 只存引用 id，由**唯一索引**保证"一条 AI 打分最多发布一篇作品"

```
users ◄──┐
users ◄──┼──── follow(follower_id, followee_id)         关注（双向均可）
         │
users ◄──┼──── works.user_id                             作品归属
photo_analysis ──(仅id引用)──► works.photo_analysis_id   唯一 1:1
         │
works ◄──┴──── work_like(user_id, work_id)               点赞明细
works ◄────── work_comment(user_id, work_id, parent_id)  评论(只一层楼中楼)
```

| 表 | 一句话职责 | 典型容量路径 |
|---|---|---|
| `works` | 摄影作品主表，冗余 `likes_count/comments_count` | 广场 / 我的作品 / 用户主页 |
| `follow` | 关注关系表（只存一对关系，无冗余列） | 粉丝列表 / 关注列表 / Feed 子查询 |
| `work_like` | 点赞明细表 | 判重、删作品级联清理 |
| `work_comment` | 评论明细表，`parent_id` 支持一层回复 | 评论列表、删评论级联 |

---

## 2. 目标 DDL（MySQL 8 / InnoDB / utf8mb4）

```sql
-- 摄影作品
CREATE TABLE works (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  user_id           BIGINT       NOT NULL,
  photo_analysis_id BIGINT       NOT NULL,
  title             VARCHAR(100) NOT NULL,
  content           TEXT,
  is_public         TINYINT(1)   NOT NULL DEFAULT 1,
  likes_count       INT          NOT NULL DEFAULT 0,
  comments_count    INT          NOT NULL DEFAULT 0,
  created_at        DATETIME(6)  NOT NULL,
  updated_at        DATETIME(6)  NULL,
  PRIMARY KEY (id),
  -- 一条 AI 打分只能发布一篇作品（并发防重复发布的最终防线）
  CONSTRAINT uk_works_analysis UNIQUE (photo_analysis_id),
  KEY idx_works_user   (user_id),
  -- 广场"最新"游标页：WHERE is_public=1 AND id<:cursor ORDER BY id DESC
  KEY idx_works_latest (is_public, id),
  -- 热门榜：ORDER BY likes_count DESC, id DESC 直接 Top-N，免 filesort
  KEY idx_works_rank   (is_public, likes_count, id),
  CONSTRAINT fk_works_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摄影作品';

-- 关注关系（A→B、B→A 是两条不同记录，因此唯一键是有方向的组合）
CREATE TABLE follow (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  follower_id BIGINT      NOT NULL,
  followee_id BIGINT      NOT NULL,
  created_at  DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  -- 同一个人不能重复关注同一人
  CONSTRAINT uk_follow UNIQUE (follower_id, followee_id),
  -- 粉丝侧查询：WHERE followee_id=?
  KEY idx_follow_followee (followee_id),
  CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES users (id),
  CONSTRAINT fk_follow_followee FOREIGN KEY (followee_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注关系';

-- 点赞明细
CREATE TABLE work_like (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  work_id    BIGINT      NOT NULL,
  user_id    BIGINT      NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  -- 同一用户对同一作品只能赞一次（重复点击并发兜底）
  CONSTRAINT uk_like UNIQUE (user_id, work_id),
  -- 删除作品时按 work_id 级联清点赞
  KEY idx_like_work (work_id),
  CONSTRAINT fk_like_work FOREIGN KEY (work_id) REFERENCES works (id),
  CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞明细';

-- 评论明细（parent_id 为空=顶楼，非空=回复；只做一层）
CREATE TABLE work_comment (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  work_id    BIGINT       NOT NULL,
  user_id    BIGINT       NOT NULL,
  parent_id  BIGINT       NULL,
  content    VARCHAR(500) NOT NULL,
  created_at DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  -- 评论列表按作品取 + id 倒序游标页，同时服务"删作品清评论"
  KEY idx_comment_work (work_id, id),
  KEY idx_comment_user (user_id)
  -- 故意不建 parent_id 外键：自关联外键会显著拖慢"删顶楼连带删子回复"的级联成本
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品评论';
```

> 说明：项目走 Hibernate `ddl-auto: update` 自动建表，表结构以上方 DDL 为准。
> 若本机 MySQL 里残留过旧版索引（如旧的 `idx_works_public`/`idx_works_hot` 单列索引），
> Hibernate update 不会删除旧索引，属正常现象，可手动 `DROP INDEX` 清理。

---

## 3. 索引依据（面试讲解版）

### 3.1 唯一索引 = 并发去重的"最后一道防线"

业务层先做"查重再插入"，但**两个并发请求都通过查重时，最终靠唯一索引挡住重复行**：

| 唯一键 | 防什么 | 为什么不只靠业务判断 |
|---|---|---|
| `uk_works_analysis(photo_analysis_id)` | 同一张打分图被发布两次 | 双击"发布"、或两处入口同时提交 |
| `uk_follow(follower_id, followee_id)` | 重复关注 | 关注按钮连点 / 接口重放 |
| `uk_like(user_id, work_id)` | 重复点赞 | 点赞连点 / 接口重放 |

InnoDB 唯一索引本身就是一棵 B+Tree，`exists` 判重与唯一约束共用同一个索引，
插入时的唯一性检查不需要额外加锁逻辑即可保证不重复计数。

### 3.2 复合索引让"查询 + 排序"一次命中

排序字段要"进索引"，否则 MySQL 要先把结果全部塞进临时表做 filesort。
社区模块两个最高频的排序查询，都让复合索引的**前导列做过滤、后续列直接给出有序结果**：

```sql
-- ① 广场最新页：过滤 + 倒序一步到位（走 idx_works_latest）
SELECT * FROM works
WHERE is_public = 1 AND id < :cursor
ORDER BY id DESC LIMIT :size;

-- ② 热门榜：直接按 (is_public, likes_count) 定位后向前扫 N 行（走 idx_works_rank）
SELECT * FROM works
WHERE is_public = 1
ORDER BY likes_count DESC, id DESC LIMIT :size;
```

`idx_works_latest(is_public, id)`：找到 `id < :cursor` 的边界行后，在索引内**从边界向前扫 size 行**即返回，天然有序、零 filesort。
`idx_works_rank(is_public, likes_count, id)`：前导 `is_public` 过滤私密作品，随后按 `likes_count DESC` 取前 N 条；`id` 收尾让"同分按新作品优先"也走索引。

其余单列/组合索引一句话解释：

- `idx_works_user(user_id)`：二级索引叶子含主键，单用户的作品天然按 `id` 有序 → 我的作品 / 用户主页作品墙。
- `uk_follow` 前缀 `(follower_id, …)`：顺带覆盖"我关注了谁 / 我关注数"，不必另建 follower 单列索引。
- `idx_follow_followee(followee_id)`：覆盖"谁关注了我 / 粉丝数 / 粉丝列表"。
- `idx_like_work(work_id)`：删除作品时 `DELETE FROM work_like WHERE work_id=?` 的必选索引，否则删除会全表扫。
- `idx_comment_work(work_id, id)`：评论游标页 `WHERE work_id=? AND id<:cursor ORDER BY id DESC` + 删作品清评论，双场景一次命中。
- `idx_comment_user(user_id)`：未来"我收到的评论"类查询预留。

---

## 4. 冗余计数列与一致性（重点）

### 4.1 为什么冗余

`likes_count / comments_count` 挂在 `works` 行上。读广场/详情页时直接取列，
**永远不需要 `COUNT(*)`**——两个明细表是"低频写、高频读"，把计数从"每次现算"变成"写时维护、读时 O(1)"。

### 4.2 怎么保证不漂移 —— 明细与计数在同一事务

以点赞为例（评论、取消赞同理）：

```
@Transactional
1. 幂等查重：uk_like 上 exists(user, work) 已存在 → 直接返回当前计数
2. INSERT work_like (user_id, work_id)
3. UPDATE works SET likes_count = likes_count + 1 WHERE id = ?
   -- 同事务提交：要么明细和计数一起成功，要么一起回滚
```

- 计数更新用 `likes_count = likes_count + 1`（列自增），不是先查后写，天然无并发覆盖。
- 极端并发下两个请求同时通过第 1 步查重，第 2 步 INSERT 会撞上 `uk_like` 唯一索引 → 异常回滚整个事务 → 明细不会重复、计数不会加两次。**这就是唯一索引兜底的意义**。
- 删除作品/删除顶楼评论在同一事务里先删明细（子回复连带删），再对计数做对应的整段扣减，保证删除后计数依然等于剩余行数。

> 面试话术：一致性靠**数据库本地事务 + 唯一索引**闭环，没有引入 Redis / MQ 做计数或事件补偿，
> 数据从第一行写入开始就强一致，读侧也不会有中间态。

### 4.3 应用层替代物理外键的一处取舍

`works.photo_analysis_id` **没有建物理外键**，而是用唯一索引 + 服务层校验：

- 发布时：校验该打分记录属于当前用户且未发布 → 插 `works`，唯一索引拦截并发重复发布。
- 删除打分记录时：先查 `uk_works_analysis` 对应作品，已发布则拒绝并提示
  `"该照片已发布为作品，请先在『我的作品』中删除对应作品"`。

原因：物理外键在"删打分记录"时只会给 `RESTRICT/NO ACTION` 生硬报错，服务层校验可以给出**业务语义化的中文提示**，同时保留唯一索引守住重复发布。

---

## 5. 游标分页：用自增主键替代 OFFSET 深分页

### 5.1 为什么不用 `LIMIT 10 OFFSET 100000`

`OFFSET` 越大，MySQL 越要**扫描并丢弃前 N 行**，还要对每页结果做一次全量排序，深分页是典型的慢查询源头。

### 5.2 实现（size + 1 判断是否还有下一页）

`works` / 评论 / Feed 的列表全部采用同一套"主键游标"方案：

```
取第 1 页：SELECT ... WHERE is_public = 1            ORDER BY id DESC LIMIT :size + 1
取第 N 页：SELECT ... WHERE is_public = 1 AND id < :cursor ORDER BY id DESC LIMIT :size + 1

返回：items = 前 size 条
      hasMore  = 实际取到 > size 条
      nextCursor = 本页最后一条的 id（前端下一页传回来）
```

- 游标用**自增主键 `id`**：既近似发布时间，又**唯一且不会并列**，天然稳定（时间戳字段容易同秒重复导致翻页错漏/重复）。
- 每次多取 1 条判断 `hasMore`，比单独 `COUNT(*)` 再翻页少一次查询。
- 热门榜 `hot` 属于 Top-N 榜单，直接 `LIMIT size` 一次取完，不提供游标翻页。
- 前端把 `nextCursor` 原样带回，深度任意也不退化；游标不暴露数据量 `total`，规避了"统计大表行数"的成本。

---

## 6. 可直接粘贴的简历 bullet

- 在 Spring Boot 单体项目中新增摄影社区子系统，独立设计并落地 4 张 MySQL 表
  （works / follow / work_like / work_comment）：用 `uk_works_analysis`、`uk_follow`、`uk_like`
  三张唯一索引作为并发去重的最终防线，不依赖 Redis / MQ 即可保证不重复发布、关注与点赞。
- 对点赞、评论采用**冗余计数列 + 本地事务内明细与计数同步 ±1** 的方案，读侧 O(1) 取数、
  无需 COUNT，用数据库本地事务闭环一致性，替代引入消息队列或缓存中间件的复杂度。
- 全站列表（广场 / 评论 / 关注 Feed / 用户作品墙）用**自增主键游标分页**
  （`id < :cursor ORDER BY id DESC LIMIT size+1` 判 hasMore）替换深分页 `OFFSET`，
  任意深度查询不退化，并避免大表 `COUNT(*)`。
- 索引与排序强绑定：`(is_public, id)` 复合索引让广场最新页免 filesort；
  `(is_public, likes_count, id)` 让热门 Top-N 直接走索引序取数；删除作品/评论在同事务内
  按 `work_id` 走二级索引级联清理明细，保证冗余计数永不失衡。

---

## 7. 本地冒烟验证清单（双账号）

前置：先后端启动自动建表 + 前端 `npm run dev`。

1. 打开 `http://localhost:5173/`，确认**未登录**时能直接访问 `/community`（作品广场为空态）。
2. 账号 A 登录 → 照片打分上传一张图 → 历史卡片悬停点 **发布到广场** → 弹窗填标题发布成功；
   卡片变为"✓ 已发布到广场"，再次尝试发布/删除该打分记录均被后端拦截并提示原因。
3. 退出 A，注册/登录账号 B → 广场能看到 A 的作品卡片（图片、作者、点赞数）→ 打开详情：
   可见大图 + AI 评分摘要 + 评论；B 点赞、评论 A 的作品（点赞按钮与计数即时变化）。
4. B 访问 A 的主页（点头像 / `/user/:username`）→ 关注 A → 关注后进入 `/feed` 能看到 A 刚发布的作品。
5. B 越权删除 A 的作品/顶楼评论 → 后端返回 400 拒绝；A 重新登录后删除自己的作品成功，
   `works.likes_count/comments_count` 与明细同步清零。
6. `SHOW CREATE TABLE works\G` 确认存在 `uk_works_analysis`、`idx_works_latest`、`idx_works_rank`。
