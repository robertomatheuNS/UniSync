package br.com.unisync.mobile.data

import br.com.unisync.mobile.data.remote.ClienteAcademico
import br.com.unisync.mobile.domain.mural.*
import org.json.JSONObject
import java.time.Instant

class RepositorioInteracoesMural(private val api: ClienteAcademico = ClienteAcademico()) : InteracoesMural {
    private fun caminho(id: String) = "/mural/${identificador(id)}"
    private fun identificador(id: String) = id.also { require(it.matches(Regex("[a-fA-F0-9]{24}"))) { "Identificador do mural inválido." } }
    override suspend fun listarComentarios(token: String, publicacaoId: String, antes: String?): PaginaComentarios {
        val query = "?limite=20" + (antes?.let { "&antes=${identificador(it)}" } ?: "")
        val json = JSONObject(api.executar("GET", caminho(publicacaoId) + "/comentarios$query", token))
        val itens = json.getJSONArray("itens").let { lista -> (0 until lista.length()).map { comentario(lista.getJSONObject(it), publicacaoId) } }
        val cursor = if (json.isNull("proxima_pagina")) null else identificador(json.getString("proxima_pagina"))
        require(cursor == null || (itens.isNotEmpty() && cursor != antes)) { "Página de comentários inválida." }
        return PaginaComentarios(itens, cursor)
    }
    override suspend fun comentar(token: String, publicacaoId: String, conteudo: String) = comentario(JSONObject(api.executar(
        "POST", caminho(publicacaoId) + "/comentarios", token, corpo(conteudo).toString())), publicacaoId)
    override suspend fun editarComentario(token: String, publicacaoId: String, id: String, versao: Int, conteudo: String): Comentario {
        require(versao > 0)
        return comentario(JSONObject(api.executar("PATCH", caminho(publicacaoId) + "/comentarios/${identificador(id)}", token,
            corpo(conteudo).put("versao", versao).toString())), publicacaoId).also { require(it.id == id) }
    }
    override suspend fun excluirComentario(token: String, publicacaoId: String, id: String, versao: Int) {
        require(versao > 0)
        api.executar("DELETE", caminho(publicacaoId) + "/comentarios/${identificador(id)}?versao=$versao", token)
    }
    override suspend fun consultarCurtida(token: String, publicacaoId: String) =
        curtida(JSONObject(api.executar("GET", caminho(publicacaoId) + "/curtida", token)), publicacaoId)
    override suspend fun curtir(token: String, publicacaoId: String, curtiu: Boolean): Curtida {
        val json = JSONObject(api.executar("PUT", caminho(publicacaoId) + "/curtida", token, JSONObject().put("curtiu", curtiu).toString()))
        return curtida(json, publicacaoId)
    }
    private fun curtida(json: JSONObject, publicacaoId: String): Curtida {
        require(json.getString("publicacao_id") == publicacaoId)
        return Curtida(publicacaoId, json.getInt("total_curtidas").also { require(it >= 0) }, json.getBoolean("curtiu"))
    }
    private fun corpo(conteudo: String) = JSONObject().put("conteudo", validarComentario(conteudo))
    private fun comentario(json: JSONObject, publicacaoId: String): Comentario {
        require(json.getString("publicacao_id") == publicacaoId) { "Comentário não pertence à publicação aberta." }
        return Comentario(identificador(json.getString("id")), publicacaoId, json.getString("autor_id"), json.getString("autor"),
            json.getString("papel_autor"), json.getString("conteudo"), Instant.parse(json.getString("criado_em")), Instant.parse(json.getString("atualizado_em")),
            json.getInt("versao").also { require(it > 0) }, json.getBoolean("pode_editar"), json.getBoolean("pode_excluir"))
    }
}
