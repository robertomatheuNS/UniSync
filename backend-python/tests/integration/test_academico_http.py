"""Exercita HTTP, validação e autorização com armazenamento controlado."""
from datetime import datetime, timezone
from types import SimpleNamespace
from uuid import uuid4

import pytest
from beanie import PydanticObjectId
from fastapi.testclient import TestClient

from src.core.security import UsuarioAutenticado, obter_usuario_atual
from src.main import app
from src.repositories import academico_repository as repo


@pytest.fixture
def ambiente(monkeypatch):
    turmas, avisos = {}, {}
    professor = UsuarioAutenticado(uuid4(), "Professora Ana", "Professor")
    aluno = UsuarioAutenticado(uuid4(), "Aluno Bruno", "Aluno")
    outro = UsuarioAutenticado(uuid4(), "Outro Professor", "Professor")

    async def criar_turma(dados):
        item = SimpleNamespace(id=PydanticObjectId(), alunos=[], ativa=True, **dados)
        turmas[item.id] = item
        return item

    async def listar_turmas(usuario_id, eh_professor):
        return [t for t in turmas.values() if t.ativa and
                (t.professor_id == usuario_id if eh_professor else usuario_id in t.alunos)]

    async def buscar_turma(identificador):
        item = turmas.get(identificador)
        return item if item and item.ativa else None

    async def entrar_turma(codigo, aluno_id):
        turma = next((t for t in turmas.values() if t.ativa and t.codigo == codigo.upper()), None)
        if turma and aluno_id not in turma.alunos:
            turma.alunos.append(aluno_id)
        return turma

    async def atualizar_turma(turma, dados):
        for campo, valor in dados.items():
            setattr(turma, campo, valor)
        return turma

    async def arquivar_turma(turma):
        turma.ativa = False

    async def criar_aviso(dados):
        agora = datetime.now(timezone.utc)
        item = SimpleNamespace(id=PydanticObjectId(), criado_em=agora, atualizado_em=agora, lidos=[], **dados)
        avisos[item.id] = item
        return item

    async def listar_avisos(ids):
        return [a for a in avisos.values() if a.turma_id in ids]

    async def buscar_aviso(identificador):
        return avisos.get(identificador)

    async def atualizar_aviso(aviso, dados):
        for campo, valor in dados.items():
            setattr(aviso, campo, valor)
        aviso.lidos = []
        return aviso

    async def marcar_lido(aviso, aluno_id):
        if aluno_id not in aviso.lidos:
            aviso.lidos.append(aluno_id)
        return aviso

    async def excluir_aviso(aviso):
        del avisos[aviso.id]

    for nome, funcao in list(locals().items()):
        if nome in ("criar_turma", "listar_turmas", "buscar_turma", "entrar_turma", "atualizar_turma",
                    "arquivar_turma", "criar_aviso", "listar_avisos", "buscar_aviso", "atualizar_aviso",
                    "marcar_lido", "excluir_aviso"):
            monkeypatch.setattr(repo, nome, funcao)

    def como(usuario):
        class Cliente(TestClient):
            def request(self, *args, **kwargs):
                app.dependency_overrides[obter_usuario_atual] = lambda: usuario
                return super().request(*args, **kwargs)
        return Cliente(app)

    try:
        yield SimpleNamespace(como=como, professor=professor, aluno=aluno, outro=outro,
                              turmas=turmas, avisos=avisos)
    finally:
        app.dependency_overrides.clear()


def criar_turma(ambiente):
    resposta = ambiente.como(ambiente.professor).post("/turmas/criar", json={
        "nome": "Turma A", "disciplina": "Engenharia de Software", "periodo": "2026.2"})
    assert resposta.status_code == 201
    return resposta.json()


def criar_aviso(ambiente, turma):
    resposta = ambiente.como(ambiente.professor).post("/avisos/criar", json={
        "turma_id": turma["id"], "titulo": "Entrega do projeto", "conteudo": "Apresentação na próxima aula."})
    assert resposta.status_code == 201
    return resposta.json()


def test_ciclo_completo_e_leitura_individual(ambiente):
    turma = criar_turma(ambiente)
    aviso = criar_aviso(ambiente, turma)
    aluno = ambiente.como(ambiente.aluno)
    assert aluno.get("/turmas/listar").json() == []
    assert aluno.get("/avisos/listar").json() == []
    entrada = aluno.post("/turmas/entrar", json={"codigo": turma["codigo"].lower()})
    assert entrada.status_code == 200
    assert entrada.json()["codigo"] is None
    assert "alunos" not in entrada.json()
    # Repetir a entrada não duplica o aluno.
    assert aluno.post("/turmas/entrar", json={"codigo": turma["codigo"]}).json()["total_alunos"] == 1
    assert aluno.get("/avisos/listar").json()[0]["lido"] is False
    assert aluno.put(f"/avisos/ler/{aviso['id']}").json()["lido"] is True
    assert aluno.put(f"/avisos/ler/{aviso['id']}").json()["lido"] is True
    outro_aluno = UsuarioAutenticado(uuid4(), "Carla", "Aluno")
    cliente = ambiente.como(outro_aluno)
    cliente.post("/turmas/entrar", json={"codigo": turma["codigo"]})
    assert cliente.get("/avisos/listar").json()[0]["lido"] is False
    professor = ambiente.como(ambiente.professor)
    revisado = professor.patch(f"/avisos/atualizar/{aviso['id']}", json={
        "titulo": "Entrega revisada", "conteudo": "Nova data de apresentação."})
    assert revisado.status_code == 200
    assert ambiente.como(ambiente.aluno).get("/avisos/listar").json()[0]["lido"] is False
    assert professor.delete(f"/avisos/excluir/{aviso['id']}").status_code == 204
    assert professor.get("/avisos/listar").json() == []


