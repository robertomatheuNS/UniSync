package br.com.unisync.mobile.domain.mural

import java.time.Instant

data class Publicacao(val id: String, val turmaId: String, val turmaNome: String, val disciplina: String,
    val autorId: String, val autor: String, val papelAutor: String, val titulo: String, val resumo: String,
    val criadoEm: Instant, val atualizadoEm: Instant, val versao: Int, val podeEditar: Boolean, val podeExcluir: Boolean,
    val conteudo: String? = null)

data class DadosPublicacao(val turmaId: String, val titulo: String, val conteudo: String) {
    fun validar(): DadosPublicacao {
        require(turmaId.matches(Regex("[a-fA-F0-9]{24}"))) { "Selecione uma turma." }
        require(titulo.trim().length in 3..100) { "O título deve ter entre 3 e 100 caracteres." }
        require(conteudo.trim().length in 1..5000) { "O conteúdo deve ter entre 1 e 5000 caracteres." }
        return copy(titulo = titulo.trim(), conteudo = conteudo.trim())
    }
}

data class PaginaPublicacoes(val itens: List<Publicacao>, val proximaPagina: String?)
interface Mural {
    suspend fun listar(token: String, turmaId: String?, antes: String?): PaginaPublicacoes
    suspend fun buscar(token: String, id: String): Publicacao
    suspend fun publicar(token: String, dados: DadosPublicacao): Publicacao
    suspend fun editar(token: String, id: String, versao: Int, dados: DadosPublicacao): Publicacao
    suspend fun excluir(token: String, id: String, versao: Int)
}
