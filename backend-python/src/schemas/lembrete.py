from beanie import PydanticObjectId
from pydantic import BaseModel, ConfigDict, Field, field_validator


class LembreteCreate(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    titulo: str = Field(min_length=1, max_length=200)


class LembreteUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    titulo: str | None = Field(default=None, min_length=1, max_length=200)
    concluido: bool | None = None

    @field_validator("titulo", "concluido")
    @classmethod
    def impedir_nulos(cls, valor):
        if valor is None:
            raise ValueError("O campo não pode ser nulo")
        return valor


class LembreteResponse(BaseModel):
    id: PydanticObjectId
    titulo: str
    concluido: bool
