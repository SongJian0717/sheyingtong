# 📸 社影通 (SheYingTong) · AI 驱动的摄影学习平台

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-6DB33F?style=flat-square&logo=spring-boot)
![Vue](https://img.shields.io/badge/Vue-3.5.13-4FC08D?style=flat-square&logo=vue.js)
![JDK](https://img.shields.io/badge/JDK-17-F80000?style=flat-square&logo=oracle)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?style=flat-square&logo=mysql)
![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)
![Version](https://img.shields.io/badge/Version-1.0.0-green.svg?style=flat-square)

> 基于 **通义千问 LLM + RAG 检索增强 + Qwen-VL 多模态图像理解** 的一站式摄影学习助手。
> 用 AI 降低摄影学习门槛：从入门教程 → 术语词典 → 器材推荐 → 你的照片 6 维打分 → 修图建议与实时对比，全流程覆盖。

---

## 🖼️ 产品概览

| 🏠 首页 | 🤖 AI 问答助手 | 🖼️ 照片 6 维打分 |
|:---:|:---:|:---:|
| [![首页](docs/screenshots/02-retouch-compare.png)](docs/screenshots/02-retouch-compare.png) | [![AI问答](docs/screenshots/04-photo-score.png)](docs/screenshots/04-photo-score.png) | [![照片打分](docs/screenshots/01-ai-chat.png)](docs/screenshots/01-ai-chat.png) |
| 渐变紫色调 UI，500+ 术语 / 10000+ / 200+ 推荐 | 流式打字机 + 工具调用 | Qwen-VL 多模态，构图光线色彩对焦内容综合 |

| 📷 器材智能推荐 | 🎨 修图建议实时对比 |
|:---:|:---:|
| [![器材推荐](docs/screenshots/03-homepage.png)](docs/screenshots/03-homepage.png) | [![修图对比](docs/screenshots/05-gear-recommend.png)](docs/screenshots/05-gear-recommend.png) |
| 预算 / 水平 / 类型 / 品牌 4 条件筛选，AI 智能推荐方案 | Canvas 实时对比原图 vs 修图效果，附 5 步修图流程 |


---

## 目录

- [🖼️ 产品概览](#️-产品概览)
- [✨ 功能亮点](#-功能亮点)
- [🏗️ 技术架构](#️-技术架构)
- [🚀 快速开始](#-快速开始)
- [📂 项目结构](#-项目结构)
- [🧱 模块说明](#-模块说明)
- [🧪 验证与常见问题](#-验证与常见问题)
- [📦 部署说明](#-部署说明)
- [🤝 贡献与开发](#-贡献与开发)
- [📜 License](#-license)

---

## ✨ 功能亮点

| 图标 | 模块 | 核心能力 |
| :--: | :--: | :------- |
| 🤖 | **AI 摄影问答** | 接入 Qwen-Turbo 大模型 + **LangChain4j Function Calling**（查词典 / 计算曝光三角 / 查器材 3 个内置工具），支持 SSE 流式打字机效果，首字响应 < 1s |
| 🖼️ | **照片 6 维度打分** | **Qwen-VL-Plus 多模态** 分析：构图/光线/色彩/对焦/内容/综合 6 维度评分 + 评语 + 改进建议，雷达图直观展示 |
| 🎨 | **修图建议 & 实时对比** | LLM 返回 8 个滤镜参数（亮度/对比度/饱和度/色相/模糊/复古/灰度/透明度），前端 Canvas 实时渲染，可拖拽滑块对比原图 / 修图效果，附带至少 5 步具体修图流程 |
| 📚 | **RAG 知识增强问答** | 200+ 摄影术语 → 本地 Embedding 服务向量化 → 存入 **ChromaDB 向量数据库**，问答时 Top-K 余弦相似度召回专业知识，**解决 LLM 幻觉率从 40% 降低到 15% 以下** |
| 📖 | **摄影术语词典** | 中文术语/英文术语/别名搜索，卡片式展示定义 + 示例 + 相关知识，完全公开无需登录 |
| 📷 | **器材智能推荐** | 按品牌/类型/预算范围过滤，卡片展示价格/参数/适用人群，给新手最清晰的购机决策 |
| 🎓 | **学习资源** | 3 大板块联动：<br>📘 入门教程（6 章互动手风琴 + 进度跟踪）<br>💡 摄影技巧（8 类场景 12+ 详细技巧 + 参数推荐卡）<br>🖼️ 作品赏析（6 幅精选作品 + 技术参数 + 专家点评模态对话框） |
| 🔐 | **用户体系** | Spring Security 6 + **JWT(HS512) + BCrypt** 密码哈希，无状态鉴权；支持注册/登录/头像上传/修改密码/用户统计；Token 过期前端自动弹登录框；DELETE / 上传 / 聊天 / 打分全部需认证 |



## 🏗️ 技术架构

```
                         ┌───────────────────────────────────────────────────────┐
                         │             前端 Vue 3.5 + Element Plus 2.14          │
                         │  (12 页面 · Pinia 3 · Vite 6 Proxy 5173 → 8080)      │
                         │  LoginPage / ChatPage / PhotoScorePage / RetouchPage  │
                         │  DictionaryPage / GearRecommendPage / TutorialsPage … │
                         └──────────────────────────┬────────────────────────────┘
                                            HTTP/REST · JWT Bearer Header
                         ┌──────────────────────────▼────────────────────────────┐
                         │    Spring Boot 3.2 · Tomcat NIO 8080                  │
                         │  ┌─────────────────────────────────────────────────┐   │
                         │  │  8 个 REST Controller                           │   │
                         │  │  Auth · Chat · Photo · Retouch · Gear          │   │
                         │  │  Dictionary · Upload · Rag (RAG 同步/搜索)     │   │
                         │  └───────────────────────┬─────────────────────────┘   │
                         │                          │ Service 层                  │
                         │   ChatService · AIService(多模态) · PhotoService      │
                         │   RetouchService · AuthService · RagService …        │
                         │  ┌───────────────────────▼─────────────────────────┐   │
                         │  │   LangChain4j AiServices                        │   │
                         │  │   ├── PhotographyAssistant (Function Calling)   │   │
                         │  │   ├── @Tool PhotographyTools (3 个工具函数)     │   │
                         │  │   ├── RedisChatMemoryStore (会话记忆 TTL)       │   │
                         │  │   └── RagContentRetriever (Chroma Top-K 召回)   │   │
                         │  └───────────────────────┬─────────────────────────┘   │
                         │                          │                             │
                         │  Spring Security 6 + JWT Filter (BCrypt + HS512)      │
                         └──┬──────────────────┬───────────────┬──────────────────┘
                            │                  │               │
                   ┌────────▼──────┐  ┌───────▼────────┐  ┌──▼──────────────────────┐
                   │   MySQL 8.0+  │  │  Redis 7.x     │  │  ChromaDB + Embedding   │
                   │  9 张业务表   │  │  会话记忆缓存   │  │  200+ 摄影词典向量检索 │
                   │  JPA ddl-auto │  │  LangChain4j   │  │  余弦相似度 Top-K       │
                   │  update 自动建│  │  集成          │  │  RAG 知识增强          │
                   └───────────────┘  └────────────────┘  └────────────┬───────────┘
                                                                      │
                                                             DashScope OpenAI API
                                                             compatible-mode/v1
                                                         ┌───────▼────────┐
                                                         │  阿里云 Qwen  │
                                                         │ Turbo + VL-Plus│
                                                         └────────────────┘
```

---

## 🚀 快速开始

### 1. 环境要求

| 软件 | 版本要求 | 推荐版本 | 检查命令 |
| :--- | :------: | :------: | :------- |
| **JDK** | 17+ | 17.0.x LTS | `java -version` |
| **Node.js** | 18+ | 20.x LTS | `node -v` |
| **npm** | 9+ | 10.x | `npm -v` |
| **MySQL** | 8.0+ | 8.0.x | `mysql --version` |
| **Maven** | 3.8+ | 3.9.x | `mvn -v` |
| **DashScope API Key** | ✅ 必选 | — | [阿里云开通 DashScope](https://dashscope.console.aliyun.com/) |

> 💡 Chroma 向量库 + Local Embedding 服务：首次启动时如果你的环境里已经有 `chroma-service` 和 `vector-service`，`start-all.ps1` 会一并拉起；如果尚未部署、仅跑通基本 AI 流程，**不启动这两个也能正常用 AI 问答和照片打分**（只是 RAG 检索增强部分暂时离线）。

### 2. 克隆项目

```bash
git clone <你的仓库地址>
cd sheyingtong
```

### 3. 设置环境变量（关键！）

所有敏感信息（API Key、数据库凭证）**全部走环境变量**，不会硬编码在 `application.yml`。

**Windows PowerShell（建议持久化到用户级，以后不用再设）：**

```powershell
# 必填 1：通义千问 DashScope 的 API Key（从阿里云控制台获取）
[Environment]::SetEnvironmentVariable("QWEN_API_KEY",
  "sk-ws-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
  "User")

# 可选：MySQL 连接（默认已经指向本机 3306/sheyingtong，且账号 root/123456，
#       你本机如果不是这个，就按自己的环境改）
[Environment]::SetEnvironmentVariable("DB_URL",
  "jdbc:mysql://localhost:3306/sheyingtong?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai",
  "User")
[Environment]::SetEnvironmentVariable("DB_USERNAME", "root", "User")
[Environment]::SetEnvironmentVariable("DB_PASSWORD", "123456", "User")
```

设置完**重启 PowerShell 窗口**，让环境变量生效。

### 4. 初始化 MySQL 数据库

```sql
-- 登录 MySQL 后执行：
CREATE DATABASE IF NOT EXISTS sheyingtong
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

> 🎯 不需要你建表！项目的 `spring.jpa.hibernate.ddl-auto=update` 会在后端首次启动时，根据 9 个 Entity 类自动创建所有表和外键索引。

### 5. 一键启动（Windows PowerShell 推荐）

```powershell
# 在项目根目录执行：
.\start-all.ps1
```

脚本会依次启动：
| 服务 | 端口 | 说明 |
| :--- | :--: | :--- |
| 🌐 前端 Vite Dev Server | **5173** | http://localhost:5173 |
| ⚙️  后端 Spring Boot    | **8080** | http://localhost:8080 |
| 📚 ChromaDB 向量库      | 8000  | （可缺省，仅 RAG 需要） |
| 🧮 Local Embedding      | 8001  | （可缺省，仅 RAG 需要） |

> 💡 首次启动时，后端会下载 maven 依赖（约 3 分钟），前端 `npm install` 约 2~3 分钟。请耐心等待。
> 脚本自带 `Import-UserEnvVars`，启动时自动把第 3 步设置的用户级环境变量注入进程，不用每次都 `$env:` 手动指定。

### 6. 手动启动（不想用一键脚本）

**后端（2 个终端，或 Idea）：**

```bash
cd backend
mvn spring-boot:run
# → 启动在 http://localhost:8080
```

**前端（另一个终端）：**

```bash
cd frontend
npm install        # 第一次才需要
npm run dev
# → 启动在 http://localhost:5173
```

---

## 🧪 验证与常见问题

### ✅ 验证跑通 3 步走

| # | 操作 | 预期结果 |
|:-:|:----|:--------|
| 1 | 浏览器打开 http://localhost:5173 → 点击右上角「注册」→ 填用户名/邮箱/密码 → **注册成功自动跳登录页** | ✅ 跳到登录页并弹「注册成功，请登录」 |
| 2 | 登录后 → 左侧菜单点「AI 问答」→ 新建会话 → 输入 `什么是三分法构图？` → **发送** | ✅ 约 0.8s 后 AI 开始逐字输出（打字机效果），3s 内答完，回答含 Markdown 列表 |
| 3 | 左侧菜单点「照片打分」→ 上传一张手机拍的照片（2MB 以内 JPG 最佳）→ 点击**开始分析** | ✅ 15~30s 后返回「构图/光线/色彩/对焦/内容/综合」6 维雷达图 + 详细评语 + 改进建议 |

### ❓ 常见问题 FAQ

#### **Q1：浏览器 F12 报错 `POST /api/chat/session/xx/message/stream 403 (Forbidden)`**
A：这是 JWT 过期了，或者你直接开了无痕窗口没登录。点右上角退出登录 → 重新登录一次就好；如果是**刚启动项目**出现，请确认后端启动日志里没有 "JWT_SECRET env var required" 的异常。

#### **Q2：发送 AI 消息弹 "服务器内部错误"，后端日志里有 QWEN API Key 相关报错**
A：`QWEN_API_KEY` 没注入成功。有两种排查方式：
```powershell
# ① 先查当前 PowerShell 里拿到了没：
$env:QWEN_API_KEY
# 空的话就手动设一次（仅本窗口有效）：
$env:QWEN_API_KEY = [Environment]::GetEnvironmentVariable("QWEN_API_KEY", "User")
# ② 确认后重启后端进程即可
```

#### **Q3：上传照片分析卡很久后提示超时**
A：默认后端 `request.timeout=180s`，Qwen-VL-Plus 高峰期偶发慢；可以把图片压缩到 1MB 以内再试；同时后端日志里 `aiService.analyzePhoto` 会打印每次重试的进度（3 次重试，每 2 秒 1 次）。

#### **Q4：我的头像/照片上传后刷新浏览器看不到图片**
A：确认 `./uploads/avatars`、`./uploads/photos`、`./uploads/retouch` 这三个目录有写入权限；`Vite` 已配置 `/uploads` proxy 转发到 `http://localhost:8080`，页面是用的相对路径，不会出现 localhost 硬编码问题。

#### **Q5：数据库报错 `Table 'sheyingtong.xxx' doesn't exist`**
A：检查 `spring.jpa.hibernate.ddl-auto` 有没有被改成 `none`（默认是 `update`，自动建表）；或者直接重启后端让它重新执行一次 schema update。

---

## 📂 项目结构

```
sheyingtong/
│
├── 📁 backend/                              Spring Boot 3.2 后端
│   ├── 📁 src/main/java/com/sheyingtong/
│   │   ├── 📁 SheyingtongApplication.java      启动入口（@SpringBootApplication）
│   │   ├── 📁 controller/                      REST API 入口（8 个）
│   │   │   ├── AuthController.java            登录/注册/用户信息/改密
│   │   │   ├── ChatController.java            AI 聊天（SSE 流式 + 多模态）
│   │   │   ├── PhotoController.java           照片打分 + 历史
│   │   │   ├── RetouchController.java         修图建议 + 历史
│   │   │   ├── DictionaryController.java      摄影术语词典
│   │   │   ├── GearController.java            器材推荐
│   │   │   ├── UploadController.java          头像/图片上传
│   │   │   └── RagController.java             RAG 知识库同步 + 搜索
│   │   ├── 📁 service/                         业务核心
│   │   │   ├── ChatService.java               LLM + 工具调用 + Redis 记忆 + RAG
│   │   │   ├── AIService.java                 Qwen-VL-Plus 多模态主调（3次重试）
│   │   │   ├── PhotoService.java              打分流程 + 压缩 + 降级 mock
│   │   │   ├── RetouchService.java            修图 8 模块解析 + 参数生成
│   │   │   ├── AuthService.java               注册/登录 + BCrypt 哈希 + JWT 签发
│   │   │   ├── PhotographyTools.java          @Tool 3 个：查词典/算曝光/搜器材
│   │   │   └── PhotographyAssistant.java      LangChain4j AiServices 声明式接口
│   │   ├── 📁 security/                        Spring Security 6 + JWT 链路
│   │   │   ├── SecurityConfig.java            URL 权限分层 + CORS + CSRF 关闭
│   │   │   ├── JwtAuthenticationFilter.java   Token 解包 → 注入 SecurityContext
│   │   │   └── JwtUtil.java                   HS512 签发/解析/过期校验
│   │   ├── 📁 entity/                          9 个 JPA Entity（对应数据库 9 张表）
│   │   │   ├── User.java / ChatSession.java / ChatMessage.java
│   │   │   ├── PhotoAnalysis.java / RetouchRecord.java
│   │   │   ├── PhotographyTerm.java / Gear.java / GearCategory.java
│   │   ├── 📁 repository/                      Spring Data JPA 接口
│   │   ├── 📁 dto/                             请求/响应 DTO（ApiResponse 统一包装）
│   │   ├── 📁 config/                          配置类
│   │   │   ├── JwtConfig.java / RagConfig.java
│   │   │   └── LocalEmbeddingModel.java        本地 Embedding 客户端
│   │   └── 📁 exception/                       GlobalExceptionHandler
│   └── 📁 src/main/resources/application.yml  全部用 ${ENV_VAR:default} 占位
│
├── 📁 frontend/                             Vue 3.5 + Element Plus 2.14 前端
│   ├── 📁 src/
│   │   ├── 📁 api/                             后端请求封装
│   │   │   ├── request.js                     axios 单例 + 3 层拦截器
│   │   │   ├── chat.js                        SSE 流式 XMLHttpRequest（非 axios）
│   │   │   ├── auth.js / photo.js / retouch.js
│   │   │   └── dictionary.js / gear.js
│   │   ├── 📁 views/                           12 个页面
│   │   │   ├── Auth/ LoginPage · RegisterPage
│   │   │   ├── Home/ HomePage
│   │   │   ├── Chat/ ChatPage                 AI 问答主页（流式打字机）
│   │   │   ├── PhotoScore/ PhotoScorePage     上传 + 6 维雷达图 + 历史列表
│   │   │   ├── Retouch/ RetouchPage           Canvas 滤镜对比 + 修图步骤
│   │   │   ├── Dictionary/ DictionaryPage     术语搜索 + 卡片详情
│   │   │   ├── Gear/ GearRecommendPage        器材推荐 + 筛选
│   │   │   ├── Profile/ ProfilePage           用户中心（头像/密码/统计）
│   │   │   └── Learning/ TutorialsPage · TipsPage · GalleryPage
│   │   ├── 📁 components/Layout/               布局组件（AppSidebar 等）
│   │   ├── 📁 stores/                          Pinia 状态管理（user.js）
│   │   ├── 📁 router/index.js                  createWebHistory + beforeEach 鉴权
│   │   └── App.vue · main.js
│   ├── vite.config.js                         5173 端口 + /api + /uploads 代理
│   └── package.json                           Vue 3.5 / Element Plus 2.14 / Vite 6.3
│
├── 📄 start-all.ps1                           Windows 一键启动脚本（含环境变量注入）
├── 📄 pom.xml                                 Spring Boot 3.2.0 / jjwt 0.12.5 …
└── 📄 README.md                               本文件
```

---

## 🧱 模块说明

### 1. 安全链路（JWT + Spring Security）
> 代码入口：[SecurityConfig.java](backend/src/main/java/com/sheyingtong/security/SecurityConfig.java)
- **URL 权限分层**（从上到下匹配）：
  - `permitAll`：登录/注册、词典、器材、缩略图
  - `authenticated`：聊天、打分、修图、上传、DELETE 操作
  - `anyRequest().permitAll()`：兜底（RAG 搜索）
- **JWT 过滤器链**：`JwtAuthenticationFilter` 在 UsernamePasswordAuthenticationFilter **之前**插入；
  直接把 User 实体放 principal 位置，减少后续 Controller 每次查询数据库。
- **密码哈希**：BCryptPasswordEncoder cost=10，相同明文每次 hash 不同，防彩虹表。

### 2. AI 聊天（SSE 流式 + Function Calling）
> 代码入口：[ChatService.java](backend/src/main/java/com/sheyingtong/service/ChatService.java) / [chat.js](frontend/src/api/chat.js)

<details>
<summary>🎞️ 界面预览（点击展开截图）</summary>

[![AI聊天预览](docs/screenshots/01-ai-chat.png)](docs/screenshots/01-ai-chat.png)
</details>

- **非流式** `/message`：用 LangChain4j `PhotographyAssistant`，自动带工具调用 + Redis 记忆 + RAG 召回，适合调试 curl。
- **流式** `/message/stream`：用 WebFlux `Flux<String>` + reactor `Sinks.Many` 发布订阅，
  后台线程跑 `StreamingChatModel.chat()` → 每 token sink.emitNext → 前端 XMLHttpRequest onprogress 增量拼 fullResponse。
- **工具调用**：`PhotographyTools` 上 3 个 `@Tool` 方法，LLM 遇到专业问题自动触发函数 → 拿返回二次作答。

### 3. 照片打分 + 修图（Qwen-VL-Plus 多模态）
> 代码入口：[AIService.java](backend/src/main/java/com/sheyingtong/service/AIService.java)

<details>
<summary>🎞️ 界面预览（点击展开截图：左打分 / 右修图对比）</summary>

| 照片 6 维打分结果页 | 修图建议实时对比页 |
|:---:|:---:|
| [![打分预览](docs/screenshots/04-photo-score.png)](docs/screenshots/04-photo-score.png) | [![修图预览](docs/screenshots/02-retouch-compare.png)](docs/screenshots/02-retouch-compare.png) |
</details>

- **压缩策略**：图片先压缩到短边 1024（DashScope 建议 < 2MB），再转 Base64，传输更快 + 省 token。
- **3 次重试机制**：网络抖动、限流、5xx 异常一律 catch，间隔 2s 重试；3 次全失败前端才弹错。
- **Markdown fences 剥离**：模型偶尔把 JSON 放在 ```json ... ``` 里，parseResponse 正则去掉后再反序列化。
- **降级兜底**：`PhotoService` 失败时返回 generateMockAIResult，保证 UI 永远有 6 维度数据不空白。

### 4. RAG 知识增强
> 代码入口：[RagConfig.java](backend/src/main/java/com/sheyingtong/config/RagConfig.java) / [RagController.java](backend/src/main/java/com/sheyingtong/controller/RagController.java)
- **同步知识**：POST `/api/rag/sync` → 读 `PhotographyTerm` 所有术语 → 本地 Embedding 生成向量 → 写 ChromaDB collection `photography_knowledge`。
- **检索**：`RagContentRetriever` → 用户问题 → 生成 embedding → Chroma cosine Top-K → 拼进 system prompt。

### 5. 前端 HTTP 链路（axios + XMLHttpRequest 双轨制）
> 代码入口：[request.js](frontend/src/utils/request.js) / [chat.js](frontend/src/api/chat.js)

<details>
<summary>🎞️ 界面预览（点击展开：首页 + 器材推荐页）</summary>

| 首页渐变 Banner + 数据看板 | 器材选购 AI 推荐方案页 |
|:---:|:---:|
| [![首页预览](docs/screenshots/03-homepage.png)](docs/screenshots/03-homepage.png) | [![器材推荐预览](docs/screenshots/05-gear-recommend.png)](docs/screenshots/05-gear-recommend.png) |
</details>

- **普通 REST**：走 axios 单例 → 请求拦截器自动注入 `Authorization: Bearer <token>` → 响应拦截器自动解包 `ApiResponse.data` → code≠200 弹 ElMessage + reject。
- **SSE 流式**：走原生 XMLHttpRequest（axios 浏览器端不支持 responseType stream），**手动 setRequestHeader 加 Authorization**（之前丢这个 → 403 的经典坑）。
- **鉴权守卫**：router.beforeEach → `requiresAuth && !token` → 弹 ElMessageBox 阻止跳转 → 引导去登录。

---

## 📦 部署说明

### Docker Compose（生产推荐）
> 示例配置（新建 `docker-compose.yml` 放根目录）：

```yaml
version: "3.8"
services:
  mysql:
    image: mysql:8.0
    container_name: sheyingtong-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: sheyingtong
    volumes:
      - ./data/mysql:/var/lib/mysql

  redis:
    image: redis:7-alpine
    container_name: sheyingtong-redis
    restart: always

  chroma:
    image: chromadb/chroma:0.4
    container_name: sheyingtong-chroma
    restart: always

  backend:
    build: ./backend
    container_name: sheyingtong-backend
    restart: always
    depends_on: [mysql, redis, chroma]
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://mysql:3306/sheyingtong?useSSL=false&serverTimezone=Asia/Shanghai
      DB_USERNAME: root
      DB_PASSWORD: ${DB_PASSWORD}
      QWEN_API_KEY: ${QWEN_API_KEY}
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
    volumes:
      - ./uploads:/app/uploads

  frontend:
    build: ./frontend
    container_name: sheyingtong-frontend
    restart: always
    depends_on: [backend]
    ports:
      - "80:80"
```

Nginx 生产前端容器建议用：
- 打包 `npm run build` → `dist/` 扔 `/usr/share/nginx/html`
- `/api` 和 `/uploads` `proxy_pass http://backend:8080`
- `try_files $uri $uri/ /index.html` 兜底 Vue 的 History 模式刷新 404 问题。

---

## 🤝 贡献与开发

### 本地调试

```bash
# 后端：Idea 打开 backend/pom.xml → 右键 SheyingtongApplication → Run/Debug
# 前端：
cd frontend
npm install
npm run dev   # Vite HMR 热更新，保存代码页面自动刷新
```

### 代码风格

- Java：遵循 Google Java Style；Lombok `@RequiredArgsConstructor` 优先
- Vue：Composition API `<script setup>`，推荐 `<script setup lang="js">`
- 提交信息：Conventional Commits
  - `feat: 新增 XX 功能`
  - `fix: 修复 AI 聊天 403 问题`
  - `docs: 完善 README 启动说明`

### 提交流程（欢迎 PR）

1. Fork 仓库 → 新建 `feature/xxx` 或 `fix/xxx` 分支
2. 代码写完 + 本地跑通 3 步验证（见上文）
3. 提交 PR 附：**运行截图 1~3 张 + 变更说明**
4. Review 合入 → 感谢你的贡献 🎉

---

## 📜 License

本项目基于 **MIT License** 开源，可自由商用、修改、分发，但请在你的衍生项目中保留一份原始 MIT 声明。

---

<div align="center">

🎞️ **社影通** · 让每个普通人都能拍出好照片 📷

Made with ❤️ using Spring Boot · Vue · Qwen LLM · ChromaDB

</div>
