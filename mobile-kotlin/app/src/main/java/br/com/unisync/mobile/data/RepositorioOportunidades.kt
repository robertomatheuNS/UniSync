package br.com.unisync.mobile.data

import br.com.unisync.mobile.data.local.*
import br.com.unisync.mobile.data.remote.ClienteAcademico
import br.com.unisync.mobile.domain.oportunidades.Oportunidade
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray

class RepositorioOportunidades(private val dao: OportunidadeDao, private val api: ClienteAcademico) {
    private val operacao = Mutex()
    val itens = dao.observar().map { lista -> lista.map {
        Oportunidade(it.id, it.titulo, it.professor, it.materia, it.descricao, it.tipo, it.dataEvento)
    } }
    suspend fun atualizar(token: String) = operacao.withLock {
        // O contrato atual oferece limite, sem cursor/página. A UI informa esse recorte.
        val resposta = JSONArray(api.executar("GET", "/oportunidades/listar?ordenar=true&limite=100", token))
        val itens = (0 until resposta.length()).map { indice ->
            val json = resposta.getJSONObject(indice)
            OportunidadeLocal(json.getString("id"), json.getString("titulo"), json.getString("professor"),
                json.getString("materia"), if (json.isNull("descricao")) null else json.getString("descricao"),
                json.getString("tipo"), json.getString("data_evento"))
        }
        dao.substituir(itens)
    }
}
