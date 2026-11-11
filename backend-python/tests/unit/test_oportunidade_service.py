
import asyncio
from unittest.mock import AsyncMock, Mock

import pytest
from beanie import PydanticObjectId

from src.schemas.oportunidade import OportunidadeUpdate
from src.repositories import oportunidade_repository
from src.services import oportunidade_service


def criar_id():
    return PydanticObjectId("507f1f77bcf86cd799439011")


def test_rejeita_atualizacao_sem_campos():
    oportunidade_id = criar_id()
    dados = OportunidadeUpdate()

    with pytest.raises(ValueError, match="Nenhum campo foi informado"):
        asyncio.run(
            oportunidade_service.atualizar_oportunidade(oportunidade_id, dados, professor_id="professor-123",)
        )


def test_buscar_oportunidade_inexistente(monkeypatch):
    oportunidade_id = criar_id()
    mock_busca = AsyncMock(return_value=None)

    monkeypatch.setattr(
        oportunidade_repository,
        "buscar_oportunidade_por_id",
        mock_busca
    )

    resultado = asyncio.run(
        oportunidade_service.buscar_oportunidade_por_id(oportunidade_id)
    )

    assert resultado is None
    mock_busca.assert_awaited_once_with(oportunidade_id)



def test_atualizar_oportunidade_existente(monkeypatch):
    oportunidade_id = criar_id()
    professor_id = "professor-123"

    dados = OportunidadeUpdate(titulo="Novo titulo da oportunidade")

    oportunidade_existente = Mock()
    oportunidade_existente.professor_id = professor_id

    oportunidade_atualizada = Mock()

    mock_busca = AsyncMock(return_value=oportunidade_existente)
    mock_atualizacao = AsyncMock(return_value=oportunidade_atualizada)

    monkeypatch.setattr(
        oportunidade_repository,
        "buscar_oportunidade_por_id",
        mock_busca,
    )

    monkeypatch.setattr(
        oportunidade_repository,
        "atualizar_oportunidade",
        mock_atualizacao,
    )

    resultado = asyncio.run(
        oportunidade_service.atualizar_oportunidade(
            oportunidade_id,
            dados,
            professor_id=professor_id,
        )
    )

    assert resultado is oportunidade_atualizada

    mock_busca.assert_awaited_once_with(oportunidade_id)

    mock_atualizacao.assert_awaited_once_with(
        oportunidade_existente,
        {"titulo": "Novo titulo da oportunidade"},
    )


    assert resultado is oportunidade_atualizada
    mock_busca.assert_awaited_once_with(oportunidade_id)
    mock_atualizacao.assert_awaited_once_with(
        oportunidade_existente,
        {"titulo": "Novo titulo da oportunidade"}
    )


def test_atualizar_oportunidade_inexistente(monkeypatch):
    oportunidade_id = criar_id()
    dados = OportunidadeUpdate(titulo="Novo titulo")

    mock_busca = AsyncMock(return_value=None)
    mock_atualizacao = AsyncMock()

    monkeypatch.setattr(
        oportunidade_repository,
        "buscar_oportunidade_por_id",
        mock_busca
    )

    monkeypatch.setattr(
        oportunidade_repository,
        "atualizar_oportunidade",
        mock_atualizacao
    )

    resultado = asyncio.run(
        oportunidade_service.atualizar_oportunidade(
            oportunidade_id,
            dados,
            professor_id="professor-123",
        )
    )

    assert resultado is None
    mock_atualizacao.assert_not_awaited()



def test_excluir_oportunidade_existente(monkeypatch):
    oportunidade_id = criar_id()
    professor_id = "professor-123"

    oportunidade_existente = Mock()
    oportunidade_existente.professor_id = professor_id

    mock_busca = AsyncMock(return_value=oportunidade_existente)
    mock_exclusao = AsyncMock(return_value=True)

    monkeypatch.setattr(
        oportunidade_repository,
        "buscar_oportunidade_por_id",
        mock_busca,
    )

    monkeypatch.setattr(
        oportunidade_repository,
        "excluir_oportunidade",
        mock_exclusao,
    )

    resultado = asyncio.run(
        oportunidade_service.excluir_oportunidade(
            oportunidade_id,
            professor_id=professor_id,
        )
    )

    assert resultado is True

    mock_busca.assert_awaited_once_with(oportunidade_id)
    mock_exclusao.assert_awaited_once_with(oportunidade_existente)



