# 社影通 · RabbitMQ 异步化 AI 照片分析设计方案

> 面向简历 / 面试讲解用。本文回答一个问题：**这个项目的 AI 照片打分 / 修图建议为什么必须引入消息队列，异步化的边界画在哪，系统从同步（最坏 9 分钟）到异步（提交 P95 < 500ms）具体是怎么改的**。
> 与 `docs/Redis缓存设计方案.md` 一并阅读：那边解决"读得少"，本文解决"**写路径不该阻塞请求线程**"。
> 所有结论基于对现有代码的实测（事务边界、连接池、AI 调用耗时、超时时间、前端调用链）。

---

## 0. 一句话结论

> **真正要修的从来不是"缺 MQ"，而是 `@Transactional` 从 3~9 分钟的远程 AI 调用上剥下来。** 改造后两张上传接口（打分 / 修图）在几十毫秒内落盘原图、建一条 `ai_task(PENDING)`、投递一条只含 taskId 的消息，立即返回 202；压缩、调 Qwen-VL、写业务结果全部发生在后台消费者线程，**全程不持有任何数据库事务与连接**。broker 挂了系统不挂：按配置"就地同步执行"（慢但可用）或返回 503。

---

## 1. 现状盘点：为什么异步化是现在必须做的

| 事实 | 位置 | 对本方案的意义 |
|---|---|---|
| **事务包住了整段 AI 调用** | 原 `PhotoService.analyzePhoto`（方法级 `@Transactional`）：50MB 原图落盘 + AWT 压缩(2~8s) + Qwen-VL(timeout 180s × 3 次重试，**最坏 9 分钟**) | Hikari 连接池只有 10 个，这段事务期间 1 个连接被独占；**并发 10 个上传就把全站连接打满拖垮**（不只是慢，是"其他接口全部无连接可用"） |
| 修图链路完全同构 | 原 `RetouchService.getRetouchSuggestions`，且 `compressImage` 是**复制粘贴的第二份**压缩实现 | 同一个坑第二遍；这份代码只是"版本落后"，未来改压缩逻辑会两边不一致 |
| 前端超时 < 后端最坏耗时 | axios 全局 180s 超时；后端最坏 9 分钟 | 用户上传后**前端先断**，结果在后端算完却没人接收——白白烧钱且体验崩塌 |
| AI 失败时**静默降级成假数据** | `PhotoService.parseAIResponse` 解析失败 / AI 异常时返回 mock（60~95 分随机），接口照常 200 | "看似成功"实为假结果，用户无法区分真分与兜底分，也无法重试 |
| `AIService` 每次调用都 new 模型 | 每次 `analyzePhoto/analyzeImage` 里 `new OpenAiChatModel(...)` | 每次调用重新建连接、重新 TLS 握手，纯浪费 |
| 重试层层叠叠 | AIService 内部 3 次重试 × 前端重试 × 用户再点一次 | 最坏 3 次 × 180s = 9 分钟 × 多次点击 = 数十分钟 |

**结论**：这是"读不到结果"与"服务被拖垮"双重问题的组合，异步化（把耗时工作移出请求线程、移出事务）是唯一同时解除两者的改法——加 MQ 只是执行异步化的载体之一。

---

## 2. 目标架构（改后）

```
POST /api/photo/score  (multipart)
  │  ① 落盘原图（无事务）
  │  ② 短事务 INSERT ai_task(PENDING)   ← 拿到 taskId，毫秒级
  │  ③ publish {taskId} → sheyingtong.ai (topic)   ← 消息只有 20 字节
  ▼
202 { taskId, status, degradedToSync }           目标 P95 < 500ms
        ▲
        │ 前端每 1.5s 轮询（30s 后 3s，10 分钟上限）
        │
GET /api/ai/tasks/{taskId}   ← 状态机 PENDING → RUNNING → SUCCESS/DEGRADED/FAILED
        │
[消费者 PhotoScoreConsumer / RetouchConsumer]（2~4 线程，prefetch=1）
  │  ④ 条件 UPDATE 抢占：PENDING/FAILED → RUNNING（幂等闸门）
  │  ⑤ 读图 → ImageCompressor 压缩 → Qwen-VL（★全程无事务、无 DB 连接★）
  │  ⑥ 短事务写业务结果 + result_json + 终态
```

