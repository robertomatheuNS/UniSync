from unittest.mock import AsyncMock
from uuid import uuid4
import pytest
from beanie import PydanticObjectId
from bson.binary import Binary
from fastapi.testclient import TestClient
from src.main import app
from src.core.security import obter_usuario_atual, UsuarioAutenticado
from src.repositories import lembrete_repository as repo
from src.models.lembrete import Lembrete
from src.schemas.lembrete import LembreteCreate, LembreteUpdate
from pydantic import ValidationError

@pytest.fixture
def usuario():
    pessoa = UsuarioAutenticado(uuid4(), "Pessoa teste", "Aluno")
    app.dependency_overrides[obter_usuario_atual] = lambda: pessoa
    yield pessoa
    app.dependency_overrides.clear()

@pytest.mark.parametrize("papel", ["Aluno", "Professor"])
def test_criar_e_listar_por_usuario(usuario, monkeypatch, papel):
    pessoa = UsuarioAutenticado(usuario.id, usuario.nome, papel)
    app.dependency_overrides[obter_usuario_atual] = lambda: pessoa
    item = {"id":str(PydanticObjectId()),"titulo":"Estudar","concluido":False}
    criar = AsyncMock(return_value=item)
    listar = AsyncMock(return_value=[item])
    monkeypatch.setattr(repo,"criar",criar)
    monkeypatch.setattr(repo,"listar",listar)
    client = TestClient(app)
    assert client.post("/lembretes/criar",json={"titulo":"  Estudar  "}).status_code == 201
    criar.assert_awaited_once_with({"agendado_em": None, "titulo":"Estudar"}, usuario.id)
    assert client.get("/lembretes/listar").json() == [{**item, "agendado_em": None}]
    listar.assert_awaited_once_with(usuario.id)

@pytest.mark.parametrize("metodo,caminho,body", [
    ("get","/lembretes/listar",None),
    ("post","/lembretes/criar",{"titulo":"Estudar"}),
    ("patch","/lembretes/atualizar/507f1f77bcf86cd799439011",{"concluido":True}),
    ("delete","/lembretes/excluir/507f1f77bcf86cd799439011",None),
])
def test_exige_token(metodo,caminho,body):
    kwargs = {"json":body} if body is not None else {}
    assert getattr(TestClient(app),metodo)(caminho,**kwargs).status_code == 401

@pytest.mark.parametrize("dados", [{"titulo":""},{"titulo":"  "},{"titulo":"x"*201},{"titulo":"ok","usuario_id":str(uuid4())}])
def test_criacao_invalida(dados):
    with pytest.raises(ValidationError):
        LembreteCreate(**dados)

@pytest.mark.parametrize("dados", [{"titulo":None},{"concluido":None},{"usuario_id":str(uuid4())}])
def test_patch_invalido(dados):
    with pytest.raises(ValidationError):
        LembreteUpdate(**dados)

def test_patch_vazio(usuario):
    resposta=TestClient(app).patch("/lembretes/atualizar/507f1f77bcf86cd799439011",json={})
    assert resposta.status_code == 400

@pytest.mark.asyncio
async def test_atualizacao_atomica_filtra_autoria(monkeypatch):
    collection=AsyncMock()
    collection.find_one_and_update.return_value=None
    monkeypatch.setattr(Lembrete,"get_pymongo_collection",lambda:collection)
    pessoa=uuid4()
    id=PydanticObjectId()
    assert await repo.atualizar(id,pessoa,{"concluido":True}) is None
    filtro,alteracoes=collection.find_one_and_update.call_args.args
    assert filtro == {"_id":id,"usuario_id":Binary.from_uuid(pessoa)}
    assert alteracoes == {"$set":{"concluido":True}}

def test_patch_outro_usuario_retorna_404(usuario,monkeypatch):
    atualizar=AsyncMock(return_value=None)
    monkeypatch.setattr(repo,"atualizar",atualizar)
    resposta=TestClient(app).patch("/lembretes/atualizar/507f1f77bcf86cd799439011",json={"concluido":True})
    assert resposta.status_code==404
    assert atualizar.call_args.args[1]==usuario.id

def test_patch_e_delete(usuario,monkeypatch):
    id=PydanticObjectId()
    monkeypatch.setattr(repo,"atualizar",AsyncMock(return_value={"_id":id,"titulo":"Novo","concluido":True}))
    excluir=AsyncMock(return_value=True)
    monkeypatch.setattr(repo,"excluir",excluir)
    client=TestClient(app)
    assert client.patch(f"/lembretes/atualizar/{id}",json={"titulo":"Novo","concluido":True}).json()["concluido"]
    assert client.delete(f"/lembretes/excluir/{id}").status_code==204
    excluir.assert_awaited_once_with(id,usuario.id)

def test_delete_outro_usuario(usuario,monkeypatch):
    monkeypatch.setattr(repo,"excluir",AsyncMock(return_value=False))
    assert TestClient(app).delete("/lembretes/excluir/507f1f77bcf86cd799439011").status_code==404

def test_cors_shell():
    response=TestClient(app).options("/lembretes/listar",headers={
        "Origin":"http://localhost:8081",
        "Access-Control-Request-Method":"GET",
        "Access-Control-Request-Headers":"authorization"})
    assert response.status_code==200
    assert response.headers["access-control-allow-origin"]=="http://localhost:8081"


@pytest.mark.parametrize("horario", ["2027-01-01T09:00:00", "2027-02-30T09:00:00Z", "data inválida"])
def test_agendamento_exige_data_e_fuso(horario):
    with pytest.raises(ValidationError):
        LembreteCreate(titulo="Entrega", agendado_em=horario)


def test_agendamento_normaliza_utc_e_permite_remover_sem_apagar_em_patch_legado():
    dados = LembreteCreate(titulo="Entrega", agendado_em="2027-01-10T09:00:00-03:00")
    assert dados.agendado_em.isoformat() == "2027-01-10T12:00:00+00:00"
    assert LembreteUpdate(titulo="Novo").model_dump(exclude_unset=True) == {"titulo": "Novo"}
    assert LembreteUpdate(agendado_em=None).model_dump(exclude_unset=True) == {"agendado_em": None}


def test_http_preserva_agendamento_na_resposta(usuario, monkeypatch):
    from datetime import datetime, timezone
    item = {"_id": PydanticObjectId(), "titulo": "Novo", "concluido": False,
            "agendado_em": datetime(2027, 1, 10, 12, tzinfo=timezone.utc)}
    atualizar = AsyncMock(return_value=item)
    monkeypatch.setattr(repo, "atualizar", atualizar)
    resposta = TestClient(app).patch(f"/lembretes/atualizar/{item['_id']}", json={"titulo": "Novo"})
    assert resposta.status_code == 200
    assert resposta.json()["agendado_em"] == "2027-01-10T12:00:00Z"
    assert atualizar.call_args.args[2] == {"titulo": "Novo"}


def test_bson_sem_fuso_e_interpretado_como_utc():
    from datetime import datetime
    from src.schemas.lembrete import LembreteResponse
    resposta = LembreteResponse(id=PydanticObjectId(), titulo="BSON", concluido=False, agendado_em=datetime(2027, 1, 10, 12))
    assert resposta.model_dump(mode="json")["agendado_em"] == "2027-01-10T12:00:00Z"
