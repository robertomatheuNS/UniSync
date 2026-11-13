package br.com.unisync.mobile

import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.domain.oportunidades.Oportunidade
import br.com.unisync.mobile.ui.oportunidades.*
import br.com.unisync.mobile.ui.theme.TemaUniSync
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

class OportunidadesUiTest {
    @get:Rule val compose = createComposeRule()
    private val evento = Oportunidade("id", "Conferência Android", "Professora Ana", "Kotlin",
        "Desenvolvimento de aplicativos Android.", "Palestra", LocalDateTime.now().plusHours(1).toString())
    @Test fun buscaAbreDetalheSemDependerDeImagemOuLink() {
        compose.setContent { TemaUniSync { Surface {
            TelaOportunidades(EstadoOportunidades(listOf(evento), lendo = false), {})
        } } }
        compose.onNodeWithText("Buscar oportunidades").performTextInput("Conferência")
        compose.onNodeWithText("Conferência Android").performScrollTo().performClick()
        compose.onNodeWithText("Desenvolvimento de aplicativos Android.").assertExists()
        compose.onNodeWithText("Fechar").performClick()
    }
    @Test fun calendarioMostraEventoNoPeriodoAtual() {
        compose.setContent { TemaUniSync { Surface {
            TelaCalendario(EstadoOportunidades(listOf(evento), lendo = false), {})
        } } }
        compose.onNodeWithText("Conferência Android").performScrollTo().assertIsDisplayed()
        val arquivo = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "calendario-layout.png")
        arquivo.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
        compose.onNodeWithText("Semana").performScrollTo().performClick()
        compose.onNodeWithText("Conferência Android").performScrollTo().assertIsDisplayed()
    }
}
