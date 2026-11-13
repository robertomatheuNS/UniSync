package br.com.unisync.mobile.ui.academico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.academico.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoAcademico(val turmas: List<Turma> = emptyList(), val avisos: List<Aviso> = emptyList(),
    val lendo: Boolean = true, val atualizando: Boolean = false, val salvando: Boolean = false,
    val erro: String? = null, val mensagem: String? = null, val confirmacao: Int = 0,
    val atualizadoEm: Long? = null)

/** Conteúdo privado mantido apenas durante esta sessão; não é compartilhado entre contas. */
class ModeloAcademico(private val repo: Academico, private val token: String, private val usuarioId: String,
    private val professor: Boolean, private val aoExpirar: () -> Unit) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoAcademico())
    val estado = _estado.asStateFlow()
    init { atualizar() }

    fun atualizar() {
        if (_estado.value.salvando || _estado.value.atualizando) return
        _estado.update { it.copy(atualizando = true, erro = null) }
        viewModelScope.launch {
            try {
                val conteudo = repo.carregar(token)
                _estado.update { it.copy(turmas = conteudo.turmas, avisos = conteudo.avisos,
                    atualizadoEm = System.currentTimeMillis()) }
            } catch (erro: CancellationException) { throw erro }
            catch (erro: FalhaApi) { falha(erro) }
            catch (_: Exception) { _estado.update { it.copy(erro = "Não foi possível atualizar turmas e avisos. Confira a conexão e tente novamente.") } }
            finally { _estado.update { it.copy(lendo = false, atualizando = false) } }
        }
    }

    fun salvarTurma(id: String?, dados: DadosTurma) = escrever("Turma salva.") {
        require(professor) { "Somente professores podem criar turmas." }
        val validado = dados.validar()
        val turma = if (id == null) repo.criarTurma(token, validado)
        else { exigirTurmaPropria(id); repo.editarTurma(token, id, validado) }
        _estado.update { estado -> estado.copy(turmas = substituir(estado.turmas, turma) { it.id },
            avisos = estado.avisos.map { if (it.turmaId == turma.id) it.copy(turmaNome = turma.nome, disciplina = turma.disciplina) else it }) }
    }

    fun entrar(codigo: String) = escrever("Entrada na turma confirmada.", atualizarDepois = true) {
        require(!professor) { "Somente alunos podem entrar por código." }
        val turma = repo.entrarTurma(token, validarCodigoTurma(codigo))
        _estado.update { it.copy(turmas = substituir(it.turmas, turma) { item -> item.id }) }
    }

    fun arquivar(id: String) = escrever("Turma arquivada.") {
        exigirTurmaPropria(id)
        repo.arquivarTurma(token, id)
        _estado.update { it.copy(turmas = it.turmas.filterNot { turma -> turma.id == id },
            avisos = it.avisos.filterNot { aviso -> aviso.turmaId == id }) }
    }

    fun salvarAviso(id: String?, dados: DadosAviso) = escrever("Aviso publicado.") {
        exigirTurmaPropria(dados.turmaId)
        val validado = dados.validar()
        val aviso = if (id == null) repo.publicarAviso(token, validado)
        else {
            exigirAvisoProprio(id)
            require(_estado.value.avisos.any { it.id == id && it.turmaId == dados.turmaId }) { "A turma de um aviso publicado não pode ser alterada." }
            repo.editarAviso(token, id, validado)
        }
        _estado.update { it.copy(avisos = substituir(it.avisos, aviso) { item -> item.id }.sortedByDescending { item -> item.criadoEm }) }
    }

    fun excluirAviso(id: String) = escrever("Aviso excluído.") {
        exigirAvisoProprio(id)
        repo.excluirAviso(token, id)
        _estado.update { it.copy(avisos = it.avisos.filterNot { aviso -> aviso.id == id }) }
    }

    fun marcarLido(id: String) = escrever("Leitura confirmada.") {
        require(!professor && _estado.value.avisos.any { it.id == id }) { "Aviso indisponível para esta conta." }
        val aviso = repo.marcarLido(token, id)
        _estado.update { it.copy(avisos = substituir(it.avisos, aviso) { item -> item.id }) }
    }

    fun limparMensagem() { _estado.update { it.copy(erro = null, mensagem = null) } }
    private fun exigirTurmaPropria(id: String) {
        require(professor && _estado.value.turmas.any { it.id == id && it.professorId == usuarioId }) {
            "Você só pode alterar suas próprias turmas."
        }
    }
    private fun exigirAvisoProprio(id: String) {
        require(professor && _estado.value.avisos.any { it.id == id && it.professorId == usuarioId }) {
            "Você só pode alterar seus próprios avisos."
        }
    }
    private fun falha(erro: FalhaApi) {
        if (erro.status == 401) aoExpirar() else _estado.update { it.copy(erro = erro.message) }
    }
    private fun escrever(mensagem: String, atualizarDepois: Boolean = false, acao: suspend () -> Unit) {
        if (_estado.value.salvando || _estado.value.atualizando) return
        _estado.update { it.copy(salvando = true, erro = null, mensagem = null) }
        viewModelScope.launch {
            var sucesso = false
            try {
                acao()
                sucesso = true
                _estado.update { it.copy(confirmacao = it.confirmacao + 1, mensagem = mensagem) }
            } catch (erro: CancellationException) { throw erro }
            catch (erro: FalhaApi) { falha(erro) }
            catch (erro: IllegalArgumentException) { _estado.update { it.copy(erro = erro.message) } }
            catch (_: Exception) { _estado.update { it.copy(erro = "Não foi possível confirmar a operação. Confira a conexão e tente novamente.") } }
            finally { _estado.update { it.copy(salvando = false) } }
            if (sucesso && atualizarDepois) atualizar()
        }
    }
    private fun <T> substituir(lista: List<T>, item: T, chave: (T) -> String): List<T> =
        if (lista.any { chave(it) == chave(item) }) lista.map { if (chave(it) == chave(item)) item else it } else lista + item
    fun fechar() { viewModelScope.cancel() }
}
