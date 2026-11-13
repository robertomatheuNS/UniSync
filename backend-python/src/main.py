from contextlib import asynccontextmanager
from src.routers import oportunidades
from fastapi import FastAPI
from src.routers import tarefas
from src.routers import academico
from src.services.academico_service import ErroAcademico
from fastapi.responses import JSONResponse
from src.routers import lembretes
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
        "http://localhost:8081",
        "http://127.0.0.1:8081",
    ],
    allow_credentials=False,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)

app.include_router(oportunidades.router)
app.include_router(tarefas.router)
app.include_router(academico.router)
app.include_router(lembretes.router)


@app.exception_handler(ErroAcademico)
async def tratar_erro_academico(request, erro: ErroAcademico):
    return JSONResponse(status_code=erro.status, content={"detail": erro.mensagem})

@app.get("/health", tags=["Health"])
async def health_check():
    return {"status": "ok"}
