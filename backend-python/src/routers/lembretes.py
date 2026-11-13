from beanie import PydanticObjectId
from fastapi import APIRouter, Depends, HTTPException
from src.core.security import UsuarioAutenticado, obter_usuario_atual
from src.repositories import lembrete_repository as repositorio
from src.schemas.lembrete import LembreteCreate, LembreteUpdate, LembreteResponse

router = APIRouter(prefix="/lembretes", tags=["Lembretes"])


@router.get("/listar", response_model=list[LembreteResponse])
async def listar(usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await repositorio.listar(usuario.id)


@router.post("/criar", status_code=201, response_model=LembreteResponse)
async def criar(dados: LembreteCreate, usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    return await repositorio.criar(dados.model_dump(), usuario.id)


@router.patch("/atualizar/{lembrete_id}", response_model=LembreteResponse)
async def atualizar(lembrete_id: PydanticObjectId, dados: LembreteUpdate,
                    usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    campos = dados.model_dump(exclude_unset=True)
    if not campos:
        raise HTTPException(400, "Informe ao menos um campo")
    resultado = await repositorio.atualizar(lembrete_id, usuario.id, campos)
    if resultado is None:
        raise HTTPException(404, "Lembrete não encontrado")
    return {"id": resultado["_id"], **resultado}


@router.delete("/excluir/{lembrete_id}", status_code=204)
async def excluir(lembrete_id: PydanticObjectId,
                  usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    if not await repositorio.excluir(lembrete_id, usuario.id):
        raise HTTPException(404, "Lembrete não encontrado")
