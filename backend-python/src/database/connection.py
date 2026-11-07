from pymongo import AsyncMongoClient

from src.core.config import settings


client = AsyncMongoClient(settings.mongodb_url)


async def conectar_mongodb():
    await client.admin.command("ping")
    print("Conexão com MongoDB estabelecida!")


async def desconectar_mongodb():
    await client.close()
    print("Conexão com MongoDB encerrada!")