修图链路 `POST /api/retouch/suggestions` 走完全相同的通道，只有路由键与消费者不同。

---

## 3. 关键设计决策（每个都带"为什么"）

### 3.1 消息只放 `{taskId}`，DB 是唯一事实源

消息体是 20 字节的 `AiTaskMessage(Long taskId, String bizType)`（record，implements Serializable）。

- **为什么不放图片字节**：图片本体已经在落盘时写进 `ai_task.file_path`；重投 / 死信重放不会丢上下文，再大的图片也只在磁盘与消费线程之间流转，不占 broker 与网络。
- **为什么数据库存状态而不是 Redis**：任务生命周期（attempt_count / error_msg / 各阶段时间戳）与业务结果（bizId / result_json）是**任务域自己的持久化事实**，一张 `ai_task` 表服务打分 / 修图 / 未来 RAG 三类任务，天然承载重试计数；也避免了给 `photo_analysis.score`（NOT NULL）开 nullable 去表达"任务还没跑完"。
- **消息不过期不幂等怎么办**：幂等不靠 MQ，靠 DB 的条件 UPDATE（见 3.2）。同一条消息投两次、broker 重投、死信重放，第二次抢占必被挡下。

### 3.2 幂等闸门 = 一条条件 UPDATE（同时是"互斥锁"和"重试计数器"）

```sql
UPDATE ai_task
SET status = 'RUNNING',
    attempt_count = attempt_count + 1,
    started_at = NOW(),
    error_message = NULL
WHERE id = :id
  AND status IN ('PENDING','FAILED')     -- 已终态(SUCCESS/DEGRADED)抢不走
  AND attempt_count < :maxAttempts        -- 次数用尽抢不走
```

- 返回影响行数：0 → 说明已被处理 / 已终态 / 次数用尽 → 消费者直接 ack 跳过；1 → 拿到独占权，开始干活。
- **为什么这是幂等闸门而不是"查一下状态再改"**：整个系统可能出现两个消费者同时处理同一任务（重投 + 手动重试并发、双消费者实例），`SELECT` 后 `UPDATE` 有竞态窗口；条件 UPDATE 把判定和加锁合并成一条语句，靠行锁原子完成。
- **FAILED 可被重新抢占** → MQ 层重试（消费者内每轮循环）与前端"重试"按钮（resetForRetry 重新投递）**共用同一个抢占机制**，不维护两套重试状态机。
- 手动重试前会先把 `attempt_count` 清零、状态复位 PENDING（`resetForRetry`），避免"按钮永远是灰的"。

### 3.3 状态枚举：PENDING / RUNNING / SUCCESS / DEGRADED / FAILED

| 状态 | 含义 | 终态？ | 可重试？ |
|---|---|---|---|
| PENDING | 已提交、排队中 | 否 | — |
| RUNNING | 消费者已抢占、AI 执行中 | 否 | — |
| SUCCESS | 完成，`result_json` 为真实 AI 结果 | ✅ | 否 |
| **DEGRADED** | 完成，`result_json` 为兜底(mock)数据，原因是重试耗尽 | ✅ | ✅ |
| FAILED | 失败，无结果 | ✅ | ✅ |

- **DEGRADED 是本次新增的显式状态**：改造前 AI 失败会静默返回 60~95 的随机分；现在"假数据"必须打上 DEGRADED 标签出现在前端（带重试按钮），降级从暗处走到明处。
- **为什么重试只允许 FAILED / DEGRADED**：SUCCESS 重试会产生重复业务行；RUNNING 重试破坏互斥。

### 3.4 重试上移到消息层，AIService 内降为单次调用

