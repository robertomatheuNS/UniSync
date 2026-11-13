from datetime import datetime, timezone
from beanie import PydanticObjectId
from pydantic import BaseModel, ConfigDict, Field, field_validator


class PublicacaoDados(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    titulo: str = Field(min_length=3, max_length=100)
    conteudo: str = Field(min_length=1, max_length=5000)


class PublicacaoCreate(PublicacaoDados):
    turma_id: PydanticObjectId


class PublicacaoUpdate(PublicacaoDados):
    versao: int = Field(ge=1)


class ResumoPublicacao(BaseModel):
    id: PydanticObjectId
    turma_id: PydanticObjectId
    turma_nome: str
    disciplina: str
    autor_id: str
    autor: str
    papel_autor: str
    titulo: str
    resumo: str
    criado_em: datetime
    atualizado_em: datetime
    versao: int
    pode_editar: bool
    pode_excluir: bool

    @field_validator("criado_em", "atualizado_em")
    @classmethod
    def explicitar_utc(cls, valor):
        return valor.replace(tzinfo=timezone.utc) if valor.tzinfo is None else valor


class PublicacaoResponse(ResumoPublicacao):
    conteudo: str


class PaginaPublicacoes(BaseModel):
    itens: list[ResumoPublicacao]
    proxima_pagina: PydanticObjectId | None = None
