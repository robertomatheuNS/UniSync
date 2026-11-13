package br.com.unisync.mobile

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.domain.academico.*
import br.com.unisync.mobile.ui.academico.*
import br.com.unisync.mobile.ui.theme.TemaUniSync
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class AcademicoUiTest {
    @get:Rule val compose = createAndroidComposeRule<AtividadeTeste>()
    private val turma = Turma("turma", "Turma A", "Engenharia de Software", "2026.2", "prof", "Ana", "AABBCCDDEEFF", 1)
    private val aviso = Aviso("aviso", "turma", "Turma A", "Engenharia de Software", "prof", "Ana", "Entrega do projeto",
        "Apresentação na próxima aula.", "2026-11-13T12:00:00Z", "2026-11-13T12:00:00Z", false)
    private val estado = EstadoAcademico(listOf(turma), listOf(aviso), lendo = false)

    @Test fun alunoVeEntradaPorCodigoSemAcoesDeProfessor() {
        var codigo: String? = null
        compose.setContent { TemaUniSync { TelaTurmas(estado, null, {}, { _, _ -> }, { codigo = it }, {}, {}, {}) } }
        compose.onNodeWithText("Criar turma").assertDoesNotExist()
        compose.onNodeWithText("Editar turma").assertDoesNotExist()
        compose.onNodeWithText("Entrar em uma turma").performClick()
        compose.onNodeWithText("Código da turma").performTextInput("AABBCCDDEEFF")
        compose.onNodeWithText("Entrar na turma").performClick()
        compose.runOnIdle { assertEquals("AABBCCDDEEFF", codigo) }
    }

    @Test fun formularioInvalidoMantemDadosEConfirmaDescarte() {
        var envios = 0
        compose.setContent { TemaUniSync { TelaTurmas(estado, "prof", {}, { _, _ -> envios++ }, {}, {}, {}, {}) } }
        compose.onNodeWithText("Criar turma").performClick()
        compose.onNodeWithText("Nome da turma").performTextInput("Turma nova")
        compose.onNodeWithText("Salvar turma").performClick()
        compose.onNodeWithText("Use entre 2 e 100 caracteres.").assertExists()
        compose.onNodeWithText("Turma nova").assertExists()
        compose.runOnIdle { assertEquals(0, envios) }
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Descartar alterações?").assertExists()
        compose.onNodeWithText("Continuar editando").performClick()
        compose.onNodeWithText("Turma nova").assertExists()
    }

    @Test fun detalheDeAlunoMantemErroEPermiteConfirmarLeitura() {
        var lido: String? = null
        compose.setContent { TemaUniSync { TelaAvisos(estado, null, null, {}, { _, _ -> }, {}, { lido = it }, {}, {}) } }
        compose.onNodeWithText("Entrega do projeto").performClick()
        compose.onNodeWithText("Editar aviso").assertDoesNotExist()
        compose.onNodeWithText("Marcar como lido").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("aviso", lido) }
    }

    @Test fun professorEditaAvisoEFormularioExibeFalhaSemPerderTexto() {
        var dados: DadosAviso? = null
        compose.setContent { TemaUniSync(temaEscuro = true) {
            FormularioAviso(aviso, listOf(turma), null, false, "Falha de conexão. Tente novamente.", { dados = it }, {})
        } }
        compose.onNodeWithText("Título do aviso").performTextReplacement("Título revisado")
        compose.onNodeWithText("Publicar aviso").performClick()
        compose.onNodeWithText("Título revisado").assertExists()
        compose.onNodeWithText("Falha de conexão. Tente novamente.").assertExists()
        compose.runOnIdle { assertEquals("Título revisado", dados?.titulo) }
        capturar("aviso-formulario-escuro.png")
    }

    @Test fun turmaExibeCodigoDoProfessorEAcaoDeArquivamentoComConfirmacao() {
        var arquivada: String? = null
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(375.dp, 667.dp))) {
                TemaUniSync { TelaTurmas(estado, "prof", {}, { _, _ -> }, {}, { arquivada = it }, {}, {}) }
            }
        }
        compose.onNodeWithText("Código de entrada: AABBCCDDEEFF").assertExists()
        capturar("turmas-claro.png")
        compose.onNodeWithText("Arquivar turma").performScrollTo().performClick()
        compose.runOnIdle { assertNull(arquivada) }
        compose.onNodeWithText("Arquivar").performClick()
        compose.runOnIdle { assertEquals("turma", arquivada) }
    }

    @Test fun turmaContinuaNavegavelEmPaisagemComFonteAmpliada() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(640.dp, 360.dp)) then
                DeviceConfigurationOverride.FontScale(2f)) {
                TemaUniSync { TelaTurmas(estado, "prof", {}, { _, _ -> }, {}, {}, {}, {}) }
            }
        }
        compose.onNodeWithText("Ver avisos").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Arquivar turma").performScrollTo().assertIsDisplayed()
        capturar("turmas-paisagem-fonte-ampliada.png")
    }

    private fun capturar(nome: String) {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        File(contexto.cacheDir, nome).outputStream().use { arquivo ->
            val alvo = if (compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty()) compose.onRoot() else compose.onNode(isDialog())
            alvo.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, arquivo)
        }
    }
}
