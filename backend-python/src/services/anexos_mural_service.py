import hashlib
import re
import unicodedata
from datetime import datetime, timezone
from src.repositories import anexos_mural_repository as repo
from src.schemas.anexos_mural import AnexoArmazenado, MAX_BYTES, TIPOS
from src.services.academico_service import ErroAcademico
from src.services.mural_service import autorizada


def apresentar(anexo, publicacao_id, pode_excluir):
    return {**anexo.model_dump(exclude={"dados"}), "publicacao_id": publicacao_id, "pode_excluir": pode_excluir}


def permissoes(item, turma, usuario):
    autor = item.autor_id == str(usuario.id)
    return autor, autor or (usuario.papel == "Professor" and turma.professor_id == str(usuario.id))


async def listar(publicacao_id, usuario):
    item, turma = await autorizada(publicacao_id, usuario)
    adicionar, excluir = permissoes(item, turma, usuario)
    return dict(itens=[apresentar(a, item.id, excluir) for a in getattr(item, "anexos", [])], pode_adicionar=adicionar)


def validar_nome_tipo(nome, tipo):
    if (not 1 <= len(nome) <= 150 or nome != nome.strip() or any(c in nome for c in '/\\')
            or any(unicodedata.category(c).startswith("C") for c in nome)):
        raise ErroAcademico(422, "Use um nome de arquivo simples, com até 150 caracteres.")
    if tipo not in TIPOS or not nome.lower().endswith(TIPOS[tipo]):
        raise ErroAcademico(415, "Selecione um PDF, PNG ou JPEG com extensão correspondente ao tipo.")


def validar_conteudo(dados, tipo):
    if not dados:
        raise ErroAcademico(422, "O arquivo está vazio.")
    if len(dados) > MAX_BYTES:
        raise ErroAcademico(413, "O arquivo deve ter até 4 MiB.")
    valido = ((tipo == "application/pdf" and re.match(rb"%PDF-(1\.[0-7]|2\.0)", dados) and b"%%EOF" in dados[-1024:])
              or (tipo == "image/png" and dados.startswith(b"\x89PNG\r\n\x1a\n") and dados.endswith(b"\x00\x00\x00\x00IEND\xaeB`\x82"))
              or (tipo == "image/jpeg" and dados.startswith(b"\xff\xd8\xff") and dados.endswith(b"\xff\xd9")))
    if not valido:
        raise ErroAcademico(415, "O conteúdo não corresponde ao tipo de arquivo selecionado.")


async def autorizar_envio(publicacao_id, usuario):
    item, _ = await autorizada(publicacao_id, usuario)
    if item.autor_id != str(usuario.id):
        raise ErroAcademico(403, "Somente o autor pode adicionar anexos à publicação.")


async def enviar(publicacao_id, anexo_id, nome, tipo, dados, usuario):
    await autorizar_envio(publicacao_id, usuario)
    validar_nome_tipo(nome, tipo)
    validar_conteudo(dados, tipo)
    novo = AnexoArmazenado(id=anexo_id, nome=nome, tipo=tipo, tamanho=len(dados),
                          sha256=hashlib.sha256(dados).hexdigest(), criado_em=datetime.now(timezone.utc), dados=dados)
    inserido = await repo.adicionar(publicacao_id, str(usuario.id), novo)
    # Revalida a turma após o envio e resolve reenvios sem duplicar o arquivo.
    item, turma = await autorizada(publicacao_id, usuario)
    existente = next((a for a in getattr(item, "anexos", []) if a.id == anexo_id), None)
    if existente is None:
        raise ErroAcademico(409, "A publicação já possui três anexos. Atualize a lista.")
    if (existente.sha256, existente.nome, existente.tipo) != (novo.sha256, nome, tipo):
        raise ErroAcademico(409, "Este envio já foi usado para outro arquivo. Selecione o arquivo novamente.")
    return apresentar(existente, item.id, permissoes(item, turma, usuario)[1]), inserido


async def obter(publicacao_id, anexo_id, usuario):
    item, turma = await autorizada(publicacao_id, usuario)
    anexo = next((a for a in getattr(item, "anexos", []) if a.id == anexo_id), None)
    if anexo is None:
        raise ErroAcademico(404, "Anexo não encontrado.")
    return anexo


async def baixar(publicacao_id, anexo_id, usuario):
    anexo = await obter(publicacao_id, anexo_id, usuario)
    dados = await repo.baixar(publicacao_id, anexo_id)
    if dados is None:
        raise ErroAcademico(404, "Anexo não encontrado.")
    await autorizada(publicacao_id, usuario)
    return anexo, dados


async def excluir(publicacao_id, anexo_id, usuario):
    item, turma = await autorizada(publicacao_id, usuario)
    if not permissoes(item, turma, usuario)[1]:
        raise ErroAcademico(403, "Somente o autor ou o professor responsável pode remover anexos.")
    # DELETE é idempotente, inclusive após perda da resposta da primeira exclusão.
    await repo.excluir(publicacao_id, anexo_id)
