from beanie import PydanticObjectId
from fastapi import APIRouter, HTTPException
from src.schemas.oportunidade import OportunidadeCreate, OportunidadeResponse, OportunidadeUpdate
from fastapi import APIRouter, HTTPException, Query
from src.services import oportunidade_service
from fastapi import Depends
from src.core.security import (UsuarioAutenticado, exigir_professor,)

router = APIRouter(
    prefix="/oportunidades",
    tags=["Oportunidades"]
)


@router.post("/criar", status_code=201,response_model=OportunidadeResponse)
async def criar_oportunidade(oportunidade: OportunidadeCreate, usuario: UsuarioAutenticado = Depends(exigir_professor)):
    return await oportunidade_service.criar_oportunidade(oportunidade, professor_id=str(usuario.id))



@router.get("/listar", response_model=list[OportunidadeResponse])
async def listar_oportunidades(
    tipo: str | None = None,
    professor: str | None = None,
    futuros: bool = False,
    ordenar: bool = False,
    limite: int | None = Query(default=None, ge=1, le=100),
):
    return await oportunidade_service.listar_oportunidades(
        tipo=tipo,
        professor=professor,
        futuros=futuros,
        ordenar=ordenar,
        limite=limite,
    )


@router.get("/buscar/{oportunidade_id}", response_model=OportunidadeResponse)
async def buscar_oportunidade_por_id(oportunidade_id: PydanticObjectId):
    resultado = await oportunidade_service.buscar_oportunidade_por_id(oportunidade_id)

    if resultado is None:
        raise HTTPException(status_code=404, detail="Oportunidade não encontrada")

    return resultado


@router.patch("/atualizar/{oportunidade_id}", response_model=OportunidadeResponse)
async def atualizar_oportunidade(oportunidade_id: PydanticObjectId, dados_atualizados: OportunidadeUpdate, usuario: UsuarioAutenticado = Depends(exigir_professor)):
    try:
        resultado = await oportunidade_service.atualizar_oportunidade(
            oportunidade_id,
            dados_atualizados,
            professor_id=str(usuario.id),
        )

        if resultado is None:
            raise HTTPException(
                status_code=404,
                detail="Oportunidade não encontrada",
            )

        return resultado

    except ValueError as erro:
        raise HTTPException(
            status_code=400,
            detail=str(erro),
        ) from erro

    except PermissionError as erro:
        raise HTTPException(
            status_code=403,
            detail=str(erro),
        ) from erro



@router.delete("/excluir/{oportunidade_id}",status_code=204)
async def excluir_oportunidade(oportunidade_id: PydanticObjectId, usuario: UsuarioAutenticado = Depends(exigir_professor)):
    try:
        resultado = await oportunidade_service.excluir_oportunidade(oportunidade_id,professor_id=str(usuario.id),)

        if not resultado:
            raise HTTPException(
                status_code=404,
                detail="Oportunidade não encontrada",
            )

    except PermissionError as erro:
        raise HTTPException(
            status_code=403,
            detail=str(erro),
        ) from erro
