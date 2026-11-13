from datetime import datetime, timezone

from beanie import PydanticObjectId
from pydantic import BaseModel, ConfigDict, Field, field_validator


class TurmaDados(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    nome: str = Field(min_length=3, max_length=80)
    disciplina: str = Field(min_length=2, max_length=100)
    periodo: str = Field(default="", max_length=30)


class EntradaTurma(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    codigo: str = Field(pattern=r"^[a-fA-F0-9]{12}$")


class TurmaResponse(TurmaDados):
    id: PydanticObjectId
    professor_id: str
    professor: str
    codigo: str | None
    total_alunos: int


class AvisoDados(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    titulo: str = Field(min_length=3, max_length=100)
    conteudo: str = Field(min_length=1, max_length=5000)


class AvisoCreate(AvisoDados):
    turma_id: PydanticObjectId


class AvisoResponse(AvisoDados):
    id: PydanticObjectId
    turma_id: PydanticObjectId
    turma_nome: str
    disciplina: str
    professor_id: str
    professor: str
    criado_em: datetime
    atualizado_em: datetime
    lido: bool

    @field_validator("criado_em", "atualizado_em")
    @classmethod
    def explicitar_utc(cls, valor: datetime) -> datetime:
        # O MongoDB armazena UTC e o driver pode devolver datetime sem tzinfo.
        return valor.replace(tzinfo=timezone.utc) if valor.tzinfo is None else valor
