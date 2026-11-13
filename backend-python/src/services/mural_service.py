from src.repositories import mural_repository as repo, academico_repository
from src.services.academico_service import turma_autorizada, ErroAcademico


def apresentar(item, turma, usuario, detalhe=False):
    proprio = item.autor_id == str(usuario.id)
    moderador = usuario.papel == "Professor" and turma.professor_id == str(usuario.id)
    resposta = dict(id=item.id, turma_id=turma.id, turma_nome=turma.nome, disciplina=turma.disciplina,
                    autor_id=item.autor_id, autor=item.autor, papel_autor=item.papel_autor,
                    titulo=item.titulo, resumo=item.conteudo[:240] + ("…" if len(item.conteudo) > 240 else ""),
                    criado_em=item.criado_em, atualizado_em=item.atualizado_em, versao=item.versao,
                    pode_editar=proprio, pode_excluir=proprio or moderador)
    if detalhe:
        resposta["conteudo"] = item.conteudo
    return resposta


async def criar(dados, usuario):
    turma = await turma_autorizada(dados.turma_id, usuario)
    item = await repo.criar({**dados.model_dump(), "autor_id": str(usuario.id), "autor": usuario.nome,
                             "papel_autor": usuario.papel})
    return apresentar(item, turma, usuario, detalhe=True)


async def listar(usuario, turma_id=None, antes=None, limite=20):
    turmas = [await turma_autorizada(turma_id, usuario)] if turma_id else await academico_repository.listar_turmas(
        str(usuario.id), usuario.papel == "Professor")
    por_id = {turma.id: turma for turma in turmas}
    itens = await repo.listar(list(por_id), antes, limite) if turmas else []
    pagina = itens[:limite]
    return dict(itens=[apresentar(item, por_id[item.turma_id], usuario) for item in pagina],
                proxima_pagina=pagina[-1].id if len(itens) > limite else None)


async def autorizada(identificador, usuario):
    item = await repo.buscar(identificador)
    if item is None:
        raise ErroAcademico(404, "Publicação não encontrada.")
    turma = await turma_autorizada(item.turma_id, usuario)
    return item, turma


async def buscar(identificador, usuario):
    item, turma = await autorizada(identificador, usuario)
    return apresentar(item, turma, usuario, detalhe=True)


async def atualizar(identificador, dados, usuario):
    item, turma = await autorizada(identificador, usuario)
    if item.autor_id != str(usuario.id):
        raise ErroAcademico(403, "Você só pode editar suas próprias publicações.")
    resultado = await repo.atualizar(item.id, str(usuario.id), dados.versao, dados.model_dump(exclude={"versao"}))
    if resultado is None:
        raise ErroAcademico(409, "A publicação foi alterada ou removida. Atualize o mural antes de editar.")
    return apresentar(resultado, turma, usuario, detalhe=True)


async def excluir(identificador, versao, usuario):
    item, turma = await autorizada(identificador, usuario)
    moderador = usuario.papel == "Professor" and turma.professor_id == str(usuario.id)
    if item.autor_id != str(usuario.id) and not moderador:
        raise ErroAcademico(403, "Somente o autor ou o professor responsável pode remover a publicação.")
    if not await repo.excluir(item.id, versao, str(usuario.id)):
        raise ErroAcademico(409, "A publicação foi alterada ou removida. Atualize o mural antes de excluir.")