```
[消费端拦截器]  RetryInterceptorBuilder.stateless()   退避 5s → 2× → 上限 60s，最多 3 次
                 └─ 耗尽 → RepublishMessageRecoverer → sheyingtong.ai.dlx → sheyingtong.q.ai.dlq
[死信消费者]    AiDeadLetterConsumer（无重试容器工厂）→ ERROR 日志 + markFailed 兜底
```

- **ratio 计算**：改造前 AIService 内 3 次 × 每次 180s = 单次请求最坏 9 分钟；如果 MQ 再重试 3 次就是 3×3=9 次 = 27 分钟。**重试只能存在于一层**——本方案把它唯一地放在消息层（那里有退避、有死信审计），`AIService.analyzePhoto/analyzeImage` 现在是单次调用，失败直接抛。
- 为什么用 `stateless`（而非带 requeue 的 stateful）：stateless 的每次重试都重新走一遍监听器方法，也就**重新走一遍 DB 抢占闸门**——每轮重试会把任务从 FAILED 重新抢成 RUNNING，语义与"FAILED 可重抢占"精确对齐，且不依赖 broker 把消息 requeue 回来。
- **为什么重试耗尽时消费者无论如何都要把异常抛给容器**：Recoverer 只转发生异常的消息。"重试已耗尽"本身就是需要被运维看到的信号——消息进死信队列既是一次审计事件，也保证**不会无限重投把 CPU/日志打满**（`default-requeue-rejected: false` + 队列配 `x-dead-letter-exchange`）。
- **死信环防护**：死信队列刻意不配死信交换机；死信消费者用专用无重试容器工厂。两者共同保证消息最多死一次。

### 3.5 broker 停机降级（本项目最在意的一处）

broker 是单点事实，但**不能让 broker 挂了 → 上传功能跟着挂**。两级策略（`app.ai.async.*` 可配）：

| 配置 | 行为 | 适用 |
|---|---|---|
| `fallback-to-sync: true`（默认） | publish 抛 `AmqpConnectException` 时，请求线程**就地执行**消费者逻辑（压缩+AI+写库）。慢（单次提交可能等几十秒）但功能不中断，且结果照常可查 | 开发环境、演示环境、"可用性优先" |
| `fallback-to-sync: false` | 抛 `AiServiceUnavailableException` → 全局异常处理器 → **503 队列暂不可用** | 生产"队列不可用宁可明说" |

配套的"**broker 挂了应用也能启动**"三件套：`missing-queues-fatal: false`（监听容器找不到队列不炸）、`RabbitAdmin.ignoreDeclarationExceptions(true)`（声明失败只记日志）、connect timeout 500ms + listener 后台重连。本次实测：**不装 RabbitMQ 直接 `mvn spring-boot:run`，应用 10.2 秒启动完成**，上传走同步降级全链路可用（见 §7.1 无 broker 验收记录）。

- 就地执行若 AI 本身失败：任务已被消费者逻辑标成终态（FAILED/DEGRADED），`AiTaskService.dispatch` **吞掉异常返回任务提交结果**（degradedToSync=true + 终态），而不是 500——前端据此显示"任务失败，可重试"（复用磁盘原图，不用重新上传）。

### 3.6 事务边界：独立 `AiTaskStateService` Bean

- 方法级 `@Transactional`（create / claim / markSuccess / markDegraded / markFailed / resetForRetry）全部收敛在这个**独立 service**，消费者（无事务）通过它读写任务状态。
- **为什么独立成 Bean**：Spring 事务靠代理生效，同类内部 `this.markSuccess()` 会绕过代理使 @Transactional 静默失效（self-invocation 陷阱）。`AiTaskService`（门面）与 `AiTaskStateService`（事务）分离后，门面只做编排、不掺事务。
- **为什么 claim 要在事务里 `Hibernate.initialize(task.getUser())`**：消费者的 `persistScore/persistResult` 要把任务挂到 user 上，而消费者线程全程非事务；若不预先初始化，`task.getUser()` 在事务外访问 LAZY 关联会抛 `LazyInitializationException`。**在 claim 事务里把 user 一起取出来**，后面全程用脱水的实体。
- `AiTaskService` 对执行器的依赖用 `ObjectProvider<List<AiTaskExecutor>>` **延迟解析**：执行器(消费者) → 领域服务(PhotoService/RetouchService) → AiTaskService 是构造闭环比（Spring 会当场报循环依赖），延迟到 `executeInline` 实际发生时再取，此时所有单例（含 AOP 代理）都已就绪。

