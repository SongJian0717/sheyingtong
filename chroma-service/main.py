from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Optional, Any
import chromadb
from chromadb.config import Settings
import os

app = FastAPI(title="Chroma Vector Service")

# Chroma 持久化目录：基于本文件所在路径，支持跨机器迁移（不要再写死用户名的绝对路径！）
#   · __file__       = .../sheyingtong/chroma-service/main.py
#   · os.path.dirname(__file__) = .../sheyingtong/chroma-service
#   · 上一级 = .../sheyingtong  →  拼上 chroma-data 就是项目根下的持久化目录
_BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
_CHROMA_PERSIST_DIR = os.path.join(_BASE_DIR, "chroma-data")
os.makedirs(_CHROMA_PERSIST_DIR, exist_ok=True)

chroma_client = chromadb.PersistentClient(
    path=_CHROMA_PERSIST_DIR,
    settings=Settings(chroma_server_cors_allow_origins=["*"])
)

@app.get("/health")
async def health():
    """Lightweight health check for startup scripts (returns immediately)."""
    return {
        "status": "ok",
        "persist_dir": _CHROMA_PERSIST_DIR,
        "collections": len(chroma_client.list_collections())
    }

@app.get("/")
async def root():
    return {"message": "Chroma Vector Service is running", "persist_dir": _CHROMA_PERSIST_DIR}

class EmbeddingRequest(BaseModel):
    texts: List[str]

class AddRequest(BaseModel):
    ids: List[str]
    documents: List[str]
    embeddings: Optional[List[List[float]]] = None
    metadatas: Optional[List[Dict[str, Any]]] = None

class QueryRequest(BaseModel):
    query_embeddings: List[List[float]]
    n_results: int = 5
    include: List[str] = ["documents", "metadatas", "distances"]

class CollectionInfo(BaseModel):
    name: str
    count: int

@app.get("/collections")
async def list_collections() -> List[CollectionInfo]:
    collections = chroma_client.list_collections()
    return [CollectionInfo(name=c.name, count=c.count()) for c in collections]

@app.post("/collections")
async def create_collection(name: str, get_or_create: bool = True):
    try:
        collection = chroma_client.get_or_create_collection(name=name)
        return {"name": collection.name, "count": collection.count()}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/collections/{name}/add")
async def add_to_collection(name: str, request: AddRequest):
    try:
        collection = chroma_client.get_or_create_collection(name=name)
        collection.add(
            ids=request.ids,
            documents=request.documents,
            embeddings=request.embeddings,
            metadatas=request.metadatas
        )
        return {"status": "success", "count": collection.count()}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/collections/{name}/query")
async def query_collection(name: str, request: QueryRequest):
    try:
        collection = chroma_client.get_or_create_collection(name=name)
        results = collection.query(
            query_embeddings=request.query_embeddings,
            n_results=request.n_results,
            include=request.include
        )
        return results
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/collections/{name}/count")
async def count_collection(name: str):
    try:
        collection = chroma_client.get_or_create_collection(name=name)
        return {"count": collection.count()}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.delete("/collections/{name}")
async def delete_collection(name: str):
    try:
        chroma_client.delete_collection(name=name)
        return {"status": "success"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/collections/{name}/reset")
async def reset_collection(name: str):
    try:
        chroma_client.delete_collection(name=name)
        collection = chroma_client.create_collection(name=name)
        return {"status": "success", "count": collection.count()}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="localhost", port=8002)