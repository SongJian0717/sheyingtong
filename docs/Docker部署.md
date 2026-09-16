# 社影通 · Docker 部署指南（Linux）

> 面向 Linux 服务器的容器化部署手册，与 `docs/RabbitMQ异步化设计方案.md` / `docs/Redis缓存设计方案.md` 相互独立。
> 部署分两档：**核心档**（MySQL + Redis + RabbitMQ + 后端 + 前端，零 Python 依赖）与 **RAG 增强档**（叠加 Chroma 向量库 + Qwen3-Embedding 向量服务，语义检索问答可用）。
> 全程不推送镜像到仓库，所有镜像在服务器本地 `docker compose up -d --build` 构建。
>
> ⚠️ 本文件描述的镜像构建与启动流程**尚未在真实 Linux 环境完整验证过**（编写环境为 Windows 无 Docker），首次部署如遇问题优先排查"端口 / 防火墙 / .env 必填项 / 模型下载网络"四项。

---

## 0. 部署总览

| 服务 | 容器名 | 镜像/构建源 | 对外端口 | 持久化 |
|---|---|---|---|---|
| frontend（nginx） | sheyingtong-frontend | `./frontend/Dockerfile` | **80 → 80** | — |
| backend（Spring Boot） | sheyingtong-backend | `./backend/Dockerfile` | —（默认不暴露，调试时打开注释） | `./uploads` bind mount |
| mysql | sheyingtong-mysql | `mysql:8.0` | — | 卷 `sheyingtong-mysql-data` |
| redis | sheyingtong-redis | `redis:7-alpine` | — | 卷 `sheyingtong-redis-data` |
| rabbitmq | sheyingtong-rabbitmq | `rabbitmq:3.13-management` | — | 卷 `sheyingtong-rabbitmq-data` |
| chroma（RAG 档） | sheyingtong-chroma | `./chroma-service/Dockerfile` | — | 卷 `sheyingtong-chroma-data` |
| vector（RAG 档） | sheyingtong-vector | `./vector-service/Dockerfile` | — | 卷 `sheyingtong-vector-models` |

- 唯一对外入口是 **nginx 80 端口**：`/api/**` 反代后端（REST + SSE 流式），`/uploads/**` 反代图片，其余路径服务前端静态文件（Vue history 路由）。
- 容器间全部走 compose 服务名通信（`mysql`、`redis`、`rabbitmq`、`backend`、`chroma`、`vector`），无需配置 host 映射。
- 后端上传目录是相对进程工作目录的 `./uploads`，容器内即 `/app/uploads`，与宿主机项目目录 `./uploads` 双向同步（bind mount）。

---

## 1. 前置环境

### 1.1 安装 Docker Engine + Compose 插件

**Ubuntu / Debian**（推荐）：

```bash
# 官方脚本（含 compose v2 插件）
curl -fsSL https://get.docker.com | sh
systemctl enable --now docker
docker compose version   # 应输出 v2.x
```

