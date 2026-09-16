# 社影通 · Redis 缓存设计方案

> 面向简历 / 面试讲解用。本文回答一个问题：**在这个"AI + 社区一体化"的摄影平台上，哪些地方真的需要 Redis 缓存、为什么、怎么缓存、边界在哪**。
> 全部结论基于对现有代码的实测盘点（端点、Repository 方法、种子数据规模、前端访问模式），
> 与 `docs/社区模块-MySQL设计要点.md` 配套：那边讲"读得准（索引/分页）"，这边讲"读得少（缓存）"。

---

## 0. 一句话结论

> **Tier A 静态知识库（词典/器材）整表缓存 → Tier B 用户聚合统计（现算 COUNT）短 TTL 缓存 → Tier C 社区热读首屏缓存。**
> 图片流不进 Redis（改 HTTP 缓存），聊天消息不缓存，写路径只做失效方；计数热行问题另立二期用"Redis 计数 + 异步刷库 + 对账"解决。

---

## 1. 现状盘点：为什么是现在谈缓存

### 1.1 有利条件（Redis 其实已经进项目了）

| 事实 | 位置 | 对本方案的意义 |
|---|---|---|
| 已引入 `spring-boot-starter-data-redis` | `backend/pom.xml` | 依赖就位，无需加包 |
| 已配置 `spring.data.redis`（localhost:6379，超时 500ms，lettuce 池 max-active=2） | `application.yml` L78-92 | 连接配置可复用。**但要清楚：classpath 里没有 commons-pool2，Spring Data Redis 不会启用连接池**，这段 pool 配置目前是空转的，实际走 Lettuce 默认的共享连接（`mvn dependency:tree -Dincludes=org.apache.commons:commons-pool2` 查不到即为此）；缓存读量上来也先不用调池，真要池化得先显式引 commons-pool2 |
| 已有可参考的降级范式：`RedisChatMemoryStore`（key 前缀 `sheyingtong:chat:memory:`，TTL 24h，惰性取 `StringRedisTemplate`，连不上自动降级 JVM 内存） | `service/RedisChatMemoryStore.java` | 新缓存层直接沿用"连不上就跳过缓存查库"的思路 |
| 无 `@Cacheable` / 无 `spring-boot-starter-cache` / 无 Caffeine / 无限流拦截器 | 全库检索确认 | 说明**没有历史包袱**，可以从零设计一套显式的缓存层 |

### 1.2 读路径现存问题（缓存的靶子）

| 问题 | 具体表现 | 位置 |
|---|---|---|
| **聚合统计每次现算** | 用户主页每次 1+N 个 COUNT（作品数/粉丝数/关注数）；个人中心统计一次 7 个 COUNT | `CommunityService.buildUserProfile` L338；`AuthService.getUserStatistics` L149-167 |
| **静态数据被反复查库** | 词典 50 条、器材 36 条，全部接口无写入口，却每次请求都打 MySQL；AI 每轮对话也会查同一批表 | `DictionaryService` 全部方法；`GearService` 全部方法；`PhotographyTools` L27/45/72 |
| **列表回源带 N+1** | 作品列表逐条 `toAuthor(work.getUser())` 初始化 LAZY user；评论列表逐条 `comment.getUser()`；打分/修图历史在序列化期懒加载 user | `CommunityService.toWorkResponse` L377、`pageComments` L251、`PhotoService.getHistory` L270 |
| **首屏必拉 & 重复拉** | 广场首屏每次进入请求 `size=12`；切"最新/热门"各一发；个人中心 `statistics + /works/mine(全量)`；打分页也拉 `/works/mine`；器材页一次挂载发 4 个请求（其中 `size=9999` 全库拉取只为算分类计数） | 前端 `CommunityPage` / `ProfilePage` / `PhotoScorePage` / `GearRecommendPage` |
| **每次整页刷新多一次鉴权查询** | `App.vue` 启动校验 token → `GET /api/auth/user`；而 `JwtAuthenticationFilter` 每个请求还会 `findByUsername` 查一次用户 | `App.vue` onMounted；`security/JwtAuthenticationFilter` |
| **大字段随列表重复下发** | `User.avatar` 是 `@Lob LONGTEXT`（Base64），作品卡/评论/主页每条都带一份 | `entity/User.java` |

