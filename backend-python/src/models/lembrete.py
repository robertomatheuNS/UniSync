from uuid import UUID
from beanie import Document
from pydantic import Field, field_validator
from datetime import datetime, timezone


class Lembrete(Document):
    usuario_id: UUID
    titulo: str = Field(min_length=1, max_length=200)
    concluido: bool = False
    agendado_em: datetime | None = None

    @field_validator("agendado_em")
    @classmethod
    def bson_em_utc(cls, valor):
        # PyMongo lê BSON sem fuso por padrão; o instante gravado é UTC.
        return valor.replace(tzinfo=timezone.utc) if valor and valor.tzinfo is None else valor

    class Settings:
        name = "lembretes"
        indexes = ["usuario_id"]
