package br.com.unisync.mobile.domain.tarefas

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

enum class StatusTarefa(val rotulo: String) {
    A_FAZER("A fazer"), EM_ANDAMENTO("Fazendo"), CONCLUIDO("Concluído")
}
enum class PrioridadeTarefa(val rotulo: String) { BAIXA("Baixa"), MEDIA("Média"), ALTA("Alta") }

data class Tarefa(val id: String, val alunoId: String, val titulo: String, val status: StatusTarefa,
    val prioridade: PrioridadeTarefa = PrioridadeTarefa.MEDIA, val prazo: LocalDate? = null,
    val turmaId: String? = null, val disciplina: String? = null) {
    fun atrasada(hoje: LocalDate = LocalDate.now()) = status != StatusTarefa.CONCLUIDO && prazo?.isBefore(hoje) == true
    fun dados() = DadosTarefa(titulo, status, prioridade, prazo, turmaId)
}

data class DadosTarefa(val titulo: String, val status: StatusTarefa = StatusTarefa.A_FAZER,
    val prioridade: PrioridadeTarefa = PrioridadeTarefa.MEDIA, val prazo: LocalDate? = null, val turmaId: String? = null) {
    fun validar(): DadosTarefa {
        require(prazo == null || prazo.year in 1..9999) { "Informe um prazo entre os anos 0001 e 9999." }
        require(turmaId == null || turmaId.matches(Regex("[a-fA-F0-9]{24}"))) { "Selecione uma turma válida." }
        return copy(titulo = validarTituloTarefa(titulo))
    }
}

val formatoPrazo: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
fun lerPrazo(texto: String): LocalDate? = if (texto.isBlank()) null else {
    require(texto.trim().matches(Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}"))) { "Informe uma data válida no formato dd/mm/aaaa." }
    try { LocalDate.parse(texto.trim(), formatoPrazo).also { require(it.year in 1..9999) } }
    catch (_: Exception) { throw IllegalArgumentException("Informe uma data válida no formato dd/mm/aaaa.") }
}

enum class OrdemTarefa(val rotulo: String) { PRAZO("Prazo"), PRIORIDADE("Prioridade") }
fun List<Tarefa>.ordenadas(ordem: OrdemTarefa = OrdemTarefa.PRAZO): List<Tarefa> {
    val porPrazo = compareBy<Tarefa> { it.prazo ?: LocalDate.MAX }
    val porPrioridade = compareByDescending<Tarefa> { it.prioridade.ordinal }
    return sortedWith((if (ordem == OrdemTarefa.PRAZO) porPrazo.then(porPrioridade) else porPrioridade.then(porPrazo))
        .thenBy { it.titulo.lowercase() }.thenBy { it.id })
}

interface Tarefas {
    fun observar(alunoId: String): Flow<List<Tarefa>>
    suspend fun ultimaAtualizacao(alunoId: String): Long?
    suspend fun atualizar(alunoId: String, token: String)
    suspend fun criar(alunoId: String, token: String, dados: DadosTarefa)
    suspend fun editar(alunoId: String, token: String, id: String, dados: DadosTarefa)
    suspend fun excluir(alunoId: String, token: String, id: String)
}

fun validarTituloTarefa(titulo: String): String {
    val normalizado = titulo.trim()
    require(normalizado.length in 1..100) { "Informe um título entre 1 e 100 caracteres." }
    return normalizado
}