> 注意：**N+1 与"列表大字段"必须在加缓存之前或同时处理**。缓存只能省掉"命中的那一次读"，冷启动/失效回源时若仍是 N+1，缓存重建依然慢（详见 §7）。

---

## 2. 判定模型：什么样的读才值得进 Redis

对每个读路径按三个维度打分，再落到四档决策：

```
                     读多写少？
                    ┌── 是 ──┐            否（写路径 / 高频变更）
                    ▼        ▼                    │
              数据能否容忍           ┌─────────────┘
              短时不一致？           ▼
              ┌── 是 ──┐        不进缓存，只做失效方
              ▼        ▼
        首屏/热点？   否（深页/低频）
        ┌─ 是 ─┐        │
        ▼      ▼        ▼
     Tier C   Tier B   不缓存（靠索引，见 MySQL 设计文档）
        │        │
        └── 写入口是否存在？── 无写入口 ──► Tier A（可长 TTL / 整表缓存）
```

| 档位 | 判定特征 | TTL 策略 | 失效策略 |
|---|---|---|---|
| **Tier A** 静态知识库 | 数据量小、**无任何写接口**、每次请求都查 | 长（小时级） | 预留版本号，将来有管理端再 bump |
| **Tier B** 聚合统计 | 写少读多、一次请求多个 COUNT、允许秒级延迟 | 短（30~60s） | 相关写操作**主动删 key** |
| **Tier C** 社区热读 | 读多写中、列表/详情首屏、允许十几秒延迟 | 极短（10~20s）+ 抖动 | 结构性写操作删 key；点赞这类高频写靠 TTL 兜底 |
| **Tier D** 其余 | 图片二进制流、聊天消息、写路径、深游标页 | —— | 不缓存或换手段（HTTP 缓存/Nginx） |

---

## 3. 缓存候选总表

### 3.1 Tier A：静态知识库（建议第一批落地）

| 目标 | 端点 / 方法 | 位置 | 决策 |
|---|---|---|---|
| 词典术语全量 | `GET /api/dictionary/terms`、`POST /api/dictionary/search`、`GET /api/dictionary/term/{term}`、`GET /api/dictionary/categories`、`GET /api/dictionary/category-count` | `DictionaryController` L28/41/50/62/69；`DictionaryService` L21-51 | **整表缓存 + 内存过滤分页** |
| 器材全量 | `GET /api/gear/search`、`/gear/{id}`、`/gear/categories`、`/gear/brands`、`/gear/popular` | `GearController` L48/64/73/80/87；`GearService` L25-145 | **整表缓存 + 内存过滤** |
| AI 工具侧复用 | `queryDictionary` / `searchGear` / `searchGearByBudget` | `PhotographyTools` L27/45/72 | 复用同一份缓存，**AI 每轮对话都打这里** |

**为什么不逐页缓存**：源数据只有 terms 50 条（data.sql，id 1~50）、gear 36 条、gear_categories 6 条，整份 JSON 在几百 KB 内；而分页接口返回的是 Spring `Page`（含分页元数据，JSON 反序列化麻烦）。整表缓存后，**分页/分类过滤/关键字匹配全部在内存完成**，既省掉序列化坑，又让"搜索"这类原本要打库的接口变成纯内存运算。

**备选（数据量增长后）**：`gear.recommend` 目前 `findAll()` 全表过滤（`GearService` L36-38），整表缓存后同样受益。

### 3.2 Tier B：用户聚合统计（现算 COUNT → 缓存）

| 目标 | 端点 / 方法 | 每次请求的 DB 次数 | TTL | 失效来源 |
|---|---|---|---|---|
| 用户公开主页 | `GET /api/users/{userId}`、`GET /api/users/username/{username}` | user 1 + 已关注判断 1 + works/followers/following 3 个 COUNT ≈ **4~5 次** | 60s | 关注/取关、发布/删除作品 |
| 个人中心统计 | `GET /api/auth/user/statistics` | user 1 + 打分/会话/修图/作品/粉丝/关注共 **7 个 COUNT** | 30~60s | 关注/取关、发布/删除作品、新增/删除打分与修图记录、新建会话 |
| （可选）登录态用户实体 | `JwtAuthenticationFilter` 每请求 `findByUsername` | 1 次/请求 | 60s | 改资料、改密码、传头像 |

> 主页与统计缓存的是**同一批计数值**，可让统计接口复用 `user:stats:{userId}` 的一份缓存，避免两套 key 打架。

