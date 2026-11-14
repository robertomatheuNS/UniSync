package br.com.unisync.mobile.ui.mural

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.mural.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EstadoConversa(val comentarios: List<Comentario> = emptyList(), val proximaPagina: String? = null,
    val lendo: Boolean = true, val atualizando: Boolean = false, val carregandoMais: Boolean = false,
    val salvando: Boolean = false, val curtindo: Boolean = false, val consultaConfirmada: Boolean = false,
    val totalCurtidas: Int = 0, val curtiu: Boolean = false, val erro: String? = null, val mensagem: String? = null,
    val confirmacaoSalvo: Int = 0, val confirmacaoExcluido: Int = 0, val ultimoExcluidoId: String? = null) {
    val ocupado get() = atualizando || carregandoMais || salvando || curtindo
}

class ModeloConversa(private val repo: InteracoesMural, private val token: String, private val usuarioId: String,
    private val publicacao: Publicacao, private val aoExpirar: () -> Unit,
    private val aoAtualizarCurtida: (Curtida) -> Unit = {}, private val aoPerderAcesso: (FalhaApi) -> Unit = {}) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoConversa(totalCurtidas = publicacao.totalCurtidas, curtiu = publicacao.curtiu))
    val estado = _estado.asStateFlow()
    private var consulta: Job? = null
    private var geracao = 0
    init { atualizar() }
    fun atualizar() {
        if (_estado.value.salvando || _estado.value.curtindo) return
        consulta?.cancel()
        val atual = ++geracao
        _estado.update { it.copy(atualizando = true, carregandoMais = false, erro = null) }
        consulta = viewModelScope.launch {
            try {
                val (pagina, curtida) = coroutineScope {
                    val comentarios = async { repo.listarComentarios(token, publicacao.id, null) }
                    val reacao = async { repo.consultarCurtida(token, publicacao.id) }
                    comentarios.await() to reacao.await()
                }
                require(curtida.publicacaoId == publicacao.id && pagina.itens.all { it.publicacaoId == publicacao.id }) { "Conversa não pertence à publicação aberta." }
                if (geracao == atual) {
                    _estado.update { it.copy(comentarios = pagina.itens, proximaPagina = pagina.proximaPagina, consultaConfirmada = true,
                        totalCurtidas = curtida.total, curtiu = curtida.curtiu) }
                    aoAtualizarCurtida(curtida)
                }
            } catch (erro: CancellationException) { throw erro
            } catch (erro: Exception) { if (geracao == atual) falha(erro)
            } finally { if (geracao == atual) _estado.update { it.copy(lendo = false, atualizando = false) } }
        }
    }
    fun carregarMais() {
        val anterior = _estado.value
        if (anterior.ocupado) return
        val cursor = anterior.proximaPagina ?: return
        val atual = geracao
        _estado.update { it.copy(carregandoMais = true, erro = null) }
        consulta = viewModelScope.launch {
            try {
                val pagina = repo.listarComentarios(token, publicacao.id, cursor)
                require(pagina.proximaPagina == null || pagina.proximaPagina != cursor) { "Página de comentários inválida." }
                if (geracao == atual) _estado.update { it.copy(comentarios = (it.comentarios + pagina.itens).distinctBy { c -> c.id }, proximaPagina = pagina.proximaPagina) }
            } catch (erro: CancellationException) { throw erro
            } catch (erro: Exception) { if (geracao == atual) falha(erro)
            } finally { if (geracao == atual) _estado.update { it.copy(carregandoMais = false) } }
        }
    }
    fun salvar(id: String?, versao: Int?, texto: String) = escrever {
        val validado = validarComentario(texto)
        val comentario = if (id == null) repo.comentar(token, publicacao.id, validado) else {
            val original = _estado.value.comentarios.find { it.id == id }
            require(original?.autorId == usuarioId && original.podeEditar) { "Você só pode editar seus próprios comentários." }
            require(versao != null && versao > 0) { "Atualize o comentário antes de editar." }
            repo.editarComentario(token, publicacao.id, id, versao, validado)
        }
        require(comentario.publicacaoId == publicacao.id)
        _estado.update { it.copy(comentarios = (it.comentarios.filterNot { c -> c.id == comentario.id } + comentario).sortedByDescending { c -> c.id },
            confirmacaoSalvo = it.confirmacaoSalvo + 1, mensagem = "Comentário salvo.") }
    }
    fun excluir(comentario: Comentario) = escrever {
        val original = _estado.value.comentarios.find { it.id == comentario.id }
        require(original?.podeExcluir == true && comentario.publicacaoId == publicacao.id) { "Você não pode remover este comentário." }
        repo.excluirComentario(token, publicacao.id, comentario.id, comentario.versao)
        _estado.update { it.copy(comentarios = it.comentarios.filterNot { c -> c.id == comentario.id }, confirmacaoExcluido = it.confirmacaoExcluido + 1,
            ultimoExcluidoId = comentario.id, mensagem = "Comentário excluído.") }
    }
    fun curtir(curtiu: Boolean) = escrever(reacao = true) {
        val resposta = repo.curtir(token, publicacao.id, curtiu)
        require(resposta.publicacaoId == publicacao.id)
        _estado.update { it.copy(totalCurtidas = resposta.total, curtiu = resposta.curtiu) }
        aoAtualizarCurtida(resposta)
    }
    private fun escrever(reacao: Boolean = false, acao: suspend () -> Unit) {
        if (_estado.value.ocupado) return
        _estado.update { it.copy(salvando = !reacao, curtindo = reacao, erro = null, mensagem = null) }
        viewModelScope.launch {
            try { acao()
            } catch (erro: CancellationException) { throw erro
            } catch (erro: Exception) { falha(erro)
            } finally { _estado.update { it.copy(salvando = false, curtindo = false) } }
        }
    }
    private fun falha(erro: Exception) {
        if (erro is FalhaApi && erro.status == 401) { aoExpirar(); return }
        val negado = erro is FalhaApi && erro.status in listOf(403, 404)
        val mensagem = when {
            erro is FalhaApi && erro.status == 409 -> "O comentário mudou. Cancele a edição e atualize os comentários para revisar a versão atual."
            erro is FalhaApi -> erro.message
            erro is IllegalArgumentException -> erro.message
            else -> "Não foi possível confirmar a operação. O texto digitado e os comentários carregados continuam disponíveis. Confira a conexão e tente novamente."
        }
        _estado.update { it.copy(erro = mensagem, comentarios = if (negado) emptyList() else it.comentarios,
            proximaPagina = if (negado) null else it.proximaPagina) }
        if (negado) aoPerderAcesso(erro as FalhaApi)
    }
    fun fechar() { viewModelScope.cancel() }
}
