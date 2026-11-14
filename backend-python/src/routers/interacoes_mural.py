from beanie import PydanticObjectId
from fastapi import APIRouter, Depends, Query
from src.core.security import UsuarioAutenticado, obter_usuario_atual
from src.schemas.interacoes_mural import ComentarioCreate, ComentarioUpdate, ComentarioResponse, PaginaComentarios, CurtidaDados, CurtidaResponse
from src.services import interacoes_mural_service as service

router = APIRouter(prefix="/mural/{publicacao_id}", tags=["Comentários e curtidas do mural"])


@router.get("/curtida", response_model=CurtidaResponse)
async def consultar_curtida(publicacao_id: PydanticObjectId, usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.consultar_curtida(publicacao_id, usuario)


@router.put("/curtida", response_model=CurtidaResponse)
async def curtir(publicacao_id: PydanticObjectId, dados: CurtidaDados, usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.curtir(publicacao_id, dados, usuario)


@router.get("/comentarios", response_model=PaginaComentarios)
async def listar(publicacao_id: PydanticObjectId, antes: PydanticObjectId | None = None,
                 limite: int = Query(default=20, ge=1, le=50), usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.listar(publicacao_id, usuario, antes, limite)


@router.post("/comentarios", response_model=ComentarioResponse, status_code=201)
async def criar(publicacao_id: PydanticObjectId, dados: ComentarioCreate, usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.criar(publicacao_id, dados, usuario)


@router.patch("/comentarios/{comentario_id}", response_model=ComentarioResponse)
async def atualizar(publicacao_id: PydanticObjectId, comentario_id: PydanticObjectId, dados: ComentarioUpdate,
                    usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await service.atualizar(publicacao_id, comentario_id, dados, usuario)


@router.delete("/comentarios/{comentario_id}", status_code=204)
async def excluir(publicacao_id: PydanticObjectId, comentario_id: PydanticObjectId, versao: int = Query(ge=1),
                  usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    await service.excluir(publicacao_id, comentario_id, versao, usuario)
