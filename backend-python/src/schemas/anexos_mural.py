from datetime import datetime, timezone
from beanie import PydanticObjectId
from pydantic import BaseModel, field_validator

MAX_ANEXOS = 3
MAX_BYTES = 4 * 1024 * 1024
TIPOS = {"application/pdf": (".pdf",), "image/png": (".png",), "image/jpeg": (".jpg", ".jpeg")}


class AnexoArmazenado(BaseModel):
    id: PydanticObjectId
    nome: str
    tipo: str
    tamanho: int
    sha256: str
    criado_em: datetime
    dados: bytes | None = None


class AnexoResponse(BaseModel):
    id: PydanticObjectId
    publicacao_id: PydanticObjectId
    nome: str
    tipo: str
    tamanho: int
    sha256: str
    criado_em: datetime
    pode_excluir: bool

    @field_validator("criado_em")
    @classmethod
    def explicitar_utc(cls, valor):
        return valor.replace(tzinfo=timezone.utc) if valor.tzinfo is None else valor


class ListaAnexos(BaseModel):
    itens: list[AnexoResponse]
    pode_adicionar: bool
    limite: int = MAX_ANEXOS
    tamanho_maximo: int = MAX_BYTES
