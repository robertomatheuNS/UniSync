package br.com.unisync.mobile

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.data.*
import br.com.unisync.mobile.data.remote.*
import br.com.unisync.mobile.ui.academico.*
import br.com.unisync.mobile.ui.theme.TemaUniSync
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

/** Usa contas técnicas e uma Activity de teste; preserva a sessão pessoal instalada. */
class AcademicoLocalE2ETest {
    @get:Rule val compose = createAndroidComposeRule<AtividadeTeste>()

    @Test fun professorCriaTurmaPublicaAvisoEAlunoEntraLeEditaRecebeRevisao() {
        val args = InstrumentationRegistry.getArguments()
        val codigoProfessor = args.getString("codigoProfessorAcademico").orEmpty()
        assumeTrue(args.getString("e2eAcademico") == "true" && codigoProfessor.isNotBlank())
        val id = UUID.randomUUID().toString()
        val auth = RepositorioAutenticacao()
        val repo = RepositorioAcademico()
        val api = ClienteAcademico()
        val senha = "Teste-$id-42!"
        val professor = runBlocking {
            auth.cadastrar("Professora Acadêmica", "prof-academico-$id@example.invalid", senha, codigoProfessor)
            auth.entrar("prof-academico-$id@example.invalid", senha)
        }
        val aluno = runBlocking {
            auth.cadastrar("Aluno Acadêmico", "aluno-academico-$id@example.invalid", senha, null)
            auth.entrar("aluno-academico-$id@example.invalid", senha)
        }
        var expirou = false
        val modeloProfessor = ModeloAcademico(repo, professor.token, professor.usuarioId, true, { expirou = true })
        val modeloAluno = ModeloAcademico(repo, aluno.token, aluno.usuarioId, false, { expirou = true })
        var professorAtivo by mutableStateOf(true)
        var tela by mutableStateOf("turmas")
        var filtro by mutableStateOf<String?>(null)
        var turmaId: String? = null
        var avisoId: String? = null
        try {
            compose.setContent {
                TemaUniSync {
                    val modelo = if (professorAtivo) modeloProfessor else modeloAluno
                    val estado by modelo.estado.collectAsState()
                    val professorId = professor.usuarioId.takeIf { professorAtivo }
                    key(professorAtivo, tela) {
                        if (tela == "turmas") TelaTurmas(estado, professorId, modelo::atualizar,
                            modelo::salvarTurma, modelo::entrar, modelo::arquivar,
                            { filtro = it; tela = "avisos" }, modelo::limparMensagem)
                        else TelaAvisos(estado, professorId, filtro, modelo::atualizar,
                            modelo::salvarAviso, modelo::excluirAviso, modelo::marcarLido,
                            { tela = "turmas" }, modelo::limparMensagem)
                    }
                }
            }
            esperarPronto(modeloProfessor)
            compose.onNodeWithText("Criar turma").performClick()
            compose.onNodeWithText("Nome da turma").performTextInput("Turma Android")
            compose.onNodeWithText("Disciplina").performTextInput("Engenharia de Software")
            compose.onNodeWithText("Período (opcional)").performTextInput("2026.2")
            compose.onNodeWithText("Salvar turma").performClick()
            compose.waitUntil(15000) { modeloProfessor.estado.value.turmas.size == 1 && !modeloProfessor.estado.value.salvando }
            val turma = modeloProfessor.estado.value.turmas.single()
            turmaId = turma.id
            capturar("academico-turma-professor.png")
            compose.onNodeWithText("Ver avisos").performScrollTo().performClick()
            compose.onNodeWithText("Publicar aviso").performClick()
            compose.onNodeWithText("Título do aviso").performTextInput("Entrega do projeto Android")
            compose.onNodeWithText("Conteúdo do aviso").performTextInput("Apresentação na próxima aula.")
            compose.onNode(hasText("Publicar aviso") and hasAnyAncestor(isDialog())).performClick()
            compose.waitUntil(15000) { modeloProfessor.estado.value.avisos.size == 1 && !modeloProfessor.estado.value.salvando }
            avisoId = modeloProfessor.estado.value.avisos.single().id
            compose.runOnIdle { professorAtivo = false; tela = "turmas" }
            esperarPronto(modeloAluno)
            compose.onNodeWithText("Entrar em uma turma").performClick()
            compose.onNodeWithText("Código da turma").performTextInput(turma.codigo!!)
            compose.onNodeWithText("Entrar na turma").performClick()
            compose.waitUntil(15000) { modeloAluno.estado.value.avisos.size == 1 && !modeloAluno.estado.value.atualizando }
            compose.onNodeWithText("Ver avisos").performScrollTo().performClick()
            compose.onNodeWithText("Não lido").assertExists()
            compose.onNodeWithText("Entrega do projeto Android").performClick()
            capturar("academico-aviso-aluno.png")
            compose.onNodeWithText("Marcar como lido").performScrollTo().performClick()
            compose.waitUntil(15000) { modeloAluno.estado.value.avisos.single().lido && !modeloAluno.estado.value.salvando }
            assertTrue(runBlocking { repo.carregar(aluno.token).avisos.single().lido })

            runBlocking {
                auth.cadastrar("Aluno sem matrícula", "fora-academico-$id@example.invalid", senha, null)
                val fora = auth.entrar("fora-academico-$id@example.invalid", senha)
                assertTrue(repo.carregar(fora.token).avisos.isEmpty())
                assertEquals(403, (runCatching { api.executar("GET", "/avisos/listar?turma_id=$turmaId", fora.token) }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(403, (runCatching { api.executar("PUT", "/avisos/ler/$avisoId", fora.token) }.exceptionOrNull() as? FalhaApi)?.status)
            }
            compose.runOnIdle { professorAtivo = true; tela = "avisos" }
            compose.onNodeWithText("Entrega do projeto Android").performClick()
            compose.onNodeWithText("Editar aviso").performScrollTo().performClick()
            compose.onNodeWithText("Título do aviso").performTextReplacement("Entrega revisada do projeto")
            compose.onNode(hasText("Publicar aviso") and hasAnyAncestor(isDialog())).performClick()
            compose.waitUntil(15000) { modeloProfessor.estado.value.avisos.single().titulo == "Entrega revisada do projeto" && !modeloProfessor.estado.value.salvando }
            val revisado = runBlocking { repo.carregar(aluno.token).avisos.single() }
            assertEquals("Entrega revisada do projeto", revisado.titulo)
            assertFalse(revisado.lido)
            assertNotNull(java.time.Instant.parse(revisado.atualizadoEm))
            compose.onNodeWithText("Entrega revisada do projeto").performClick()
            compose.onNodeWithText("Excluir aviso").performScrollTo().performClick()
            compose.onNodeWithText("Excluir").performClick()
            compose.waitUntil(15000) { modeloProfessor.estado.value.avisos.isEmpty() && !modeloProfessor.estado.value.salvando }
            avisoId = null
            compose.runOnIdle { tela = "turmas" }
            compose.onNodeWithText("Arquivar turma").performScrollTo().performClick()
            compose.onNodeWithText("Arquivar").performClick()
            compose.waitUntil(15000) { modeloProfessor.estado.value.turmas.isEmpty() && !modeloProfessor.estado.value.salvando }
            turmaId = null
            assertTrue(runBlocking { repo.carregar(aluno.token).turmas.isEmpty() })
            assertFalse(expirou)
        } finally {
            modeloProfessor.fechar(); modeloAluno.fechar()
            runBlocking {
                avisoId?.let { repo.excluirAviso(professor.token, it) }
                turmaId?.let { repo.arquivarTurma(professor.token, it) }
            }
        }
    }

    private fun esperarPronto(modelo: ModeloAcademico) {
        compose.waitUntil(15000) { !modelo.estado.value.atualizando && !modelo.estado.value.lendo }
        assertNull(modelo.estado.value.erro)
    }
    private fun capturar(nome: String) {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        File(contexto.cacheDir, nome).outputStream().use { arquivo ->
            val alvo = if (compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty()) compose.onRoot() else compose.onNode(isDialog())
            alvo.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, arquivo)
        }
    }
}