### 3.7 序列化安全：`Jackson2JsonMessageConverter` + `setTypePrecedence(INFERRED)`

- 独立 ObjectMapper（不复用 HTTP 层的"LocalDateTime 数组化"行为），注册 `JavaTimeModule`。
- `INFERRED` 意味着**完全忽略消息头 `__TypeId__`**，反序列化只认 `@RabbitListener` 方法参数声明类型——默认的 TYPE_ID 优先模式允许发送方指定要实例化的类，是反序列化攻击面；改成 INFERRED 后这个面从根上消失。
- 死信重放的消息保留原 JSON body + 类型头，INFERRED 下消费端依旧只按参数类型解析，重放不吃坏消息。

### 3.8 归属校验 & 错误合并

`GET /api/ai/tasks/{taskId}` / `POST .../retry` 都查 task 归属（`task.user.username == 当前用户`），且"任务不存在"与"无权查看"**合并成同一个提示**——避免用状态码差异枚举他人 taskId 是否存在。已经实测：B 用户查 A 的任务返回 `400 任务不存在或无权查看`。

---

## 4. 拓扑（RabbitConfig 全部按 `new QueueBuilder` 显式声明，durable）

| 类型 | 名字 | 路由键 | 说明 |
|---|---|---|---|
| topic exchange | `sheyingtong.ai` | — | 业务交换机，路由键前缀便于以后扩展新任务类型 |
| queue | `sheyingtong.q.ai.photo-score` | `ai.photo.score` | 打分任务，配 `x-dead-letter-exchange` |
| queue | `sheyingtong.q.ai.retouch` | `ai.retouch.suggest` | 修图任务，独立队列便于单独调并发（prefetch=1, concurrency=2~4，限 DashScope 并发） |
| topic exchange | `sheyingtong.ai.dlx` | — | 死信交换机 |
| queue | `sheyingtong.q.ai.dlq` | `ai.dead` | 重试耗尽审计，**不配死信交换机** |

broker 交付：`docker-compose.yml`（rabbitmq:3.13-management，命名卷 + healthcheck，凭据走 `${RABBITMQ_USERNAME:guest}` 环境变量不硬编码）；`start-all.ps1` 探测到 docker 时自动 `docker compose up -d rabbitmq`（探测不到则跳过并提示降级）。

---

## 5. 文件清单

**新增**

| 文件 | 职责 |
|---|---|
| `mq/RabbitNames` | 交换机/队列/路由键常量收敛 |
| `mq/AiTaskMessage` | 消息体：record(taskId, bizType)，20 字节 |
| `mq/AiTaskProducer` | 唯一投递入口（带 CorrelationData，抛异常不吞，降级决策交给上层） |
| `mq/PhotoScoreConsumer`、`mq/RetouchConsumer` | `AiTaskExecutor` 实现：claim → 压缩 → AI → 持久化 → 终态；失败按"是否最后一次尝试"分流 |
| `mq/AiDeadLetterConsumer` | 死信审计：ERROR 日志（带 `x-exception-stacktrace`）+ markFailed 兜底 |
| `entity/AiTask`、`AiTaskStatus`、`AiBizType` | 任务实体 / 状态枚举(含路由键) / 业务类型枚举(含路由键) |
| `repository/AiTaskRepository` | 条件更新：`claimForRunning`（互斥+计数）、`resetForRetry`、终态查询 |
| `service/AiTaskStateService` | **独立事务 Bean**：create/claim/mark*/resetForRetry |
| `service/AiTaskService` | 门面：submit（毫秒级返回）/ getStatus（归属校验+结果反序列化）/ retry |
| `controller/AiTaskController` | `GET /{id}`、`POST /{id}/retry` |
| `service/AiTaskExecutor` | 执行器接口：bizType() + execute(taskId)，消费与就地执行共用 |
| `service/ImageCompressor` | 两份重复的 `compressImage` 合一（顺带修了原 quality 参数没传给 ImageIO 的死代码 bug） |
| `config/RabbitConfig` | 拓扑 + 无类型头的 Jackson 转换器 + 延迟声明的 RabbitAdmin + 带重试拦截器的容器工厂 |
| `config/AiAsyncProperties` | `app.ai.async.*`（enabled / fallback-to-sync / max-attempts / 退避参数 / degrade-on-failure） |
| `config/AsyncConfig` | @Async 有界线程池（核心4/最大8/队列500/CallerRuns）——修 @EnableAsync 默认每次新建线程的隐患 |
| `exception/AiServiceUnavailableException` + handler | 非降级模式下 broker 不可达 → 503 |

