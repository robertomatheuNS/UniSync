
from unittest.mock import AsyncMock, patch
from uuid import uuid4

import pytest
from beanie import PydanticObjectId

from src.models.tarefa import Tarefa, StatusTarefa
from src.schemas.tarefa import TarefaCreate, TarefaUpdate
from src.services import tarefa_service


@pytest.mark.asyncio
async def test_criar_tarefa():
    aluno_id = uuid4()
    dados = TarefaCreate(titulo="Estudar Python")

    with patch.object(
        tarefa_service.tarefa_repository,
        "criar_tarefa",
        new_callable=AsyncMock,
    ) as mock_criar:
        mock_criar.side_effect = lambda tarefa: tarefa

        resultado = await tarefa_service.criar_tarefa(
            dados,
            aluno_id,
        )

    assert resultado.aluno_id == aluno_id
    assert resultado.titulo == "Estudar Python"
    assert resultado.status == StatusTarefa.A_FAZER
    mock_criar.assert_awaited_once()


@pytest.mark.asyncio
async def test_listar_tarefas():
    aluno_id = uuid4()

    with patch.object(
        tarefa_service.tarefa_repository,
        "listar_tarefas",
        new_callable=AsyncMock,
    ) as mock_listar:
        mock_listar.return_value = []

        resultado = await tarefa_service.listar_tarefas(aluno_id)

    assert resultado == []
    mock_listar.assert_awaited_once_with(aluno_id)


@pytest.mark.asyncio
async def test_buscar_tarefa_de_outro_aluno():
    aluno_dono = uuid4()
    outro_aluno = uuid4()
    tarefa_id = PydanticObjectId()

    tarefa = Tarefa(
        aluno_id=aluno_dono,
        titulo="Tarefa privada",
    )

    with patch.object(
        tarefa_service.tarefa_repository,
        "buscar_tarefa_por_id",
        new_callable=AsyncMock,
    ) as mock_buscar:
        mock_buscar.return_value = tarefa

        with pytest.raises(PermissionError):
            await tarefa_service.buscar_tarefa_por_id(
                tarefa_id,
                outro_aluno,
            )


@pytest.mark.asyncio
async def test_atualizar_status():
    aluno_id = uuid4()
    tarefa_id = PydanticObjectId()

    tarefa = Tarefa(
        aluno_id=aluno_id,
        titulo="Estudar Python",
    )

    dados = TarefaUpdate(status="EM_ANDAMENTO")

    with (
        patch.object(
            tarefa_service.tarefa_repository,
            "buscar_tarefa_por_id",
            new_callable=AsyncMock,
            return_value=tarefa,
        ),
        patch.object(
            tarefa_service.tarefa_repository,
            "atualizar_tarefa",
            new_callable=AsyncMock,
        ) as mock_atualizar,
    ):
        mock_atualizar.return_value = tarefa

        await tarefa_service.atualizar_tarefa(
            tarefa_id,
            dados,
            aluno_id,
        )

    mock_atualizar.assert_awaited_once_with(
        tarefa,
        {"status": StatusTarefa.EM_ANDAMENTO},
    )


@pytest.mark.asyncio
async def test_atualizar_sem_campos():
    with pytest.raises(
        ValueError,
        match="Nenhum campo foi informado",
    ):
        await tarefa_service.atualizar_tarefa(
            PydanticObjectId(),
            TarefaUpdate(),
            uuid4(),
        )


@pytest.mark.asyncio
async def test_excluir_tarefa_de_outro_aluno():
    aluno_dono = uuid4()
    outro_aluno = uuid4()

    tarefa = Tarefa(
        aluno_id=aluno_dono,
        titulo="Tarefa privada",
    )

    with patch.object(
        tarefa_service.tarefa_repository,
        "buscar_tarefa_por_id",
        new_callable=AsyncMock,
        return_value=tarefa,
    ):
        with pytest.raises(PermissionError):
            await tarefa_service.excluir_tarefa(
                PydanticObjectId(),
                outro_aluno,
            )
