package br.com.unisync.mobile.ui.mural

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.mural.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

data class EstadoAnexos(val itens: List<Anexo> = emptyList(), val podeAdicionar: Boolean = false,
    val atualizando: Boolean = false, val enviando: Boolean = false, val abrindoId: String? = null,
    val excluindoId: String? = null, val progresso: Int = 0, val erro: String? = null, val mensagem: String? = null,
    val confirmacaoEnvio: Int = 0, val confirmacaoExclusao: Int = 0) {
    val ocupado get() = atualizando || enviando || abrindoId != null || excluindoId != null
    val operando get() = enviando || abrindoId != null || excluindoId != null
}

class ModeloAnexos(private val repo: AnexosMural, private val token: String, private val publicacaoId: String,
    private val aoExpirar: () -> Unit, private val aoPerderAcesso: (FalhaApi) -> Unit = {}) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoAnexos())
    val estado = _estado.asStateFlow()
    init { atualizar() }
    fun atualizar() {
        if (_estado.value.ocupado) return
        _estado.update { it.copy(atualizando = true, erro = null) }
        viewModelScope.launch {
            try { consultar() }
            catch (erro: CancellationException) { throw erro }
            catch (erro: Exception) { falha(erro) }
            finally { _estado.update { it.copy(atualizando = false) } }
        }
    }
    private suspend fun consultar() {
        val lista = repo.listar(token, publicacaoId)
        require(lista.itens.all { it.publicacaoId == publicacaoId })
        _estado.update { it.copy(itens = lista.itens, podeAdicionar = lista.podeAdicionar) }
    }
    fun enviar(envioId: String, ler: suspend () -> ArquivoEnvio) {
        if (_estado.value.ocupado || !_estado.value.podeAdicionar) return
        _estado.update { it.copy(enviando = true, progresso = 0, erro = null, mensagem = null) }
        viewModelScope.launch {
            try {
                val arquivo = validarArquivoEnvio(ler())
                val anexo = repo.enviar(token, publicacaoId, envioId, arquivo) { progresso -> _estado.update { it.copy(progresso = progresso.coerceIn(0, 100)) } }
                require(anexo.publicacaoId == publicacaoId && anexo.id == envioId)
                _estado.update { it.copy(itens = (it.itens.filterNot { a -> a.id == anexo.id } + anexo),
                    confirmacaoEnvio = it.confirmacaoEnvio + 1, mensagem = "Anexo enviado.") }
            } catch (erro: CancellationException) { throw erro }
            catch (erro: Exception) { falha(erro) }
            finally { _estado.update { it.copy(enviando = false) } }
        }
    }
    fun abrir(anexo: Anexo, aoAbrir: suspend (ByteArray) -> Unit) {
        if (_estado.value.ocupado || _estado.value.itens.none { it.id == anexo.id }) return
        _estado.update { it.copy(abrindoId = anexo.id, erro = null, mensagem = null) }
        viewModelScope.launch {
            try { val dados = repo.baixar(token, anexo); ensureActive(); aoAbrir(dados) }
            catch (erro: CancellationException) { throw erro }
            catch (erro: Exception) {
                if (erro is FalhaApi && erro.status == 404) {
                    try { consultar(); _estado.update { it.copy(erro = "O anexo não está mais disponível. A lista foi atualizada.") } }
                    catch (falha: CancellationException) { throw falha }
                    catch (erroConsulta: Exception) { falha(erroConsulta) }
                } else falha(erro)
            } finally { _estado.update { it.copy(abrindoId = null) } }
        }
    }
    fun excluir(anexo: Anexo, aoLimparArquivo: () -> Unit = {}) {
        if (_estado.value.ocupado) return
        val original = _estado.value.itens.find { it.id == anexo.id }
        if (original?.podeExcluir != true || anexo.publicacaoId != publicacaoId) return
        _estado.update { it.copy(excluindoId = anexo.id, erro = null, mensagem = null) }
        viewModelScope.launch {
            try {
                repo.excluir(token, publicacaoId, anexo.id)
                _estado.update { it.copy(itens = it.itens.filterNot { a -> a.id == anexo.id },
                    confirmacaoExclusao = it.confirmacaoExclusao + 1, mensagem = "Anexo removido.") }
                aoLimparArquivo()
            } catch (erro: CancellationException) { throw erro }
            catch (erro: Exception) { falha(erro) }
            finally { _estado.update { it.copy(excluindoId = null) } }
        }
    }
    private fun falha(erro: Exception) {
        if (erro is FalhaApi && erro.status == 401) { aoExpirar(); return }
        val negado = erro is FalhaApi && erro.status in listOf(403, 404)
        val mensagem = when {
            erro is FalhaApi && erro.status == 409 -> "Não foi possível adicionar este anexo. Atualize a lista; se necessário, selecione o arquivo novamente."
            erro is FalhaApi || erro is IllegalArgumentException -> erro.message
            else -> "Não foi possível confirmar a operação. Confira a conexão e tente novamente. O arquivo selecionado foi mantido."
        }
        _estado.update { it.copy(erro = mensagem, itens = if (negado) emptyList() else it.itens, podeAdicionar = if (negado) false else it.podeAdicionar) }
        if (negado) aoPerderAcesso(erro as FalhaApi)
    }
    fun fechar() { viewModelScope.cancel() }
    companion object { fun novoEnvioId() = UUID.randomUUID().toString().replace("-", "").take(24) }
}
