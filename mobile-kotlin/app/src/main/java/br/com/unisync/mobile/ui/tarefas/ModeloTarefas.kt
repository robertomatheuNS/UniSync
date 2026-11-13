package br.com.unisync.mobile.ui.tarefas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.unisync.mobile.data.SessaoAutenticada
import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.tarefas.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EstadoTarefas(
    val itens: List<Tarefa> = emptyList(), val lendo: Boolean = true,
    val atualizando: Boolean = false, val salvando: Boolean = false,
    val atualizadoEm: Long? = null, val erro: String? = null,
    val confirmacao: Int = 0
)

class ModeloTarefas(private val sessao: SessaoAutenticada, private val repositorio: Tarefas,
    private val aoExpirar: () -> Unit) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoTarefas())
    val estado = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            repositorio.observar(sessao.usuarioId).catch {
                _estado.update { it.copy(lendo = false, erro = "Não foi possível ler as tarefas salvas.") }
            }.collect { itens -> _estado.update { it.copy(itens = itens, lendo = false) } }
        }
        atualizar()
    }

    fun atualizar() {
        if (_estado.value.atualizando || _estado.value.salvando) return
        _estado.update { it.copy(atualizando = true, erro = null) }
        viewModelScope.launch {
            try {
                val anterior = repositorio.ultimaAtualizacao(sessao.usuarioId)
                _estado.update { it.copy(atualizadoEm = anterior) }
                repositorio.atualizar(sessao.usuarioId, sessao.token)
                val atualizado = repositorio.ultimaAtualizacao(sessao.usuarioId)
                _estado.update { it.copy(atualizadoEm = atualizado) }
            } catch (erro: CancellationException) { throw erro
            } catch (erro: Exception) { falha(erro)
            } finally { _estado.update { it.copy(atualizando = false) } }
        }
    }

    fun salvar(id: String?, titulo: String, status: StatusTarefa) = escrever {
        val validado = validarTituloTarefa(titulo)
        if (id == null) repositorio.criar(sessao.usuarioId, sessao.token, validado)
        else repositorio.editar(sessao.usuarioId, sessao.token, id, validado, status)
    }
    fun excluir(tarefa: Tarefa) = escrever { repositorio.excluir(sessao.usuarioId, sessao.token, tarefa.id) }

    private fun escrever(acao: suspend () -> Unit) {
        if (_estado.value.salvando || _estado.value.atualizando) return
        _estado.update { it.copy(salvando = true, erro = null) }
        viewModelScope.launch {
            try {
                acao()
                _estado.update { it.copy(confirmacao = it.confirmacao + 1) }
            } catch (erro: CancellationException) { throw erro
            } catch (erro: Exception) { falha(erro)
            } finally { _estado.update { it.copy(salvando = false) } }
        }
    }
    private fun falha(erro: Exception) {
        if (erro is FalhaApi && erro.status == 401) { aoExpirar(); return }
        val mensagem = when (erro) {
            is FalhaApi -> erro.message!!
            is IllegalArgumentException -> erro.message ?: "Confira os dados informados."
            else -> "Não foi possível atualizar. Os dados salvos continuam disponíveis. Confira a conexão e tente novamente."
        }
        _estado.update { it.copy(erro = mensagem) }
    }
    fun fechar() { viewModelScope.cancel() }
}
