package br.com.unisync.mobile.ui.mural

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.mural.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EstadoMural(val itens: List<Publicacao> = emptyList(), val filtroTurma: String? = null,
    val proximaPagina: String? = null, val lendo: Boolean = true, val atualizando: Boolean = false,
    val carregandoMais: Boolean = false, val salvando: Boolean = false, val atualizadoEm: Long? = null,
    val erro: String? = null, val mensagem: String? = null, val confirmacao: Int = 0,
    val abrindoId: String? = null, val detalhe: Publicacao? = null, val carregandoDetalhe: Boolean = false, val erroDetalhe: String? = null)

/** Publicações privadas permanecem somente nesta sessão. Cada página é autorizada pelo servidor. */
class ModeloMural(private val repo: Mural, private val token: String, private val usuarioId: String,
    private val aoExpirar: () -> Unit) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoMural())
    val estado = _estado.asStateFlow()
    private var consulta: Job? = null
    private var abertura: Job? = null
    private var geracao = 0
    init { atualizar() }
    fun atualizar(turmaId: String? = _estado.value.filtroTurma) {
        if (_estado.value.salvando) return
        consulta?.cancel()
        val atual = ++geracao
        val mudou = turmaId != _estado.value.filtroTurma
        if (mudou) fecharDetalhe()
        _estado.update { it.copy(filtroTurma = turmaId, atualizando = true, carregandoMais = false, erro = null,
            itens = if (mudou) emptyList() else it.itens, proximaPagina = if (mudou) null else it.proximaPagina,
            atualizadoEm = if (mudou) null else it.atualizadoEm) }
        consulta = viewModelScope.launch {
            try {
                val pagina = repo.listar(token, turmaId, null)
                if (atual == geracao) _estado.update { it.copy(itens = pagina.itens, proximaPagina = pagina.proximaPagina, atualizadoEm = System.currentTimeMillis()) }
            } catch (erro: CancellationException) { throw erro
            } catch (erro: Exception) { if (atual == geracao) falha(erro)
            } finally { if (atual == geracao) _estado.update { it.copy(lendo = false, atualizando = false) } }
        }
    }
    fun carregarMais() {
        val estado = _estado.value
        if (estado.salvando || estado.atualizando || estado.carregandoMais) return
        val cursor = estado.proximaPagina ?: return
        val atual = geracao
        _estado.update { it.copy(carregandoMais = true, erro = null) }
        consulta = viewModelScope.launch {
            try {
                val pagina = repo.listar(token, estado.filtroTurma, cursor)
                require(pagina.proximaPagina == null || pagina.proximaPagina != cursor) { "Página de publicações inválida." }
                if (atual == geracao) _estado.update { it.copy(itens = (it.itens + pagina.itens).distinctBy { item -> item.id }, proximaPagina = pagina.proximaPagina) }
            } catch (erro: CancellationException) { throw erro
            } catch (erro: Exception) { if (atual == geracao) falha(erro)
            } finally { if (atual == geracao) _estado.update { it.copy(carregandoMais = false) } }
        }
    }
    fun abrir(id: String) {
        if (_estado.value.salvando) return
        abertura?.cancel()
        _estado.update { it.copy(abrindoId = id, detalhe = null, carregandoDetalhe = true, erroDetalhe = null, erro = null) }
        abertura = viewModelScope.launch {
            try {
                val item = repo.buscar(token, id)
                if (_estado.value.abrindoId == id) _estado.update { it.copy(detalhe = item) }
            } catch (erro: CancellationException) { throw erro
            } catch (erro: Exception) { if (_estado.value.abrindoId == id) falha(erro, detalhe = true)
            } finally { if (_estado.value.abrindoId == id) _estado.update { it.copy(carregandoDetalhe = false) } }
        }
    }
    fun fecharDetalhe() {
        if (_estado.value.salvando) return
        abertura?.cancel()
        _estado.update { it.copy(abrindoId = null, detalhe = null, carregandoDetalhe = false, erroDetalhe = null) }
    }
    fun salvar(id: String?, versao: Int?, dados: DadosPublicacao) = escrever("Publicação salva.") {
        val valido = dados.validar()
        val item = if (id == null) repo.publicar(token, valido) else {
            val original = _estado.value.detalhe
            require(original?.id == id && original.podeEditar && original.autorId == usuarioId) { "Você só pode editar suas próprias publicações." }
            require(original.turmaId == valido.turmaId) { "A turma de uma publicação não pode ser alterada." }
            require(versao != null && versao > 0) { "Atualize a publicação antes de editar." }
            repo.editar(token, id, versao, valido)
        }
        _estado.update { atual ->
            val visivel = atual.filtroTurma == null || atual.filtroTurma == item.turmaId
            atual.copy(itens = if (visivel) (atual.itens.filterNot { it.id == item.id } + item.copy(conteudo = null)).sortedByDescending { it.id } else atual.itens,
                detalhe = item, abrindoId = item.id)
        }
    }
    fun excluir(item: Publicacao) = escrever("Publicação excluída.") {
        val conhecido = _estado.value.detalhe?.takeIf { it.id == item.id } ?: _estado.value.itens.find { it.id == item.id }
        require(conhecido?.podeExcluir == true) { "Você não pode remover esta publicação." }
        repo.excluir(token, item.id, item.versao)
        _estado.update { it.copy(itens = it.itens.filterNot { publicacao -> publicacao.id == item.id }, detalhe = null, abrindoId = null) }
    }
    fun limparMensagem() { _estado.update { it.copy(erro = null, mensagem = null) } }
    private fun escrever(mensagem: String, acao: suspend () -> Unit) {
        if (_estado.value.salvando || _estado.value.atualizando || _estado.value.carregandoMais || _estado.value.carregandoDetalhe) return
        _estado.update { it.copy(salvando = true, erro = null, mensagem = null) }
        viewModelScope.launch {
            try { acao(); _estado.update { it.copy(confirmacao = it.confirmacao + 1, mensagem = mensagem) }
            } catch (erro: CancellationException) { throw erro
            } catch (erro: Exception) { falha(erro)
            } finally { _estado.update { it.copy(salvando = false) } }
        }
    }
    private fun falha(erro: Exception, detalhe: Boolean = false) {
        if (erro is FalhaApi && erro.status == 401) { aoExpirar(); return }
        val mensagem = when {
            erro is FalhaApi && erro.status == 409 -> "A publicação mudou. Cancele a edição e atualize o mural para revisar a versão atual."
            erro is FalhaApi -> erro.message
            erro is IllegalArgumentException -> erro.message
            else -> "Não foi possível atualizar o mural. O conteúdo já carregado permanece disponível. Confira a conexão e tente novamente."
        }
        _estado.update { atual ->
            val negado = erro is FalhaApi && erro.status in listOf(403, 404)
            val turmaNegada = if (detalhe) atual.itens.find { it.id == atual.abrindoId }?.turmaId else null
            atual.copy(erro = if (detalhe) atual.erro else mensagem, erroDetalhe = if (detalhe) mensagem else atual.erroDetalhe,
                itens = if (negado) { if (turmaNegada != null) atual.itens.filterNot { it.turmaId == turmaNegada } else emptyList() } else atual.itens,
                detalhe = if (negado) null else atual.detalhe, proximaPagina = if (negado) null else atual.proximaPagina)
        }
    }
    fun fechar() { viewModelScope.cancel() }
}
