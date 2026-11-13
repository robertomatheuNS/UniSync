package br.com.unisync.mobile

import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.academico.*
import br.com.unisync.mobile.ui.academico.ModeloAcademico
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloAcademicoTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun encerrar() { Dispatchers.resetMain() }
    private val turma = Turma("turma", "Turma A", "Kotlin", "2026.2", "prof-a", "Ana", "AABBCCDDEEFF", 1)
    private val aviso = Aviso("aviso", "turma", "Turma A", "Kotlin", "prof-a", "Ana", "Entrega", "Conteúdo",
        "2026-11-13T12:00:00Z", "2026-11-13T12:00:00Z", false)

    private inner class Fake : Academico {
        var conteudo = ConteudoAcademico(listOf(turma), listOf(aviso))
        var falha: Exception? = null
        var pendente: CompletableDeferred<Unit>? = null
        var escritas = 0
        override suspend fun carregar(token: String): ConteudoAcademico { falha?.let { throw it }; return conteudo }
        private suspend fun escrever() { pendente?.await(); falha?.let { throw it }; escritas++ }
        override suspend fun criarTurma(token: String, dados: DadosTurma): Turma { escrever(); return turma.copy(id = "nova", nome = dados.nome) }
        override suspend fun editarTurma(token: String, id: String, dados: DadosTurma): Turma { escrever(); return turma.copy(nome = dados.nome) }
        override suspend fun entrarTurma(token: String, codigo: String): Turma { escrever(); return turma }
        override suspend fun arquivarTurma(token: String, id: String) { escrever() }
        override suspend fun publicarAviso(token: String, dados: DadosAviso): Aviso { escrever(); return aviso.copy(titulo = dados.titulo) }
        override suspend fun editarAviso(token: String, id: String, dados: DadosAviso): Aviso { escrever(); return aviso.copy(titulo = dados.titulo) }
        override suspend fun excluirAviso(token: String, id: String) { escrever() }
        override suspend fun marcarLido(token: String, id: String): Aviso { escrever(); return aviso.copy(lido = true) }
    }

    @Test fun falhaDeAtualizacaoPreservaUltimaConsultaDaSessao() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloAcademico(repo, "token", "aluno", false, {})
        runCurrent(); val atualizado = modelo.estado.value.atualizadoEm
        repo.falha = IOException(); modelo.atualizar(); runCurrent()
        assertEquals(listOf(turma), modelo.estado.value.turmas)
        assertEquals(listOf(aviso), modelo.estado.value.avisos)
        assertEquals(atualizado, modelo.estado.value.atualizadoEm)
        assertNotNull(modelo.estado.value.erro); assertFalse(modelo.estado.value.atualizando); modelo.fechar()
    }

    @Test fun alunoNaoExecutaAcoesDeProfessorMesmoChamandoModelo() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloAcademico(repo, "token", "aluno", false, {})
        runCurrent()
        modelo.salvarTurma(null, DadosTurma("Turma B", "Kotlin")); runCurrent()
        modelo.arquivar("turma"); runCurrent()
        modelo.salvarAviso(null, DadosAviso("turma", "Título", "Texto")); runCurrent()
        modelo.excluirAviso("aviso"); runCurrent()
        assertEquals(0, repo.escritas); assertEquals(0, modelo.estado.value.confirmacao); modelo.fechar()
    }

    @Test fun outroProfessorNaoEditaTurmaNemAvisoDeOutraConta() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloAcademico(repo, "token", "prof-b", true, {})
        runCurrent(); modelo.salvarTurma("turma", DadosTurma("Turma B", "Kotlin")); runCurrent()
        modelo.salvarAviso("aviso", DadosAviso("turma", "Título", "Texto")); runCurrent()
        assertEquals(0, repo.escritas); assertEquals("Turma A", modelo.estado.value.turmas.single().nome); modelo.fechar()
    }

    @Test fun cliqueDuplicadoSoPublicaUmaVezEConfirmaAposResposta() = runTest(dispatcher) {
        val repo = Fake().apply { pendente = CompletableDeferred() }
        val modelo = ModeloAcademico(repo, "token", "prof-a", true, {})
        runCurrent(); val dados = DadosAviso("turma", "Título", "Texto")
        modelo.salvarAviso(null, dados); modelo.salvarAviso(null, dados); runCurrent()
        assertEquals(0, modelo.estado.value.confirmacao); assertTrue(modelo.estado.value.salvando)
        repo.pendente!!.complete(Unit); runCurrent()
        assertEquals(1, repo.escritas); assertEquals(1, modelo.estado.value.confirmacao)
        assertEquals("Título", modelo.estado.value.avisos.single().titulo); modelo.fechar()
    }

    @Test fun falhaDeEscritaNaoRemoveAvisoNemFechaFormulario() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloAcademico(repo, "token", "prof-a", true, {})
        runCurrent(); repo.falha = IOException(); modelo.excluirAviso("aviso"); runCurrent()
        assertEquals(listOf(aviso), modelo.estado.value.avisos)
        assertEquals(0, modelo.estado.value.confirmacao); assertNull(modelo.estado.value.mensagem); modelo.fechar()
    }

    @Test fun leituraSoMudaAposConfirmacaoDoServidor() = runTest(dispatcher) {
        val repo = Fake().apply { pendente = CompletableDeferred() }
        val modelo = ModeloAcademico(repo, "token", "aluno", false, {})
        runCurrent(); modelo.marcarLido("aviso"); runCurrent()
        assertFalse(modelo.estado.value.avisos.single().lido)
        repo.pendente!!.complete(Unit); runCurrent()
        assertTrue(modelo.estado.value.avisos.single().lido); modelo.fechar()
    }

    @Test fun arquivarTurmaRemoveSeusAvisosDaSessao() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloAcademico(repo, "token", "prof-a", true, {})
        runCurrent(); modelo.arquivar("turma"); runCurrent()
        assertTrue(modelo.estado.value.turmas.isEmpty()); assertTrue(modelo.estado.value.avisos.isEmpty()); modelo.fechar()
    }

    @Test fun novaSessaoNaoExibeDadosPrivadosDaAnteriorQuandoRedeFalha() = runTest(dispatcher) {
        val repo = Fake(); val anterior = ModeloAcademico(repo, "token-a", "aluno-a", false, {})
        runCurrent(); assertFalse(anterior.estado.value.turmas.isEmpty()); anterior.fechar()
        repo.falha = IOException()
        val nova = ModeloAcademico(repo, "token-b", "aluno-b", false, {})
        runCurrent(); assertTrue(nova.estado.value.turmas.isEmpty()); assertTrue(nova.estado.value.avisos.isEmpty()); nova.fechar()
    }

    @Test fun erro403PreservaSessaoE401SolicitaLogin() = runTest(dispatcher) {
        var expirou = false
        val repo = Fake(); val modelo = ModeloAcademico(repo, "token", "prof-a", true, { expirou = true })
        runCurrent(); repo.falha = FalhaApi(403, "Sem permissão"); modelo.excluirAviso("aviso"); runCurrent()
        assertFalse(expirou); assertEquals("Sem permissão", modelo.estado.value.erro)
        repo.falha = FalhaApi(401, "Sessão expirada"); modelo.excluirAviso("aviso"); runCurrent()
        assertTrue(expirou); modelo.fechar()
    }

    @Test fun validaENormalizaCamposDeTurmaAvisoECodigo() {
        assertEquals("AABBCCDDEEFF", validarCodigoTurma(" aabbccddeeff "))
        assertTrue(runCatching { validarCodigoTurma("XYZ") }.isFailure)
        assertEquals("Turma A", DadosTurma(" Turma A ", " Kotlin ").validar().nome)
        assertTrue(runCatching { DadosTurma("  ", "Kotlin").validar() }.isFailure)
        assertTrue(runCatching { DadosAviso("turma", "Título", " ").validar() }.isFailure)
        assertTrue(runCatching { DadosAviso("", "Título", "Texto").validar() }.isFailure)
    }
}