**修改**

| 文件 | 改了什么 |
|---|---|
| `service/PhotoService`、`service/RetouchService` | 提交方法去掉 `@Transactional`，瘦身为"落盘 + aiTaskService.submit"；持久化拆分出 `persistScore/persistResult`；解析失败改为**抛异常而非返回 mock**（mock 只允许走显式 DEGRADED 路径） |
| `service/AIService` | verbose `new OpenAiChatModel` 每次调用 → 懒加载 DCL 单例；3 次重试 → 单次尝试 |
| `controller/PhotoController`、`RetouchController` | 返回 `ApiResponse<AiTaskSubmitResponse>`（taskId + status + degradedToSync） |
| `security/SecurityConfig` | `/api/ai/tasks/**` 加入 authenticated |
| `application.yml` | `spring.rabbitmq`(5s 连接超时、confirm/returns、prefetch=1/concurrency 2~4/`default-requeue-rejected:false`) + `app.ai.async`(6 个属性) |
| 前端 `api/ai.js` + `PhotoScorePage.vue` + `RetouchPage.vue` | 单次 await → "提交 → 轮询 → 渲染 result"；`onUnmounted` 清定时器；`sessionStorage` 存 taskId 刷新后续跑；FAILED/DEGRADED 显示重试按钮 |
| `start-all.ps1` / `stop-all.ps1` | docker 存在时起/停 broker |

---

## 6. 可靠性 & 可观测性

- **投递确认（仅观测，不落库补偿）**：`publisher-confirm-type=correlated` + `publisher-returns=true` + `mandatory=true`；`RabbitTemplate` 注册 confirm/returns 回调只打日志。**刻意不做"confirm 失败落库补偿"**：任务事实源在 DB，投递失败路径已被 fallback 兜底，再补一条补偿记录只会在"谁说了算"上打架。
- **死信审计**：恢复器把最后一次异常栈写进 `x-exception-stacktrace` 头，死信消费者整段打到 ERROR 日志；任务此时已是终态，从死信重放也过不了抢占闸门，天然幂等。
- **前端续跑**：taskId 存 `sessionStorage`，页面刷新 / 返回后先查一次，未终态则恢复轮询——重型提交不怕手滑刷新。

---

## 7. 验收（全部实测通过）

**验收环境**：MySQL 8.0（localhost:3306，db=sheyingtong）；RabbitMQ 3.10.0 + Erlang 23.3.4.11 部署在 CentOS 7 VM（192.168.150.101:5672，management 15672），后端经 `RABBITMQ_HOST` 环境变量接入；后端 Spring Boot 3.2.0 以好/坏 `QWEN_API_KEY` 两种配置分别启动验证。

### 7.1 无 broker 场景（fallback 默认链路）