**CentOS 7** 已于 2024-06-30 停止维护（EOL），官方 docker-ce 仓库已下架，不自带 compose 插件，安装繁琐，见[附录 A](#附录-a-centos-7-eol-安装-docker-不再推荐)。

### 1.2 服务器配置建议

| 部署档 | 内存建议 | 磁盘建议 | 说明 |
|---|---|---|---|
| 核心档 | 4 GB+ | 5 GB+ | JVM 默认堆 + MySQL/Redis/RabbitMQ 三个常驻 |
| RAG 档 | 8 GB+ | 10 GB+ | 向量服务加载 1.2 GB 模型进内存，首次要再下载 1.2 GB 缓存 |

### 1.3 防火墙放行

```bash
# 以 firewalld 为例（按需调整为 ufw/iptables/安全组）
firewall-cmd --permanent --add-port=80/tcp && firewall-cmd --reload
```

> 云服务器还需在控制台安全组放行 80 端口。

---

## 2. 核心档部署（5 分钟）

### 2.1 拷贝项目

```bash
git clone <你的仓库地址> sheyingtong && cd sheyingtong
# 或直接从本机打包上传：tar czf sheyingtong.tar.gz --exclude=node_modules --exclude=target --exclude=dist --exclude=uploads --exclude=.env --exclude=.git sheyingtong
```

> `.gitignore` 已排除 `.env`、`backend/uploads/`、`frontend/node_modules`、`frontend/dist`、`backend/target`、`vector-service/models` 等，不会进入 git；`docs`、各 Dockerfile 均在库内。

### 2.2 创建 .env（必填三项）

```bash
cp .env.example .env
openssl rand -base64 48   # 生成 JWT_SECRET，复制到 .env
vi .env
```

| 变量 | 必填 | 说明 |
|---|---|---|
| `QWEN_API_KEY` | ✅ | 通义千问 API Key（[DashScope 控制台](https://dashscope.console.aliyun.com/)）。**打分 / 修图建议 / AI 聊天全部走它**，未填时 `docker compose up` 直接报错拒绝启动 |
| `JWT_SECRET` | ✅ | 签名密钥，**必须固定一个长随机串**。留空时后端每次重启动态生成 → 所有已登录用户 token 全部失效 |
| `DB_PASSWORD` | ✅ | MySQL root 密码，**只在首次创建 mysql-data 卷时生效** |
| `RABBITMQ_USERNAME/PASSWORD/VHOST` | 可选 | 默认 `guest/guest`，生产建议修改 |
| `OPENAI_API_KEY/BASE_URL` | 可选 | 预留占位，**当前业务代码未使用**（应用内全部走 qwen） |

### 2.3 构建并启动

```bash
docker compose up -d --build
docker compose ps --format "table {{.Name}}\t{{.Status}}"
```

启动时序：Compose 会等 `mysql`、`redis`、`rabbitmq` 全部通过健康检查后再拉起 `backend`，前端最后一个起来。

### 2.4 验证

```bash
docker compose logs -f backend      # 看到 "Started ... Application in Xs" 即为启动成功
curl -s http://127.0.0.1/api/health # 或浏览器访问 http://<服务器IP>/
```

- 首次启动后端会执行 `data.sql` + `SeedDataRunner` 自动建表并灌入种子数据（摄影术语 200+ 条、器材库等），**无需手工初始化库**。
- **默认没有内置账号**，打开首页注册一个即可。
- 上传图片会落在宿主机 `./uploads` 下，容器重启不丢。

---

## 3. RAG 增强档（可选：语义检索问答）

核心档的 AI 问答是"纯大模型"；RAG 档额外启用 **Chroma 向量库 + 本地向量化服务**，让后端在提问时检索摄影术语/器材知识库注入上下文（`/api/rag/status` 可查状态）。

### 3.1 启动

```bash
docker compose -f docker-compose.yml -f docker-compose.rag.yml up -d --build
```

先启动过核心档再叠加也很安全：RAG 档只在原 compose 之上**追加/覆盖**服务，不会重建 MySQL 等。

### 3.2 首次启动的两次等待

1. **模型下载**：vector 服务首次从 HuggingFace 下载 Qwen3-Embedding-0.6B（约 1.2 GB）到卷 `sheyingtong-vector-models`，`start_period` 给了 60 s 且 retries 20，下载慢需要 `docker compose -f ... logs -f vector` 观察进度。服务器无法访问 HuggingFace 时：
   - 设置镜像环境变量 `export HF_ENDPOINT=https://hf-mirror.com` 再 `up`；或
   - 在任意能上网的机器上先跑一次 `vector-service` 完成下载，把 `models` 目录拷到服务器并改挂载；或
   - 干脆退回核心档的内置 BGE 模型（见 3.4）。
2. **全量向量化**：Chroma 首次启动后后端自动同步 MySQL 中全部术语/器材（500+ 段，约 **1~3 分钟**，日志关键词 `【RAG自动同步】`）。期间 RAG 检索结果为空属正常。

### 3.3 切换 embedding 模型 → 必须先清空向量库

后端支持两种向量模型，**向量维度不一致（1024 ↔ 512），换模型后旧向量无法检索，必须清库重建**：

```bash
docker compose -f docker-compose.yml -f docker-compose.rag.yml down
docker volume rm sheyingtong-chroma-data
docker compose -f docker-compose.yml -f docker-compose.rag.yml up -d --build
```

### 3.4 想用内置 BGE 模型（不跑 Python 服务）

核心档默认就是内置 BGE-small-zh（512 维、零外部依赖）。如果 RAG 档启动后又想省掉 vector 服务：在 `.env` 或 compose 覆盖 `EMBEDDING_LOCAL_SERVICE_ENABLED=false` 并去掉 `EMBEDDING_LOCAL_SERVICE_URL`，同时按 3.3 清空 chroma 卷。

---

## 4. 数据持久化与备份

### 4.1 数据落在哪

| 数据 | 位置 | 备份方式 |
|---|---|---|
| MySQL 全部业务数据 | 卷 `sheyingtong-mysql-data` | mysqldump（推荐） |
| 上传图片 / 头像 | 项目目录 `./uploads` | 直接打包目录 |
| RabbitMQ 消息（未消费队列） | 卷 `sheyingtong-rabbitmq-data` | 一般无需备份，消费后即消失 |
| Redis 缓存 | 卷 `sheyingtong-redis-data` | 可随时清空，重建 |
| Chroma 向量库（RAG 档） | 卷 `sheyingtong-chroma-data` | 可重建；或备份后改回 `app.rag.auto-sync=false` 时手动恢复 |
| 向量模型缓存（RAG 档） | 卷 `sheyingtong-vector-models` | 无需备份（重下/拷目录即可） |

### 4.2 备份命令

```bash
# 1) MySQL 全库导出到宿主机
docker exec sheyingtong-mysql sh -c 'mysqldump -uroot -p"<DB_PASSWORD>" --single-transaction --databases sheyingtong' \
  > sheyingtong_backup_$(date +%F).sql

# 2) 上传目录
tar czf uploads_$(date +%F).tar.gz uploads/
```

### 4.3 恢复 / 迁移到新机器

```bash
# 新机器同样建好 .env 并启动后：
docker cp sheyingtong_backup_2026-09-15.sql sheyingtong-mysql:/tmp/backup.sql
docker exec sheyingtong-mysql sh -c 'mysql -uroot -p"<DB_PASSWORD>" < /tmp/backup.sql'
tar xzf uploads_2026-09-15.tar.gz   # 解回项目目录
```

---

## 5. 升级 / 重启 / 运维

```bash
# 修改代码后重建（只重建有变动的镜像，MySQL 等不动）
docker compose up -d --build

# 仅重启全部容器
docker compose restart

# 查看日志
docker compose logs -f backend | frontend | mysql

# 全部停止（保留数据卷）
docker compose down

# 彻底清理（慎用！删数据卷）
docker compose down -v
```

运维要点：

- **`restart: unless-stopped`**：宿主机重启 / dockerd 重启后所有容器自动拉起，无需人工干预。
- **`JWT_SECRET` 一旦上线就不要改**。改动 → 重启后端 → 全员需要重新登录。要轮换只能接受"踢下线一次"。
- **`DB_PASSWORD` 只在首次创建 `mysql-data` 卷时生效**。要改库密码：以 root 进容器 `ALTER USER 'root'@'%' IDENTIFIED BY '新密码'`，再同步改 `.env` 中 `DB_PASSWORD` 后 `docker compose up -d` 重建 backend。
- 升级期间若需停服：`docker compose stop frontend` 即可对外"暂停"，数据层不动。

---

## 6. 常见问题（FAQ）

### 6.1 提交照片后一直在"处理中"，或 AI 没反应？
- 检查 RabbitMQ：`docker compose logs -f backend` 是否有 `消息投递失败，降级为就地同步执行`。**broker 不可达时系统按配置自动降级为同步执行（请求挂到 AI 返回为止，功能不中断）**；broker 恢复后无需任何操作，异步链路自动回归。
- 检查 `QWEN_API_KEY` 是否有效：对话一发就报鉴权错误基本就是它。
- 首次 start 时 backend 依赖 rabbitmq 健康才启动，若 rabbitmq 起得慢（健康检查 30 s），backend 会等待而非失败。

### 6.2 上传的图片 404 / 头像不显示？
- 后端按容器内 `/app/uploads` 解析，它是宿主机 `./uploads` 的 bind mount。确认宿主机目录**存在且有写权限**（`mkdir -p uploads chmod 777 uploads`，或让容器以非 root 启动时给 `chown`）。
- 检查 nginx 反代配置没被改动过——`/uploads/` 前缀必须保留，浏览器直接访问 `http://IP/uploads/<文件名>` 应返回图片。
- 日志 `No such file or directory` 且容器内 `/app/uploads` 为空，多半是 bind mount 没挂上：`docker inspect sheyingtong-backend | grep -A5 Mounts`。

### 6.3 AI 聊天流式输出卡住/一次性吐整段？
- SSE 被反代 buffering 吞了。项目自带 `frontend/nginx.conf` 已配 `proxy_buffering off; proxy_cache off;`，**不要**在自建反代（如云 LB、Caddy）上对 `/api/` 开缓存。
- 云负载均衡（ALB/CLB）默认也缓冲，需在控制台对该路径关闭响应改写缓存。

### 6.4 时间显示差 8 小时 / 日志时区不对？
- MySQL 与后端容器已显式 `TZ=Asia/Shanghai`（backend 另加 `-Duser.timezone`），业务时间正常。
- nginx/alpine 镜像默认 UTC，其 access/error 日志时间比北京时间慢 8h，属正常现象，不影响业务。
- 若再嵌套宿主机时区（`/etc/localtime` 裸挂载不考虑），外部访问时间正常即可。

### 6.5 启动后 backend 日志里有 Chroma 连接失败的 error？
- 这是**已知遗留**：`ChromaRagService.initAsync` 在每次上下文刷新时无条件尝试同步一次向量库，没有自动同步开关（`app.rag.auto-sync` 只能关掉 `RagAutoSyncRunner`）。
- 核心档部署时 Chroma 不存在 → 会打一条 `Chroma RAG知识库异步初始化失败` 的 error 日志。**不影响任何业务功能**（RAG 检索在核心档本就未启用），可忽略。

### 6.6 模型下载很慢 / 一直失败？
- 工具静默重试：`docker compose -f docker-compose.yml -f docker-compose.rag.yml logs -f vector`。
- 服务器无法直连 HuggingFace：临时 `export HF_ENDPOINT=https://hf-mirror.com` 后重启；或走 3.2 的"本机下载后拷目录"方案。
- 磁盘不够：`du -sh vector-service/models` 确认模型缓存落在卷上而非容器内。

### 6.7 端口 80 被占用 / 想改对外端口？
- 改 `docker-compose.yml` 中 frontend 的 `ports: "8080:80"`（左侧为宿主端口），然后 `docker compose up -d frontend`。

---

## 附录 A：CentOS 7（EOL）安装 Docker（不再推荐）

CentOS 7 官方软件源已进入 EOL 归档（vault），`download.docker.com` 的 centos/7 仓库也已下线，且**不自带 compose v2 插件**。如果必须用：

```bash
# 1) 用 vault 源装基础包 + 手动装 docker rpm（示例 CPU 版）
yum install -y yum-utils
curl -fsSL https://download.docker.com/linux/centos/7/x86_64/stable/Packages/ \
  # 从上述目录挑选 docker-ce-<版本>.el7.x86_64.rpm 与对应 python3 依赖逐一下载安装

# 2) compose v2 插件以独立二进制安装
mkdir -p ~/.docker/cli-plugins
curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
  -o ~/.docker/cli-plugins/docker-compose && chmod +x ~/.docker/cli-plugins/docker-compose
```

> 强烈建议：生产环境优先 **Ubuntu 22.04 / Debian 12 / CentOS 8 以上**，本指南命令以这些系统为准。

---

## 附录 B：关键环境变量速查（backend 容器）

| 变量 | 核心档值 | RAG 档值 |
|---|---|---|
| `DB_URL` | `jdbc:mysql://mysql:3306/sheyingtong?...`（compose 内置） | 同左 |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `$DB_PASSWORD` | 同左 |
| `SPRING_DATA_REDIS_HOST` | `redis` | 同左 |
| `RABBITMQ_HOST` / `*_USERNAME` / `*_PASSWORD` / `*_VHOST` | compose 内置 | 同左 |
| `JWT_SECRET` | .env | .env |
| `QWEN_API_KEY` | .env | .env |
| `EMBEDDING_LOCAL_SERVICE_ENABLED` | `false`（内置 BGE） | `true`（external） |
| `EMBEDDING_LOCAL_SERVICE_URL` | — | `http://vector:8001` |
| `CHROMA_HOST` / `CHROMA_PORT` | — | `chroma` / `8002` |
| `APP_RAG_AUTO_SYNC` | `false` | `true` |