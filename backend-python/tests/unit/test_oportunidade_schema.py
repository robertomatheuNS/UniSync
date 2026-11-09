
from datetime import datetime
import pytest
from pydantic import ValidationError
from src.schemas.oportunidade import OportunidadeCreate, OportunidadeUpdate


def dados_validos():
    return {
        "titulo": "Palestra de Python",
        "professor": "Ana Ribeiro",
        "materia": "Programação",
        "tipo": "PALESTRA",
        "data_evento": datetime(2026, 12, 10, 19, 0),
    }


def test_rejeita_titulo_curto():
    dados = dados_validos()
    dados["titulo"] = "abcd"

    with pytest.raises(ValidationError):
        OportunidadeCreate(**dados)


def test_aceita_titulo_com_cinco_caracteres():
    dados = dados_validos()
    dados["titulo"] = "abcde"

    oportunidade = OportunidadeCreate(**dados)

    assert oportunidade.titulo == "abcde"


def test_rejeita_titulo_com_mais_de_cem_caracteres():
    dados = dados_validos()
    dados["titulo"] = "a" * 101

    with pytest.raises(ValidationError):
        OportunidadeCreate(**dados)


def test_rejeita_campo_extra():
    dados = dados_validos()
    dados["campo_inexistente"] = "valor"

    with pytest.raises(ValidationError):
        OportunidadeCreate(**dados)


def test_rejeita_campo_obrigatorio_ausente():
    dados = dados_validos()
    del dados["professor"]

    with pytest.raises(ValidationError):
        OportunidadeCreate(**dados)


def test_diferencia_descricao_omitida_de_descricao_nula():
    sem_descricao = OportunidadeUpdate(
        titulo="Palestra de Python"
    )

    com_descricao_nula = OportunidadeUpdate(
        titulo="Palestra de Python",
        descricao=None,
    )

    assert sem_descricao.model_dump(exclude_unset=True) == {
        "titulo": "Palestra de Python"
    }

    assert com_descricao_nula.model_dump(exclude_unset=True) == {
        "titulo": "Palestra de Python",
        "descricao": None,
    }


def test_rejeita_titulo_nulo_na_atualizacao():
    with pytest.raises(ValidationError):
        OportunidadeUpdate(titulo=None)