### 3.3 Tier C：社区热读首屏

| 目标 | 端点 | 热度依据 | TTL | 是否个人化 |
|---|---|---|---|---|
| 作品广场首屏 | `GET /api/works?sort=latest\|hot&size=12` | 每次进入 + 每次切排序各一发 | 15s | 否（`liked` 不在列表里） |
| 用户作品墙首屏 | `GET /api/users/{id}/works?size=12` | 每次进他人主页必发 | 20s | 否 |
| 关注动态先页 | `GET /api/feed?size=12` | 登录用户每次进 Feed 页 | 20s | **是**（按 userId 分 key） |
| 作品详情 | `GET /api/works/{id}` | 详情是最热读；从列表点进必发 | 30s | **含 `liked` 字段 → 只缓存公共部分** |
| 评论首屏 | `GET /api/works/{id}/comments?size=20` | 详情页紧随其后发 | 15s | 否 |
| 深度游标页（cursor 非空） | 同上各列表 | 命中率低、key 爆炸 | —— | **不缓存**，靠 `(is_public, id)` 复合索引 |

**详情页的个人化处理**：`WorkDetailResponse` 里 `liked` 是"当前登录者是否点过赞"（`CommunityService.getWorkDetail` L147-168 里现查 `workLikeRepository.existsByWorkIdAndUserId`）。缓存中**只存不带 `liked` 的公共部分**，命中缓存后在内存里补一次 `liked`（一次走 `uk_like` 索引的判存，成本极低），避免"甲点过赞、乙看到已点赞"的串号事故。

### 3.4 Tier D：不进 Redis 的清单（同样是设计结论）

| 对象 | 结论 | 正确做法 |
|---|---|---|
| `/api/photo/image/{id}`、`/api/retouch/image/{id}` | **不用 Redis** | 这是磁盘文件流（`FileSystemResource` 实时读 `./uploads`），二进制进 Redis 是内存灾难。应加 `Cache-Control`/`ETag` + Nginx 静态托管；前端已用 `<img>` 原生请求 |
| 聊天消息 `GET /api/chat/session/{id}/messages` | 不缓存 | 按会话小分页查库即可；且当前实时性要求高，缓存收益 < 一致性问题 |
| 所有写路径（发布/点赞/评论/关注/打分/修图） | 不作缓存对象 | 只作为**失效触发方**（见 §6） |
| AI 分析结果（打分/修图详情） | 暂不缓存 | 单次写入后基本不变，属"一写多读"，但价值低于 Tier A/B/C；二期若需要，按 id 缓存长 TTL |

---

## 4. 缓存设计规范

### 4.1 实现风格：手写 `CacheService`（不用 `@Cacheable`）

选型理由（面试可直接讲）：

- 项目**现有习惯就是显式**：`RedisChatMemoryStore` 手写读写 + 降级，没有引入 Spring Cache 抽象；
- `@Cacheable` 的 key 生成、代理自调用失效、序列化器行为较"隐晦"，而本项目结构里**没有统一 Jackson 定制**，注解缓存一旦序列化出问题排查成本高；
- 手写封装能把 **key 规范、TTL 抖动、降级开关、命中率日志**都做成显式代码，正好是简历上"我清楚每个缓存为何存在"的证据。

建议形态（落地时创建）：

```
service/CacheService.java          // 通用 get/set/delete + JSON 序列化 + 降级
service/CacheKeys.java             // key 常量统一出口，写路径失效时引用它
config/CacheProperties.java        // app.cache.enabled / app.cache.*-ttl
```

关键实现约定：

