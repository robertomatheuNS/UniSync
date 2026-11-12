from contextlib import asynccontextmanager
from src.routers import oportunidades
from fastapi import FastAPI
from src.routers import tarefas
from fastapi.middleware.cors import CORSMiddleware

from src.database.connection import (
    conectar_mongodb,
    desconectar_mongodb,
)

@asynccontextmanager
async def lifespan(app: FastAPI):
    await conectar_mongodb()

    yield

    await desconectar_mongodb()

app = FastAPI(
    title="UniSync - Motor de Oportunidades",
    description="API responsável pelo gerenciamento de oportunidades acadêmicas.",
    version="0.1.0",
    lifespan=lifespan,
)


app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://127.0.0.1:5173",
    ],
    allow_credentials=False,
    allow_methods=["GET", "POST", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)

app.include_router(oportunidades.router)
app.include_router(tarefas.router)

@app.get("/health", tags=["Health"])
async def health_check():
    return {"status": "ok"}