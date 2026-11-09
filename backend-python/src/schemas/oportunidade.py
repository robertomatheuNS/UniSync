from pydantic import BaseModel, Field, ConfigDict, field_validator
from beanie import PydanticObjectId
from datetime import datetime

class OportunidadeCreate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    titulo: str = Field(min_length=5, max_length=100)
    professor: str
    materia: str
    descricao: str | None = None
    tipo: str
    data_evento: datetime

class OportunidadeUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    titulo: str | None = Field(default=None, min_length=5, max_length=100)
    professor: str | None = None
    materia: str | None = None
    descricao: str | None = None
    tipo: str | None = None
    data_evento: datetime | None = None

    @field_validator("titulo", "professor", "materia", "tipo", "data_evento")
    @classmethod
    def validar_campos_nao_nulos(cls, valor):
        
        if valor is None:
            raise ValueError("Não é permitido enviar campos None")
        
        return valor

class OportunidadeResponse(BaseModel):
    id: PydanticObjectId
    professor_id: str | None = None
    titulo: str = Field(min_length=5, max_length=100)
    professor: str
    materia: str
    descricao: str | None = None
    tipo: str
    data_evento: datetime