
import pytest
from pydantic import ValidationError

from src.schemas.tarefa import TarefaCreate, TarefaUpdate


def test_criar_tarefa_com_titulo_valido():
    tarefa = TarefaCreate(titulo="Estudar Python")

    assert tarefa.titulo == "Estudar Python"


def test_criar_tarefa_sem_titulo():
    with pytest.raises(ValidationError):
        TarefaCreate()


def test_criar_tarefa_com_titulo_vazio():
    with pytest.raises(ValidationError):
        TarefaCreate(titulo="")


def test_criar_tarefa_com_campo_extra():
    with pytest.raises(ValidationError):
        TarefaCreate(
            titulo="Estudar Python",
            aluno_id="123"
        )


def test_atualizar_status_valido():
    tarefa = TarefaUpdate(status="EM_ANDAMENTO")

    assert tarefa.status.value == "EM_ANDAMENTO"


def test_atualizar_status_invalido():
    with pytest.raises(ValidationError):
        TarefaUpdate(status="CANCELADO")


def test_atualizar_titulo_nulo():
    with pytest.raises(ValidationError):
        TarefaUpdate(titulo=None)


def test_atualizar_status_nulo():
    with pytest.raises(ValidationError):
        TarefaUpdate(status=None)


@pytest.mark.parametrize("valor", ["   ", "\t\n"])
def test_titulo_apenas_espacos_rejeitado(valor):
    with pytest.raises(ValidationError):
        TarefaCreate(titulo=valor)
    with pytest.raises(ValidationError):
        TarefaUpdate(titulo=valor)


def test_campos_opcionais_e_limpeza_explicita():
    dados = TarefaCreate(titulo="  Estudar  ", prioridade="ALTA", prazo="2028-02-29")
    assert dados.titulo == "Estudar"
    assert dados.prazo.isoformat() == "2028-02-29"
    assert TarefaUpdate(prazo=None, turma_id=None).model_dump(exclude_unset=True) == {"prazo": None, "turma_id": None}
    assert TarefaUpdate(status="CONCLUIDO").model_dump(exclude_unset=True) == {"status": "CONCLUIDO"}


@pytest.mark.parametrize("campo,valor", [("prazo", "2027-02-29"), ("prioridade", None), ("prioridade", "URGENTE"), ("turma_id", "invalido"), ("disciplina", "forjada")])
def test_metadados_invalidos_rejeitados(campo, valor):
    with pytest.raises(ValidationError):
        TarefaUpdate(**{campo: valor})
