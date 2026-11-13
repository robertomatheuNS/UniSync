from pymongo import ReturnDocument
from bson.binary import Binary
from src.models.lembrete import Lembrete


async def criar(dados, usuario_id):
    lembrete = Lembrete(usuario_id=usuario_id, **dados)
    await lembrete.insert()
    return lembrete


async def listar(usuario_id):
    return await Lembrete.find(Lembrete.usuario_id == usuario_id).sort("_id").to_list()


async def atualizar(lembrete_id, usuario_id, dados):
    # O filtro de autoria participa da atualização atômica.
    return await Lembrete.get_pymongo_collection().find_one_and_update(
        {"_id": lembrete_id, "usuario_id": Binary.from_uuid(usuario_id)},
        {"$set": dados},
        return_document=ReturnDocument.AFTER,
    )


async def excluir(lembrete_id, usuario_id):
    resultado = await Lembrete.get_pymongo_collection().delete_one(
        {"_id": lembrete_id, "usuario_id": Binary.from_uuid(usuario_id)}
    )
    return resultado.deleted_count == 1
