package br.com.unisync.mobile.ui.oportunidades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.oportunidades.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EstadoOportunidades(val itens: List<Oportunidade> = emptyList(), val lendo: Boolean = true,
    val atualizando: Boolean = false, val erro: String? = null, val salvando: Boolean = false,
    val confirmacao: Int = 0)

class ModeloOportunidades(private val repo: Oportunidades, private val token: String,
    private val aoExpirar: () -> Unit, private val usuarioId: String = "",
    private val nomeProfessor: String = "", private val professor: Boolean = false) : ViewModel() {
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
        if (_estado.value.atualizando || _estado.value.salvando) return
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
    fun salvar(id: String?, dados: DadosOportunidade) = escrever {
        require(professor) { "Somente professores podem publicar oportunidades." }
        val validado = dados.validar()
        if (id == null) repo.publicar(token, nomeProfessor, validado)
        else {
            exigirAutoria(id)
            repo.editar(token, id, validado)
        }
    }
    fun excluir(id: String) = escrever { exigirAutoria(id); repo.excluir(token, id) }
    private fun exigirAutoria(id: String) {
        require(professor && usuarioId.isNotBlank() && _estado.value.itens.any { it.id == id && it.professorId == usuarioId }) {
            "Você só pode alterar oportunidades publicadas por sua conta."
        }
    }
    private fun escrever(acao: suspend () -> Unit) {
        if (_estado.value.salvando || _estado.value.atualizando) return
        _estado.update { it.copy(salvando = true, erro = null) }
        viewModelScope.launch {
            try {
                acao()
                _estado.update { it.copy(confirmacao = it.confirmacao + 1) }
            } catch (erro: CancellationException) { throw erro }
            catch (erro: FalhaApi) {
                if (erro.status == 401) aoExpirar() else _estado.update { it.copy(erro = erro.message) }
            } catch (erro: IllegalArgumentException) { _estado.update { it.copy(erro = erro.message) } }
            catch (_: Exception) { _estado.update { it.copy(erro = "Não foi possível confirmar a operação. Confira a conexão e tente novamente.") } }
            finally { _estado.update { it.copy(salvando = false) } }
        }
    }
    fun fechar() { viewModelScope.cancel() }
}
