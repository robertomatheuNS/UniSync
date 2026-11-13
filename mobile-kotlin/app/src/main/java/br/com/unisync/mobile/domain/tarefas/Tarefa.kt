package br.com.unisync.mobile.domain.tarefas

import kotlinx.coroutines.flow.Flow

enum class StatusTarefa(val rotulo: String) {
    A_FAZER("A fazer"), EM_ANDAMENTO("Fazendo"), CONCLUIDO("Concluído")
}

data class Tarefa(val id: String, val alunoId: String, val titulo: String, val status: StatusTarefa)

interface Tarefas {
    fun observar(alunoId: String): Flow<List<Tarefa>>
    suspend fun ultimaAtualizacao(alunoId: String): Long?
    suspend fun atualizar(alunoId: String, token: String)
    suspend fun criar(alunoId: String, token: String, titulo: String)
    suspend fun editar(alunoId: String, token: String, id: String, titulo: String, status: StatusTarefa)
    suspend fun excluir(alunoId: String, token: String, id: String)
}

fun validarTituloTarefa(titulo: String): String {
    val normalizado = titulo.trim()
    require(normalizado.length in 1..100) { "Informe um título entre 1 e 100 caracteres." }
    return normalizado
}
