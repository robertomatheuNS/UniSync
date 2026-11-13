from datetime import date
from uuid import UUID

from beanie import PydanticObjectId
from pydantic import BaseModel, ConfigDict, Field, field_validator

from src.models.tarefa import StatusTarefa, PrioridadeTarefa


class TarefaCreate(BaseModel):
    titulo: str = Field(min_length=1, max_length=100)
    prioridade: PrioridadeTarefa = PrioridadeTarefa.MEDIA
    prazo: date | None = None
    turma_id: PydanticObjectId | None = None

    model_config = ConfigDict(extra="forbid")

    @field_validator("titulo", mode="before")
    @classmethod
    def normalizar_titulo(cls, valor):
        return valor.strip() if isinstance(valor, str) else valor


class TarefaUpdate(BaseModel):
    titulo: str | None = Field(default=None, min_length=1, max_length=100)
    status: StatusTarefa | None = None
    prioridade: PrioridadeTarefa | None = None
    prazo: date | None = None
    turma_id: PydanticObjectId | None = None

    model_config = ConfigDict(extra="forbid")

    @field_validator("titulo", mode="before")
    @classmethod
    def normalizar_titulo(cls, valor):
        return valor.strip() if isinstance(valor, str) else valor

    @field_validator("titulo", "status", "prioridade")
    @classmethod
    def impedir_valores_nulos(cls, valor):
        if valor is None:
            raise ValueError("O campo não pode ser nulo")
        return valor


class TarefaResponse(BaseModel):
    id: PydanticObjectId
    aluno_id: UUID
    titulo: str
    status: StatusTarefa
    prioridade: PrioridadeTarefa = PrioridadeTarefa.MEDIA
    prazo: date | None = None
    turma_id: PydanticObjectId | None = None
    disciplina: str | None = None