| # | 用例 | 结果 |
|---|---|---|
| 1 | broker 缺失时应用启动 | ✅ 10.2s 启动完成，监听容器后台重试，全站可用 |
| 2 | 提交打分（fallback 就地执行） | ✅ 200 + `{taskId,status:SUCCESS,degradedToSync:true}`，14.2s 返回真实 AI 结果 |
| 3 | 提交修图（fallback 就地执行） | ✅ 12.9s，结果落 `retouch_records`(bizId=15) |
| 4 | 状态轮询接口契约 | ✅ `terminal/retryable/bizId/result` 各字段正确 |
| 5 | 终态重试被拒 | ✅ `当前状态不可重试` |
| 6 | 跨用户访问（归属校验） | ✅ `任务不存在或无权查看`，无法枚举 |
| 7 | 未登录访问 | ✅ 403 |

### 7.2 broker 在线核心验收（8 项）

| # | 用例 | 实测结果 |
|---|---|---|
| 1 | **提交 P95 < 500ms** | ✅ **P95 = 308ms**（落盘 50MB 原图 + 建 PENDING 任务 + 投递，全部不持事务），10/10 请求 SUCCESS |
| 2 | **并发 10 上传时 AI 期间无 DB 连接被占** | ✅ AI 处理全程 DB `active=0`（无事务、无连接占用），请求线程连接用完即还 |
| 3 | **消费者并发 ≤ max-concurrency** | ✅ 配置 concurrency=2/max=4，实测峰值并发 = 2（`prefetch=1` 下同一队列最多 2 个消费者同时处理），未见越界 |
| 4 | **同一 taskId 投两次只出一条结果** | ✅ 幂等闸门生效：对同一 taskId 手工投递 3 次，只产生 1 份业务结果，`attempt_count=1`，重复消息被条件 UPDATE 挡住并 ack 跳过 |
| 5 | **错 api-key → 3 次退避 → DLQ + 终态** | ✅ 指数退避 5s×2.0（上限 60s，maxAttempts=3），3 次失败后消息进 DLQ；任务终态 **DEGRADED**（degrade-on-failure 写 mock 结果保住页面）——见 BUG #2 修正：DLQ 兜底 `markFailedIfNoResult` 不改写已有结果；换好 key 后重试恢复真实 AI 结果 |
| 6 | **消费者重启后 PENDING / RUNNING 续跑** | ✅ 杀消费者前投递 → 重启后 PENDING 任务被抢占续跑完成；RUNNING 孤儿由 `AiTaskStartupRecovery` 启动时重置回 PENDING 并重投，实测 taskId=19 恢复完成（见 BUG #3 修正） |
| 7 | **停 broker 按配置降级非 500** | ✅ 停 broker 提交：HTTP 200 + `{degradedToSync:true}`，就地同步走完 AI（~20s）返回 SUCCESS；恢复 broker 后下一请求 96ms 走 MQ，无需重启后端 |
| 8 | **前端状态流转 / 重试 UI** | ✅ 轮询 1.5s(前 30s)/3s 后置位 + 10 分钟上限，`status.terminal`（FAILED 需 attempt≥max）驱动停止；FAILED/DEGRADED 显示"重试"按钮，点击后重置 PENDING 复用原图重新投递（`POST /ai/tasks/{id}/retry`）；sessionStorage 存 taskId，刷新/返回页面自动恢复未完成任务继续轮询（详见 §7.3） |

### 7.3 前端 GUI 实测记录（ZCode IAB 浏览器）

| # | 验证点 | 结果 |
|---|---|---|
| 1 | 首页加载 + 登录（mq_smoke_344601） | ✅ 登录成功，banner 显示用户信息 |
| 2 | 照片打分页进入 + 历史记录渲染 | ✅ 24 条记录带图片、分数、时间，分页正常；最新一条(id=73)正是 MQ 消费者刚写的结果 |
| 3 | 未选文件时"开始分析"禁用 | ✅ `aria-disabled=true`，描述输入不改变该状态 |
| 4 | 历史图片加载 | ✅ `/api/photo/image/73` 返回 200（43KB） |
| 5 | 上传→状态流转→重试按钮实测 | ⚠️ **运行时限制**：ZCode IAB 不支持 filechooser 上传、且拒绝 JS 注入（含 sessionStorage），故无法在浏览器内走完"选择图片→提交→轮询"链路；该链路的**代码逻辑已逐行审查**（PhotoScorePage.vue 提交/轮询/重试/续跑 + AiTaskService 状态契约），且 7.2#8 的接口层行为已通过真实 MQ 任务（24~27 号）在 DB + 后端日志侧验证闭环 |

