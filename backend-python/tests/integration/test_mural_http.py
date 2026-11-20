from datetime import datetime, timezone
from types import SimpleNamespace
from uuid import uuid4
from unittest.mock import AsyncMock
import pytest
from beanie import PydanticObjectId
from fastapi.testclient import TestClient
from src.main import app
from src.core.security import UsuarioAutenticado, obter_usuario_atual
from src.repositories import mural_repository as repo, academico_repository
from src.models.mural import Publicacao


@pytest.fixture
def ambiente(monkeypatch):
    professor = UsuarioAutenticado(uuid4(), "Ana", "Professor")
    aluno = UsuarioAutenticado(uuid4(), "Bruno", "Aluno")
    colega = UsuarioAutenticado(uuid4(), "Carla", "Aluno")
    fora = UsuarioAutenticado(uuid4(), "Fora", "Aluno")
    outro_prof = UsuarioAutenticado(uuid4(), "Outro", "Professor")
    turma = SimpleNamespace(id=PydanticObjectId(), nome="Turma A", disciplina="Kotlin", professor_id=str(professor.id),
                            alunos=[str(aluno.id), str(colega.id)], ativa=True)
    itens = {}
    async def buscar_turma(id):
        return turma if turma.id == id and turma.ativa else None
    async def listar_turmas(usuario_id, eh_professor):
        return [turma] if turma.ativa and (turma.professor_id == usuario_id if eh_professor else usuario_id in turma.alunos) else []
    async def criar(dados):
        agora = datetime.now(timezone.utc)
        item = SimpleNamespace(id=PydanticObjectId(), versao=1, excluida=False, criado_em=agora, atualizado_em=agora, **dados)
        itens[item.id] = item
        return item
    async def listar(turmas, antes, limite):
        return sorted([item for item in itens.values() if item.turma_id in turmas and not item.excluida and
                       (antes is None or item.id < antes)], key=lambda item: item.id, reverse=True)[:limite + 1]
    async def buscar(id):
        item = itens.get(id)
        return item if item and not item.excluida else None
    async def atualizar(id, autor_id, versao, dados):
        item = await buscar(id)
        if item is None or item.autor_id != autor_id or item.versao != versao:
            return None
        for k, v in dados.items():
            setattr(item, k, v)
        item.versao += 1
        item.atualizado_em = datetime.now(timezone.utc)
        return item
    async def excluir(id, versao, usuario_id):
        item = await buscar(id)
        if item is None or item.versao != versao:
            return False
        item.excluida = True
        item.excluida_por = usuario_id
        return True
    for nome in ("criar", "listar", "buscar", "atualizar", "excluir"):
        monkeypatch.setattr(repo, nome, locals()[nome])
    monkeypatch.setattr(academico_repository, "buscar_turma", buscar_turma)
    monkeypatch.setattr(academico_repository, "listar_turmas", listar_turmas)
    def como(usuario):
        class Cliente(TestClient):
            def request(self, *args, **kwargs):
                app.dependency_overrides[obter_usuario_atual] = lambda: usuario
                return super().request(*args, **kwargs)
        return Cliente(app)
    try:
        yield SimpleNamespace(como=como, turma=turma, itens=itens, professor=professor, aluno=aluno, colega=colega, fora=fora, outro_prof=outro_prof)
    finally:
        app.dependency_overrides.clear()


def publicar(a, usuario=None, titulo="Publicação Kotlin", conteudo="Conteúdo da turma"):
    resposta = a.como(usuario or a.aluno).post("/mural/criar", json={"turma_id": str(a.turma.id), "titulo": titulo, "conteudo": conteudo})
    assert resposta.status_code == 201
    return resposta.json()


def test_ciclo_autor_edita_e_exclui_com_versao(ambiente):
    a = ambiente
    item = publicar(a)
    assert item["autor_id"] == str(a.aluno.id) and item["autor"] == "Bruno"
    assert item["pode_editar"] and item["pode_excluir"]
    client = a.como(a.aluno)
    revisado = client.patch(f"/mural/atualizar/{item['id']}", json={"titulo": "Título revisado", "conteudo": "Novo conteúdo", "versao": 1})
    assert revisado.status_code == 200 and revisado.json()["versao"] == 2
    assert client.delete(f"/mural/excluir/{item['id']}?versao=1").status_code == 409
    assert client.delete(f"/mural/excluir/{item['id']}?versao=2").status_code == 204
    assert client.get(f"/mural/buscar/{item['id']}").status_code == 404
    assert client.get("/mural/listar").json()["itens"] == []


def test_colega_le_mas_nao_edita_nem_exclui(ambiente):
    a = ambiente
    item = publicar(a)
    client = a.como(a.colega)
    detalhe = client.get(f"/mural/buscar/{item['id']}").json()
    assert not detalhe["pode_editar"] and not detalhe["pode_excluir"]
    assert client.patch(f"/mural/atualizar/{item['id']}", json={"titulo": "Tentativa", "conteudo": "Outro", "versao": 1}).status_code == 403
    assert client.delete(f"/mural/excluir/{item['id']}?versao=1").status_code == 403


def test_professor_modera_sem_editar_texto_do_aluno(ambiente):
    a = ambiente
    item = publicar(a)
    client = a.como(a.professor)
    detalhe = client.get(f"/mural/buscar/{item['id']}").json()
    assert not detalhe["pode_editar"] and detalhe["pode_excluir"]
    assert client.patch(f"/mural/atualizar/{item['id']}", json={"titulo": "Tentativa", "conteudo": "Outro", "versao": 1}).status_code == 403
    assert client.delete(f"/mural/excluir/{item['id']}?versao=1").status_code == 204
    assert a.itens[PydanticObjectId(item["id"])].excluida_por == str(a.professor.id)
    assert publicar(a, a.professor)["papel_autor"] == "Professor"


