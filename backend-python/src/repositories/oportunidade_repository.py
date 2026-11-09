from src.models.oportunidade import Oportunidade
from beanie import PydanticObjectId
from datetime import datetime, timezone

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

async def atualizar_oportunidade(
    oportunidade: Oportunidade,
    novos_dados: dict
    
):
    for chave, valor in novos_dados.items():
        setattr(oportunidade, chave, valor)

    await oportunidade.save()

    return oportunidade

async def excluir_oportunidade(oportunidade: Oportunidade):
    await oportunidade.delete()
    return True