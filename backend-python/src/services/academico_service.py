import secrets

from beanie import PydanticObjectId

from src.core.security import UsuarioAutenticado
from src.repositories import academico_repository as repo
from src.schemas.academico import AvisoCreate, AvisoDados, EntradaTurma, TurmaDados


class ErroAcademico(Exception):
    def __init__(self, status: int, mensagem: str):
        self.status = status
        self.mensagem = mensagem


def exigir_professor(usuario: UsuarioAutenticado):
    if usuario.papel != "Professor":
        raise ErroAcademico(403, "Ação permitida apenas para professores.")


def apresentar_turma(turma, usuario):
    return dict(id=turma.id, nome=turma.nome, disciplina=turma.disciplina,
                periodo=turma.periodo, professor_id=turma.professor_id,
                professor=turma.professor, total_alunos=len(turma.alunos),
                codigo=turma.codigo if turma.professor_id == str(usuario.id) else None)


def apresentar_aviso(aviso, turma, usuario):
    return dict(id=aviso.id, turma_id=turma.id, turma_nome=turma.nome,
                disciplina=turma.disciplina, professor_id=aviso.professor_id,
                professor=turma.professor, titulo=aviso.titulo, conteudo=aviso.conteudo,
                criado_em=aviso.criado_em, atualizado_em=aviso.atualizado_em,
                lido=usuario.papel == "Professor" or str(usuario.id) in aviso.lidos)


async def turma_autorizada(turma_id: PydanticObjectId, usuario, escrita=False):
    turma = await repo.buscar_turma(turma_id)
    if turma is None:
        raise ErroAcademico(404, "Turma não encontrada.")
    dono = usuario.papel == "Professor" and turma.professor_id == str(usuario.id)
    matriculado = usuario.papel == "Aluno" and str(usuario.id) in turma.alunos
    if not dono and (escrita or not matriculado):
        raise ErroAcademico(403, "Você não tem acesso a esta turma.")
    return turma


async def criar_turma(dados: TurmaDados, usuario):
    exigir_professor(usuario)
    turma = await repo.criar_turma({**dados.model_dump(), "professor_id": str(usuario.id),
                                  "professor": usuario.nome, "codigo": secrets.token_hex(6).upper()})
    return apresentar_turma(turma, usuario)


async def listar_turmas(usuario):
    turmas = await repo.listar_turmas(str(usuario.id), usuario.papel == "Professor")
    return [apresentar_turma(turma, usuario) for turma in turmas]


async def entrar_turma(dados: EntradaTurma, usuario):
    if usuario.papel != "Aluno":
        raise ErroAcademico(403, "Somente alunos podem entrar por código.")
    turma = await repo.entrar_turma(dados.codigo, str(usuario.id))
    if turma is None:
        raise ErroAcademico(404, "Código de turma não encontrado.")
    return apresentar_turma(turma, usuario)


async def atualizar_turma(turma_id, dados: TurmaDados, usuario):
    turma = await turma_autorizada(turma_id, usuario, escrita=True)
    return apresentar_turma(await repo.atualizar_turma(turma, dados.model_dump()), usuario)


async def arquivar_turma(turma_id, usuario):
    turma = await turma_autorizada(turma_id, usuario, escrita=True)
    await repo.arquivar_turma(turma)


async def criar_aviso(dados: AvisoCreate, usuario):
    turma = await turma_autorizada(dados.turma_id, usuario, escrita=True)
    aviso = await repo.criar_aviso({**dados.model_dump(), "professor_id": str(usuario.id)})
    return apresentar_aviso(aviso, turma, usuario)


async def listar_avisos(usuario, turma_id=None):
    if turma_id is not None:
        turmas = [await turma_autorizada(turma_id, usuario)]
    else:
        turmas = await repo.listar_turmas(str(usuario.id), usuario.papel == "Professor")
    por_id = {turma.id: turma for turma in turmas}
    avisos = await repo.listar_avisos(list(por_id)) if turmas else []
    return [apresentar_aviso(aviso, por_id[aviso.turma_id], usuario) for aviso in avisos]


async def aviso_autorizado(aviso_id, usuario, escrita=False):
    aviso = await repo.buscar_aviso(aviso_id)
    if aviso is None:
        raise ErroAcademico(404, "Aviso não encontrado.")
    turma = await turma_autorizada(aviso.turma_id, usuario, escrita=escrita)
    if escrita and aviso.professor_id != str(usuario.id):
        raise ErroAcademico(403, "Você só pode alterar seus próprios avisos.")
    return aviso, turma


async def atualizar_aviso(aviso_id, dados: AvisoDados, usuario):
    aviso, turma = await aviso_autorizado(aviso_id, usuario, escrita=True)
    return apresentar_aviso(await repo.atualizar_aviso(aviso, dados.model_dump()), turma, usuario)


async def marcar_lido(aviso_id, usuario):
    if usuario.papel != "Aluno":
        raise ErroAcademico(403, "A confirmação de leitura é exclusiva do aluno.")
    aviso, turma = await aviso_autorizado(aviso_id, usuario)
    return apresentar_aviso(await repo.marcar_lido(aviso, str(usuario.id)), turma, usuario)


async def excluir_aviso(aviso_id, usuario):
    aviso, _ = await aviso_autorizado(aviso_id, usuario, escrita=True)
    await repo.excluir_aviso(aviso)
