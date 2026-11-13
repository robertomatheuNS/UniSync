package br.com.unisync.mobile.data

import br.com.unisync.mobile.data.local.*
import br.com.unisync.mobile.data.remote.ClienteAcademico
import br.com.unisync.mobile.domain.oportunidades.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

class RepositorioOportunidades(private val dao: OportunidadeDao, private val api: ClienteAcademico) : Oportunidades {
    private val operacao = Mutex()
    override val itens = dao.observar().map { lista -> lista.map {
        Oportunidade(it.id, it.titulo, it.professor, it.materia, it.descricao, it.tipo, it.dataEvento, it.professorId)
    } }
    override suspend fun atualizar(token: String) = operacao.withLock {
        // O contrato atual oferece limite, sem cursor/página. A UI informa esse recorte.
        val resposta = JSONArray(api.executar("GET", "/oportunidades/listar?ordenar=true&limite=100", token))
        val itens = (0 until resposta.length()).map { indice ->
            val json = resposta.getJSONObject(indice)
            local(json)
        }
        dao.substituir(itens)
    }
    override suspend fun publicar(token: String, nomeProfessor: String, dados: DadosOportunidade) = operacao.withLock {
        val corpo = corpo(dados).put("professor", nomeProfessor)
        val resposta = api.executar("POST", "/oportunidades/criar", token, corpo.toString())
        dao.salvar(listOf(local(JSONObject(resposta))))
    }
    override suspend fun editar(token: String, id: String, dados: DadosOportunidade) = operacao.withLock {
        val resposta = api.executar("PATCH", "/oportunidades/atualizar/${identificador(id)}", token, corpo(dados).toString())
        dao.salvar(listOf(local(JSONObject(resposta))))
    }
    override suspend fun excluir(token: String, id: String) = operacao.withLock {
        api.executar("DELETE", "/oportunidades/excluir/${identificador(id)}", token)
        dao.excluir(id)
    }
    private fun corpo(dados: DadosOportunidade): JSONObject {
        val validado = dados.validar()
        return JSONObject().put("titulo", validado.titulo).put("materia", validado.materia)
            .put("tipo", validado.tipo).put("descricao", validado.descricao)
            .put("data_evento", validado.dataParaApi())
    }
    private fun identificador(id: String): String {
        require(id.matches(Regex("[a-fA-F0-9]{24}"))) { "Identificador de oportunidade inválido." }
        return id
    }
    private fun local(json: JSONObject) = OportunidadeLocal(identificador(json.getString("id")),
        json.getString("titulo"), json.getString("professor"), json.getString("materia"),
        if (json.isNull("descricao")) null else json.getString("descricao"), json.getString("tipo"),
        json.getString("data_evento"), if (json.isNull("professor_id")) null else json.getString("professor_id"))
}
