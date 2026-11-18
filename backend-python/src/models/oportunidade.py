from datetime import datetime

from beanie import Document

class Oportunidade(Document):
    professor_id: str | None = None
    titulo: str
    professor: str
    materia: str
    descricao: str | None
    tipo: str
    url_inscricao: str | None = None
    prazo_inscricao: datetime | None = None
    data_evento: datetime

    class Settings:
        name = "oportunidades"