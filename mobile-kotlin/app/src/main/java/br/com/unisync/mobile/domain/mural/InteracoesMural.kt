package br.com.unisync.mobile.domain.mural

import java.time.Instant

data class Comentario(val id: String, val publicacaoId: String, val autorId: String, val autor: String,
    val papelAutor: String, val conteudo: String, val criadoEm: Instant, val atualizadoEm: Instant,
    val versao: Int, val podeEditar: Boolean, val podeExcluir: Boolean)
data class PaginaComentarios(val itens: List<Comentario>, val proximaPagina: String?)
data class Curtida(val publicacaoId: String, val total: Int, val curtiu: Boolean)
fun validarComentario(conteudo: String) = conteudo.trim().also {
    require(it.length in 1..2000) { "O comentário deve ter entre 1 e 2000 caracteres." }
}
interface InteracoesMural {
    suspend fun listarComentarios(token: String, publicacaoId: String, antes: String?): PaginaComentarios
    suspend fun comentar(token: String, publicacaoId: String, conteudo: String): Comentario
    suspend fun editarComentario(token: String, publicacaoId: String, id: String, versao: Int, conteudo: String): Comentario
    suspend fun excluirComentario(token: String, publicacaoId: String, id: String, versao: Int)
    suspend fun consultarCurtida(token: String, publicacaoId: String): Curtida
    suspend fun curtir(token: String, publicacaoId: String, curtiu: Boolean): Curtida
}
