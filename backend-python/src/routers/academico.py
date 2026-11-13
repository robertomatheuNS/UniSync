from beanie import PydanticObjectId
from fastapi import APIRouter, Depends

from src.core.security import UsuarioAutenticado, obter_usuario_atual
from src.schemas.academico import (AvisoCreate, AvisoDados, AvisoResponse,
                                   EntradaTurma, TurmaDados, TurmaResponse)
from src.services import academico_service as service

router = APIRouter(tags=["Turmas e avisos"])


@router.get("/turmas/listar", response_model=list[TurmaResponse])
async def listar_turmas(usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.listar_turmas(usuario)


@router.post("/turmas/criar", response_model=TurmaResponse, status_code=201)
async def criar_turma(dados: TurmaDados, usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.criar_turma(dados, usuario)


@router.post("/turmas/entrar", response_model=TurmaResponse)
async def entrar_turma(dados: EntradaTurma, usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.entrar_turma(dados, usuario)


@router.patch("/turmas/atualizar/{turma_id}", response_model=TurmaResponse)
async def atualizar_turma(turma_id: PydanticObjectId, dados: TurmaDados,
                          usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.atualizar_turma(turma_id, dados, usuario)


@router.post("/turmas/arquivar/{turma_id}", status_code=204)
async def arquivar_turma(turma_id: PydanticObjectId, usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    await service.arquivar_turma(turma_id, usuario)


@router.get("/avisos/listar", response_model=list[AvisoResponse])
async def listar_avisos(turma_id: PydanticObjectId | None = None,
                        usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.listar_avisos(usuario, turma_id)


@router.post("/avisos/criar", response_model=AvisoResponse, status_code=201)
async def criar_aviso(dados: AvisoCreate, usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.criar_aviso(dados, usuario)


@router.patch("/avisos/atualizar/{aviso_id}", response_model=AvisoResponse)
async def atualizar_aviso(aviso_id: PydanticObjectId, dados: AvisoDados,
                          usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.atualizar_aviso(aviso_id, dados, usuario)


@router.put("/avisos/ler/{aviso_id}", response_model=AvisoResponse)
async def marcar_lido(aviso_id: PydanticObjectId, usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.marcar_lido(aviso_id, usuario)


@router.delete("/avisos/excluir/{aviso_id}", status_code=204)
async def excluir_aviso(aviso_id: PydanticObjectId, usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    await service.excluir_aviso(aviso_id, usuario)