| 约定 | 说明 |
|---|---|
| key 前缀 | `sheyingtong:cache:{域}:{业务键}`，与聊天记忆统一 `sheyingtong:` 命名空间。调用方只写 `dict:terms:all` 这样的短 key，前缀由 `CacheService` 统一拼 |
| 序列化 | 自己 `new` 一份 `ObjectMapper` 并注册 `JavaTimeModule`、关闭 `WRITE_DATES_AS_TIMESTAMPS` → 缓存体内 `LocalDateTime` 一律 ISO 字符串 |
| 为什么单独配 Mapper | 本项目接口层把 `LocalDateTime` 序列化成数组（前端已经为此写了兼容函数）。缓存体若沿用数组格式，**反序列化回对象会失败**；缓存内部统一 ISO 最稳，且不影响接口行为（接口层照旧输出数组） |
| 降级 | `StringRedisTemplate` 惰性获取 + 连接类错误一次即本次运行降级为"无缓存直查 DB"，沿用 `RedisChatMemoryStore` 的 `redisDisabled` 思路；但只有"连不上/超时"才停用，序列化异常只删脏 key 重建成，不关掉整个缓存层 |
| 开关 | `app.cache.enabled=false` 时所有缓存读写直接短路，便于做"开/关缓存"的对比演示 |
| 空值 | 缓存 `__NULL__` 占位 + 30s TTL，防穿透 |
| 空集合 | 回源结果为空集合时**不写缓存**：避免把"种子数据还没跑完"这类瞬时空状态固化 12 小时 |
| 抖动 | 实际 TTL = 基准 TTL × (0.9~1.1) 随机，防雪崩 |
| 观测 | 每次读打一行 `hit/miss` DEBUG 日志，每 20 次访问打一行 INFO 命中率汇总（含 Redis 错误计数） |

### 4.2 Key 与 TTL 一览

| Key | TTL | 内容 |
|---|---|---|
| `sheyingtong:cache:dict:terms:all` | 12h | 全部术语（含分类/关键字检索所需字段） |
| `sheyingtong:cache:dict:ver` | 长期 | 词典版本号（预留，管理端写入时 bump） |
| `sheyingtong:cache:gear:all` | 12h | 全部器材（含所属分类） |
| `sheyingtong:cache:gear:categories` | 12h | 器材分类全量 |
| `sheyingtong:cache:gear:brands` | 12h | 品牌去重列表（`DISTINCT + ORDER BY` 由 MySQL 完成，见 §5.1） |
| `sheyingtong:cache:user:profile:{userId}` | 60s | 主页 DTO（公共部分） |
| `sheyingtong:cache:user:stats:{userId}` | 30s | 个人统计 DTO |
| `sheyingtong:cache:works:list:{sort}:p1` | 15s | 广场首屏（sort=latest/hot） |
| `sheyingtong:cache:works:detail:{workId}` | 30s | 详情公共部分（不含 liked） |
| `sheyingtong:cache:works:comments:{workId}:p1` | 15s | 评论首屏 |
| `sheyingtong:cache:userworks:{userId}:p1` | 20s | 用户作品墙首屏 |
| `sheyingtong:cache:feed:{userId}:p1` | 20s | 关注动态首屏（用户维度） |

---

## 5. 分场景设计

### 5.1 静态知识库（Tier A）

```
请求 ──► CacheService.get(cacheKey) ──命中──► 内存过滤/分页/匹配 ──► 返回
                  │
                  └──未命中──► Repository 全量查询 ──► 写缓存(12h+抖动) ──► 内存过滤 ──► 返回
```

- 词典：一次载入 `photography_terms` 全表，`terms` 接口在内存做 `category` 过滤 + 分页切片，`search` 接口在内存做关键字匹配（数据量 50 条，内存匹配远快于 SQL `LIKE`），`categories`/`category-count` 直接基于内存分组（现在 `getCategoryCount` 已经是 Java 内存分组，只是每次都要 `findAll()`）。
- 器材：同理载入 `gear` 全表，`popular` 由内存派生（按 rating 降序取前 6，同分按 id 升序保证翻页稳定）。
- `categories`/`brands` 各自单独缓存，而不在内存里重排：`brands` 对应 `SELECT DISTINCT brand FROM gear ORDER BY brand`，MySQL 的 `utf8mb4_unicode_ci` 是**大小写不敏感**排序（`Nikon` 在 `NiSi` 前，因为 `k < s`），Java 字符串序是按码元比（大写 `S` 在小写 `k` 前），实测这两者结果不同。所以把"去重 + 排序"交给数据库，缓存数据库给出的顺序，不去模拟排序规则。
- 回源时的分页/排序交给数据库，不在 Java 侧重排：术语按 `ORDER BY term`、器材按 `ORDER BY id` 加载后缓存，避免 Java 字符序与 MySQL 排序规则产生任何偏差。
- **AI 工具侧白拿**：`PhotographyTools` 的三个查询改为走同一份缓存，收益最大——用户每发一条聊天消息，工具链可能查多次词库/器材。
- 失效：当前**无写入口**，不需要失效逻辑；预留 `dict:ver` 版本号，将来有管理端时"改数据 → bump 版本 → key 自然换新"。

