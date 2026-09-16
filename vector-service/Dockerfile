# ---- Qwen3-Embedding-0.6B 向量服务（RAG 档可选） ----
# 注意：模型缓存 1.2GB 在 .gitignore 里，不随 git 分发；首次启动经 TRANSFORMERS_OFFLINE=0
# 在线下载到 TRANSFORMERS_CACHE 指向的卷（/models），后续启动复用卷缓存，无需联网
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
# 先装 CPU 版 torch：pypi 默认 wheel 带完整 CUDA（2GB+），这台无 GPU 服务器完全用不上
# 会白白拉 nvidia-cudnn/cuda-toolkit 浪费时间空间；--index-url 官方 CPU 源绕过
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu
# -i 国内镜像：服务器在国内时 pip 拉其余依赖（transformers 等）才不超时
RUN pip install --no-cache-dir -i https://mirrors.aliyun.com/pypi/simple/ -r requirements.txt

COPY main.py .

# 在线下载开关 + 模型缓存指向持久卷（main.py 用 setdefault，环境变量优先）
ENV TRANSFORMERS_OFFLINE=0 \
    HF_DATASETS_OFFLINE=0 \
    TRANSFORMERS_CACHE=/models \
    SENTENCE_TRANSFORMERS_HOME=/models

EXPOSE 8001

# 国内网络从 hf-mirror.com 拉 Qwen3-Embedding 模型（1.2GB）；能直连 HF 时可去掉此行
ENV HF_ENDPOINT=https://hf-mirror.com

# 用 uvicorn CLI 作入口（main.py 的 __main__ 分支不执行，监听地址由 CLI 保证 0.0.0.0）
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]