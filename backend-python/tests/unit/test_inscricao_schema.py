from datetime import datetime, timezone
from types import SimpleNamespace
from unittest.mock import AsyncMock
import pytest
from pydantic import ValidationError
from beanie import PydanticObjectId
from src.schemas.oportunidade import OportunidadeCreate, OportunidadeUpdate, OportunidadeResponse
from src.services import oportunidade_service as service
from src.repositories import oportunidade_repository as repo


def base():
    return dict(titulo="Palestra Kotlin", professor="Ana", materia="Kotlin", tipo="Palestra", data_evento="2028-12-10T19:00:00Z")


def test_contrato_antigo_continua_aceito_e_patch_omite_campos_novos():
    item = OportunidadeCreate(**base())
    assert item.url_inscricao is None and item.prazo_inscricao is None
    assert OportunidadeUpdate(titulo="Título atualizado").model_dump(exclude_unset=True) == {"titulo": "Título atualizado"}


def test_prazo_converte_para_utc_e_nulos_explicitos_removem_campos():
    item = OportunidadeCreate(**base(), url_inscricao=" https://EXAMPLE.org/inscricao ", prazo_inscricao="2028-12-10T15:00:00-03:00")
    assert item.url_inscricao == "https://example.org/inscricao"
    assert item.prazo_inscricao == datetime(2028, 12, 10, 18, tzinfo=timezone.utc)
    assert OportunidadeUpdate(url_inscricao=None, prazo_inscricao=None).model_dump(exclude_unset=True) == {"url_inscricao": None, "prazo_inscricao": None}


@pytest.mark.parametrize("url", ["javascript:alert(1)", "http://example.org", "file:///etc/passwd", "https://user:pass@example.org", "https://", "https://example.org/a b", "https://example.org/\nfoo", "https://example.org/" + "a" * 2048])
def test_rejeita_link_invalido_ou_nao_https(url):
    with pytest.raises(ValidationError):
        OportunidadeCreate(**base(), url_inscricao=url)


@pytest.mark.parametrize("prazo", ["2028-12-10T15:00:00", "2028-12-11T19:00:00Z"])
def test_rejeita_prazo_sem_fuso_ou_posterior_ao_evento(prazo):
    with pytest.raises(ValidationError):
        OportunidadeCreate(**base(), prazo_inscricao=prazo)


def test_resposta_de_mongo_explica_utc_no_prazo():
    resposta = OportunidadeResponse(id=PydanticObjectId(), **base(), prazo_inscricao=datetime(2028, 12, 9, 12))
    assert resposta.model_dump(mode="json")["prazo_inscricao"].endswith("Z")


@pytest.mark.asyncio
async def test_patch_legado_preserva_inscricao_e_valida_data_com_documento_persistido(monkeypatch):
    item = SimpleNamespace(professor_id="prof", data_evento=datetime(2028, 12, 10, 19), prazo_inscricao=datetime(2028, 12, 9, 12))
    monkeypatch.setattr(repo, "buscar_oportunidade_por_id", AsyncMock(return_value=item))
    salvar = AsyncMock(return_value=item)
    monkeypatch.setattr(repo, "atualizar_oportunidade", salvar)
    await service.atualizar_oportunidade(PydanticObjectId(), OportunidadeUpdate(titulo="Título atualizado"), "prof")
    assert salvar.await_args.args[1] == {"titulo": "Título atualizado"}
    with pytest.raises(ValueError, match="posterior"):
        await service.atualizar_oportunidade(PydanticObjectId(), OportunidadeUpdate(data_evento="2028-12-08T19:00:00Z"), "prof")
    assert salvar.await_count == 1


@pytest.mark.asyncio
async def test_autoria_e_conferida_antes_de_validar_prazo_persistido(monkeypatch):
    monkeypatch.setattr(repo, "buscar_oportunidade_por_id", AsyncMock(return_value=SimpleNamespace(professor_id="outro")))
    with pytest.raises(PermissionError):
        await service.atualizar_oportunidade(PydanticObjectId(), OportunidadeUpdate(prazo_inscricao=None), "prof")
