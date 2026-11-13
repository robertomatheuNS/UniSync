package br.com.unisync.mobile.ui.lembretes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.unisync.mobile.data.SessaoAutenticada
import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.lembretes.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EstadoLembretes(val itens: List<Lembrete> = emptyList(), val lendo: Boolean = true,
    val atualizando: Boolean = false, val salvando: Boolean = false, val atualizadoEm: Long? = null,
    val erro: String? = null, val confirmacao: Int = 0)

class ModeloLembretes(private val sessao: SessaoAutenticada, private val repositorio: Lembretes,
    private val aoExpirar: () -> Unit) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoLembretes())
    val estado = _estado.asStateFlow()
    init {
        viewModelScope.launch {
            repositorio.observar(sessao.usuarioId).catch { _estado.update { it.copy(lendo = false, erro = "Não foi possível ler os lembretes salvos.") } }
                .collect { itens -> _estado.update { it.copy(itens = itens, lendo = false) } }
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
                val data = repositorio.ultimaAtualizacao(sessao.usuarioId)
                _estado.update { it.copy(atualizadoEm = data) }
            } catch (erro: CancellationException) { throw erro
            } catch (erro: Exception) { falha(erro)
            } finally { _estado.update { it.copy(atualizando = false) } }
        }
    }
    fun salvar(id: String?, dados: DadosLembrete) = escrever {
        val valido = dados.validar()
        if (id == null) repositorio.criar(sessao.usuarioId, sessao.token, valido.copy(concluido = false))
        else { exigirProprio(id); repositorio.editar(sessao.usuarioId, sessao.token, id, valido) }
    }
    fun concluir(item: Lembrete) = escrever {
        exigirProprio(item.id); repositorio.concluir(sessao.usuarioId, sessao.token, item.id, !item.concluido)
    }
    fun excluir(item: Lembrete) = escrever { exigirProprio(item.id); repositorio.excluir(sessao.usuarioId, sessao.token, item.id) }
    private fun exigirProprio(id: String) {
        require(_estado.value.itens.any { it.id == id && it.usuarioId == sessao.usuarioId }) { "Lembrete não encontrado nesta conta." }
    }
    private fun escrever(acao: suspend () -> Unit) {
        if (_estado.value.salvando || _estado.value.atualizando) return
        _estado.update { it.copy(salvando = true, erro = null) }
        viewModelScope.launch {
            try { acao(); _estado.update { it.copy(confirmacao = it.confirmacao + 1) }
            } catch (erro: CancellationException) { throw erro
            } catch (erro: Exception) { falha(erro)
            } finally { _estado.update { it.copy(salvando = false) } }
        }
    }
    private fun falha(erro: Exception) {
        if (erro is FalhaApi && erro.status == 401) { aoExpirar(); return }
        _estado.update { it.copy(erro = when (erro) {
            is FalhaApi -> erro.message
            is IllegalArgumentException -> erro.message
            else -> "Não foi possível atualizar os lembretes. Os dados salvos continuam disponíveis. Confira a conexão e tente novamente."
        }) }
    }
    fun fechar() { viewModelScope.cancel() }
}
