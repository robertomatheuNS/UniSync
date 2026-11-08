from pydantic import BaseModel, Field
from beanie import PydanticObjectId
from datetime import datetime

class OportunidadeCreate(BaseModel):
    titulo: str = Field(min_length=5, max_length=100)
    professor: str
    materia: str
    descricao: str | None = None
    tipo: str
    data_evento: datetime

class OportunidadeUpdate(BaseModel):
    titulo: str | None = Field(default=None, min_length=5, max_length=100)
    professor: str | None = None
    materia: str | None = None
    descricao: str | None = None
    tipo: str | None = None
    data_evento: datetime | None = None

class OportunidadeResponse(BaseModel):
    id: PydanticObjectId
    titulo: str = Field(min_length=5, max_length=100)
    professor: str
    materia: str
    descricao: str | None = None
    tipo: str
    data_evento: datetime