from datetime import datetime, timezone
from pymongo import ReturnDocument
from src.models.mural import Publicacao


async def criar(dados):
    return await Publicacao(**dados).insert()


async def listar(turmas, antes, limite):
    filtro = {"turma_id": {"$in": turmas}, "excluida": False}
    if antes is not None:
        filtro["_id"] = {"$lt": antes}
    cursor = Publicacao.get_pymongo_collection().find(filtro, {"anexos.dados": 0}).sort("_id", -1).limit(limite + 1)
    return [Publicacao.model_validate(item) async for item in cursor]


async def buscar(identificador):
    item = await Publicacao.get_pymongo_collection().find_one({"_id": identificador, "excluida": False}, {"anexos.dados": 0})
    return Publicacao.model_validate(item) if item else None


async def atualizar(identificador, autor_id, versao, dados):
    item = await Publicacao.get_pymongo_collection().find_one_and_update(
        {"_id": identificador, "autor_id": autor_id, "versao": versao, "excluida": False},
        {"$set": {**dados, "atualizado_em": datetime.now(timezone.utc)}, "$inc": {"versao": 1}},
        return_document=ReturnDocument.AFTER, projection={"anexos.dados": 0},
    )
    return Publicacao.model_validate(item) if item else None


async def excluir(identificador, versao, usuario_id):
    resultado = await Publicacao.get_pymongo_collection().update_one(
        {"_id": identificador, "versao": versao, "excluida": False},
        {"$set": {"excluida": True, "excluida_por": usuario_id, "atualizado_em": datetime.now(timezone.utc)},
         "$inc": {"versao": 1}, "$unset": {"anexos": ""}},
    )
    return resultado.modified_count == 1
