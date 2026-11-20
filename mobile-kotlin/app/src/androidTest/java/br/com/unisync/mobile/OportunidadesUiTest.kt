package br.com.unisync.mobile

import android.graphics.Bitmap
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
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
import java.time.Instant
import br.com.unisync.mobile.domain.oportunidades.DadosOportunidade
import br.com.unisync.mobile.domain.tarefas.*
import br.com.unisync.mobile.ui.tarefas.EstadoTarefas

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
    @Test fun agendaIncluiPrazosEAbreTarefaSelecionada() {
        val tarefa = Tarefa("prazo", "aluno", "Entrega do projeto", StatusTarefa.A_FAZER,
            PrioridadeTarefa.ALTA, java.time.LocalDate.now(), disciplina = "Kotlin")
        var aberta: Tarefa? = null
        compose.setContent { TemaUniSync { Surface {
            TelaCalendario(EstadoOportunidades(listOf(evento), lendo = false), {}, EstadoTarefas(listOf(tarefa), lendo = false), { aberta = it })
        } } }
        compose.onNodeWithText("Entrega do projeto").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Abrir tarefa").performScrollTo().performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(tarefa, aberta) }
        compose.onNodeWithText("Dia").performScrollTo().performClick()
        compose.onNodeWithText("Entrega do projeto").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Próximo").performScrollTo().performClick()
        compose.onNodeWithText("Entrega do projeto").assertDoesNotExist()
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

    @Test fun detalheAbreLinkHttpsECompartilhaPeloSeletorAndroid() {
        val link = "https://example.org/inscricao"
        val item = evento.copy(dataEvento = Instant.now().plusSeconds(86400).toString(), urlInscricao = link,
            prazoInscricao = Instant.now().plusSeconds(3600))
        val acoes = mutableListOf<Intent>()
        compose.setContent {
            val base = LocalContext.current
            val contexto = remember(base) { object : ContextWrapper(base) {
                override fun startActivity(intent: Intent) { acoes.add(intent) }
            } }
            CompositionLocalProvider(LocalContext provides contexto) {
                TemaUniSync(temaEscuro = true) { Surface {
                    TelaOportunidades(EstadoOportunidades(listOf(item), lendo = false), {}, oportunidadeInicial = item.id)
                } }
            }
        }
        compose.onNodeWithText("Abrir site de inscrição").performScrollTo().assertIsDisplayed()
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "oportunidade-inscricao.png").outputStream().use {
            compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Abrir site de inscrição").performClick()
        compose.onNodeWithText("Compartilhar oportunidade").performScrollTo().performClick()
        compose.runOnIdle {
            org.junit.Assert.assertEquals(Intent.ACTION_VIEW, acoes[0].action)
            org.junit.Assert.assertEquals(link, acoes[0].dataString)
            org.junit.Assert.assertEquals(Intent.ACTION_CHOOSER, acoes[1].action)
            val enviado = IntentCompat.getParcelableExtra(acoes[1], Intent.EXTRA_INTENT, Intent::class.java)!!
            org.junit.Assert.assertEquals(Intent.ACTION_SEND, enviado.action)
            val texto = enviado.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            org.junit.Assert.assertTrue(texto.contains(link) && texto.contains(item.titulo) && texto.contains("Prazo de inscrição"))
        }
    }

    @Test fun agendaAbrePrazoDeInscricaoMesmoQuandoEventoEstaForaDoMes() {
        val item = evento.copy(dataEvento = Instant.now().plusSeconds(86400L * 45).toString(), prazoInscricao = Instant.now().plusSeconds(120))
        compose.setContent { TemaUniSync { Surface { TelaCalendario(EstadoOportunidades(listOf(item), lendo = false), {}) } } }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Prazo de inscrição"))
        compose.onNodeWithText("Prazo de inscrição").performClick()
        compose.onNodeWithText("Inscrições encerradas").assertDoesNotExist()
        compose.onNode(hasText("Prazo de inscrição:", substring = true) and hasAnyAncestor(isDialog())).assertExists()
    }

    @Test fun professorPodeCorrigirLinkInvalidoERemoverPrazo() {
        val item = evento.copy(urlInscricao = "https://example.org/inscricao", prazoInscricao = Instant.now().plusSeconds(1800))
        var dados: DadosOportunidade? = null
        compose.setContent { TemaUniSync { FormularioOportunidade(item, false, null, { dados = it }, {}) } }
        val link = compose.onNodeWithText("Link de inscrição (opcional)")
        link.performScrollTo().performTextReplacement("http://example.org")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Salvar alterações").performClick()
        link.performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Informe um endereço HTTPS sem usuário ou senha.").assertExists()
        compose.runOnIdle { org.junit.Assert.assertNull(dados) }
        link.performTextReplacement("https://example.org/novo")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Remover prazo").performScrollTo().performClick()
        compose.onNodeWithText("Salvar alterações").performClick()
        compose.runOnIdle {
            org.junit.Assert.assertEquals("https://example.org/novo", dados?.urlInscricao)
            org.junit.Assert.assertNull(dados?.prazoInscricao)
        }
    }
}
