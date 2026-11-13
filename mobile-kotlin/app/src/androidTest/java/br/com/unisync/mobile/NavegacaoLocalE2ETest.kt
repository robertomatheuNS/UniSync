package br.com.unisync.mobile

import android.graphics.Bitmap
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
        compose.onNode(hasText("Tarefas") and hasClickAction()).performClick()
        compose.onNodeWithText("Nova tarefa").assertExists()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Atualizando tarefas…").fetchSemanticsNodes().isEmpty() }
        val arquivo = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "navegacao-layout.png")
        arquivo.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
        compose.onNode(hasContentDescription("Oportunidades") and hasClickAction()).performClick()
        compose.onNodeWithText("Buscar oportunidades").assertExists()
        compose.onNode(hasText("Perfil") and hasClickAction()).performClick()
        compose.onNodeWithText("Usar tema do sistema").assertExists()
        compose.waitUntil(15000) { compose.onNodeWithText("Sair da conta").isEnabled() }
        compose.onNodeWithText("Sair da conta").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodes(hasText("Login") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun SemanticsNodeInteraction.isEnabled(): Boolean = runCatching { assertIsEnabled(); true }.getOrDefault(false)
}
