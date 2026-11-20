package br.com.unisync.mobile.data

import br.com.unisync.mobile.data.local.*
import br.com.unisync.mobile.data.remote.ClienteAcademico
import br.com.unisync.mobile.domain.lembretes.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class RepositorioLembretes(private val dao: LembreteDao, private val api: ClienteAcademico) : Lembretes {
    private val operacao = Mutex()
    suspend fun limparCache() = operacao.withLock { dao.limparTudo() }
    override fun observar(usuarioId: String) = dao.observar(usuarioId).map { itens -> itens.map {
        Lembrete(it.id, it.usuarioId, it.titulo, it.concluido, it.agendadoEm?.let(Instant::parse))
    } }
    override suspend fun ultimaAtualizacao(usuarioId: String) = dao.atualizadoEm(usuarioId)
    override suspend fun atualizar(usuarioId: String, token: String) = operacao.withLock {
        val itens = JSONArray(api.executar("GET", "/lembretes/listar", token))
        dao.substituir(usuarioId, (0 until itens.length()).map { local(itens.getJSONObject(it), usuarioId) }, System.currentTimeMillis())
    }
    override suspend fun criar(usuarioId: String, token: String, dados: DadosLembrete) = operacao.withLock {
        val json = corpo(dados)
        // Novos lembretes começam pendentes; o contrato Web antigo continua válido.
        val resposta = api.executar("POST", "/lembretes/criar", token, json.toString())
        dao.salvar(listOf(local(JSONObject(resposta), usuarioId)))
    }
    override suspend fun editar(usuarioId: String, token: String, id: String, dados: DadosLembrete) = operacao.withLock {
        val resposta = api.executar("PATCH", "/lembretes/atualizar/${identificador(id)}", token, corpo(dados).put("concluido", dados.concluido).toString())
        dao.salvar(listOf(local(JSONObject(resposta), usuarioId)))
    }
    override suspend fun concluir(usuarioId: String, token: String, id: String, concluido: Boolean) = operacao.withLock {
        val resposta = api.executar("PATCH", "/lembretes/atualizar/${identificador(id)}", token, JSONObject().put("concluido", concluido).toString())
        dao.salvar(listOf(local(JSONObject(resposta), usuarioId)))
    }
    override suspend fun excluir(usuarioId: String, token: String, id: String) = operacao.withLock {
        api.executar("DELETE", "/lembretes/excluir/${identificador(id)}", token); dao.excluir(usuarioId, id)
    }
    private fun corpo(dados: DadosLembrete): JSONObject {
        val valido = dados.validar()
        return JSONObject().put("titulo", valido.titulo).put("agendado_em", valido.agendadoEm?.toString() ?: JSONObject.NULL)
    }
    private fun identificador(id: String) = id.also { require(it.matches(Regex("[a-fA-F0-9]{24}"))) { "Identificador de lembrete inválido." } }
    private fun local(json: JSONObject, usuarioId: String): LembreteLocal {
        require(usuarioId.isNotBlank())
        val agendado = if (json.isNull("agendado_em")) null else Instant.parse(json.getString("agendado_em")).toString()
        return LembreteLocal(identificador(json.getString("id")), usuarioId, json.getString("titulo"), json.getBoolean("concluido"), agendado)
    }
}
