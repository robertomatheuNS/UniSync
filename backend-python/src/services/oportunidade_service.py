from src.models.oportunidade import Oportunidade
from src.schemas.oportunidade import OportunidadeCreate, OportunidadeUpdate
from src.repositories import oportunidade_repository
from beanie import PydanticObjectId


async def criar_oportunidade(oportunidade: OportunidadeCreate, professor_id: str):
    dados = oportunidade.model_dump()
    dados["professor_id"] = professor_id

    nova_oportunidade = Oportunidade(**dados)

    return await oportunidade_repository.criar_oportunidade(
        nova_oportunidade
    )


async def listar_oportunidades(
    tipo: str | None = None,
    professor: str | None = None,
    futuros: bool = False,
    ordenar: bool = False,
    limite: int | None = None,
):
    return await oportunidade_repository.listar_oportunidades(
        tipo=tipo,
        professor=professor,
        futuros=futuros,
        ordenar=ordenar,
        limite=limite,
    )

async def buscar_oportunidade_por_id(id: PydanticObjectId):
    return await oportunidade_repository.buscar_oportunidade_por_id(id)


async def atualizar_oportunidade(oportunidade_id: PydanticObjectId, dados_atualizados: OportunidadeUpdate, professor_id: str):
    dados_novos = dados_atualizados.model_dump(exclude_unset=True)

    if not dados_novos:
        raise ValueError("Nenhum campo foi informado")

    oportunidade = await oportunidade_repository.buscar_oportunidade_por_id(oportunidade_id)

    if oportunidade is None:
        return None

    if oportunidade.professor_id != professor_id:
        raise PermissionError(
            "Você não tem permissão para alterar esta oportunidade"
        )

    return await oportunidade_repository.atualizar_oportunidade(oportunidade, dados_novos,)



async def excluir_oportunidade(oportunidade_id: PydanticObjectId, professor_id: str):
    oportunidade = await oportunidade_repository.buscar_oportunidade_por_id(oportunidade_id)

    if oportunidade is None:
        return False

    if oportunidade.professor_id != professor_id:
        raise PermissionError(
            "Você não tem permissão para excluir esta oportunidade"
        )

    await oportunidade_repository.excluir_oportunidade(
        oportunidade
    )

    return True