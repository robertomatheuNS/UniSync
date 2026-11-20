from pydantic import BaseModel, Field, ConfigDict, HttpUrl, field_validator, model_validator
from beanie import PydanticObjectId
from datetime import datetime, timezone
from urllib.parse import urlsplit


def validar_intervalo_inscricao(prazo, evento):
    # Datas de evento legadas sem fuso continuam aceitas; MongoDB as armazena como UTC.
    if prazo is not None and evento is not None:
        evento_utc = evento.replace(tzinfo=timezone.utc) if evento.tzinfo is None else evento.astimezone(timezone.utc)
        prazo_utc = prazo.replace(tzinfo=timezone.utc) if prazo.tzinfo is None else prazo.astimezone(timezone.utc)
        if prazo_utc > evento_utc:
            raise ValueError("O prazo de inscrição não pode ser posterior ao evento")


class CamposInscricao(BaseModel):
    url_inscricao: str | None = Field(default=None, max_length=2048)
    prazo_inscricao: datetime | None = None

    @field_validator("url_inscricao")
    @classmethod
    def validar_url(cls, valor):
        if valor is None or not valor.strip():
            return None
        valor = valor.strip()
        if not valor.lower().startswith("https://") or "\\" in valor or any(c.isspace() or ord(c) < 32 for c in valor):
            raise ValueError("Informe um endereço HTTPS válido")
        partes = urlsplit(valor)
        url = HttpUrl(valor)
        if not partes.hostname or partes.username is not None or url.port == 0:
            raise ValueError("Informe um endereço HTTPS sem usuário ou senha")
        return str(url)

    @field_validator("prazo_inscricao")
    @classmethod
    def validar_fuso(cls, valor):
        if valor is not None:
            if valor.tzinfo is None or valor.utcoffset() is None:
                raise ValueError("O prazo de inscrição exige fuso horário")
            return valor.astimezone(timezone.utc)
        return None

    @model_validator(mode="after")
    def validar_datas(self):
        validar_intervalo_inscricao(self.prazo_inscricao, getattr(self, "data_evento", None))
        return self


class OportunidadeCreate(CamposInscricao):
    model_config = ConfigDict(extra="forbid")
    titulo: str = Field(min_length=5, max_length=100)
    professor: str
    materia: str
    descricao: str | None = None
    tipo: str
    data_evento: datetime


class OportunidadeUpdate(CamposInscricao):
    model_config = ConfigDict(extra="forbid")
    titulo: str | None = Field(default=None, min_length=5, max_length=100)
    materia: str | None = None
    descricao: str | None = None
    tipo: str | None = None
    data_evento: datetime | None = None

    @field_validator("titulo", "materia", "tipo", "data_evento")
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
    url_inscricao: str | None = None
    prazo_inscricao: datetime | None = None

    @field_validator("prazo_inscricao", "data_evento")
    @classmethod
    def explicitar_utc(cls, valor):
        return valor.replace(tzinfo=timezone.utc) if valor is not None and valor.tzinfo is None else valor
