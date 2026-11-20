package br.com.unisync.mobile

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.data.*
import br.com.unisync.mobile.data.remote.*
import br.com.unisync.mobile.domain.academico.DadosTurma
import br.com.unisync.mobile.domain.mural.DadosPublicacao
import br.com.unisync.mobile.ui.mural.*
import br.com.unisync.mobile.ui.theme.TemaUniSync
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class MuralLocalE2ETest {
    @get:Rule val compose = createAndroidComposeRule<AtividadeTeste>()
    @Test fun alunoPublicaEditaComConflitoProfessorModeraEPaginasIsolamTurma() {
        val args = InstrumentationRegistry.getArguments()
        val codigo = args.getString("codigoProfessorMural").orEmpty()
        assumeTrue(args.getString("e2eMural") == "true" && codigo.isNotBlank())
        val id = UUID.randomUUID().toString()
        val senha = "Teste-$id-42!"
        val auth = RepositorioAutenticacao()
        fun cadastrar(nome: String, prefixo: String, codigoProfessor: String? = null) = runBlocking {
            val email = "$prefixo-$id@example.invalid"
            auth.cadastrar(nome, email, senha, codigoProfessor); auth.entrar(email, senha)
        }
        val professor = cadastrar("Professora Mural", "prof-mural", codigo)
        val aluno = cadastrar("Aluno Mural", "aluno-mural")
        val colega = cadastrar("Colega Mural", "colega-mural")
        val fora = cadastrar("Conta sem matrícula", "fora-mural")
        val academico = RepositorioAcademico()
        val repo = RepositorioMural()
        val api = ClienteAcademico()
        val turma = runBlocking {
            academico.criarTurma(professor.token, DadosTurma("Turma Mural", "Engenharia de Software")).also {
                academico.entrarTurma(aluno.token, it.codigo!!); academico.entrarTurma(colega.token, it.codigo)
            }
        }
        var arquivada = false
        var expirou = false
        val modeloAluno = ModeloMural(repo, aluno.token, aluno.usuarioId) { expirou = true }
        val modeloProfessor = ModeloMural(repo, professor.token, professor.usuarioId) { expirou = true }
        var professorAtivo by mutableStateOf(false)
        try {
            compose.setContent { TemaUniSync {
                val modelo = if (professorAtivo) modeloProfessor else modeloAluno
                val estado by modelo.estado.collectAsState()
                key(professorAtivo) { TelaMural(estado, listOf(turma), { modelo.atualizar() }, modelo::atualizar, modelo::carregarMais,
                    modelo::abrir, modelo::fecharDetalhe, modelo::salvar, modelo::excluir, {}) }
            } }
            esperar(modeloAluno); esperar(modeloProfessor)
            compose.onNode(hasText("Nova publicação") and hasClickAction()).performClick()
            compose.onNodeWithText("Título da publicação").performTextInput("Projeto colaborativo Android")
            compose.onNodeWithText("Conteúdo da publicação").performScrollTo().performTextInput("Vamos compartilhar as dúvidas da atividade.")
            compose.onNodeWithText("Salvar publicação").performClick()
            compose.waitUntil(15000) { !modeloAluno.estado.value.salvando && modeloAluno.estado.value.confirmacao == 1 }
            assertNull(modeloAluno.estado.value.erro)
            val item = modeloAluno.estado.value.detalhe!!
            assertEquals(aluno.usuarioId, item.autorId)
            capturar("mural-publicacao-aluno.png")
            runBlocking {
                assertFalse(repo.buscar(colega.token, item.id).podeEditar)
                assertFalse(repo.buscar(colega.token, item.id).podeExcluir)
                assertEquals(403, (runCatching { repo.editar(colega.token, item.id, 1, DadosPublicacao(turma.id, "Tentativa", "Texto")) }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(403, (runCatching { repo.excluir(colega.token, item.id, 1) }.exceptionOrNull() as? FalhaApi)?.status)
                assertTrue(repo.listar(fora.token, null, null).itens.isEmpty())
                assertEquals(403, (runCatching { repo.buscar(fora.token, item.id) }.exceptionOrNull() as? FalhaApi)?.status)
                // Simula outro cliente usando a API com uma versão mais recente.
                val corpo = JSONObject().put("titulo", item.titulo).put("conteudo", "Revisão feita por outro cliente").put("versao", 1)
                api.executar("PATCH", "/mural/atualizar/${item.id}", aluno.token, corpo.toString())
            }
            compose.onNodeWithText("Editar publicação").performScrollTo().performClick()
            compose.onNodeWithText("Título da publicação").performTextReplacement("Rascunho em conflito")
            compose.onNodeWithText("Salvar publicação").performClick()
            compose.waitUntil(15000) { !modeloAluno.estado.value.salvando && modeloAluno.estado.value.erro != null }
            assertEquals(1, modeloAluno.estado.value.confirmacao)
            compose.onNodeWithText("Título da publicação").assertTextContains("Rascunho em conflito")
            compose.onNodeWithText("Cancelar").performClick(); compose.onNodeWithText("Descartar").performClick()
            compose.onNodeWithText("Fechar publicação").performClick()
            modeloAluno.atualizar(); esperar(modeloAluno)
            modeloAluno.abrir(item.id)
            compose.waitUntil(15000) { !modeloAluno.estado.value.carregandoDetalhe && modeloAluno.estado.value.detalhe?.versao == 2 }
            compose.onNodeWithText("Editar publicação").performScrollTo().performClick()
            compose.onNodeWithText("Título da publicação").performTextReplacement("Projeto revisado Android")
            compose.onNodeWithText("Salvar publicação").performClick()
            compose.waitUntil(15000) { !modeloAluno.estado.value.salvando && modeloAluno.estado.value.detalhe?.versao == 3 }
            compose.onNodeWithText("Fechar publicação").performClick()
            runBlocking { repeat(20) { numero -> repo.publicar(colega.token, DadosPublicacao(turma.id, "Dúvida de paginação $numero", "Conteúdo de teste da turma.")) } }
            modeloAluno.atualizar(); esperar(modeloAluno)
            assertEquals(20, modeloAluno.estado.value.itens.size); assertNotNull(modeloAluno.estado.value.proximaPagina)
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Carregar mais publicações"))
            compose.onNodeWithText("Carregar mais publicações").performClick()
            compose.waitUntil(15000) { !modeloAluno.estado.value.carregandoMais && modeloAluno.estado.value.itens.size == 21 }
            assertEquals(21, modeloAluno.estado.value.itens.map { it.id }.distinct().size)
            assertNull(modeloAluno.estado.value.proximaPagina)
            modeloProfessor.abrir(item.id)
            compose.waitUntil(15000) { !modeloProfessor.estado.value.carregandoDetalhe && modeloProfessor.estado.value.detalhe != null }
            compose.runOnIdle { professorAtivo = true }
            compose.onNodeWithText("Editar publicação").assertDoesNotExist()
            compose.onNodeWithText("Excluir publicação").performScrollTo().performClick()
            compose.onNodeWithText("Excluir").performClick()
            compose.waitUntil(15000) { !modeloProfessor.estado.value.salvando && modeloProfessor.estado.value.confirmacao == 1 }
            runBlocking { assertEquals(404, (runCatching { repo.buscar(aluno.token, item.id) }.exceptionOrNull() as? FalhaApi)?.status) }
            compose.onNode(hasText("Nova publicação") and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText("Título da publicação").performTextInput("Organização da apresentação")
            compose.onNodeWithText("Conteúdo da publicação").performScrollTo().performTextInput("Compartilhem suas ideias para a próxima aula.")
            compose.onNodeWithText("Salvar publicação").performClick()
            compose.waitUntil(15000) { !modeloProfessor.estado.value.salvando && modeloProfessor.estado.value.confirmacao == 2 }
            val publicacaoProfessor = modeloProfessor.estado.value.detalhe!!
            capturar("mural-publicacao-professor.png")
            runBlocking {
                val leitura = repo.buscar(aluno.token, publicacaoProfessor.id)
                assertEquals("Professor", leitura.papelAutor); assertFalse(leitura.podeEditar)
                academico.arquivarTurma(professor.token, turma.id); arquivada = true
                assertTrue(repo.listar(aluno.token, null, null).itens.isEmpty())
                assertEquals(404, (runCatching { repo.buscar(aluno.token, publicacaoProfessor.id) }.exceptionOrNull() as? FalhaApi)?.status)
            }
            assertFalse(expirou)
        } finally {
            modeloAluno.fechar(); modeloProfessor.fechar()
            if (!arquivada) runBlocking { academico.arquivarTurma(professor.token, turma.id) }
        }
    }
    private fun esperar(modelo: ModeloMural) {
        compose.waitUntil(15000) { !modelo.estado.value.lendo && !modelo.estado.value.atualizando }
        assertNull(modelo.estado.value.erro)
    }
    private fun capturar(nome: String) {
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, nome).outputStream().use {
            compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
