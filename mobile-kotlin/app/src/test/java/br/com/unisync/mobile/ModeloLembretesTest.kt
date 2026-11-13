package br.com.unisync.mobile

import br.com.unisync.mobile.data.SessaoAutenticada
import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.lembretes.*
import br.com.unisync.mobile.ui.lembretes.ModeloLembretes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException
import java.time.Instant
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloLembretesTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun encerrar() { Dispatchers.resetMain() }
    private fun sessao(): SessaoAutenticada {
        val dados = """{"exp":4102444800,"http://schemas.microsoft.com/ws/2008/06/identity/claims/role":"Aluno","nameid":"eu"}"""
        return SessaoAutenticada.doToken("h.${Base64.getUrlEncoder().withoutPadding().encodeToString(dados.toByteArray())}.s")
    }
    private class Fake : Lembretes {
        val itens = MutableStateFlow(listOf(Lembrete("a", "eu", "Original")))
        var falha: Exception? = null
        var recebido: DadosLembrete? = null
        var chamadas = 0
        override fun observar(usuarioId: String) = itens
        override suspend fun ultimaAtualizacao(usuarioId: String) = 123L
        override suspend fun atualizar(usuarioId: String, token: String) { falha?.let { throw it } }
        override suspend fun criar(usuarioId: String, token: String, dados: DadosLembrete) { chamadas++; falha?.let { throw it }; recebido = dados }
        override suspend fun editar(usuarioId: String, token: String, id: String, dados: DadosLembrete) { criar(usuarioId, token, dados) }
        override suspend fun concluir(usuarioId: String, token: String, id: String, concluido: Boolean) { chamadas++ }
        override suspend fun excluir(usuarioId: String, token: String, id: String) { chamadas++ }
    }
    @Test fun falhaPreservaCacheDataERascunhoSemConfirmar() = runTest(dispatcher) {
        val repo = Fake().apply { falha = IOException() }
        val modelo = ModeloLembretes(sessao(), repo) {}
        runCurrent()
        assertEquals(123L, modelo.estado.value.atualizadoEm)
        modelo.salvar("a", DadosLembrete("Novo", agendadoEm = Instant.parse("2028-02-29T12:30:00Z"))); runCurrent()
        assertEquals("Original", modelo.estado.value.itens.single().titulo)
        assertEquals(0, modelo.estado.value.confirmacao); assertNotNull(modelo.estado.value.erro)
        modelo.fechar()
    }
    @Test fun bloqueiaDuploEnvioERespeitaAutoria() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloLembretes(sessao(), repo) {}; runCurrent()
        val dados = DadosLembrete("Entrega", agendadoEm = Instant.parse("2028-02-29T12:30:00Z"))
        modelo.salvar(null, dados); modelo.salvar(null, dados); runCurrent()
        assertEquals(1, repo.chamadas); assertEquals(dados, repo.recebido)
        modelo.salvar("outro", dados); runCurrent()
        assertEquals(1, repo.chamadas); assertEquals(1, modelo.estado.value.confirmacao)
        modelo.fechar()
    }
    @Test fun tokenRecusadoEncerraSessao() = runTest(dispatcher) {
        var expirou = false
        val modelo = ModeloLembretes(sessao(), Fake().apply { falha = FalhaApi(401, "Expirou") }) { expirou = true }
        runCurrent(); assertTrue(expirou); modelo.fechar()
    }
}
