# ---- Chroma 向量库自建服务（RAG 档可选） ----
# 后端（ChromaRagService）调用的是本项目自建 FastAPI 路由（/collections/{name}/add 等），
# 不是官方 chromadb 镜像的 API，故必须构建此服务，不能用 chromadb/chroma 官方镜像替代。
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
# -i 国内镜像：服务器在国内时 pip 拉 chromadb 及其依赖才不超时
RUN pip install --no-cache-dir -i https://mirrors.aliyun.com/pypi/simple/ -r requirements.txt

COPY main.py .

EXPOSE 8002

# main.py 里 uvicorn.run(host="localhost") 只监听容器回环，容器间访问会失败；
# 用 uvicorn CLI 作入口强制 0.0.0.0（__main__ 分支不会执行）
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8002"]