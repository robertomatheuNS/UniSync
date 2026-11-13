from beanie import PydanticObjectId
from fastapi import APIRouter, Depends, Query
from src.core.security import UsuarioAutenticado, obter_usuario_atual
from src.schemas.mural import PublicacaoCreate, PublicacaoUpdate, PublicacaoResponse, PaginaPublicacoes
from src.services import mural_service as service

router = APIRouter(prefix="/mural", tags=["Mural colaborativo"])


@router.get("/listar", response_model=PaginaPublicacoes)
async def listar(turma_id: PydanticObjectId | None = None, antes: PydanticObjectId | None = None,
                 limite: int = Query(default=20, ge=1, le=50), usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.listar(usuario, turma_id, antes, limite)


@router.post("/criar", response_model=PublicacaoResponse, status_code=201)
async def criar(dados: PublicacaoCreate, usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.criar(dados, usuario)


@router.get("/buscar/{publicacao_id}", response_model=PublicacaoResponse)
async def buscar(publicacao_id: PydanticObjectId, usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.buscar(publicacao_id, usuario)


@router.patch("/atualizar/{publicacao_id}", response_model=PublicacaoResponse)
async def atualizar(publicacao_id: PydanticObjectId, dados: PublicacaoUpdate,
                    usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.atualizar(publicacao_id, dados, usuario)


@router.delete("/excluir/{publicacao_id}", status_code=204)
async def excluir(publicacao_id: PydanticObjectId, versao: int = Query(ge=1),
                  usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    await service.excluir(publicacao_id, versao, usuario)
