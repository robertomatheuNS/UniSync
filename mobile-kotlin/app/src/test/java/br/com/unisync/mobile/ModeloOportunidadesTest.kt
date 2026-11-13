package br.com.unisync.mobile

import br.com.unisync.mobile.domain.oportunidades.*
import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.ui.oportunidades.ModeloOportunidades
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException
import java.time.*

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloOportunidadesTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun encerrar() { Dispatchers.resetMain() }
    private val dados = DadosOportunidade("Palestra Android", "Kotlin", "Descrição", "Palestra", LocalDateTime.of(2027, 1, 10, 14, 30))
    private class Fake : Oportunidades {
        override val itens = MutableStateFlow(listOf(Oportunidade("id", "Palestra Android", "Ana", "Kotlin", null,
            "Palestra", "2027-01-10T14:30:00", "prof-a")))
        var escritas = 0
        var falha: Exception? = null
        var pendente: CompletableDeferred<Unit>? = null
        override suspend fun atualizar(token: String) = Unit
        private suspend fun escrever() { pendente?.await(); falha?.let { throw it }; escritas++ }
        override suspend fun publicar(token: String, nomeProfessor: String, dados: DadosOportunidade) = escrever()
        override suspend fun editar(token: String, id: String, dados: DadosOportunidade) = escrever()
        override suspend fun excluir(token: String, id: String) = escrever()
    }
    @Test fun alunoNaoPublicaMesmoChamandoViewModel() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloOportunidades(repo, "token", {})
        runCurrent(); modelo.salvar(null, dados); runCurrent()
        assertEquals(0, repo.escritas); assertNotNull(modelo.estado.value.erro); modelo.fechar()
    }
    @Test fun professorNaoEditaNemExcluiConteudoDeOutraConta() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloOportunidades(repo, "token", {}, "prof-b", "Bruno", true)
        runCurrent(); modelo.salvar("id", dados); runCurrent(); modelo.excluir("id"); runCurrent()
        assertEquals(0, repo.escritas); assertEquals(0, modelo.estado.value.confirmacao); modelo.fechar()
    }
    @Test fun falhaPreservaConteudoENaoConfirmaSucesso() = runTest(dispatcher) {
        val repo = Fake().apply { falha = IOException() }
        val modelo = ModeloOportunidades(repo, "token", {}, "prof-a", "Ana", true)
        runCurrent(); modelo.salvar("id", dados.copy(titulo = "Título alterado")); runCurrent()
        assertEquals("Palestra Android", modelo.estado.value.itens.single().titulo)
        assertEquals(0, modelo.estado.value.confirmacao); assertFalse(modelo.estado.value.salvando); modelo.fechar()
    }
    @Test fun cliqueDuplicadoEnviaUmaUnicaPublicacao() = runTest(dispatcher) {
        val repo = Fake().apply { pendente = CompletableDeferred() }
        val modelo = ModeloOportunidades(repo, "token", {}, "prof-a", "Ana", true)
        runCurrent(); modelo.salvar(null, dados); modelo.salvar(null, dados); runCurrent()
        assertTrue(modelo.estado.value.salvando); assertEquals(0, modelo.estado.value.confirmacao)
        repo.pendente!!.complete(Unit); runCurrent()
        assertEquals(1, repo.escritas); assertEquals(1, modelo.estado.value.confirmacao); modelo.fechar()
    }
    @Test fun erro403NaoEncerraSessaoMas401SolicitaLogin() = runTest(dispatcher) {
        var expirou = false
        val repo = Fake().apply { falha = FalhaApi(403, "Sem permissão") }
        val modelo = ModeloOportunidades(repo, "token", { expirou = true }, "prof-a", "Ana", true)
        runCurrent(); modelo.excluir("id"); runCurrent(); assertFalse(expirou)
        repo.falha = FalhaApi(401, "Sessão expirada")
        modelo.excluir("id"); runCurrent(); assertTrue(expirou); modelo.fechar()
    }
    @Test fun validaCamposENormalizaTituloAntesDeEnviar() {
        assertEquals("Palestra Android", dados.copy(titulo = " Palestra Android ").validar().titulo)
        assertTrue(runCatching { dados.copy(titulo = "abcd").validar() }.isFailure)
        assertTrue(runCatching { dados.copy(materia = " ").validar() }.isFailure)
        assertTrue(runCatching { dados.copy(tipo = " ").validar() }.isFailure)
        assertEquals("2027-01-10T14:30-03:00", dados.dataParaApi(ZoneId.of("America/Maceio")))
    }
    @Test fun rejeitaHorarioInexistenteNaMudancaDeFuso() {
        val invalido = dados.copy(horarioLocal = LocalDateTime.of(2027,3,14,2,30))
        assertTrue(runCatching { invalido.validar(ZoneId.of("America/New_York")) }.isFailure)
    }
}
