from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import numpy as np
import os

# 用 setdefault 而非直接赋值：本机保持"离线优先"（读 ./models 缓存），
# Docker 部署时可经环境变量覆盖为在线下载（TRANSFORMERS_OFFLINE=0）
os.environ.setdefault('TRANSFORMERS_CACHE', './models')
os.environ.setdefault('SENTENCE_TRANSFORMERS_HOME', './models')
os.environ.setdefault('TRANSFORMERS_OFFLINE', '1')
os.environ.setdefault('HF_DATASETS_OFFLINE', '1')

app = FastAPI(title="Qwen3-Embedding Service", version="1.0.0")

model = None


class EmbeddingRequest(BaseModel):
    texts: List[str]


class EmbeddingResponse(BaseModel):
    embeddings: List[List[float]]
    dimensions: int


@app.on_event("startup")
async def startup_event():
    global model
    print("Loading Qwen3-Embedding-0.6B model...")
    try:
        from sentence_transformers import SentenceTransformer
        model_name = "Qwen/Qwen3-Embedding-0.6B"
        model = SentenceTransformer(model_name)
        print(f"Qwen3-Embedding-0.6B model loaded successfully")
        print(f"Model dimension: {model.get_sentence_embedding_dimension()}")
    except Exception as e:
        print(f"Failed to load model: {e}")
        raise e


@app.post("/embed", response_model=EmbeddingResponse)
async def embed(request: EmbeddingRequest):
    if not model:
        raise HTTPException(status_code=500, detail="Model not loaded")
    
    if not request.texts or len(request.texts) == 0:
        raise HTTPException(status_code=400, detail="No texts provided")
    
    try:
        embeddings = model.encode(request.texts, convert_to_numpy=True)
        dimensions = model.get_sentence_embedding_dimension()
        
        return EmbeddingResponse(
            embeddings=embeddings.tolist(),
            dimensions=dimensions
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Embedding failed: {str(e)}")


@app.get("/health")
async def health():
    if model:
        return {"status": "healthy", "model": "Qwen3-Embedding-0.6B"}
    else:
        return {"status": "unhealthy", "model": "not loaded"}


@app.get("/")
async def root():
    return {"message": "Qwen3-Embedding-0.6B Service is running"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)