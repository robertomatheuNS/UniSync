from src.models.oportunidade import Oportunidade
from src.schemas.oportunidade import OportunidadeCreate, OportunidadeUpdate
from src.repositories import oportunidade_repository
from beanie import PydanticObjectId


async def criar_oportunidade(oportunidade: OportunidadeCreate):
    dados_da_oportunidade = oportunidade.model_dump()

    nova_oportunidade = Oportunidade(**dados_da_oportunidade)

    return await oportunidade_repository.criar_oportunidade(nova_oportunidade)

async def listar_oportunidades():
    return await oportunidade_repository.listar_oportunidades()

async def buscar_oportunidade_por_id(id: PydanticObjectId):
    return await oportunidade_repository.buscar_oportunidade_por_id(id)

async def atualizar_oportunidade(oportunidade_id: PydanticObjectId, dados_atualizados: OportunidadeUpdate):
    
    dados_novos = dados_atualizados.model_dump(exclude_unset=True)

    if not dados_novos:
        raise ValueError("Nenhum campo foi informado")

    oportunidade = await oportunidade_repository.buscar_oportunidade_por_id(oportunidade_id)

    if oportunidade is None:
        return None

    return await oportunidade_repository.atualizar_oportunidade(oportunidade, dados_novos)

async def excluir_oportunidade(oportunidade_id: PydanticObjectId):
    oportunidade = await oportunidade_repository.buscar_oportunidade_por_id(oportunidade_id)

    if oportunidade is None:
        return False

    await oportunidade_repository.excluir_oportunidade(oportunidade)

    return True