### 5.2 用户统计与主页（Tier B）

- 主页与统计接口共用 `user:stats:{userId}`，值含 `worksCount/followersCount/followingCount` 等计数；主页 DTO 的其余字段可由一次用户查询补全。
- 取消关注/关注、发布/删除作品时必须**主动删 key**（不能只靠 TTL，否则用户点完关注刷新还是旧数字，观感很差）。
- 新增打分/修图/会话后统计口径变化，同样删 `user:stats:{userId}`。

### 5.3 社区热读（Tier C）

- **只缓存首屏**：前端每次进入页面都是首屏请求（`size=12` / `size=20`），滚动到底才走游标翻页。深页缓存 key 数量随用户行为爆炸且复用率低，**交给 `idx_works_latest(is_public,id)` 索引**（见 MySQL 设计文档），这是"缓存与索引分工"的正例子。
- **详情页拆分**：公共部分进缓存，`liked` 命中后现查合并。
- **计数取舍**：列表缓存体里带 `likesCount/commentsCount`。**点赞不逐次删列表缓存**（热门作品点赞会写放大），靠 15s TTL 自然过期；但**详情缓存立即失效**，保证"点完赞再进详情数字立刻对"。
- **Feed 按用户分 key**：Feed 是"我关注的人"的结果集，天然按 `userId` 隔离。

---

## 6. 失效矩阵（写操作 → 需删除的 key）

| 写操作 | 位置 | 需失效的 key |
|---|---|---|
| `POST /api/works`（发布） | `CommunityService.publish` L47 | `works:list:latest:p1`、`works:list:hot:p1`、`userworks:{作者}:p1`、`user:stats:{作者}`、`user:profile:{作者}` |
| `DELETE /api/works/{id}` | `deleteWork` L70 | 同上 + `works:detail:{id}`、`works:comments:{id}:p1` |
| `POST/DELETE /api/works/{id}/like` | `like` L190 / `unlike` L208 | `works:detail:{id}`（列表只靠 TTL） |
| `POST/DELETE 评论` | `addComment` L221 / `deleteComment` L268 | `works:detail:{id}`（commentsCount）、`works:comments:{id}:p1` |
| `POST/DELETE /api/users/{id}/follow` | `follow` L294 / `unfollow` L315 | `user:profile:{双方}`、`user:stats:{双方}`、`feed:{关注者}:p1` |
| `POST /api/photo/score`、`DELETE /api/photo/{id}` | `PhotoService` L73/284 | `user:stats:{本人}` |
| `POST /api/retouch/suggestions`、`DELETE /api/retouch/{id}` | `RetouchService` L62/331 | `user:stats:{本人}` |
| `POST /api/chat/session` | `ChatService.createSession` L119 | `user:stats:{本人}` |
| `PUT /api/auth/user`、改密码、传头像 | `AuthService` L98/133、`UploadController` L27 | `user:profile:{本人}`（若做用户实体缓存则一并删） |

> 原则：**写路径只负责删 key，不负责写 key**（Cache Aside）。删 key 失败（Redis 抖动）不抛错、只记日志——绝不能因缓存问题让业务写失败。

---

## 7. 冷路径配套：N+1 与缓存体裁剪

缓存不能掩盖慢查询，反而会放大它（失效瞬间所有请求一起回源）。所以落地 Tier C 之前必须做两件事：

1. **消除作者/评论者 N+1**：给 `WorkRepository` 的列表查询方法加 `@EntityGraph(attributePaths = "user")`（或 fetch join），给评论查询批量预取 user（现在只批量预取了 parent）。
2. **缓存体剥离大字段**：`User.avatar` 是 `LONGTEXT` Base64，作品卡/评论里重复出现。列表与列表缓存体只放 `AuthorInfo` 的精简字段（`id/username/nickname/avatar` 中的头像应改为短标识或按需加载），否则缓存内存占用和出网流量都被头像吃掉。

这两项与缓存是**同一批改动**：先修冷路径 → 再上缓存，才能讲出"缓存命中 QPS 提升 + 冷回源也不慢"的完整故事。

> **Phase 1 已顺手做掉器材侧这一项**：`Gear.category` 是 `FetchType.EAGER` 的 `ManyToOne`，`findAll()` 会额外产生 36 次查询；已用 `@EntityGraph(attributePaths = "category")` 覆盖 `GearRepository.findAll()` / `findAll(Sort)` 消除，缓存整表回源时只剩 1 条 SQL。
> 剩下两项（作品/评论列表的 `@EntityGraph(user)`、缓存体剥离 Base64 头像）留到 Phase 3 之前完成。

