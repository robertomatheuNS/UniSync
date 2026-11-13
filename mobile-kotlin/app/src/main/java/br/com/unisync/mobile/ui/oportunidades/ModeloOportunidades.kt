package br.com.unisync.mobile.ui.oportunidades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.unisync.mobile.data.RepositorioOportunidades
import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.oportunidades.Oportunidade
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EstadoOportunidades(val itens: List<Oportunidade> = emptyList(), val lendo: Boolean = true,
    val atualizando: Boolean = false, val erro: String? = null)

class ModeloOportunidades(private val repo: RepositorioOportunidades, private val token: String,
    private val aoExpirar: () -> Unit) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoOportunidades())
    val estado = _estado.asStateFlow()
    init {
        viewModelScope.launch {
            repo.itens.catch { _estado.update { it.copy(lendo = false, erro = "Não foi possível ler as oportunidades salvas.") } }
                .collect { itens -> _estado.update { it.copy(itens = itens, lendo = false) } }
        }
        atualizar()
    }
    fun atualizar() {
        if (_estado.value.atualizando) return
        _estado.update { it.copy(atualizando = true, erro = null) }
        viewModelScope.launch {
            try { repo.atualizar(token) }
            catch (erro: CancellationException) { throw erro }
            catch (erro: FalhaApi) {
                if (erro.status == 401) aoExpirar() else _estado.update { it.copy(erro = erro.message) }
            } catch (_: Exception) {
                _estado.update { it.copy(erro = "Não foi possível atualizar. Exibindo as oportunidades salvas; confira a conexão e tente novamente.") }
            } finally { _estado.update { it.copy(atualizando = false) } }
        }
    }
    fun fechar() { viewModelScope.cancel() }
}
