from src.repositories import interacoes_mural_repository as repo, mural_repository
from src.services.mural_service import autorizada
from src.services.academico_service import ErroAcademico


def apresentar(item, turma, usuario):
    proprio = item.autor_id == str(usuario.id)
    moderador = usuario.papel == "Professor" and turma.professor_id == str(usuario.id)
    return dict(id=item.id, publicacao_id=item.publicacao_id, autor_id=item.autor_id, autor=item.autor,
                papel_autor=item.papel_autor, conteudo=item.conteudo, criado_em=item.criado_em,
                atualizado_em=item.atualizado_em, versao=item.versao, pode_editar=proprio, pode_excluir=proprio or moderador)


def apresentar_curtida(item, usuario):
    curtidas = getattr(item, "curtidas", [])
    return dict(publicacao_id=item.id, total_curtidas=len(curtidas), curtiu=str(usuario.id) in curtidas)


async def consultar_curtida(publicacao_id, usuario):
    item, _ = await autorizada(publicacao_id, usuario)
    return apresentar_curtida(item, usuario)


async def curtir(publicacao_id, dados, usuario):
    await autorizada(publicacao_id, usuario)
    item = await repo.curtir(publicacao_id, str(usuario.id), dados.curtiu)
    if item is None:
        raise ErroAcademico(404, "Publicação não encontrada.")
    return apresentar_curtida(item, usuario)


async def criar(publicacao_id, dados, usuario):
    _, turma = await autorizada(publicacao_id, usuario)
    item = await repo.criar({**dados.model_dump(), "publicacao_id": publicacao_id, "autor_id": str(usuario.id),
                             "autor": usuario.nome, "papel_autor": usuario.papel})
    # Uma remoção concorrente da publicação não deve confirmar um comentário invisível.
    if await mural_repository.buscar(publicacao_id) is None:
        await repo.excluir(item.id, publicacao_id, item.versao, str(usuario.id))
        raise ErroAcademico(404, "Publicação não encontrada.")
    return apresentar(item, turma, usuario)


async def listar(publicacao_id, usuario, antes=None, limite=20):
    _, turma = await autorizada(publicacao_id, usuario)
    itens = await repo.listar(publicacao_id, antes, limite)
    pagina = itens[:limite]
    return dict(itens=[apresentar(item, turma, usuario) for item in pagina],
                proxima_pagina=pagina[-1].id if len(itens) > limite else None)


async def comentario_autorizado(publicacao_id, comentario_id, usuario):
    _, turma = await autorizada(publicacao_id, usuario)
    item = await repo.buscar(comentario_id)
    if item is None or item.publicacao_id != publicacao_id:
        raise ErroAcademico(404, "Comentário não encontrado.")
    return item, turma


async def atualizar(publicacao_id, comentario_id, dados, usuario):
    item, turma = await comentario_autorizado(publicacao_id, comentario_id, usuario)
    if item.autor_id != str(usuario.id):
        raise ErroAcademico(403, "Você só pode editar seus próprios comentários.")
    resultado = await repo.atualizar(item.id, publicacao_id, str(usuario.id), dados.versao, dados.conteudo)
    if resultado is None:
        raise ErroAcademico(409, "O comentário mudou. Atualize a conversa antes de editar.")
    return apresentar(resultado, turma, usuario)


async def excluir(publicacao_id, comentario_id, versao, usuario):
    item, turma = await comentario_autorizado(publicacao_id, comentario_id, usuario)
    moderador = usuario.papel == "Professor" and turma.professor_id == str(usuario.id)
    if item.autor_id != str(usuario.id) and not moderador:
        raise ErroAcademico(403, "Somente o autor ou o professor responsável pode remover o comentário.")
    if not await repo.excluir(item.id, publicacao_id, versao, str(usuario.id)):
        raise ErroAcademico(409, "O comentário mudou. Atualize a conversa antes de excluir.")
