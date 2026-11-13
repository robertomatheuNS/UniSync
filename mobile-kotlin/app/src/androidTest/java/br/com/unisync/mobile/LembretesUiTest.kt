package br.com.unisync.mobile

import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.domain.lembretes.*
import br.com.unisync.mobile.ui.lembretes.*
import br.com.unisync.mobile.ui.oportunidades.*
import br.com.unisync.mobile.ui.theme.TemaUniSync
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.*

class LembretesUiTest {
    @get:Rule val compose = createAndroidComposeRule<AtividadeTeste>()
    private val item = Lembrete("a", "eu", "Revisar projeto", agendadoEm = Instant.parse("2028-02-29T12:30:00Z"))
    @Test fun dataInvalidaMantemFormularioERemoverAgendamentoPermiteSalvar() {
        var salvo: DadosLembrete? = null
        compose.setContent { TemaUniSync { Surface {
            TelaLembretes(EstadoLembretes(listOf(item), lendo = false), {}, { _, dados -> salvo = dados }, {}, {})
        } } }
        compose.onNodeWithText("Editar").performScrollTo().performClick()
        compose.onNodeWithText("Data (opcional)").performScrollTo().performTextReplacement("31/02/2028")
        compose.onNodeWithText("Salvar lembrete").performClick()
        compose.onNodeWithText("Informe uma data válida no formato dd/mm/aaaa.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertNull(salvo) }
        compose.onNodeWithText("Remover agendamento").performScrollTo().performClick()
        compose.onNodeWithText("Salvar lembrete").performClick()
        compose.runOnIdle { assertEquals(item.dados().copy(agendadoEm = null), salvo) }
    }
    @Test fun editarTituloPreservaInstanteEConfirmacaoAnteriorNaoFechaNovoRascunho() {
        var salvo: DadosLembrete? = null
        compose.setContent { TemaUniSync { Surface {
            TelaLembretes(EstadoLembretes(listOf(item), lendo = false, confirmacao = 2), {}, { _, dados -> salvo = dados }, {}, {}, lembreteInicial = item.id)
        } } }
        compose.onNodeWithText("Título do lembrete").performTextReplacement("Revisar Kotlin")
        compose.onNodeWithText("Salvar lembrete").performClick()
        compose.runOnIdle { assertEquals(item.dados().copy(titulo = "Revisar Kotlin"), salvo) }
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Descartar alterações?").assertExists()
        compose.onNodeWithText("Continuar editando").performClick()
        compose.onNodeWithText("Título do lembrete").assertTextContains("Revisar Kotlin")
    }
    @Test fun formularioEscuroComFonteAmpliadaFuncionaEmPaisagem() {
        var salvo: DadosLembrete? = null
        compose.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(640.dp, 360.dp)) then DeviceConfigurationOverride.FontScale(2f)) {
            TemaUniSync(temaEscuro = true) { Surface {
                TelaLembretes(EstadoLembretes(lendo = false), {}, { _, dados -> salvo = dados }, {}, {})
            } }
        } }
        compose.onNodeWithText("Novo lembrete").performScrollTo().performClick()
        compose.onNodeWithText("Título do lembrete").performScrollTo().performTextInput("Apresentar projeto")
        compose.onNodeWithText("Data (opcional)").performScrollTo().performTextInput("29/02/2028")
        compose.onNodeWithText("Horário (opcional)").performScrollTo().performTextInput("09:30")
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "lembrete-formulario-escuro.png").outputStream().use {
            compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Salvar lembrete").performClick()
        compose.runOnIdle { assertNotNull(salvo?.agendadoEm) }
    }
    @Test fun agendaMostraLembreteNoDiaDoAparelhoEAbreItem() {
        val agora = ZonedDateTime.now().withHour(14).withMinute(30)
        val lembrete = item.copy(agendadoEm = agora.toInstant())
        var aberto: Lembrete? = null
        compose.setContent { TemaUniSync { Surface {
            TelaCalendario(EstadoOportunidades(lendo = false), {}, lembretes = EstadoLembretes(listOf(lembrete), lendo = false), aoAbrirLembrete = { aberto = it })
        } } }
        compose.onNodeWithText("Revisar projeto").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Abrir lembrete").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(lembrete, aberto) }
        compose.onNodeWithText("Dia").performScrollTo().performClick()
        compose.onNodeWithText("Revisar projeto").performScrollTo().assertIsDisplayed()
    }
}
