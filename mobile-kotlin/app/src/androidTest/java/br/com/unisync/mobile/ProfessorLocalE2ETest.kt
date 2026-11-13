package br.com.unisync.mobile

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.data.*
import br.com.unisync.mobile.data.remote.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID

/** Opt-in: requer dois códigos de professor novos no ambiente local. Nunca usa contas reais. */
class ProfessorLocalE2ETest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun cadastraProfessorPelaTelaPublicaEditaExcluiEValidaAutorizacaoNoServidor() {
        val argumentos = InstrumentationRegistry.getArguments()
        val codigoA = argumentos.getString("codigoProfessorA").orEmpty()
        val codigoB = argumentos.getString("codigoProfessorB").orEmpty()
        assumeTrue(argumentos.getString("e2eLocal") == "true" && codigoA.isNotBlank() && codigoB.isNotBlank())
        val id = UUID.randomUUID().toString()
        val email = "prof-mobile-$id@example.invalid"
        val senha = "Teste-$id-42!"
        val titulo = "Palestra Android $id"
        val novoTitulo = "Palestra revisada $id"
        val api = RepositorioAutenticacao()
        val academico = ClienteAcademico()
        var sessao: SessaoAutenticada? = null
        var idCriada: String? = null
        try {
            compose.waitUntil(15000) { compose.onAllNodes(hasText("Cadastre-se") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasText("Cadastre-se") and hasClickAction()).performClick()
            compose.onNodeWithText("Professor").performScrollTo().performClick()
            compose.onNodeWithText("Nome completo").performTextInput("Professora Mobile")
            compose.onNodeWithText("E-mail").performTextInput(email)
            compose.onNodeWithText("Senha").performTextInput(senha)
            compose.onNodeWithText("Código do professor").performScrollTo().performTextInput(codigoA)
            compose.onNode(hasText("Criar cadastro") and hasClickAction()).performScrollTo().performClick()
            compose.waitUntil(20000) { compose.onAllNodesWithText("Cadastro realizado").fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasText("Entrar") and hasClickAction()).performClick()
            compose.onNodeWithText("E-mail").performTextInput(email)
            compose.onNodeWithText("Senha").performTextInput(senha)
            compose.onNode(hasText("Entrar") and hasClickAction()).performScrollTo().performClick()
            compose.waitUntil(20000) { compose.onAllNodesWithText("Olá, Professora Mobile").fetchSemanticsNodes().isNotEmpty() }
            sessao = runBlocking { api.entrar(email, senha) }
            assertEquals(PerfilUsuario.PROFESSOR, sessao.perfil)
            compose.onNode(hasContentDescription("Oportunidades") and hasClickAction()).performClick()
            compose.waitUntil(15000) { runCatching { compose.onNode(hasText("Publicar oportunidade") and hasClickAction()).assertIsEnabled(); true }.getOrDefault(false) }
            compose.onNode(hasText("Publicar oportunidade") and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText("Título").performTextInput(titulo)
            compose.onNodeWithText("Matéria").performTextInput("Engenharia de Software")
            compose.onNodeWithText("Categoria").performTextInput("Palestra")
            compose.onNodeWithText("Descrição (opcional)").performScrollTo().performTextInput("Publicação de teste Android.")
            compose.onNodeWithText("Publicar").performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithText("Publicar").fetchSemanticsNodes().isEmpty() }
            val itens = runBlocking { JSONArray(academico.executar("GET", "/oportunidades/listar", sessao.token)) }
            val publicada = (0 until itens.length()).map { itens.getJSONObject(it) }.single { it.getString("titulo") == titulo }
            idCriada = publicada.getString("id")
            assertEquals(sessao.usuarioId, publicada.getString("professor_id"))

            // O servidor deve recusar aluno e outro professor, mesmo sem passar pelos controles da UI.
            runBlocking {
                api.cadastrar("Outro Professor", "outro-prof-$id@example.invalid", senha, codigoB)
                val outro = api.entrar("outro-prof-$id@example.invalid", senha)
                val alteracao = JSONObject().put("titulo", "Alteração não autorizada").toString()
                assertEquals(403, (runCatching { academico.executar("PATCH", "/oportunidades/atualizar/$idCriada", outro.token, alteracao) }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(403, (runCatching { academico.executar("DELETE", "/oportunidades/excluir/$idCriada", outro.token) }.exceptionOrNull() as? FalhaApi)?.status)
                api.cadastrar("Aluno Teste", "aluno-prof-$id@example.invalid", senha, null)
                val aluno = api.entrar("aluno-prof-$id@example.invalid", senha)
                val corpo = JSONObject().put("titulo", "Publicação não autorizada").put("materia", "Kotlin")
                    .put("tipo", "Palestra").put("professor", "Aluno").put("data_evento", OffsetDateTime.now().plusDays(1).toString())
                assertEquals(403, (runCatching { academico.executar("POST", "/oportunidades/criar", aluno.token, corpo.toString()) }.exceptionOrNull() as? FalhaApi)?.status)
            }
            compose.onNodeWithText(titulo).performScrollTo().performClick()
            compose.onNodeWithText("Editar oportunidade").performScrollTo().performClick()
            compose.onNodeWithText("Título").performTextReplacement(novoTitulo)
            val captura = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "professor-formulario.png")
            captura.outputStream().use { compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
            compose.onNodeWithText("Salvar alterações").performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithText("Salvar alterações").fetchSemanticsNodes().isEmpty() }
            val alterada = runBlocking { JSONObject(academico.executar("GET", "/oportunidades/buscar/$idCriada", sessao.token)) }
            assertEquals(novoTitulo, alterada.getString("titulo"))
            compose.onNodeWithText(novoTitulo).performScrollTo().performClick()
            compose.onNodeWithText("Excluir oportunidade").performScrollTo().performClick()
            compose.onNodeWithText("Excluir").performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithText(novoTitulo).fetchSemanticsNodes().isEmpty() }
            assertEquals(404, runBlocking { (runCatching { academico.executar("GET", "/oportunidades/buscar/$idCriada", sessao.token) }.exceptionOrNull() as? FalhaApi)?.status })
            idCriada = null
            compose.onNode(hasContentDescription("Perfil") and hasClickAction()).performClick()
            compose.onNodeWithText("Sair da conta").performScrollTo().performClick()
            compose.waitUntil(15000) { compose.onAllNodes(hasText("Login") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        } finally {
            val token = sessao?.token
            if (idCriada != null && token != null) runBlocking { academico.executar("DELETE", "/oportunidades/excluir/$idCriada", token) }
        }
    }
}
