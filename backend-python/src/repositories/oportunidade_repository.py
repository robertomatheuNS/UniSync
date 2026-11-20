from src.models.oportunidade import Oportunidade
from beanie import PydanticObjectId
from datetime import datetime, timezone
from pymongo import ReturnDocument

async def criar_oportunidade(oportunidade: Oportunidade):
    await oportunidade.insert()
    return oportunidade

async def listar_oportunidades(
    tipo: str | None = None,
    professor: str | None = None,
    futuros: bool = False,
    ordenar: bool = False,
    limite: int | None = None,
):

    filtro = {}

    if tipo is not None:
        filtro["tipo"] = tipo

    if professor is not None:
        filtro["professor"] = professor

    if futuros:
        agora = datetime.now(timezone.utc)
        filtro["data_evento"] = {"$gte": agora}

    consulta = Oportunidade.find(filtro)

    if ordenar:
        consulta = consulta.sort("data_evento")

    if limite is not None:
        consulta = consulta.limit(limite)

    return await consulta.to_list()


async def buscar_oportunidade_por_id(oportunidade_id: PydanticObjectId):
    return await Oportunidade.get(oportunidade_id)

async def atualizar_oportunidade(oportunidade: Oportunidade, novos_dados: dict):
    filtro = {"_id": oportunidade.id, "professor_id": oportunidade.professor_id}
    # Protege a relação entre datas em edições parciais concorrentes.
    if "data_evento" in novos_dados and "prazo_inscricao" not in novos_dados:
        filtro["prazo_inscricao"] = oportunidade.prazo_inscricao
    elif "prazo_inscricao" in novos_dados and "data_evento" not in novos_dados:
        filtro["data_evento"] = oportunidade.data_evento
    item = await Oportunidade.get_pymongo_collection().find_one_and_update(
        filtro, {"$set": novos_dados}, return_document=ReturnDocument.AFTER,
    )
    if item is not None:
        return Oportunidade.model_validate(item)
    if await buscar_oportunidade_por_id(oportunidade.id) is not None:
        raise ValueError("As datas foram alteradas por outro cliente. Atualize e tente novamente")
    return None


async def excluir_oportunidade(oportunidade: Oportunidade):
    await oportunidade.delete()
    return True
