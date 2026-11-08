from src.models.oportunidade import Oportunidade
from beanie import PydanticObjectId

async def criar_oportunidade(oportunidade: Oportunidade):
    await oportunidade.insert()
    return oportunidade

async def listar_oportunidades(tipo: str | None = None):
    filtro = {}

    if tipo is not None:
        filtro["tipo"] = tipo

    return await Oportunidade.find(filtro).to_list()

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