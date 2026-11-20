package br.com.unisync.mobile.data

import br.com.unisync.mobile.data.remote.ClienteAcademico
import br.com.unisync.mobile.domain.mural.*
import org.json.JSONObject
import java.time.Instant

class RepositorioMural(private val api: ClienteAcademico = ClienteAcademico()) : Mural {
    override suspend fun listar(token: String, turmaId: String?, antes: String?): PaginaPublicacoes {
        val query = buildList {
            add("limite=20")
            turmaId?.let { add("turma_id=${identificador(it)}") }
            antes?.let { add("antes=${identificador(it)}") }
        }.joinToString("&")
        val json = JSONObject(api.executar("GET", "/mural/listar?$query", token))
        val itens = json.getJSONArray("itens").let { lista -> (0 until lista.length()).map { converter(lista.getJSONObject(it)) } }
        val cursor = if (json.isNull("proxima_pagina")) null else identificador(json.getString("proxima_pagina"))
        require(cursor == null || (itens.isNotEmpty() && cursor != antes)) { "Página de publicações inválida." }
        require(turmaId == null || itens.all { it.turmaId == turmaId }) { "Resposta não pertence à turma selecionada." }
        return PaginaPublicacoes(itens, cursor)
    }
    override suspend fun buscar(token: String, id: String): Publicacao =
        converter(JSONObject(api.executar("GET", "/mural/buscar/${identificador(id)}", token))).also {
            require(it.id == id && it.conteudo != null) { "Detalhe de publicação inválido." }
        }
    override suspend fun publicar(token: String, dados: DadosPublicacao): Publicacao =
        converter(JSONObject(api.executar("POST", "/mural/criar", token, corpo(dados).put("turma_id", dados.turmaId).toString())))
    override suspend fun editar(token: String, id: String, versao: Int, dados: DadosPublicacao): Publicacao {
        require(versao > 0)
        return converter(JSONObject(api.executar("PATCH", "/mural/atualizar/${identificador(id)}", token, corpo(dados).put("versao", versao).toString())))
    }
    override suspend fun excluir(token: String, id: String, versao: Int) {
        require(versao > 0)
        api.executar("DELETE", "/mural/excluir/${identificador(id)}?versao=$versao", token)
    }
    private fun corpo(dados: DadosPublicacao) = dados.validar().let { JSONObject().put("titulo", it.titulo).put("conteudo", it.conteudo) }
    private fun identificador(id: String) = id.also { require(it.matches(Regex("[a-fA-F0-9]{24}"))) { "Identificador de publicação inválido." } }
    private fun converter(json: JSONObject) = Publicacao(identificador(json.getString("id")), identificador(json.getString("turma_id")),
        json.getString("turma_nome"), json.getString("disciplina"), json.getString("autor_id"), json.getString("autor"), json.getString("papel_autor"),
        json.getString("titulo"), json.getString("resumo"), Instant.parse(json.getString("criado_em")), Instant.parse(json.getString("atualizado_em")),
        json.getInt("versao").also { require(it > 0) }, json.getBoolean("pode_editar"), json.getBoolean("pode_excluir"),
        if (json.isNull("conteudo")) null else json.getString("conteudo"))
}
