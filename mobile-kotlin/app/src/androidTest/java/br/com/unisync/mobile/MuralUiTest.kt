package br.com.unisync.mobile

import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.domain.academico.Turma
import br.com.unisync.mobile.domain.mural.*
import br.com.unisync.mobile.ui.mural.*
import br.com.unisync.mobile.ui.theme.TemaUniSync
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant

class MuralUiTest {
    @get:Rule val compose = createAndroidComposeRule<AtividadeTeste>()
    private val turma = Turma("111111111111111111111111", "Turma A", "Kotlin", "2026.2", "prof", "Ana", null, 2)
    private val item = Publicacao("222222222222222222222222", turma.id, turma.nome, turma.disciplina, "aluno", "Bruno", "Aluno",
        "Dúvida sobre Kotlin", "Resumo da dúvida", Instant.EPOCH, Instant.EPOCH, 3, true, true, "Conteúdo completo da dúvida")
    @Test fun formularioValidaPreservaRascunhoEConfirmaDescarte() {
        var salvo: DadosPublicacao? = null
        compose.setContent { TemaUniSync { Surface {
            TelaMural(EstadoMural(lendo = false), listOf(turma), {}, {}, {}, {}, {}, { _, _, dados -> salvo = dados }, {}, {})
        } } }
        compose.onNode(hasText("Nova publicação") and hasClickAction()).performClick()
        compose.onNodeWithText("Salvar publicação").performClick()
        compose.onNodeWithText("Use entre 3 e 100 caracteres.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertNull(salvo) }
        compose.onNodeWithText("Título da publicação").performScrollTo().performTextInput("Pergunta Kotlin")
        compose.onNodeWithText("Conteúdo da publicação").performScrollTo().performTextInput("Como organizar o projeto?")
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Continuar editando").performClick()
        compose.onNodeWithText("Salvar publicação").performClick()
        compose.runOnIdle { assertEquals(DadosPublicacao(turma.id, "Pergunta Kotlin", "Como organizar o projeto?"), salvo) }
    }
    @Test fun autorEditaComVersaoEProfessorModeraSemEditar() {
        var salvo: Pair<Int?, DadosPublicacao>? = null
        var removido: Publicacao? = null
        var estado by mutableStateOf(EstadoMural(listOf(item.copy(conteudo = null)), lendo = false, abrindoId = item.id, detalhe = item))
        compose.setContent { TemaUniSync { Surface {
            TelaMural(estado, listOf(turma), {}, {}, {}, {}, {}, { _, versao, dados -> salvo = versao to dados }, { removido = it }, {})
        } } }
        compose.onNodeWithText("Editar publicação").performScrollTo().performClick()
        compose.onNodeWithText("Título da publicação").performTextReplacement("Revisão da dúvida")
        compose.onNodeWithText("Salvar publicação").performClick()
        compose.runOnIdle { assertEquals(3, salvo?.first) }
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Descartar").performClick()
        compose.runOnIdle { estado = estado.copy(detalhe = item.copy(podeEditar = false)) }
        compose.onNodeWithText("Editar publicação").assertDoesNotExist()
        compose.onNodeWithText("Excluir publicação").performScrollTo().performClick()
        compose.onNodeWithText("Excluir").performClick()
        compose.runOnIdle { assertEquals(item.id, removido?.id); assertEquals(3, removido?.versao) }
    }
    @Test fun formularioEscuroEmPaisagemComFonteAmpliada() {
        var salvo = false
        compose.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(640.dp, 360.dp)) then DeviceConfigurationOverride.FontScale(2f)) {
            TemaUniSync(temaEscuro = true) { Surface {
                TelaMural(EstadoMural(lendo = false), listOf(turma), {}, {}, {}, {}, {}, { _, _, _ -> salvo = true }, {}, {})
            } }
        } }
        compose.onNode(hasText("Nova publicação") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Título da publicação").performScrollTo().performTextInput("Projeto em grupo")
        compose.onNodeWithText("Conteúdo da publicação").performScrollTo().performTextInput("Vamos organizar as atividades da turma.")
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "mural-formulario-escuro.png").outputStream().use {
            compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Salvar publicação").performClick()
        compose.runOnIdle { assertTrue(salvo) }
    }
    @Test fun leitorAbreConteudoCompletoSemEditarNemExcluir() {
        compose.setContent { TemaUniSync { Surface {
            TelaMural(EstadoMural(listOf(item.copy(conteudo = null)), lendo = false, abrindoId = item.id,
                detalhe = item.copy(podeEditar = false, podeExcluir = false)), listOf(turma), {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {})
        } } }
        compose.onNodeWithText("Conteúdo completo da dúvida").assertExists()
        compose.onNodeWithText("Editar publicação").assertDoesNotExist()
        compose.onNodeWithText("Excluir publicação").assertDoesNotExist()
    }
    @Test fun recriarComposicaoRecuperaRascunhoEVersaoOriginal() {
        val restauracao = StateRestorationTester(compose)
        var versaoServidor = 3
        var salvo: Pair<Int?, DadosPublicacao>? = null
        restauracao.setContent {
            var estado by remember { mutableStateOf(EstadoMural(listOf(item.copy(conteudo = null)), lendo = false)) }
            TemaUniSync { Surface {
                TelaMural(estado, listOf(turma), {}, {}, {}, { id -> estado = estado.copy(abrindoId = id, detalhe = item.copy(versao = versaoServidor)) },
                    { estado = estado.copy(abrindoId = null, detalhe = null) }, { _, versao, dados -> salvo = versao to dados }, {}, {})
            } }
        }
        compose.onNodeWithText(item.titulo).performScrollTo().performClick()
        compose.onNodeWithText("Editar publicação").performScrollTo().performClick()
        compose.onNodeWithText("Título da publicação").performTextReplacement("Rascunho preservado")
        versaoServidor = 4
        restauracao.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Título da publicação").assertTextContains("Rascunho preservado")
        compose.onNodeWithText("Salvar publicação").performClick()
        compose.runOnIdle { assertEquals(3, salvo?.first); assertEquals("Rascunho preservado", salvo?.second?.titulo) }
    }
    @Test fun falhaDeConsultaNaoAfirmaQueATurmaNaoTemPublicacoes() {
        compose.setContent { TemaUniSync { Surface {
            TelaMural(EstadoMural(lendo = false, erro = "Serviço indisponível"), listOf(turma), {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {})
        } } }
        compose.onNodeWithText("Serviço indisponível").assertExists()
        compose.onNodeWithText("Nenhuma publicação neste filtro").assertDoesNotExist()
    }
    @Test fun carregaOutraPaginaPorAcaoExplicita() {
        var carregou = false
        compose.setContent { TemaUniSync { Surface {
            TelaMural(EstadoMural(listOf(item.copy(conteudo = null)), proximaPagina = item.id, lendo = false), listOf(turma), {}, {}, { carregou = true }, {}, {}, { _, _, _ -> }, {}, {})
        } } }
        compose.onNodeWithText("Carregar mais publicações").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(carregou) }
    }
}
