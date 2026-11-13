package br.com.unisync.mobile

import br.com.unisync.mobile.data.SessaoAutenticada
import br.com.unisync.mobile.domain.auth.*
import br.com.unisync.mobile.ui.auth.ModeloSessao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloSessaoTest {
    private val dispatcher = StandardTestDispatcher()
    private val token = "header." + Base64.getUrlEncoder().withoutPadding().encodeToString(
        """{"exp":2000,"http://schemas.microsoft.com/ws/2008/06/identity/claims/role":"Aluno"}""".toByteArray()) + ".signature"
    private val api = object : Autenticacao {
        override suspend fun entrar(email: String, senha: String) = SessaoAutenticada.doToken(token, 1000)
        override suspend fun cadastrar(nome: String, email: String, senha: String, codigoProfessor: String?) = Unit
    }
    private class Cofre(var token: String? = null, var falhar: Boolean = false) : ArmazenamentoSessao {
        override suspend fun lerToken() = token
        override suspend fun salvarToken(token: String) { if (falhar) throw IOException("Falha ao salvar"); this.token = token }
        override suspend fun limpar() { token = null }
    }
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun finalizar() { Dispatchers.resetMain() }

    @Test fun restauraSessaoPersistida() = runTest(dispatcher) {
        val modelo = ModeloSessao(api, Cofre(token)) { 1000 }
        runCurrent()
        assertNotNull(modelo.sessao)
        assertFalse(modelo.estado.restaurando)
        modelo.encerrar(); runCurrent()
    }

    @Test fun loginPersisteAntesDeLiberarAreaProtegidaELogoutRemoveToken() = runTest(dispatcher) {
        val cofre = Cofre()
        val modelo = ModeloSessao(api, cofre) { 1000 }
        runCurrent()
        modelo.entrar("aluno@example.com", "senha")
        assertTrue(modelo.estado.carregando)
        runCurrent()
        assertEquals(token, cofre.token)
        assertNotNull(modelo.sessao)
        modelo.encerrar(); runCurrent()
        assertNull(modelo.sessao)
        assertNull(cofre.token)
    }

    @Test fun falhaAoPersistirNaoAutentica() = runTest(dispatcher) {
        val modelo = ModeloSessao(api, Cofre(falhar = true)) { 1000 }
        runCurrent()
        modelo.entrar("a@example.com", "senha"); runCurrent()
        assertNull(modelo.sessao)
        assertNotNull(modelo.estado.mensagem)
        assertFalse(modelo.estado.carregando)
    }

    @Test fun expiraAoRetornarAoApp() = runTest(dispatcher) {
        var agora = 1000L
        val cofre = Cofre(token)
        val modelo = ModeloSessao(api, cofre) { agora }
        runCurrent()
        agora = 2000
        modelo.verificarValidade(); runCurrent()
        assertNull(modelo.sessao)
        assertNull(cofre.token)
        assertTrue(modelo.estado.mensagem!!.contains("expirou"))
        advanceTimeBy(1000000); runCurrent()
    }

    @Test fun cadastroConcluidoNaoIniciaSessao() = runTest(dispatcher) {
        val modelo = ModeloSessao(api, Cofre()) { 1000 }
        runCurrent()
        modelo.cadastrar("Aluno", "a@example.com", "senha123", null); runCurrent()
        assertTrue(modelo.estado.cadastrado)
        assertNull(modelo.sessao)
    }

    @Test fun tokenCorrompidoEhRemovido() = runTest(dispatcher) {
        val cofre = Cofre("invalido")
        val modelo = ModeloSessao(api, cofre) { 1000 }
        runCurrent()
        assertNull(modelo.sessao)
        assertNull(cofre.token)
        assertTrue(modelo.estado.solicitarLogin)
    }

    @Test fun temporizadorExpiraSessaoAberta() = runTest(dispatcher) {
        val cofre = Cofre(token)
        val modelo = ModeloSessao(api, cofre) { 1000 }
        runCurrent()
        advanceTimeBy(1000000); runCurrent()
        assertNull(modelo.sessao)
        assertNull(cofre.token)
        assertTrue(modelo.estado.solicitarLogin)
    }

    @Test fun falhaNoCadastroMantemFluxoDeCadastro() = runTest(dispatcher) {
        val apiComFalha = object : Autenticacao by api {
            override suspend fun cadastrar(nome: String, email: String, senha: String, codigoProfessor: String?) {
                throw IOException("E-mail já cadastrado")
            }
        }
        val modelo = ModeloSessao(apiComFalha, Cofre()) { 1000 }
        runCurrent()
        modelo.cadastrar("Aluno", "a@example.com", "senha123", null); runCurrent()
        assertNotNull(modelo.estado.mensagem)
        assertFalse(modelo.estado.solicitarLogin)
        assertFalse(modelo.estado.cadastrado)
    }
}
