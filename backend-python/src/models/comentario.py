from datetime import datetime, timezone
from beanie import Document, PydanticObjectId
from pydantic import Field
from pymongo import IndexModel


class Comentario(Document):
    publicacao_id: PydanticObjectId
    autor_id: str
    autor: str
    papel_autor: str
    conteudo: str
    criado_em: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    atualizado_em: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    versao: int = 1
    excluido: bool = False
    excluido_por: str | None = None

    class Settings:
        name = "comentarios_mural"
        indexes = [IndexModel([("publicacao_id", 1), ("excluido", 1), ("_id", -1)])]
