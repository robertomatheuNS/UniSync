from datetime import datetime

from beanie import Document

class Oportunidade(Document):
    titulo: str
    professor: str
    descricao: str | None
    tipo: str
    data_evento: datetime

    class Settings:
        name = "oportunidades"