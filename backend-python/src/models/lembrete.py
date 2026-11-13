from uuid import UUID
from beanie import Document
from pydantic import Field


class Lembrete(Document):
    usuario_id: UUID
    titulo: str = Field(min_length=1, max_length=200)
    concluido: bool = False

    class Settings:
        name = "lembretes"
        indexes = ["usuario_id"]
