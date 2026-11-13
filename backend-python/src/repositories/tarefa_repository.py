
from beanie import PydanticObjectId
from uuid import UUID

from src.models.tarefa import Tarefa


async def criar_tarefa(tarefa: Tarefa) -> Tarefa:
    await tarefa.insert()
    return tarefa


async def listar_tarefas(aluno_id: UUID) -> list[Tarefa]:
    return await Tarefa.find(
        Tarefa.aluno_id == aluno_id
    ).to_list()


async def buscar_tarefa_por_id(
    tarefa_id: PydanticObjectId,
) -> Tarefa | None:
    return await Tarefa.get(tarefa_id)


async def atualizar_tarefa(
    tarefa: Tarefa,
    novos_dados: dict,
) -> Tarefa:
    # PATCH altera somente os campos enviados, inclusive em clientes legados.
    await tarefa.set(novos_dados)
    return tarefa


async def excluir_tarefa(tarefa: Tarefa) -> bool:
    await tarefa.delete()
    return True