---

## 8. 二期延伸（与缓存正交，但共用同一套 Redis 能力）

### 8.1 计数 Redis 化（解决缓存解决不了的热行写）

`works.likes_count/comments_count` 现在是事务内 ±1 的强一致冗余列（见 MySQL 设计文档），单条热门作品会成为行锁热点。二期方案：

- 写：`INCR/DECR sheyingtong:cnt:work:{id}:likes`，明细照旧落库（`uk_like` 兜底幂等）；
- 读：详情/列表优先读 Redis 计数，缺失则回源 DB；
- 落库：定时任务（如每 30s 或按阈值触发）批量 `UPDATE works SET likes_count=?` 合并刷库；
- 对账：低峰跑"计数 vs `work_like` 明细 count(*)"校正任务，差异写告警日志。

> 这套"异步最终一致 + 定时对账"是简历上比"我用了 Redis"更值钱的表述。本期**只预留 key 与方法位**，不实现。

### 8.2 JWT 会话治理

当前登出只清客户端 token（`AuthController.logout` L78-82 对服务端无效果），token 在 24h 内仍然有效。二期可加：

- 登出/改密码时把 `jti` 写入 `sheyingtong:jwt:blacklist:{jti}`，TTL = 剩余有效期；过滤器加一次判存；
- 配套 refresh token 与"改密码即失效全部旧 token"。

---

## 9. 分阶段落地清单与验收

| 阶段 | 内容 | 前置 | 验收标准 |
|---|---|---|---|
| **Phase 1** ✅ 已落地 | `CacheService` + 开关 + 降级 + 命中日志；Tier A 全部（词典/器材 + `PhotographyTools` 复用） | 无 | 连续访问同一词典/器材接口，`show-sql` 只出现首次 SELECT；命中日志 hit 率 > 90%；**停掉 Redis 后接口仍 200**（降级但不报错） |
| **Phase 2** | Tier B（主页/统计 + 用户实体候选）与失效点 | Phase 1 | 进个人中心/主页时 COUNT 查询从 7/5 次降为 0~1 次；关注/发布后立刻刷新数值正确（证明失效生效）；TTL 到期后自动回源 |
| **Phase 3** | Tier C（广场/详情/评论/Feed/用户墙首屏）+ `liked` 合并 | **§7 的 N+1 与缓存体裁剪先完成** | 广场首屏冷/热 P95 对比数据；点赞后进详情计数即时正确；游标翻页正确性回归（深度页不受缓存影响） |
| 可选优化 | 前端配合项：消除 `gear` 页 `size=9999` 全库拉取（改后端聚合返回分类计数）、`/works/mine` 重复拉取合并 | —— | 器材页挂载请求数 4 → 2；个人中心/打分页不再重复拉全量作品 |

**统一压测与演示建议**：用同一批接口做"缓存关 / 缓存开"两轮压测（并发 50~100，记录 QPS、P95、DB 连接数），再补一次降级演示，三组数据一起进 README/docs——这是把"我加了缓存"变成"我能证明缓存有用且不会拖垮系统"的关键。降级演示不必真去停 Redis 服务（Windows 上 `net stop Redis` 需要管理员权限），更省事的做法是另起一个实例并指定一个没人监听的端口：`mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8090 --spring.data.redis.port=6399"`，效果等价且不影响本机 Redis。


### 9.1 Phase 1 实测结果

改动落在 4 个文件：新增 `service/CacheService.java`、`service/CacheKeys.java`、`config/CacheProperties.java`；改造 `service/DictionaryService.java`、`service/GearService.java`、`service/PhotographyTools.java`（改用同一份缓存，不再直接注入 Repository）、`repository/GearRepository.java`（加 `@EntityGraph`）；配置加 `app.cache.*`。

验证方式：脚本按"接口返回 == MySQL 真值"逐项对拍（术语总数/排序/分页元数据/分类过滤/关键字匹配集、器材总数/排序/品牌与价格过滤集/rubric TOP6/详情一致性），共 29 项，全部 PASS。SQL 计数取自 `show-sql` 日志里 `    select` 开头的行数。

