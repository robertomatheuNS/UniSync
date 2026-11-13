package br.com.unisync.mobile.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.unisync.mobile.data.SessaoAutenticada
import br.com.unisync.mobile.domain.auth.ArmazenamentoSessao
import br.com.unisync.mobile.domain.auth.Autenticacao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

data class EstadoAutenticacao(
    val restaurando: Boolean = true,
    val carregando: Boolean = false,
    val cadastrado: Boolean = false,
    val solicitarLogin: Boolean = false,
    val mensagem: String? = null
)

class ModeloSessao(
    private val autenticacao: Autenticacao,
    private val armazenamento: ArmazenamentoSessao,
    private val limparCache: suspend () -> Unit = {},
    private val agora: () -> Long = { System.currentTimeMillis() / 1000 }
) : ViewModel() {
    var sessao by mutableStateOf<SessaoAutenticada?>(null)
        private set

    var estado by mutableStateOf(EstadoAutenticacao())
        private set
    private var expiracao: Job? = null

    init {
        viewModelScope.launch {
            try {
                armazenamento.lerToken()?.let { ativar(SessaoAutenticada.doToken(it, agora())) }
                estado = EstadoAutenticacao(restaurando = false)
            } catch (erro: CancellationException) {
                throw erro
            } catch (_: Exception) {
                runCatching { armazenamento.limpar() }
                estado = EstadoAutenticacao(restaurando = false, solicitarLogin = true, mensagem = "Não foi possível recuperar a sessão. Entre novamente.")
            }
        }
    }

    fun entrar(email: String, senha: String) = executar {
        val novaSessao = autenticacao.entrar(email.trim(), senha)
        if (novaSessao.expirada(agora())) throw IOException("Sua sessão expirou. Entre novamente.")
        armazenamento.salvarToken(novaSessao.token)
        ativar(novaSessao)
    }

    fun cadastrar(nome: String, email: String, senha: String, codigo: String?) = executar {
        autenticacao.cadastrar(nome.trim(), email.trim(), senha, codigo?.trim())
        estado = estado.copy(cadastrado = true)
    }

    fun limparMensagem() {
        if (!estado.carregando) estado = EstadoAutenticacao(restaurando = false)
    }

    fun encerrar() = executar {
        limparCache()
        armazenamento.limpar()
        expiracao?.cancel()
        sessao = null
    }

    fun verificarValidade() {
        if (sessao?.expirada(agora()) == true) expirar()
    }

    fun sessaoRecusada() { expirar() }

    private fun ativar(novaSessao: SessaoAutenticada) {
        sessao = novaSessao
        expiracao?.cancel()
        expiracao = viewModelScope.launch {
            delay((novaSessao.expiraEm - agora()).coerceAtLeast(0) * 1000)
            expirar()
        }
    }

    private fun expirar() {
        sessao = null
        estado = EstadoAutenticacao(restaurando = false, carregando = true, solicitarLogin = true, mensagem = "Sua sessão expirou. Entre novamente.")
        viewModelScope.launch {
            try { limparCache(); armazenamento.limpar() }
            catch (erro: CancellationException) { throw erro }
            catch (_: Exception) { /* A sessão já foi revogada em memória. */ }
            finally { estado = estado.copy(carregando = false) }
        }
    }

    private fun executar(acao: suspend () -> Unit) {
        if (estado.carregando || estado.restaurando) return
        estado = EstadoAutenticacao(restaurando = false, carregando = true)
        viewModelScope.launch {
            try {
                acao()
            } catch (erro: CancellationException) {
                throw erro
            } catch (erro: IOException) {
                estado = estado.copy(mensagem = erro.message ?: "Confira a conexão e tente novamente.")
            } catch (_: Exception) {
                estado = estado.copy(mensagem = "Não foi possível concluir a operação. Tente novamente.")
            } finally {
                estado = estado.copy(carregando = false)
            }
        }
    }
}
