
from unittest.mock import AsyncMock, patch
from uuid import uuid4

import pytest
from beanie import PydanticObjectId
from fastapi.testclient import TestClient

from src.main import app
from src.core.security import (
    UsuarioAutenticado,
    obter_usuario_atual,
)
from src.services import tarefa_service


@pytest.fixture
def cliente_aluno():
    aluno = UsuarioAutenticado(
        id=uuid4(),
        nome="Aluno Teste",
        papel="Aluno",
    )

    app.dependency_overrides[obter_usuario_atual] = (
        lambda: aluno
    )

    try:
        yield TestClient(app), aluno
    finally:
        app.dependency_overrides.clear()


def tarefa_falsa(aluno_id, titulo="Estudar Python"):
    return {
        "id": str(PydanticObjectId()),
        "aluno_id": str(aluno_id),
        "titulo": titulo,
        "status": "A_FAZER",
    }


def test_aluno_cria_tarefa(cliente_aluno):
    client, aluno = cliente_aluno
    tarefa = tarefa_falsa(aluno.id)

    with patch.object(
        tarefa_service,
        "criar_tarefa",
        new_callable=AsyncMock,
        return_value=tarefa,
    ) as mock_criar:
        resposta = client.post(
            "/tarefas/criar",
            json={"titulo": "Estudar Python"},
        )

    assert resposta.status_code == 201
    assert resposta.json()["titulo"] == "Estudar Python"
    assert resposta.json()["aluno_id"] == str(aluno.id)
    mock_criar.assert_awaited_once()


def test_aluno_lista_tarefas(cliente_aluno):
    client, aluno = cliente_aluno
    tarefa = tarefa_falsa(aluno.id)

    with patch.object(
        tarefa_service,
        "listar_tarefas",
        new_callable=AsyncMock,
        return_value=[tarefa],
    ) as mock_listar:
        resposta = client.get("/tarefas/listar")

    assert resposta.status_code == 200
    assert len(resposta.json()) == 1
    assert resposta.json()[0]["titulo"] == "Estudar Python"
    mock_listar.assert_awaited_once_with(aluno.id)


def test_professor_nao_pode_criar_tarefa():
    professor = UsuarioAutenticado(
        id=uuid4(),
        nome="Professor Teste",
        papel="Professor",
    )

    app.dependency_overrides[obter_usuario_atual] = (
        lambda: professor
    )

    try:
        client = TestClient(app)

        resposta = client.post(
            "/tarefas/criar",
            json={"titulo": "Tarefa de teste"},
        )

        assert resposta.status_code == 403
    finally:
        app.dependency_overrides.clear()


def test_aluno_nao_acessa_tarefa_de_outro(cliente_aluno):
    client, _ = cliente_aluno
    tarefa_id = str(PydanticObjectId())

    with patch.object(
        tarefa_service,
        "buscar_tarefa_por_id",
        new_callable=AsyncMock,
        side_effect=PermissionError(
            "Você não tem permissão para acessar esta tarefa"
        ),
    ):
        resposta = client.get(
            f"/tarefas/buscar/{tarefa_id}"
        )

    assert resposta.status_code == 403


def test_buscar_tarefa_inexistente(cliente_aluno):
    client, _ = cliente_aluno
    tarefa_id = str(PydanticObjectId())

    with patch.object(
        tarefa_service,
        "buscar_tarefa_por_id",
        new_callable=AsyncMock,
        return_value=None,
    ):
        resposta = client.get(
            f"/tarefas/buscar/{tarefa_id}"
        )

    assert resposta.status_code == 404


def test_aluno_atualiza_status_tarefa(cliente_aluno):
    client, aluno = cliente_aluno
    tarefa_id = PydanticObjectId()

    tarefa = tarefa_falsa(aluno.id)
    tarefa["id"] = str(tarefa_id)
    tarefa["status"] = "EM_ANDAMENTO"

    with patch.object(
        tarefa_service,
        "atualizar_tarefa",
        new_callable=AsyncMock,
        return_value=tarefa,
    ) as mock_atualizar:
        resposta = client.patch(
            f"/tarefas/atualizar/{tarefa_id}",
            json={"status": "EM_ANDAMENTO"},
        )

    assert resposta.status_code == 200
    assert resposta.json()["status"] == "EM_ANDAMENTO"

    mock_atualizar.assert_awaited_once()
    args = mock_atualizar.await_args.args

    assert args[0] == tarefa_id
    assert args[1].status.value == "EM_ANDAMENTO"
    assert args[2] == aluno.id


