
from unittest.mock import AsyncMock, patch
from uuid import uuid4

import pytest
from beanie import PydanticObjectId

from src.models.tarefa import Tarefa, StatusTarefa
from src.schemas.tarefa import TarefaCreate, TarefaUpdate
from src.services import tarefa_service

@pytest.fixture(autouse=True)
def simular_colecao_beanie():
    with patch.object(
        Tarefa,
        "get_pymongo_collection",
        return_value=None,
    ):
        yield
        
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


@pytest.mark.asyncio
async def test_criar_com_turma_usa_disciplina_do_servidor():
    from types import SimpleNamespace
    aluno_id = uuid4()
    turma_id = PydanticObjectId()
    turma = SimpleNamespace(alunos=[str(aluno_id)], disciplina="Matemática")
    with (
        patch.object(tarefa_service.academico_repository, "buscar_turma", AsyncMock(return_value=turma)),
        patch.object(tarefa_service.tarefa_repository, "criar_tarefa", AsyncMock(side_effect=lambda tarefa: tarefa)),
    ):
        tarefa = await tarefa_service.criar_tarefa(TarefaCreate(titulo="Prova", prazo="2027-01-10", prioridade="ALTA", turma_id=turma_id), aluno_id)
    assert tarefa.disciplina == "Matemática"
    assert tarefa.turma_id == turma_id
    assert tarefa.prazo.isoformat() == "2027-01-10"
    assert tarefa.prioridade == "ALTA"


@pytest.mark.asyncio
@pytest.mark.parametrize("turma", [None, type("TurmaFora", (), {"alunos": [], "disciplina": "Privada"})()])
async def test_rejeita_turma_arquivada_ou_sem_matricula(turma):
    with patch.object(tarefa_service.academico_repository, "buscar_turma", AsyncMock(return_value=turma)):
        with pytest.raises(PermissionError):
            await tarefa_service.criar_tarefa(TarefaCreate(titulo="Privada", turma_id=PydanticObjectId()), uuid4())


@pytest.mark.asyncio
async def test_patch_legado_preserva_metadados_e_limpa_vinculo_explicito():
    aluno_id, turma_id = uuid4(), PydanticObjectId()
    tarefa = Tarefa(aluno_id=aluno_id, titulo="Revisão", prazo="2027-01-10", prioridade="ALTA", turma_id=turma_id, disciplina="Kotlin")
    with (
        patch.object(tarefa_service.tarefa_repository, "buscar_tarefa_por_id", AsyncMock(return_value=tarefa)),
        patch.object(tarefa_service.tarefa_repository, "atualizar_tarefa", AsyncMock(return_value=tarefa)) as atualizar,
        patch.object(tarefa_service.academico_repository, "buscar_turma", AsyncMock()) as buscar_turma,
    ):
        await tarefa_service.atualizar_tarefa(PydanticObjectId(), TarefaUpdate(status="CONCLUIDO"), aluno_id)
        atualizar.assert_awaited_with(tarefa, {"status": StatusTarefa.CONCLUIDO})
        await tarefa_service.atualizar_tarefa(PydanticObjectId(), TarefaUpdate(turma_id=turma_id), aluno_id)
        buscar_turma.assert_not_awaited()  # Preserva o vínculo inclusive após a turma ser arquivada.
        await tarefa_service.atualizar_tarefa(PydanticObjectId(), TarefaUpdate(turma_id=None, prazo=None), aluno_id)
        atualizar.assert_awaited_with(tarefa, {"turma_id": None, "prazo": None, "disciplina": None})
