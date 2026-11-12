
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
