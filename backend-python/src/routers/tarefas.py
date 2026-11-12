
from beanie import PydanticObjectId
from fastapi import APIRouter, Depends, HTTPException, status

from src.core.security import UsuarioAutenticado, obter_usuario_atual
from src.schemas.tarefa import TarefaCreate, TarefaUpdate, TarefaResponse
from src.services import tarefa_service


router = APIRouter(
    prefix="/tarefas",
    tags=["Tarefas"],
)


def exigir_aluno(
    usuario: UsuarioAutenticado = Depends(obter_usuario_atual),
) -> UsuarioAutenticado:
    if usuario.papel != "Aluno":
        raise HTTPException(
            status_code=403,
            detail="Acesso permitido apenas para alunos",
        )

    return usuario


@router.post(
    "/criar",
    response_model=TarefaResponse,
    status_code=status.HTTP_201_CREATED,
)
async def criar_tarefa(
    dados: TarefaCreate,
    usuario: UsuarioAutenticado = Depends(exigir_aluno),
):
    return await tarefa_service.criar_tarefa(
        dados,
        usuario.id,
    )


@router.get(
    "/listar",
    response_model=list[TarefaResponse],
)
async def listar_tarefas(
    usuario: UsuarioAutenticado = Depends(exigir_aluno),
):
    return await tarefa_service.listar_tarefas(usuario.id)


@router.get(
    "/buscar/{tarefa_id}",
    response_model=TarefaResponse,
)
async def buscar_tarefa(
    tarefa_id: PydanticObjectId,
    usuario: UsuarioAutenticado = Depends(exigir_aluno),
):
    try:
        tarefa = await tarefa_service.buscar_tarefa_por_id(
            tarefa_id,
            usuario.id,
        )

        if tarefa is None:
            raise HTTPException(
                status_code=404,
                detail="Tarefa não encontrada",
            )

        return tarefa

    except PermissionError as erro:
        raise HTTPException(
            status_code=403,
            detail=str(erro),
        ) from erro


@router.patch(
    "/atualizar/{tarefa_id}",
    response_model=TarefaResponse,
)
async def atualizar_tarefa(
    tarefa_id: PydanticObjectId,
    dados: TarefaUpdate,
    usuario: UsuarioAutenticado = Depends(exigir_aluno),
):
    try:
        tarefa = await tarefa_service.atualizar_tarefa(
            tarefa_id,
            dados,
            usuario.id,
        )

        if tarefa is None:
            raise HTTPException(
                status_code=404,
                detail="Tarefa não encontrada",
            )

        return tarefa

    except PermissionError as erro:
        raise HTTPException(
            status_code=403,
            detail=str(erro),
        ) from erro

    except ValueError as erro:
        raise HTTPException(
            status_code=400,
            detail=str(erro),
        ) from erro


@router.delete(
    "/excluir/{tarefa_id}",
    status_code=status.HTTP_204_NO_CONTENT,
)
async def excluir_tarefa(
    tarefa_id: PydanticObjectId,
    usuario: UsuarioAutenticado = Depends(exigir_aluno),
):
    try:
        excluida = await tarefa_service.excluir_tarefa(
            tarefa_id,
            usuario.id,
        )

        if not excluida:
            raise HTTPException(
                status_code=404,
                detail="Tarefa não encontrada",
            )

    except PermissionError as erro:
        raise HTTPException(
            status_code=403,
            detail=str(erro),
        ) from erro
