from datetime import datetime, timezone
from pymongo import ReturnDocument
from src.models.comentario import Comentario
from src.models.mural import Publicacao


async def curtir(publicacao_id, usuario_id, curtiu):
    operador = "$addToSet" if curtiu else "$pull"
    item = await Publicacao.get_pymongo_collection().find_one_and_update(
        {"_id": publicacao_id, "excluida": False}, {operador: {"curtidas": usuario_id}},
        return_document=ReturnDocument.AFTER,
    )
    return Publicacao.model_validate(item) if item else None


async def criar(dados):
    return await Comentario(**dados).insert()


async def listar(publicacao_id, antes, limite):
    filtro = {"publicacao_id": publicacao_id, "excluido": False}
    if antes is not None:
        filtro["_id"] = {"$lt": antes}
    return await Comentario.find(filtro).sort("-_id").limit(limite + 1).to_list()


async def buscar(comentario_id):
    return await Comentario.find_one({"_id": comentario_id, "excluido": False})


async def atualizar(comentario_id, publicacao_id, autor_id, versao, conteudo):
    item = await Comentario.get_pymongo_collection().find_one_and_update(
        {"_id": comentario_id, "publicacao_id": publicacao_id, "autor_id": autor_id, "versao": versao, "excluido": False},
        {"$set": {"conteudo": conteudo, "atualizado_em": datetime.now(timezone.utc)}, "$inc": {"versao": 1}},
        return_document=ReturnDocument.AFTER,
    )
    return Comentario.model_validate(item) if item else None


async def excluir(comentario_id, publicacao_id, versao, usuario_id):
    resultado = await Comentario.get_pymongo_collection().update_one(
        {"_id": comentario_id, "publicacao_id": publicacao_id, "versao": versao, "excluido": False},
        {"$set": {"excluido": True, "excluido_por": usuario_id, "atualizado_em": datetime.now(timezone.utc)}, "$inc": {"versao": 1}},
    )
    return resultado.modified_count == 1
