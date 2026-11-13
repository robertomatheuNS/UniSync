from datetime import datetime, timezone

from beanie import Document, PydanticObjectId
from pydantic import Field
from pymongo import IndexModel


class Turma(Document):
    nome: str
    disciplina: str
    periodo: str = ""
    professor_id: str
    professor: str
    codigo: str
    alunos: list[str] = Field(default_factory=list)
    ativa: bool = True

    class Settings:
        name = "turmas"
        indexes = [IndexModel("codigo", unique=True), "professor_id", "alunos"]


class Aviso(Document):
    turma_id: PydanticObjectId
    professor_id: str
    titulo: str
    conteudo: str
    criado_em: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    atualizado_em: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    lidos: list[str] = Field(default_factory=list)

    class Settings:
        name = "avisos"
        indexes = ["turma_id"]
