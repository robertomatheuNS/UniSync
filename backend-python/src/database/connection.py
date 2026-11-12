
from pymongo import AsyncMongoClient
from beanie import init_beanie
from src.core.config import settings
from src.models.oportunidade import Oportunidade
from src.models.tarefa import Tarefa


client = AsyncMongoClient(settings.mongodb_url)


async def conectar_mongodb():
    await client.admin.command("ping")

    await init_beanie(
        database=client[settings.mongodb_database],
        document_models=[Oportunidade, Tarefa]
    )

    print("Conexão com MongoDB estabelecida!")


async def desconectar_mongodb():
    await client.close()
    print("Conexão com MongoDB encerrada!")
