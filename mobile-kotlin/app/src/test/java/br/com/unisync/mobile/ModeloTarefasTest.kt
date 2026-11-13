package br.com.unisync.mobile

import br.com.unisync.mobile.data.SessaoAutenticada
import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.tarefas.*
import br.com.unisync.mobile.ui.tarefas.ModeloTarefas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloTarefasTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun encerrar() { Dispatchers.resetMain() }
    private fun sessao(): SessaoAutenticada {
        val dados = """{"exp":4102444800,"http://schemas.microsoft.com/ws/2008/06/identity/claims/role":"Aluno","nameid":"aluno-a"}"""
        return SessaoAutenticada.doToken("header.${Base64.getUrlEncoder().withoutPadding().encodeToString(dados.toByteArray())}.signature")
    }
    private class Fake : Tarefas {
        val itens = MutableStateFlow(listOf(Tarefa("a", "aluno-a", "Estudar", StatusTarefa.A_FAZER)))
        var falha: Exception? = null
        override fun observar(alunoId: String) = itens
        override suspend fun ultimaAtualizacao(alunoId: String) = 100L
        override suspend fun atualizar(alunoId: String, token: String) { falha?.let { throw it } }
        override suspend fun criar(alunoId: String, token: String, titulo: String) { falha?.let { throw it } }
        override suspend fun editar(alunoId: String, token: String, id: String, titulo: String, status: StatusTarefa) { falha?.let { throw it } }
        override suspend fun excluir(alunoId: String, token: String, id: String) { falha?.let { throw it } }
    }
    @Test fun falhaDeRedePreservaCacheEDataAnterior() = runTest(dispatcher) {
        val repo = Fake().apply { falha = IOException() }
        val modelo = ModeloTarefas(sessao(), repo) {}
        runCurrent()
        assertEquals(1, modelo.estado.value.itens.size)
        assertEquals(100L, modelo.estado.value.atualizadoEm)
        assertNotNull(modelo.estado.value.erro)
        assertFalse(modelo.estado.value.atualizando)
        modelo.fechar()
    }
    @Test fun escritaSemConfirmacaoNaoMudaCacheNemFechaFormulario() = runTest(dispatcher) {
        val repo = Fake()
        val modelo = ModeloTarefas(sessao(), repo) {}
        runCurrent()
        repo.falha = IOException()
        modelo.salvar("a", "Novo título", StatusTarefa.CONCLUIDO); runCurrent()
        assertEquals("Estudar", modelo.estado.value.itens.single().titulo)
        assertEquals(0, modelo.estado.value.confirmacao)
        assertNotNull(modelo.estado.value.erro)
        modelo.fechar()
    }
    @Test fun rejeicao401SolicitaNovaAutenticacao() = runTest(dispatcher) {
        var expirada = false
        val modelo = ModeloTarefas(sessao(), Fake().apply { falha = FalhaApi(401, "Sessão expirada") }) { expirada = true }
        runCurrent()
        assertTrue(expirada)
        modelo.fechar()
    }
    @Test fun tituloNaoPermiteEspacosOuMaisDe100Caracteres() {
        assertTrue(runCatching { validarTituloTarefa("   ") }.isFailure)
        assertTrue(runCatching { validarTituloTarefa("a".repeat(101)) }.isFailure)
        assertEquals("Estudar", validarTituloTarefa(" Estudar "))
    }
}