def test_atualizar_tarefa_sem_campos(cliente_aluno):
    client, _ = cliente_aluno
    tarefa_id = PydanticObjectId()

    with patch.object(
        tarefa_service,
        "atualizar_tarefa",
        new_callable=AsyncMock,
        side_effect=ValueError("Nenhum campo foi informado"),
    ):
        resposta = client.patch(
            f"/tarefas/atualizar/{tarefa_id}",
            json={},
        )

    assert resposta.status_code == 400
    assert resposta.json()["detail"] == "Nenhum campo foi informado"



def test_aluno_exclui_tarefa(cliente_aluno):
    client, aluno = cliente_aluno
    tarefa_id = PydanticObjectId()

    with patch.object(
        tarefa_service,
        "excluir_tarefa",
        new_callable=AsyncMock,
        return_value=True,
    ) as mock_excluir:
        resposta = client.delete(
            f"/tarefas/excluir/{tarefa_id}"
        )

    assert resposta.status_code == 204
    mock_excluir.assert_awaited_once_with(
        tarefa_id,
        aluno.id,
    )


def test_excluir_tarefa_inexistente(cliente_aluno):
    client, _ = cliente_aluno
    tarefa_id = PydanticObjectId()

    with patch.object(
        tarefa_service,
        "excluir_tarefa",
        new_callable=AsyncMock,
        return_value=False,
    ):
        resposta = client.delete(
            f"/tarefas/excluir/{tarefa_id}"
        )

    assert resposta.status_code == 404


def test_aluno_nao_exclui_tarefa_de_outro(cliente_aluno):
    client, aluno = cliente_aluno
    tarefa_id = PydanticObjectId()

    with patch.object(
        tarefa_service,
        "excluir_tarefa",
        new_callable=AsyncMock,
        side_effect=PermissionError(
            "Você não tem permissão para acessar esta tarefa"
        ),
    ) as mock_excluir:
        resposta = client.delete(
            f"/tarefas/excluir/{tarefa_id}"
        )

    assert resposta.status_code == 403
    mock_excluir.assert_awaited_once_with(
        tarefa_id,
        aluno.id,
    )


def test_contrato_legado_responde_com_defaults(cliente_aluno):
    client, aluno = cliente_aluno
    with patch.object(tarefa_service, "criar_tarefa", AsyncMock(return_value=tarefa_falsa(aluno.id))):
        resposta = client.post("/tarefas/criar", json={"titulo": "Legada"})
    assert resposta.status_code == 201
    assert resposta.json()["prioridade"] == "MEDIA"
    assert resposta.json()["prazo"] is None
    assert resposta.json()["turma_id"] is None


def test_criar_tarefa_sem_matricula_retorna_403(cliente_aluno):
    client, _ = cliente_aluno
    with patch.object(tarefa_service, "criar_tarefa", AsyncMock(side_effect=PermissionError("Turma não permitida"))):
        resposta = client.post("/tarefas/criar", json={"titulo": "Privada", "turma_id": str(PydanticObjectId())})
    assert resposta.status_code == 403


def test_resposta_preserva_prazo_sem_fuso_e_disciplina(cliente_aluno):
    from datetime import date
    client, aluno = cliente_aluno
    tarefa = {**tarefa_falsa(aluno.id), "prazo": date(2028, 2, 29), "prioridade": "ALTA", "turma_id": PydanticObjectId(), "disciplina": "Kotlin"}
    with patch.object(tarefa_service, "listar_tarefas", AsyncMock(return_value=[tarefa])):
        resposta = client.get("/tarefas/listar")
    assert resposta.status_code == 200
    assert resposta.json()[0]["prazo"] == "2028-02-29"
    assert resposta.json()[0]["disciplina"] == "Kotlin"
