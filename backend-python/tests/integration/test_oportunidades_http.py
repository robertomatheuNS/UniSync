
from datetime import datetime
from unittest.mock import AsyncMock
from fastapi import FastAPI
from fastapi.testclient import TestClient
from src.routers import oportunidades
from src.services import oportunidade_service


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


def test_criar_oportunidade_retorna_201(monkeypatch):
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


def test_criar_oportunidade_invalida_retorna_422():
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


def test_atualizacao_sem_campos_retorna_400(monkeypatch):
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


def test_excluir_oportunidade_retorna_204(monkeypatch):
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
    mock_exclusao.assert_awaited_once()


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
