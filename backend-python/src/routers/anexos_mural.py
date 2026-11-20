from urllib.parse import quote
from beanie import PydanticObjectId
from fastapi import APIRouter, Depends, Query, Request, Response
from src.core.security import UsuarioAutenticado, obter_usuario_atual
from src.schemas.anexos_mural import AnexoResponse, ListaAnexos, MAX_BYTES
from src.services import anexos_mural_service as service
from src.services.academico_service import ErroAcademico

router = APIRouter(prefix="/mural/{publicacao_id}/anexos", tags=["Anexos do mural"])


@router.get("", response_model=ListaAnexos)
async def listar(publicacao_id: PydanticObjectId, response: Response, usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    response.headers["Cache-Control"] = "private, no-store"
    return await service.listar(publicacao_id, usuario)


@router.put("/{anexo_id}", response_model=AnexoResponse, status_code=201,
            responses={200: {"model": AnexoResponse}, 413: {"description": "Arquivo maior que 4 MiB"},
                       415: {"description": "Tipo de arquivo não aceito"}},
            openapi_extra={"requestBody": {"required": True, "content": {
                tipo: {"schema": {"type": "string", "format": "binary"}}
                for tipo in ("application/pdf", "image/png", "image/jpeg")}}})
async def enviar(publicacao_id: PydanticObjectId, anexo_id: PydanticObjectId, request: Request, response: Response,
                 nome: str = Query(min_length=1, max_length=150), usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    await service.autorizar_envio(publicacao_id, usuario)
    tipo = request.headers.get("content-type", "").split(";")[0].strip().lower()
    service.validar_nome_tipo(nome, tipo)
    if request.headers.get("content-encoding", "identity").lower() != "identity":
        raise ErroAcademico(415, "Envie o arquivo sem compressão de transporte.")
    tamanho = request.headers.get("content-length")
    if tamanho is not None:
        if not tamanho.isdecimal():
            raise ErroAcademico(400, "Tamanho de arquivo inválido.")
        if int(tamanho) > MAX_BYTES:
            raise ErroAcademico(413, "O arquivo deve ter até 4 MiB.")
    conteudo = bytearray()
    async for parte in request.stream():
        if len(conteudo) + len(parte) > MAX_BYTES:
            raise ErroAcademico(413, "O arquivo deve ter até 4 MiB.")
        conteudo.extend(parte)
    if tamanho is not None and int(tamanho) != len(conteudo):
        raise ErroAcademico(400, "O envio do arquivo ficou incompleto.")
    item, criado = await service.enviar(publicacao_id, anexo_id, nome, tipo, bytes(conteudo), usuario)
    response.status_code = 201 if criado else 200
    response.headers["Cache-Control"] = "no-store"
    return item


@router.get("/{anexo_id}/conteudo", response_class=Response)
async def baixar(publicacao_id: PydanticObjectId, anexo_id: PydanticObjectId,
                 usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    anexo, dados = await service.baixar(publicacao_id, anexo_id, usuario)
    return Response(dados, media_type=anexo.tipo, headers={
        "Content-Disposition": "attachment; filename*=UTF-8''" + quote(anexo.nome, safe=""),
        "Cache-Control": "private, no-store", "X-Content-Type-Options": "nosniff",
        "Content-Security-Policy": "sandbox", "ETag": '"' + anexo.sha256 + '"',
    })


@router.delete("/{anexo_id}", status_code=204)
async def excluir(publicacao_id: PydanticObjectId, anexo_id: PydanticObjectId,
                  usuario: UsuarioAutenticado = Depends(obter_usuario_atual)):
    await service.excluir(publicacao_id, anexo_id, usuario)
