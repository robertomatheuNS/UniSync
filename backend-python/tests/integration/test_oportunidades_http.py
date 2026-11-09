
from datetime import datetime
from unittest.mock import AsyncMock
from fastapi import FastAPI
from fastapi.testclient import TestClient
from src.schemas.oportunidade import OportunidadeUpdate
from src.routers import oportunidades
from src.services import oportunidade_service
from uuid import uuid4
import pytest
from src.core.security import (UsuarioAutenticado, exigir_professor, obter_usuario_atual)
from beanie import PydanticObjectId


ID_VALIDO = "507f1f77bcf86cd799439011"


def dados_oportunidade():
    return {
        "titulo": "Palestra de Python",
        "professor": "Ana Ribeiro",
        "materia": "Programação",
        "descricao": "Introdução à linguagem Python",
        "tipo": "PALESTRA",
        "data_evento": "2026-12-10T19:00:00",
    }


def oportunidade_salva():
    return {
        "id": ID_VALIDO,
        **dados_oportunidade(),
    }


app_teste = FastAPI()
app_teste.include_router(oportunidades.router)

client = TestClient(app_teste)

@pytest.fixture
def professor_autenticado():
    professor = UsuarioAutenticado(
        id=uuid4(),
        nome="Professor Teste",
        papel="Professor",
    )

    app_teste.dependency_overrides[exigir_professor] = (
        lambda: professor
    )

    yield professor

    app_teste.dependency_overrides.pop(
        exigir_professor,
        None,
    )


def test_criar_oportunidade_retorna_201(monkeypatch, professor_autenticado):
    mock_criacao = AsyncMock(return_value=oportunidade_salva())

    monkeypatch.setattr(
        oportunidade_service,
        "criar_oportunidade",
        mock_criacao
    )

    resposta = client.post(
        "/oportunidades/criar",
        json=dados_oportunidade()
    )

    assert resposta.status_code == 201
    assert resposta.json()["titulo"] == "Palestra de Python"

    mock_criacao.assert_awaited_once()

    args, kwargs = mock_criacao.await_args

    assert kwargs["professor_id"] == str(professor_autenticado.id)


def test_criar_oportunidade_invalida_retorna_422(professor_autenticado):
    dados = dados_oportunidade()
    dados["titulo"] = "abc"

    resposta = client.post(
        "/oportunidades/criar",
        json=dados
    )

    assert resposta.status_code == 422


def test_listar_oportunidades_com_filtro(monkeypatch):
    mock_listagem = AsyncMock(
        return_value=[oportunidade_salva()]
    )

    monkeypatch.setattr(
        oportunidade_service,
        "listar_oportunidades",
        mock_listagem
    )

    resposta = client.get(
        "/oportunidades/listar?tipo=PALESTRA"
    )

    assert resposta.status_code == 200
    assert len(resposta.json()) == 1
    assert resposta.json()[0]["tipo"] == "PALESTRA"

    mock_listagem.assert_awaited_once_with(
        tipo="PALESTRA",
        professor=None,
        futuros=False,
        ordenar=False,
        limite=None,
    )


def test_buscar_oportunidade_inexistente_retorna_404(monkeypatch):
    mock_busca = AsyncMock(return_value=None)

    monkeypatch.setattr(
        oportunidade_service,
        "buscar_oportunidade_por_id",
        mock_busca
    )

    resposta = client.get(
        f"/oportunidades/buscar/{ID_VALIDO}"
    )

    assert resposta.status_code == 404
    mock_busca.assert_awaited_once()


def test_atualizacao_sem_campos_retorna_400(monkeypatch, professor_autenticado):
    mock_atualizacao = AsyncMock(
        side_effect=ValueError("Nenhum campo foi informado")
    )

    monkeypatch.setattr(
        oportunidade_service,
        "atualizar_oportunidade",
        mock_atualizacao
    )

    resposta = client.patch(
        f"/oportunidades/atualizar/{ID_VALIDO}",
        json={}
    )

    assert resposta.status_code == 400
    assert resposta.json()["detail"] == "Nenhum campo foi informado"


def test_excluir_oportunidade_retorna_204(monkeypatch, professor_autenticado):
    mock_exclusao = AsyncMock(return_value=True)

    monkeypatch.setattr(
        oportunidade_service,
        "excluir_oportunidade",
        mock_exclusao
    )

    resposta = client.delete(
        f"/oportunidades/excluir/{ID_VALIDO}"
    )

    assert resposta.status_code == 204
    assert resposta.content == b""

    mock_exclusao.assert_awaited_once_with(
        PydanticObjectId(ID_VALIDO),
        professor_id=str(professor_autenticado.id),
    )


def test_listar_avisos_do_dashboard(monkeypatch):
    mock_listagem = AsyncMock(
        return_value=[oportunidade_salva()]
    )

    monkeypatch.setattr(
        oportunidade_service,
        "listar_oportunidades",
        mock_listagem,
    )

    resposta = client.get(
        "/oportunidades/listar",
        params={
            "futuros": "true",
            "ordenar": "true",
            "limite": 8,
        },
    )

    assert resposta.status_code == 200

    mock_listagem.assert_awaited_once_with(
        tipo=None,
        professor=None,
        futuros=True,
        ordenar=True,
        limite=8,
    )


def test_rejeitar_limite_invalido():
    resposta = client.get(
        "/oportunidades/listar",
        params={"limite": -1},
    )

    assert resposta.status_code == 422


def test_criar_sem_token_retorna_401():
    resposta = client.post(
        "/oportunidades/criar",
        json=dados_oportunidade(),
    )

    assert resposta.status_code == 401


def test_aluno_nao_pode_criar_oportunidade():
    aluno = UsuarioAutenticado(
        id=uuid4(),
        nome="Aluno Teste",
        papel="Aluno",
    )

    app_teste.dependency_overrides[obter_usuario_atual] = (
        lambda: aluno
    )

    try:
        resposta = client.post(
            "/oportunidades/criar",
            json=dados_oportunidade(),
        )

        assert resposta.status_code == 403

    finally:
        app_teste.dependency_overrides.pop(
            obter_usuario_atual,
            None,
        )


def test_outro_professor_nao_pode_atualizar(
    monkeypatch,
    professor_autenticado,
):
    mock_atualizacao = AsyncMock(
        side_effect=PermissionError(
            "Você não tem permissão para alterar esta oportunidade"
        )
    )

    monkeypatch.setattr(
        oportunidade_service,
        "atualizar_oportunidade",
        mock_atualizacao,
    )

    resposta = client.patch(
        f"/oportunidades/atualizar/{ID_VALIDO}",
        json={"titulo": "Titulo atualizado"},
    )

    assert resposta.status_code == 403

    mock_atualizacao.assert_awaited_once_with(
        PydanticObjectId(ID_VALIDO),
        OportunidadeUpdate(titulo="Titulo atualizado"),
        professor_id=str(professor_autenticado.id),
    )


def test_outro_professor_nao_pode_excluir(
    monkeypatch,
    professor_autenticado,
):
    mock_exclusao = AsyncMock(
        side_effect=PermissionError(
            "Você não tem permissão para excluir esta oportunidade"
        )
    )

    monkeypatch.setattr(
        oportunidade_service,
        "excluir_oportunidade",
        mock_exclusao,
    )

    resposta = client.delete(
        f"/oportunidades/excluir/{ID_VALIDO}"
    )

    assert resposta.status_code == 403

    mock_exclusao.assert_awaited_once_with(
        PydanticObjectId(ID_VALIDO),
        professor_id=str(professor_autenticado.id),
    )