@pytest.mark.parametrize("perfil", ["fora", "outro_prof"])
def test_todas_operacoes_isolam_turmas(ambiente, perfil):
    a = ambiente
    item = publicar(a)
    client = a.como(getattr(a, perfil))
    assert client.get("/mural/listar").json() == {"itens": [], "proxima_pagina": None}
    assert client.get(f"/mural/listar?turma_id={a.turma.id}").status_code == 403
    assert client.get(f"/mural/buscar/{item['id']}").status_code == 403
    assert client.post("/mural/criar", json={"turma_id": str(a.turma.id), "titulo": "Tentativa", "conteudo": "Texto"}).status_code == 403
    assert client.patch(f"/mural/atualizar/{item['id']}", json={"titulo": "Tentativa", "conteudo": "Texto", "versao": 1}).status_code == 403
    assert client.delete(f"/mural/excluir/{item['id']}?versao=1").status_code == 403


def test_arquivamento_oculta_e_bloqueia_publicacoes_existentes(ambiente):
    a = ambiente
    item = publicar(a)
    a.turma.ativa = False
    for usuario in [a.aluno, a.professor]:
        client = a.como(usuario)
        assert client.get("/mural/listar").json()["itens"] == []
        assert client.get(f"/mural/buscar/{item['id']}").status_code == 404
        assert client.delete(f"/mural/excluir/{item['id']}?versao=1").status_code == 404


def test_paginacao_estavel_resumo_e_datas_utc(ambiente):
    a = ambiente
    for i in range(5):
        publicar(a, titulo=f"Publicação {i}", conteudo="Texto longo " * 100)
    client = a.como(a.aluno)
    primeira = client.get("/mural/listar?limite=2").json()
    segunda = client.get(f"/mural/listar?limite=2&antes={primeira['proxima_pagina']}").json()
    ultima = client.get(f"/mural/listar?limite=2&antes={segunda['proxima_pagina']}").json()
    itens = primeira["itens"] + segunda["itens"] + ultima["itens"]
    assert len({item["id"] for item in itens}) == 5
    assert ultima["proxima_pagina"] is None
    assert all("conteudo" not in item and len(item["resumo"]) <= 241 for item in itens)
    assert itens[0]["criado_em"].endswith("Z")
    assert len(client.get(f"/mural/buscar/{itens[0]['id']}").json()["conteudo"]) > 241


def test_revisao_antiga_nao_sobrescreve_conteudo_novo(ambiente):
    a = ambiente
    item = publicar(a)
    client = a.como(a.aluno)
    payload = {"titulo": "Revisão nova", "conteudo": "Novo", "versao": 1}
    assert client.patch(f"/mural/atualizar/{item['id']}", json=payload).status_code == 200
    assert client.patch(f"/mural/atualizar/{item['id']}", json={**payload, "conteudo": "Antigo"}).status_code == 409
    assert client.get(f"/mural/buscar/{item['id']}").json()["conteudo"] == "Novo"


@pytest.mark.parametrize("campo,valor", [("autor_id", "forjado"), ("papel_autor", "Professor"), ("titulo", "  "), ("conteudo", ""), ("conteudo", "a"*5001)])
def test_rejeita_dados_invalidos_ou_autoria_forjada(ambiente, campo, valor):
    a = ambiente
    dados = {"turma_id": str(a.turma.id), "titulo": "Título válido", "conteudo": "Texto", campo: valor}
    assert a.como(a.aluno).post("/mural/criar", json=dados).status_code == 422


@pytest.mark.parametrize("query", ["limite=0", "limite=51", "antes=invalido", "turma_id=invalida"])
def test_valida_paginacao(ambiente, query):
    assert ambiente.como(ambiente.aluno).get(f"/mural/listar?{query}").status_code == 422


@pytest.mark.parametrize("metodo,caminho,payload", [
    ("get", "/mural/listar", None), ("get", "/mural/buscar/507f1f77bcf86cd799439011", None),
    ("post", "/mural/criar", {"turma_id": "507f1f77bcf86cd799439011", "titulo": "Teste", "conteudo": "Texto"}),
    ("patch", "/mural/atualizar/507f1f77bcf86cd799439011", {"titulo": "Teste", "conteudo": "Texto", "versao": 1}),
    ("delete", "/mural/excluir/507f1f77bcf86cd799439011?versao=1", None),
])
def test_exige_token(metodo, caminho, payload):
    kwargs = {"json": payload} if payload else {}
    assert getattr(TestClient(app), metodo)(caminho, **kwargs).status_code == 401


@pytest.mark.asyncio
async def test_repositorio_atualiza_com_autoria_e_versao_no_filtro_atomico(monkeypatch):
    colecao = AsyncMock()
    colecao.find_one_and_update.return_value = None
    monkeypatch.setattr(Publicacao, "get_pymongo_collection", lambda: colecao)
    id = PydanticObjectId()
    assert await repo.atualizar(id, "autor", 3, {"titulo": "Novo"}) is None
    filtro, atualizacao = colecao.find_one_and_update.call_args.args
    assert filtro == {"_id": id, "autor_id": "autor", "versao": 3, "excluida": False}
    assert atualizacao["$inc"] == {"versao": 1}
