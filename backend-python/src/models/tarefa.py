
from enum import Enum
from datetime import date
from uuid import UUID

from beanie import Document, PydanticObjectId
from pydantic import Field


class StatusTarefa(str, Enum):
    A_FAZER = "A_FAZER"
    EM_ANDAMENTO = "EM_ANDAMENTO"
    CONCLUIDO = "CONCLUIDO"


class PrioridadeTarefa(str, Enum):
    BAIXA = "BAIXA"
    MEDIA = "MEDIA"
    ALTA = "ALTA"


class Tarefa(Document):
    aluno_id: UUID
    titulo: str = Field(min_length=1, max_length=100)
    status: StatusTarefa = StatusTarefa.A_FAZER
    prioridade: PrioridadeTarefa = PrioridadeTarefa.MEDIA
    prazo: date | None = None
    turma_id: PydanticObjectId | None = None
    disciplina: str | None = None

    class Settings:
        name = "tarefas"