def test_excluir_oportunidade_inexistente(monkeypatch):
    oportunidade_id = criar_id()

    mock_busca = AsyncMock(return_value=None)
    mock_exclusao = AsyncMock()

    monkeypatch.setattr(
        oportunidade_repository,
        "buscar_oportunidade_por_id",
        mock_busca
    )

    monkeypatch.setattr(
        oportunidade_repository,
        "excluir_oportunidade",
        mock_exclusao
    )

    resultado = asyncio.run(
        oportunidade_service.excluir_oportunidade(
            oportunidade_id,
            professor_id="professor-123",
        )
    )

    assert resultado is False
    mock_exclusao.assert_not_awaited()


def test_listar_oportunidades_com_filtro(monkeypatch):
    oportunidades = [Mock(), Mock()]

    mock_listagem = AsyncMock(return_value=oportunidades)

    monkeypatch.setattr(
        oportunidade_repository,
        "listar_oportunidades",
        mock_listagem
    )

    resultado = asyncio.run(
        oportunidade_service.listar_oportunidades(tipo="MONITORIA")
    )

    assert resultado is oportunidades
    mock_listagem.assert_awaited_once_with(
        tipo="MONITORIA",
        professor=None,
        futuros=False,
        ordenar=False,
        limite=None,
    )


def test_listar_todas_as_oportunidades(monkeypatch):
    oportunidades = [Mock(), Mock(), Mock()]

    mock_listagem = AsyncMock(return_value=oportunidades)

    monkeypatch.setattr(
        oportunidade_repository,
        "listar_oportunidades",
        mock_listagem
    )

    resultado = asyncio.run(
        oportunidade_service.listar_oportunidades()
    )

    assert resultado is oportunidades
    mock_listagem.assert_awaited_once_with(
        tipo=None,
        professor=None,
        futuros=False,
        ordenar=False,
        limite=None,
    )


def test_impedir_atualizacao_por_outro_professor(monkeypatch):
    oportunidade_id = criar_id()

    oportunidade = Mock()
    oportunidade.professor_id = "professor-123"

    mock_busca = AsyncMock(return_value=oportunidade)
    mock_atualizacao = AsyncMock()

    monkeypatch.setattr(
        oportunidade_repository,
        "buscar_oportunidade_por_id",
        mock_busca,
    )

    monkeypatch.setattr(
        oportunidade_repository,
        "atualizar_oportunidade",
        mock_atualizacao,
    )

    with pytest.raises(PermissionError):
        asyncio.run(
            oportunidade_service.atualizar_oportunidade(
                oportunidade_id,
                OportunidadeUpdate(titulo="Novo titulo"),
                professor_id="professor-456",
            )
        )

    mock_atualizacao.assert_not_awaited()


def test_impedir_exclusao_por_outro_professor(monkeypatch):
    oportunidade_id = criar_id()

    oportunidade = Mock()
    oportunidade.professor_id = "professor-123"

    mock_busca = AsyncMock(return_value=oportunidade)
    mock_exclusao = AsyncMock()

    monkeypatch.setattr(
        oportunidade_repository,
        "buscar_oportunidade_por_id",
        mock_busca,
    )

    monkeypatch.setattr(
        oportunidade_repository,
        "excluir_oportunidade",
        mock_exclusao,
    )

    with pytest.raises(PermissionError):
        asyncio.run(
            oportunidade_service.excluir_oportunidade(
                oportunidade_id,
                professor_id="professor-456",
            )
        )

    mock_exclusao.assert_not_awaited()


def test_impedir_atualizacao_sem_proprietario(monkeypatch):
    oportunidade_id = criar_id()

    oportunidade = Mock()
    oportunidade.professor_id = None

    mock_busca = AsyncMock(return_value=oportunidade)
    mock_atualizacao = AsyncMock()

    monkeypatch.setattr(
        oportunidade_repository,
        "buscar_oportunidade_por_id",
        mock_busca,
    )

    monkeypatch.setattr(
        oportunidade_repository,
        "atualizar_oportunidade",
        mock_atualizacao,
    )

    with pytest.raises(PermissionError):
        asyncio.run(
            oportunidade_service.atualizar_oportunidade(
                oportunidade_id,
                OportunidadeUpdate(titulo="Novo titulo"),
                professor_id="professor-123",
            )
        )

    mock_atualizacao.assert_not_awaited()
