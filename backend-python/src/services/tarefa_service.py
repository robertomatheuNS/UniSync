
from uuid import UUID

from beanie import PydanticObjectId

from src.models.tarefa import Tarefa
from src.schemas.tarefa import TarefaCreate, TarefaUpdate
from src.repositories import tarefa_repository


async def criar_tarefa(
    dados: TarefaCreate,
    aluno_id: UUID,
) -> Tarefa:
    tarefa = Tarefa(
        aluno_id=aluno_id,
        titulo=dados.titulo,
    )

    return await tarefa_repository.criar_tarefa(tarefa)


async def listar_tarefas(aluno_id: UUID) -> list[Tarefa]:
    return await tarefa_repository.listar_tarefas(aluno_id)


async def buscar_tarefa_por_id(
    tarefa_id: PydanticObjectId,
    aluno_id: UUID,
) -> Tarefa | None:
    tarefa = await tarefa_repository.buscar_tarefa_por_id(tarefa_id)

    if tarefa is None:
        return None

    if tarefa.aluno_id != aluno_id:
        raise PermissionError("Você não tem permissão para acessar esta tarefa")

    return tarefa


async def atualizar_tarefa(
    tarefa_id: PydanticObjectId,
    dados: TarefaUpdate,
    aluno_id: UUID,
) -> Tarefa | None:
    novos_dados = dados.model_dump(exclude_unset=True)

    if not novos_dados:
        raise ValueError("Nenhum campo foi informado")

    tarefa = await buscar_tarefa_por_id(tarefa_id, aluno_id)

    if tarefa is None:
        return None

    return await tarefa_repository.atualizar_tarefa(
        tarefa,
        novos_dados,
    )


async def excluir_tarefa(
    tarefa_id: PydanticObjectId,
    aluno_id: UUID,
) -> bool:
    tarefa = await buscar_tarefa_por_id(tarefa_id, aluno_id)

    if tarefa is None:
        return False

    return await tarefa_repository.excluir_tarefa(tarefa)
