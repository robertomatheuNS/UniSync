package br.com.unisync.mobile.data

import br.com.unisync.mobile.data.remote.ClienteAcademico
import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.academico.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject

class RepositorioAcademico(private val api: ClienteAcademico = ClienteAcademico()) : Academico {
    override suspend fun carregar(token: String): ConteudoAcademico = coroutineScope {
        val turmas = async { lista(api.executar("GET", "/turmas/listar", token), ::turma) }
        val avisos = async { lista(api.executar("GET", "/avisos/listar", token), ::aviso) }
        ConteudoAcademico(turmas.await(), avisos.await())
    }

    override suspend fun criarTurma(token: String, dados: DadosTurma): Turma =
        turma(JSONObject(api.executar("POST", "/turmas/criar", token, corpo(dados).toString())))

    override suspend fun editarTurma(token: String, id: String, dados: DadosTurma): Turma =
        turma(JSONObject(api.executar("PATCH", "/turmas/atualizar/${identificador(id)}", token, corpo(dados).toString())))

    override suspend fun entrarTurma(token: String, codigo: String): Turma = try {
        turma(JSONObject(api.executar("POST", "/turmas/entrar", token,
            JSONObject().put("codigo", validarCodigoTurma(codigo)).toString())))
    } catch (erro: FalhaApi) {
        if (erro.status == 404) throw FalhaApi(404, "Código de turma não encontrado. Confira com o professor.")
        throw erro
    }

    override suspend fun arquivarTurma(token: String, id: String) {
        api.executar("POST", "/turmas/arquivar/${identificador(id)}", token)
    }

    override suspend fun publicarAviso(token: String, dados: DadosAviso): Aviso =
        aviso(JSONObject(api.executar("POST", "/avisos/criar", token,
            corpo(dados).put("turma_id", identificador(dados.turmaId)).toString())))

    override suspend fun editarAviso(token: String, id: String, dados: DadosAviso): Aviso =
        aviso(JSONObject(api.executar("PATCH", "/avisos/atualizar/${identificador(id)}", token, corpo(dados).toString())))

    override suspend fun excluirAviso(token: String, id: String) {
        api.executar("DELETE", "/avisos/excluir/${identificador(id)}", token)
    }

    override suspend fun marcarLido(token: String, id: String): Aviso =
        aviso(JSONObject(api.executar("PUT", "/avisos/ler/${identificador(id)}", token)))

    private fun corpo(dados: DadosTurma): JSONObject = dados.validar().let {
        JSONObject().put("nome", it.nome).put("disciplina", it.disciplina).put("periodo", it.periodo)
    }
    private fun corpo(dados: DadosAviso): JSONObject = dados.validar().let {
        JSONObject().put("titulo", it.titulo).put("conteudo", it.conteudo)
    }
    private fun identificador(id: String): String = id.also {
        require(it.matches(Regex("[a-fA-F0-9]{24}"))) { "Identificador acadêmico inválido." }
    }
    private fun <T> lista(resposta: String, converter: (JSONObject) -> T): List<T> = JSONArray(resposta).let { json ->
        (0 until json.length()).map { converter(json.getJSONObject(it)) }
    }
    private fun turma(json: JSONObject) = Turma(identificador(json.getString("id")), json.getString("nome"),
        json.getString("disciplina"), json.getString("periodo"), json.getString("professor_id"),
        json.getString("professor"), if (json.isNull("codigo")) null else json.getString("codigo"), json.getInt("total_alunos"))
    private fun aviso(json: JSONObject) = Aviso(identificador(json.getString("id")), identificador(json.getString("turma_id")),
        json.getString("turma_nome"), json.getString("disciplina"), json.getString("professor_id"),
        json.getString("professor"), json.getString("titulo"), json.getString("conteudo"),
        json.getString("criado_em"), json.getString("atualizado_em"), json.getBoolean("lido"))
}
