
from enum import Enum
from uuid import UUID

from beanie import Document
from pydantic import Field


class StatusTarefa(str, Enum):
    A_FAZER = "A_FAZER"
    EM_ANDAMENTO = "EM_ANDAMENTO"
    CONCLUIDO = "CONCLUIDO"


class Tarefa(Document):
    aluno_id: UUID
    titulo: str = Field(min_length=1, max_length=100)
    status: StatusTarefa = StatusTarefa.A_FAZER

    class Settings:
        name = "tarefas"