@pytest.mark.parametrize("perfil", ["Aluno", "Outro Professor"])
def test_nao_permite_ler_nem_alterar_turma_e_avisos_alheios(ambiente, perfil):
    turma = criar_turma(ambiente)
    aviso = criar_aviso(ambiente, turma)
    usuario = ambiente.aluno if perfil == "Aluno" else ambiente.outro
    cliente = ambiente.como(usuario)
    assert cliente.get("/turmas/listar").json() == []
    assert cliente.get("/avisos/listar").json() == []
    assert cliente.get("/avisos/listar", params={"turma_id": turma["id"]}).status_code == 403
    assert cliente.patch(f"/turmas/atualizar/{turma['id']}", json={"nome": "Outra turma", "disciplina": "Kotlin"}).status_code == 403
    assert cliente.post(f"/turmas/arquivar/{turma['id']}").status_code == 403
    assert cliente.post("/avisos/criar", json={"turma_id": turma["id"], "titulo": "Intruso", "conteudo": "Não autorizado"}).status_code == 403
    assert cliente.patch(f"/avisos/atualizar/{aviso['id']}", json={"titulo": "Intruso", "conteudo": "Não autorizado"}).status_code == 403
    assert cliente.delete(f"/avisos/excluir/{aviso['id']}").status_code == 403
    assert cliente.put(f"/avisos/ler/{aviso['id']}").status_code == 403


def test_aluno_matriculado_nao_publica_nem_edita(ambiente):
    turma = criar_turma(ambiente)
    aviso = criar_aviso(ambiente, turma)
    aluno = ambiente.como(ambiente.aluno)
    aluno.post("/turmas/entrar", json={"codigo": turma["codigo"]})
    assert aluno.post("/turmas/criar", json={"nome": "Minha turma", "disciplina": "Kotlin"}).status_code == 403
    assert aluno.post("/avisos/criar", json={"turma_id": turma["id"], "titulo": "Intruso", "conteudo": "Texto"}).status_code == 403
    assert aluno.delete(f"/avisos/excluir/{aviso['id']}").status_code == 403


def test_turma_arquivada_oculta_conteudo_e_recusa_novas_entradas(ambiente):
    turma = criar_turma(ambiente)
    aviso = criar_aviso(ambiente, turma)
    aluno = ambiente.como(ambiente.aluno)
    aluno.post("/turmas/entrar", json={"codigo": turma["codigo"]})
    professor = ambiente.como(ambiente.professor)
    assert professor.post(f"/turmas/arquivar/{turma['id']}").status_code == 204
    assert professor.get("/turmas/listar").json() == []
    assert aluno.get("/avisos/listar").json() == []
    assert aluno.put(f"/avisos/ler/{aviso['id']}").status_code == 404
    assert aluno.post("/turmas/entrar", json={"codigo": turma["codigo"]}).status_code == 404


@pytest.mark.parametrize("corpo", [
    {"nome": "   ", "disciplina": "Kotlin"},
    {"nome": "Turma A", "disciplina": " "},
    {"nome": "Turma A", "disciplina": "Kotlin", "professor_id": "forjado"},
])
def test_validacao_de_turma(ambiente, corpo):
    assert ambiente.como(ambiente.professor).post("/turmas/criar", json=corpo).status_code == 422


@pytest.mark.parametrize("conteudo", ["", "  ", "x" * 5001])
def test_validacao_de_aviso(ambiente, conteudo):
    turma = criar_turma(ambiente)
    resposta = ambiente.como(ambiente.professor).post("/avisos/criar", json={
        "turma_id": turma["id"], "titulo": "Aviso", "conteudo": conteudo})
    assert resposta.status_code == 422


def test_codigo_invalido_e_recurso_inexistente(ambiente):
    aluno = ambiente.como(ambiente.aluno)
    assert aluno.post("/turmas/entrar", json={"codigo": "ABC"}).status_code == 422
    assert aluno.post("/turmas/entrar", json={"codigo": "AABBCCDDEEFF"}).status_code == 404
    assert aluno.put(f"/avisos/ler/{PydanticObjectId()}").status_code == 404


def test_rotas_exigem_autenticacao():
    cliente = TestClient(app)
    assert cliente.get("/turmas/listar").status_code == 401
    assert cliente.get("/avisos/listar").status_code == 401


def test_datas_do_mongodb_sao_serializadas_com_fuso_utc(ambiente):
    turma = criar_turma(ambiente)
    aviso = criar_aviso(ambiente, turma)
    salvo = ambiente.avisos[PydanticObjectId(aviso["id"])]
    salvo.criado_em = datetime(2026, 11, 13, 12, 30)
    salvo.atualizado_em = datetime(2026, 11, 13, 13, 0)
    resposta = ambiente.como(ambiente.professor).get("/avisos/listar").json()[0]
    assert resposta["criado_em"] == "2026-11-13T12:30:00Z"
    assert resposta["atualizado_em"] == "2026-11-13T13:00:00Z"


def test_preflight_da_web_permite_confirmacao_de_leitura():
    resposta = TestClient(app).options("/avisos/ler/000000000000000000000001", headers={
        "Origin": "http://127.0.0.1:5173", "Access-Control-Request-Method": "PUT",
        "Access-Control-Request-Headers": "Authorization"})
    assert resposta.status_code == 200
    assert "PUT" in resposta.headers["access-control-allow-methods"]
