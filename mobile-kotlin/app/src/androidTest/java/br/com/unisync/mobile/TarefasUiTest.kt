package br.com.unisync.mobile

import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.domain.tarefas.*
import br.com.unisync.mobile.ui.tarefas.*
import br.com.unisync.mobile.ui.theme.TemaUniSync
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

class TarefasUiTest {
    @get:Rule val compose = createAndroidComposeRule<AtividadeTeste>()
    private val tarefa = Tarefa("id", "aluno", "Estudar Kotlin", StatusTarefa.A_FAZER,
        PrioridadeTarefa.ALTA, LocalDate.of(2027, 1, 20), "123456789012345678901234", "Kotlin")

    @Test fun editaStatusPreservaPrazoPrioridadeEDisciplina() {
        var salvo: Pair<String?, DadosTarefa>? = null
        compose.setContent { TemaUniSync { Surface {
            TelaTarefas(EstadoTarefas(itens = listOf(tarefa), lendo = false), {}, { id, dados -> salvo = id to dados }, {})
        } } }
        compose.onNodeWithText("Estudar Kotlin").assertExists()
        capturar("tarefas-layout.png", false)
        compose.onNodeWithText("Editar").performScrollTo().performClick()
        compose.onNodeWithText("Título").performTextReplacement("Revisar Kotlin")
        compose.onNodeWithText("Fazendo").performScrollTo().performClick()
        compose.onNodeWithText("Salvar").performClick()
        compose.runOnIdle { assertEquals("id" to tarefa.dados().copy(titulo = "Revisar Kotlin", status = StatusTarefa.EM_ANDAMENTO), salvo) }
    }
    @Test fun dataInvalidaMantemRascunhoERemoverPrazoPermiteSalvar() {
        var salvo: DadosTarefa? = null
        compose.setContent { TemaUniSync { Surface {
            TelaTarefas(EstadoTarefas(itens = listOf(tarefa), lendo = false), {}, { _, dados -> salvo = dados }, {})
        } } }
        compose.onNodeWithText("Editar").performScrollTo().performClick()
        compose.onNodeWithText("Prazo (opcional)").performScrollTo().performTextReplacement("31/02/2027")
        compose.onNodeWithText("Salvar").performClick()
        compose.onNodeWithText("Data inválida. Use dd/mm/aaaa.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertNull(salvo) }
        compose.onNodeWithText("Remover prazo").performScrollTo().performClick()
        compose.onNodeWithText("Salvar").performClick()
        compose.runOnIdle { assertEquals(tarefa.dados().copy(prazo = null), salvo) }
    }
    @Test fun formularioEscuroEmPaisagemComFonteAmpliadaPermiteSalvar() {
        var salvo: DadosTarefa? = null
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(640.dp, 360.dp)) then DeviceConfigurationOverride.FontScale(2f)) {
                TemaUniSync(temaEscuro = true) { Surface {
                    TelaTarefas(EstadoTarefas(lendo = false), {}, { _, dados -> salvo = dados }, {})
                } }
            }
        }
        compose.onNodeWithText("Nova tarefa").performScrollTo().performClick()
        compose.onNodeWithText("Título").performScrollTo().performTextInput("Entrega acessível")
        compose.onNodeWithText("Alta").performScrollTo().performClick()
        capturar("tarefa-formulario-escuro.png", true)
        compose.onNodeWithText("Salvar").performClick()
        compose.runOnIdle { assertEquals(PrioridadeTarefa.ALTA, salvo?.prioridade) }
    }
    private fun capturar(nome: String, dialogo: Boolean) {
        val arquivo = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, nome)
        arquivo.outputStream().use { (if (dialogo) compose.onNode(isDialog()) else compose.onRoot()).captureToImage()
            .asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