| 场景 | 结果 |
|---|---|
| 清空全部 cache key 后跑完整一轮（约 35 次请求，覆盖词典+器材 8 个接口） | 新增 SQL **4 条**，恰好等于 4 个整表 key（术语/器材/分类/品牌）各回源一次 |
| 紧接着再跑同样一轮（全命中缓存） | 新增 SQL **0 条** |
| 命中率日志 | `访问 40 次, 命中 36, 未命中 4, 命中率 90%, Redis 错误 0 次`（4 次未命中就是 4 次回源） |
| 降级：另起一个实例把 `spring.data.redis.port` 指向无人监听的端口 | 8 个接口全部 **HTTP 200**，返回内容与正常实例一致；日志出现一次 `Redis 不可达，本次运行期内停用缓存（接口继续直查数据库，可用性不受影响）`，之后不再重试 |

两个实测中发现、需要记下来的事实：

1. **`updated_at` 会在缓存里滞后**：`data.sql` 用的是 `INSERT IGNORE ... ON DUPLICATE KEY UPDATE updated_at = NOW()`，每次启动都会刷新所有种子行的 `updated_at`；而整表缓存最长 12h 才过期，缓存里的 `updatedAt` 可能是启动前的值。词典/器材页面不使用该字段，所以无用户可见影响——但它说明"长 TTL 缓存静态表"必须确认哪些列真的静态。
2. **`Page` 序列化里的 `paged`/`unpaged` 字段顺序在不同 JVM 实例间不保证一致**（同一实例内稳定，来自 Jackson 反射方法序）。这是改造前就存在的现象，不是缓存引入的，接口消费方只读 `content`/`totalElements`，不受影响。


---

## 10. 简历 / 面试表述建议

- 设计并落地**分层缓存方案**：按"静态知识库 / 聚合统计 / 社区热读"三档分别确定 TTL 与失效策略（Cache Aside + 主动失效矩阵），词典与器材接口转为整表内存化缓存，聚合统计接口 COUNT 次数由 7 次降至 0~1 次。
- 缓存层实现**优雅降级与开关**：Redis 不可用时自动跳过缓存直查 DB，服务可用性不受影响；支持开关对比压测，量化命中率与 P95 收益。
- 结合缓存改造**消除列表接口的作者 N+1**（`@EntityGraph`）并裁剪 Base64 大字段，使冷回源同样可控。
- 针对数据库计数热行瓶颈，规划"**Redis 计数 + 定时批量刷库 + 明细对账**"的最终一致方案，替代纯事务内 ±1 的强一致写法。

---

## 11. 附录：相关代码位置索引

| 关注点 | 位置 |
|---|---|
| **缓存层实现（Phase 1 新增）** | `service/CacheService.java`（读写/降级/抖动/命中率）、`service/CacheKeys.java`（key 常量）、`config/CacheProperties.java`（`app.cache.*`） |
| 缓存开关与 TTL 配置 | `backend/src/main/resources/application.yml`（`app.cache` 段） |
| Redis 连接与连接池配置 | `backend/src/main/resources/application.yml` L78-92 |
| 现有 Redis 使用范例（降级思路来源） | `backend/src/main/java/com/sheyingtong/service/RedisChatMemoryStore.java` |
| 社区读路径与 N+1 | `service/CommunityService.java`（L89 广场、L128 用户作品墙、L147 详情、L174 Feed、L251 评论、L338 主页统计、L377 `toWorkResponse`） |
| 个人统计 7 个 COUNT | `service/AuthService.java` L149-167 |
| 静态知识库（已整表缓存） | `service/DictionaryService.java`、`service/GearService.java` |
| 器材 `category` 的 N+1 修复 | `repository/GearRepository.java`（`@EntityGraph` 覆盖 `findAll`） |
| AI 工具侧查库（已改走缓存） | `service/PhotographyTools.java` |
| 列表/评论 Repository | `repository/WorkRepository.java`、`repository/WorkCommentRepository.java`、`repository/WorkLikeRepository.java`、`repository/FollowRepository.java` |
| 图片文件流（不缓存对象） | `controller/PhotoController.java` L115、`controller/RetouchController.java` L96 |
| 每请求鉴权查询 | `security/JwtAuthenticationFilter.java`、`controller/AuthController.java` L68/78 |
| 社区表结构与索引（配套阅读） | `docs/社区模块-MySQL设计要点.md` |
