from src.models.oportunidade import Oportunidade

async def criar_oportunidade(oportunidade: Oportunidade):
    await oportunidade.insert()
    return oportunidade

async def listar_oportunidades():
    oportunidades = await Oportunidade.find_all().to_list()
    return oportunidades