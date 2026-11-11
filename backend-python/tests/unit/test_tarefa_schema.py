
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
