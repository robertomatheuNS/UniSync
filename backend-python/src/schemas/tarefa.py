
from uuid import UUID

from beanie import PydanticObjectId
from pydantic import BaseModel, ConfigDict, Field, field_validator

from src.models.tarefa import StatusTarefa


class TarefaCreate(BaseModel):
    titulo: str = Field(min_length=1, max_length=100)

    model_config = ConfigDict(extra="forbid")


class TarefaUpdate(BaseModel):
    titulo: str | None = Field(
        default=None,
        min_length=1,
        max_length=100,
    )
    status: StatusTarefa | None = None

    model_config = ConfigDict(extra="forbid")

    @field_validator("titulo", "status")
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
