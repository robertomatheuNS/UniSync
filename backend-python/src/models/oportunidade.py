from datetime import datetime

data: datetime

from beanie import Document

class Oportunidade(Document):
    titulo: str
    professor: str
    descricao: str
    tipo: str
    data_evento: datetime

    class Settings:
        name = "oportunidades"