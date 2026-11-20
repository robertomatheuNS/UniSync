package br.com.unisync.mobile

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.data.*
import br.com.unisync.mobile.data.local.ModoTema
import br.com.unisync.mobile.data.remote.ClienteAcademico
import br.com.unisync.mobile.domain.academico.*
import br.com.unisync.mobile.domain.oportunidades.DadosOportunidade
import br.com.unisync.mobile.ui.principal.AreaPrincipal
import br.com.unisync.mobile.ui.theme.TemaUniSync
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.Instant
import org.json.JSONObject
import java.util.UUID

/** Navegação real por perfil com contas técnicas, sem trocar a sessão salva da instalação. */
class InicioLocalE2ETest {
    @get:Rule val compose = createAndroidComposeRule<AtividadeTeste>()
    @Test fun painelAbreItensDoAlunoConfirmaLeituraEPermiteGerenciamentoDoProfessor() {
        val args = InstrumentationRegistry.getArguments()
        val codigo = args.getString("codigoProfessorInicio").orEmpty()
        assumeTrue(args.getString("e2eInicio") == "true" && codigo.isNotBlank())
        val id = UUID.randomUUID().toString()
        val auth = RepositorioAutenticacao()
        val senha = "Teste-$id-42!"
        fun cadastrar(nome: String, prefixo: String, codigoProfessor: String?) = runBlocking {
            val email = "$prefixo-$id@example.invalid"
            auth.cadastrar(nome, email, senha, codigoProfessor); auth.entrar(email, senha)
        }
        val professor = cadastrar("Professora Painel", "prof-inicio", codigo)
        val aluno = cadastrar("Aluno Painel", "aluno-inicio", null)
        val repo = RepositorioAcademico()
        val app = compose.activity.application as UniSyncApplication
        val turma = runBlocking { repo.criarTurma(professor.token, DadosTurma("Turma Painel", "Engenharia de Software")) }
        val reservada = runBlocking { repo.criarTurma(professor.token, DadosTurma("Turma Reservada", "Outra disciplina")) }
        var oportunidadeId: String? = null
        var sessao by mutableStateOf(aluno)
        var expirou = false
        try {
            val avisos = runBlocking {
                repo.entrarTurma(aluno.token, turma.codigo!!)
                repo.publicarAviso(professor.token, DadosAviso(reservada.id, "Aviso reservado", "Texto de outra turma."))
                (1..4).map { numero -> repo.publicarAviso(professor.token, DadosAviso(turma.id, "Aviso Painel $numero", "Conteúdo completo do aviso $numero.")) }
            }
            val aviso = avisos.last()
            val tituloOportunidade = "Oficina Painel ${id.take(8)}"
            val link = "https://example.org/inscricao"
            val prazo = Instant.parse("1999-12-31T23:59:00Z")
            val oportunidade = runBlocking {
                // Data antiga garante presença no recorte ordenado por evento da API, mesmo com dados de outros testes.
                app.oportunidades.publicar(professor.token, professor.nome, DadosOportunidade(tituloOportunidade, "Kotlin", "Conteúdo completo da oficina do painel.", "Workshop", LocalDateTime.of(2000, 1, 1, 12, 0), link, prazo))
                app.oportunidades.itens.first().single { it.titulo == tituloOportunidade }
            }
            oportunidadeId = oportunidade.id
            runBlocking {
                val api = ClienteAcademico()
                val editada = JSONObject(api.executar("PATCH", "/oportunidades/atualizar/${oportunidade.id}", professor.token,
                    JSONObject().put("materia", "Kotlin atualizado").toString()))
                assertEquals(link, editada.getString("url_inscricao"))
                assertEquals(prazo, Instant.parse(editada.getString("prazo_inscricao")))
            }
            compose.setContent { TemaUniSync { key(sessao.token) {
                AreaPrincipal(sessao, app, ModoTema.SISTEMA, { expirou = true }, {}, null, false)
            } } }
            compose.waitUntil(20000) { compose.onAllNodesWithTag("inicio-aviso:${aviso.id}").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Aviso reservado").assertDoesNotExist()
            compose.onNodeWithText("Aviso Painel 1").assertDoesNotExist()
            compose.onNodeWithTag("inicio-aviso:${aviso.id}").performScrollTo().performClick()
            compose.onNode(hasText(aviso.conteudo) and hasAnyAncestor(isDialog())).assertExists()
            compose.onNodeWithText("Editar aviso").assertDoesNotExist()
            runBlocking { assertFalse(repo.carregar(aluno.token).avisos.single { it.id == aviso.id }.lido) }
            compose.onNodeWithText("Marcar como lido").performScrollTo().performClick()
            compose.waitUntil(15000) { compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty() }
            runBlocking { assertTrue(repo.carregar(aluno.token).avisos.single { it.id == aviso.id }.lido) }
            voltar()
            compose.onNodeWithTag("inicio-aviso:${aviso.id}").performScrollTo().assertTextContains("Lido")
            compose.onNode(hasScrollAction()).performScrollToNode(hasText(tituloOportunidade))
            compose.onNodeWithTag("inicio-oportunidade:${oportunidade.id}").performClick()
            compose.onNodeWithText("Conteúdo completo da oficina do painel.").assertExists()
            compose.onNode(hasText("Prazo de inscrição:", substring = true) and hasAnyAncestor(isDialog())).assertExists()
            compose.onNodeWithText("Abrir site de inscrição").assertDoesNotExist()
            compose.onNodeWithText("Editar oportunidade").assertDoesNotExist()
            compose.onNodeWithText("Fechar").performClick(); voltar()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Olá, Aluno Painel"))
            compose.onNodeWithText("Olá, Aluno Painel").assertExists()
            compose.runOnIdle { sessao = professor }
            compose.waitUntil(20000) { compose.onAllNodesWithTag("inicio-aviso:${aviso.id}").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("inicio-aviso:${aviso.id}").performScrollTo().performClick()
            compose.onNodeWithText("Editar aviso").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Marcar como lido").assertDoesNotExist()
            compose.onNodeWithText("Fechar").performClick(); voltar()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText(tituloOportunidade))
            compose.onNodeWithTag("inicio-oportunidade:${oportunidade.id}").performClick()
            compose.onNodeWithText("Editar oportunidade").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Fechar").performClick(); voltar()
            runBlocking {
                app.oportunidades.editar(professor.token, oportunidade.id, DadosOportunidade(tituloOportunidade,
                    "Kotlin", "Inscrições removidas", "Workshop", LocalDateTime.of(2000, 1, 1, 12, 0)))
                val limpa = app.oportunidades.itens.first().single { it.id == oportunidade.id }
                assertNull(limpa.urlInscricao); assertNull(limpa.prazoInscricao)
            }
            compose.onNode(isDialog()).assertDoesNotExist()
            assertFalse(expirou)
        } finally {
            runBlocking {
                try {
                    oportunidadeId?.let { ClienteAcademico().executar("DELETE", "/oportunidades/excluir/$it", professor.token) }
                } finally {
                    repo.arquivarTurma(professor.token, turma.id); repo.arquivarTurma(professor.token, reservada.id)
                    app.limparDadosPrivados()
                }
            }
        }
    }
    private fun voltar() {
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }
}
