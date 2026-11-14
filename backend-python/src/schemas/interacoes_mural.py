from datetime import datetime, timezone
from beanie import PydanticObjectId
from pydantic import BaseModel, ConfigDict, Field, StrictBool, field_validator


class ComentarioCreate(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    conteudo: str = Field(min_length=1, max_length=2000)


class ComentarioUpdate(ComentarioCreate):
    versao: int = Field(ge=1)


class ComentarioResponse(BaseModel):
    id: PydanticObjectId
    publicacao_id: PydanticObjectId
    autor_id: str
    autor: str
    papel_autor: str
    conteudo: str
    criado_em: datetime
    atualizado_em: datetime
    versao: int
    pode_editar: bool
    pode_excluir: bool

    @field_validator("criado_em", "atualizado_em")
    @classmethod
    def explicitar_utc(cls, valor):
        return valor.replace(tzinfo=timezone.utc) if valor.tzinfo is None else valor


class PaginaComentarios(BaseModel):
    itens: list[ComentarioResponse]
    proxima_pagina: PydanticObjectId | None = None


class CurtidaDados(BaseModel):
    model_config = ConfigDict(extra="forbid")
    curtiu: StrictBool


class CurtidaResponse(BaseModel):
    publicacao_id: PydanticObjectId
    total_curtidas: int
    curtiu: bool
