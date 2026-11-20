package br.com.unisync.mobile

import android.graphics.Bitmap
import android.content.Intent
import br.com.unisync.mobile.data.SessaoAutenticada
import br.com.unisync.mobile.data.local.SessaoSegura
import br.com.unisync.mobile.data.remote.ClienteAcademico
import org.junit.Assert.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.data.RepositorioAutenticacao
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class NavegacaoLocalE2ETest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun loginRealAbreDashboardNavegaERetornaAoInicioAposLogout() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("e2eLocal") == "true")
        val id = UUID.randomUUID().toString()
        val email = "interface-$id@example.invalid"
        val senha = "Teste-$id-42!"
        runBlocking { RepositorioAutenticacao().cadastrar("Teste Interface", email, senha, null) }
        compose.waitUntil(15000) { compose.onAllNodes(hasText("Login") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Login") and hasClickAction()).performClick()
        compose.onNodeWithText("E-mail").performTextInput(email)
        compose.onNodeWithText("Senha").performTextInput(senha)
        compose.onNode(hasText("Entrar") and hasClickAction()).performScrollTo().performClick()
        compose.waitUntil(20000) { compose.onAllNodesWithText("Olá, Teste Interface").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Meus lembretes"))
        compose.onNodeWithText("Meus lembretes").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Atualizando lembretes…").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Novo lembrete").performClick()
        compose.onNodeWithText("Título do lembrete").performTextInput("Lembrete da navegação")
        compose.onNodeWithText("Salvar lembrete").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Salvar lembrete").fetchSemanticsNodes().isEmpty() }
        val app = compose.activity.application as UniSyncApplication
        val sessao = runBlocking { SessaoAutenticada.doToken(SessaoSegura(app).lerToken()!!) }
        val lembrete = runBlocking { app.banco.lembretes().listar(sessao.usuarioId).single() }
        try {
        compose.onNode(hasText("Tarefas") and hasClickAction()).performClick()
        compose.onNodeWithText("Nova tarefa").assertExists()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Atualizando tarefas…").fetchSemanticsNodes().isEmpty() }
        val arquivo = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "navegacao-layout.png")
        arquivo.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
        compose.onNode(hasContentDescription("Oportunidades") and hasClickAction()).performClick()
        compose.onNodeWithText("Buscar oportunidades").assertExists()
        compose.onNode(hasText("Perfil") and hasClickAction()).performClick()
        compose.onNodeWithText("Usar tema do sistema").assertExists()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Lembretes: desativados"))
        compose.onNodeWithText("Lembretes: desativados").assertExists()
        // O mesmo destino e extras usados pelo PendingIntent da notificação.
        compose.runOnIdle { compose.activity.startActivity(Intent(app, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("usuario_alerta", sessao.usuarioId).putExtra("destino_alerta", "lembretes").putExtra("item_alerta", lembrete.id)) }
        compose.waitUntil(15000) { compose.onAllNodesWithText("Editar lembrete").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Título do lembrete").assertTextContains("Lembrete da navegação")
        compose.onNodeWithText("Cancelar").performClick()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("Editar lembrete").assertDoesNotExist()
        compose.onNode(hasText("Perfil") and hasClickAction()).performClick()
        compose.runOnIdle { compose.activity.startActivity(Intent(app, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("usuario_alerta", "outra-conta").putExtra("destino_alerta", "lembretes").putExtra("item_alerta", lembrete.id)) }
        compose.waitForIdle()
        compose.onNodeWithText("Editar lembrete").assertDoesNotExist()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Sair da conta"))
        compose.waitUntil(15000) { compose.onNodeWithText("Sair da conta").isEnabled() }
        compose.onNodeWithText("Sair da conta").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodes(hasText("Login") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        runBlocking { assertTrue(app.banco.lembretes().listar(sessao.usuarioId).isEmpty()); assertTrue(app.banco.alertas().listar(sessao.usuarioId).isEmpty()) }
        } finally { runBlocking {
            try { ClienteAcademico().executar("DELETE", "/lembretes/excluir/${lembrete.id}", sessao.token) }
            finally { app.limparDadosPrivados(); SessaoSegura(app).limpar() }
        } }
    }
    private fun SemanticsNodeInteraction.isEnabled(): Boolean = runCatching { assertIsEnabled(); true }.getOrDefault(false)
}
