from datetime import datetime, timezone
from beanie import Document, PydanticObjectId
from pydantic import Field
from pymongo import IndexModel


class Publicacao(Document):
    turma_id: PydanticObjectId
    autor_id: str
    autor: str
    papel_autor: str
    titulo: str
    conteudo: str
    criado_em: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    atualizado_em: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    versao: int = 1
    excluida: bool = False
    excluida_por: str | None = None

    class Settings:
        name = "publicacoes"
        indexes = [IndexModel([("turma_id", 1), ("excluida", 1), ("_id", -1)])]
