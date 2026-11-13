package br.com.unisync.mobile.data

import br.com.unisync.mobile.data.local.*
import br.com.unisync.mobile.data.remote.ClienteAcademico
import br.com.unisync.mobile.domain.tarefas.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

class RepositorioTarefas(private val dao: TarefaDao, private val api: ClienteAcademico) : Tarefas {
    private val operacao = Mutex()
    suspend fun limparCache() = operacao.withLock { dao.limparTudo() }
    override fun observar(alunoId: String) = dao.observar(alunoId).map { itens ->
        itens.map { Tarefa(it.id, it.alunoId, it.titulo, StatusTarefa.valueOf(it.status)) }
    }
    override suspend fun ultimaAtualizacao(alunoId: String) = dao.atualizadoEm(alunoId)
    override suspend fun atualizar(alunoId: String, token: String) = operacao.withLock {
        val resposta = JSONArray(api.executar("GET", "/tarefas/listar", token))
        val itens = (0 until resposta.length()).map { local(resposta.getJSONObject(it), alunoId) }
        dao.substituir(alunoId, itens, System.currentTimeMillis())
    }
    override suspend fun criar(alunoId: String, token: String, titulo: String) = operacao.withLock {
        val corpo = JSONObject().put("titulo", validarTituloTarefa(titulo))
        val resultado = api.executar("POST", "/tarefas/criar", token, corpo.toString())
        dao.salvar(listOf(local(JSONObject(resultado), alunoId)))
    }
    override suspend fun editar(alunoId: String, token: String, id: String, titulo: String, status: StatusTarefa) = operacao.withLock {
        val corpo = JSONObject().put("titulo", validarTituloTarefa(titulo)).put("status", status.name)
        val resultado = api.executar("PATCH", "/tarefas/atualizar/${identificador(id)}", token, corpo.toString())
        dao.salvar(listOf(local(JSONObject(resultado), alunoId)))
    }
    override suspend fun excluir(alunoId: String, token: String, id: String) = operacao.withLock {
        api.executar("DELETE", "/tarefas/excluir/${identificador(id)}", token)
        dao.excluir(alunoId, id)
    }
    private fun identificador(id: String): String {
        require(id.matches(Regex("[a-fA-F0-9]{24}"))) { "Identificador de tarefa inválido." }
        return id
    }
    private fun local(json: JSONObject, alunoId: String): TarefaLocal {
        require(alunoId.isNotBlank() && json.getString("aluno_id") == alunoId) { "Resposta de tarefa não pertence ao usuário conectado." }
        return TarefaLocal(identificador(json.getString("id")), alunoId, json.getString("titulo"),
            StatusTarefa.valueOf(json.getString("status")).name)
    }
}
