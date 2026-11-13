from datetime import datetime, timezone

from beanie import PydanticObjectId

from src.models.academico import Aviso, Turma


async def criar_turma(dados: dict) -> Turma:
    return await Turma(**dados).insert()


async def listar_turmas(usuario_id: str, professor: bool) -> list[Turma]:
    campo = "professor_id" if professor else "alunos"
    return await Turma.find({"ativa": True, campo: usuario_id}).sort("nome").to_list()


async def buscar_turma(turma_id: PydanticObjectId) -> Turma | None:
    return await Turma.find_one({"_id": turma_id, "ativa": True})


async def entrar_turma(codigo: str, aluno_id: str) -> Turma | None:
    turma = await Turma.find_one({"codigo": codigo.upper(), "ativa": True})
    if turma is None:
        return None
    # A operação atômica evita duplicar a matrícula em pedidos simultâneos.
    await turma.update({"$addToSet": {"alunos": aluno_id}})
    return await buscar_turma(turma.id)


async def atualizar_turma(turma: Turma, dados: dict) -> Turma:
    await turma.set(dados)
    return turma


async def arquivar_turma(turma: Turma) -> None:
    await turma.set({"ativa": False})


async def criar_aviso(dados: dict) -> Aviso:
    return await Aviso(**dados).insert()


async def listar_avisos(turmas: list[PydanticObjectId]) -> list[Aviso]:
    return await Aviso.find({"turma_id": {"$in": turmas}}).sort("-criado_em").to_list()


async def buscar_aviso(aviso_id: PydanticObjectId) -> Aviso | None:
    return await Aviso.get(aviso_id)


async def atualizar_aviso(aviso: Aviso, dados: dict) -> Aviso:
    # Uma revisão deve reaparecer como não lida para os alunos.
    await aviso.set({**dados, "atualizado_em": datetime.now(timezone.utc), "lidos": []})
    return aviso


async def marcar_lido(aviso: Aviso, aluno_id: str) -> Aviso:
    await aviso.update({"$addToSet": {"lidos": aluno_id}})
    return await Aviso.get(aviso.id)


async def excluir_aviso(aviso: Aviso) -> None:
    await aviso.delete()
