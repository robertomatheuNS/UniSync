from src.models.mural import Publicacao
from src.schemas.anexos_mural import MAX_ANEXOS


async def adicionar(publicacao_id, autor_id, anexo):
    # O limite e a ausência do id são verificados na mesma escrita do binário.
    resultado = await Publicacao.get_pymongo_collection().update_one(
        {"_id": publicacao_id, "autor_id": autor_id, "excluida": False, "anexos.id": {"$ne": anexo.id},
         "$expr": {"$lt": [{"$size": {"$ifNull": ["$anexos", []]}}, MAX_ANEXOS]}},
        {"$push": {"anexos": anexo.model_dump()}},
    )
    return resultado.modified_count == 1


async def baixar(publicacao_id, anexo_id):
    item = await Publicacao.get_pymongo_collection().find_one(
        {"_id": publicacao_id, "excluida": False, "anexos.id": anexo_id},
        {"anexos": {"$elemMatch": {"id": anexo_id}}, "_id": 0},
    )
    return item["anexos"][0]["dados"] if item else None


async def excluir(publicacao_id, anexo_id):
    resultado = await Publicacao.get_pymongo_collection().update_one(
        {"_id": publicacao_id, "excluida": False, "anexos.id": anexo_id},
        {"$pull": {"anexos": {"id": anexo_id}}},
    )
    return resultado.modified_count == 1
