from beanie import PydanticObjectId
from pydantic import BaseModel, ConfigDict, Field, field_validator, AwareDatetime
from datetime import datetime, timezone


class ComAgendamento(BaseModel):
    agendado_em: AwareDatetime | None = None

    @field_validator("agendado_em")
    @classmethod
    def normalizar_fuso(cls, valor):
        return valor.astimezone(timezone.utc) if valor else None


class LembreteCreate(ComAgendamento):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    titulo: str = Field(min_length=1, max_length=200)


class LembreteUpdate(ComAgendamento):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    titulo: str | None = Field(default=None, min_length=1, max_length=200)
    concluido: bool | None = None

    @field_validator("titulo", "concluido")
    @classmethod
    def impedir_nulos(cls, valor):
        if valor is None:
            raise ValueError("O campo não pode ser nulo")
        return valor


class LembreteResponse(ComAgendamento):
    @field_validator("agendado_em", mode="before")
    @classmethod
    def bson_em_utc(cls, valor):
        return valor.replace(tzinfo=timezone.utc) if isinstance(valor, datetime) and valor.tzinfo is None else valor

    id: PydanticObjectId
    titulo: str
    concluido: bool
