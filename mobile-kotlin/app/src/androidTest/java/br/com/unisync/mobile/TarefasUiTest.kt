package br.com.unisync.mobile

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.domain.tarefas.*
import br.com.unisync.mobile.ui.tarefas.*
import br.com.unisync.mobile.ui.theme.TemaUniSync
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class TarefasUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun editaTarefaComStatusSemArrastarECapturaLayout() {
        var salvo: Triple<String?, String, StatusTarefa>? = null
        compose.setContent {
            TemaUniSync {
                TelaTarefas(EstadoTarefas(itens = listOf(Tarefa("id", "aluno", "Estudar Kotlin", StatusTarefa.A_FAZER)), lendo = false), {},
                    { id, titulo, status -> salvo = Triple(id, titulo, status) }, {})
            }
        }
        compose.onNodeWithText("Estudar Kotlin").assertExists()
        val arquivo = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "tarefas-layout.png")
        arquivo.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithText("Editar").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Revisar Kotlin")
        compose.onNodeWithText("Fazendo").performScrollTo().performClick()
        compose.onNodeWithText("Salvar").performClick()
        compose.runOnIdle { assertEquals(Triple("id", "Revisar Kotlin", StatusTarefa.EM_ANDAMENTO), salvo) }
    }
}