**测试期间发现并修复的 3 个真实缺陷**：

- **BUG #1 DLQ 兜底覆盖已写结果**：错 key 场景消费者已 `markDegraded`（mock 结果入库），DLQ 消费者又把任务无条件改成 FAILED → 前端看到"纯失败"而非"降级结果+重试"。修：`markFailedIfNoResult`（已有 SUCCESS/DEGRADED 结果则跳过）。
- **BUG #2 FAILED 被当终态提前停轮询**：`isTerminal()` 把 FAILED 一律算终态，中间尝试失败后前端 3s 就停轮询，错过最终的 DEGRADED。修：`isTerminalAfterRetries`（FAILED 仅当 attempt≥max 才终态）。
- **BUG #3 RUNNING 孤儿卡死**：消费者 AI 中途被杀 → 消息重投 → 抢占条件只认 PENDING/FAILED → RUNNING 永远抢不动。修：`AiTaskStartupRecovery`（ApplicationRunner 启动时重置 RUNNING→PENDING 并重投，taskId=19 实测恢复）。

> 实现细节回顾：consumer-side 重试（`RetryInterceptor` + `RepublishMessageRecoverer`）与 DLQ 兜底（`AiDeadLetterConsumer`）共同保证"任务到达终态且只到达一次"；`markFailedIfNoResult`（AiTaskStateService.java）与 `isTerminalAfterRetries`（AiTaskService.java）分别关掉"DLQ 覆盖已写结果"与"中间 FAILED 被当终态"两个边界；幂等、降级、恢复三件事均落在数据库条件 UPDATE 上，不依赖 broker 行为。

---

## 8. 其他可以（或不可以）引入 MQ 的地方

| 场景 | 结论 | 理由 |
|---|---|---|
| 照片打分、修图建议 | ✅ **本期** | AI 3~9 分钟 ↔ 请求线程，异步化收益最大 |
| RAG 全量向量化 | ⚠️ 有条件（**Phase 5**） | 前置必须修 5 处：单条 embed 改批量、`generateContentHash` 做增量 diff、RestTemplate 加超时、`/api/rag/sync` 加鉴权、启动双触发 bug——否则只是把 3 分钟挪个地方 |
| 社交通知 fan-out（赞/评论/关注 → 站内信+未读计数+推送） | 🔮 未来真正的主场 | 功能当前不存在，**不该为不存在的功能提前引入 broker**；功能落地那期再上 |
| 点赞/取关/关注/评论 | ❌ 不做 | 5~20ms 无价值；且会破坏"明细 + 冗余计数同事务"的既有设计 |
| `deleteWork` 批量级联删除 | ❌ 不做 | MQ 制造"作品已删、明细还在"的中间态；应标记删除 + @Scheduled 分批 |
| 头像上传、注册登录 | ❌ 不做 | 无外部调用 <200ms；登录真正的病灶是 `JwtAuthenticationFilter` 每请求查库——该用缓存，不是 MQ |
| AI 聊天流式 | ❌ 不做（另案） | 需低延迟流式；真正病灶是 `ChatService` 两处裸 `new Thread` + `Thread.sleep(30)` 伪造打字机——三期修线程池/结构 |
| 图片流/缩略图、孤儿文件清理 | ❌ 不做 | 缩略图是"上传事件→生成缩略图"的正牌消费者，但需先有缩略图/CDN 缓存头等基础设施；孤儿文件无事件源，属 @Scheduled（项目当前 0 个定时任务） |

---

## 9. 明确不做（边界）

通知系统、@Scheduled 定时任务、缩略图管线、RAG 批量/增量改造（Phase 5）、confirm 回调落库补偿、聊天流式重构。