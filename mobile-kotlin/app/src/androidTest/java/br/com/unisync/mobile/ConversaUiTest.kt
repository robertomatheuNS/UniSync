package br.com.unisync.mobile

import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import br.com.unisync.mobile.domain.mural.*
import br.com.unisync.mobile.domain.academico.Turma
import br.com.unisync.mobile.ui.mural.*
import br.com.unisync.mobile.ui.theme.TemaUniSync
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ConversaUiTest {
    @get:Rule val compose = createAndroidComposeRule<AtividadeTeste>()
    private val turma = Turma("111111111111111111111111", "Turma A", "Kotlin", "", "prof", "Ana", null, 2)
    private val publicacao = Publicacao("222222222222222222222222", turma.id, turma.nome, turma.disciplina, "aluno", "Bruno", "Aluno", "Dúvida Kotlin", "Resumo", Instant.EPOCH, Instant.EPOCH, 1, true, true, "Texto da publicação")
    private val comentario = Comentario("333333333333333333333333", publicacao.id, "aluno", "Bruno", "Aluno", "Comentário original", Instant.EPOCH, Instant.EPOCH, 3, true, true)
    private val mural = EstadoMural(listOf(publicacao), lendo = false, abrindoId = publicacao.id, detalhe = publicacao)
    private fun rolar(texto: String) {
        if (texto in listOf("Enviar comentário", "Salvar comentário")) compose.onNodeWithText(texto).assertIsDisplayed()
        else compose.onNode(hasScrollAction() and hasAnyAncestor(isDialog())).performScrollToNode(hasText(texto))
    }
    @Test fun falhaMantemRascunhoECurtidaNaoLimpaTexto() {
        var estado by mutableStateOf(EstadoConversa(lendo = false, consultaConfirmada = true))
        compose.setContent { TemaUniSync { Surface {
            TelaMural(mural, listOf(turma), {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {}, estado,
                aoSalvarComentario = { _, _, _ -> estado = estado.copy(erro = "Falha de conexão") },
                aoCurtir = { estado = estado.copy(curtiu = true, totalCurtidas = 1) })
        } } }
        rolar("Seu comentário"); compose.onNodeWithText("Seu comentário").performTextInput("Meu rascunho")
        compose.onNodeWithText("Enviar comentário").assertIsDisplayed().performClick()
        compose.onNodeWithText("Seu comentário").assertTextContains("Meu rascunho")
        rolar("Curtir (0)"); compose.onNodeWithText("Curtir (0)").performClick()
        compose.onNodeWithText("Seu comentário").assertTextContains("Meu rascunho")
        compose.onNodeWithText("Fechar publicação").performClick()
        compose.onNodeWithText("Descartar comentário não salvo?").assertExists()
        compose.onNodeWithText("Continuar escrevendo").performClick()
        compose.onNodeWithText("Seu comentário").assertTextContains("Meu rascunho")
    }
    @Test fun erroDeValidacaoNaoEnviaEConfirmacaoAnteriorNaoApagaNovoTexto() {
        var enviou = false
        compose.setContent { TemaUniSync { Surface {
            TelaMural(mural, listOf(turma), {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {}, EstadoConversa(lendo = false, consultaConfirmada = true, confirmacaoSalvo = 2),
                aoSalvarComentario = { _, _, _ -> enviou = true })
        } } }
        rolar("Enviar comentário"); compose.onNodeWithText("Enviar comentário").performClick()
        compose.onNodeWithText("Use entre 1 e 2000 caracteres.").assertExists()
        compose.runOnIdle { assertFalse(enviou) }
        compose.onNodeWithText("Seu comentário").performTextInput("Novo texto")
        compose.onNodeWithText("Seu comentário").assertTextContains("Novo texto")
    }
    @Test fun leitorNaoRecebeAcoesDeEdicaoOuExclusaoDeTerceiros() {
        compose.setContent { TemaUniSync { Surface {
            TelaMural(mural, listOf(turma), {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {}, EstadoConversa(listOf(comentario.copy(podeEditar = false, podeExcluir = false)), lendo = false, consultaConfirmada = true))
        } } }
        rolar("Comentário original")
        compose.onNodeWithText("Editar comentário").assertDoesNotExist()
        compose.onNodeWithText("Excluir comentário").assertDoesNotExist()
    }
    @Test fun professorExcluiComentarioSemEditarEEnviaVersaoConfirmada() {
        var excluido: Comentario? = null
        compose.setContent { TemaUniSync { Surface {
            TelaMural(mural, listOf(turma), {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {}, EstadoConversa(listOf(comentario.copy(podeEditar = false)), lendo = false, consultaConfirmada = true),
                aoExcluirComentario = { excluido = it })
        } } }
        rolar("Excluir comentário"); compose.onNodeWithText("Excluir comentário").performClick()
        compose.onNodeWithText("Excluir").performClick()
        compose.runOnIdle { assertEquals(3, excluido?.versao) }
    }
    @Test fun conversaEscuraPermiteComentarComConfiguracaoDoSistema() {
        var enviado: String? = null
        compose.setContent { TemaUniSync(temaEscuro = true) { Surface {
            TelaMural(mural, listOf(turma), {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {}, EstadoConversa(lendo = false, consultaConfirmada = true),
                aoSalvarComentario = { _, _, texto -> enviado = texto })
        } } }
        rolar("Seu comentário"); compose.onNodeWithText("Seu comentário").performTextInput("Uma dúvida para a turma")
        rolar("Enviar comentário")
        compose.onNodeWithText("Enviar comentário").assertIsDisplayed()
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "mural-comentario-escuro.png").outputStream().use {
            compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Enviar comentário").performClick()
        compose.runOnIdle { assertEquals("Uma dúvida para a turma", enviado) }
    }
    @Test fun recriarTelaPreservaRascunhoEVersaoDoComentario() {
        val restauracao = StateRestorationTester(compose)
        var versaoServidor = 3
        var salvo: Pair<Int?, String>? = null
        restauracao.setContent { TemaUniSync { Surface {
            TelaMural(mural, listOf(turma), {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {}, EstadoConversa(listOf(comentario.copy(versao = versaoServidor)), lendo = false, consultaConfirmada = true),
                aoSalvarComentario = { _, versao, texto -> salvo = versao to texto })
        } } }
        rolar("Editar comentário"); compose.onNode(hasText("Editar comentário") and hasClickAction()).performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Rascunho preservado")
        versaoServidor = 4
        restauracao.emulateSavedInstanceStateRestore()
        rolar("Salvar comentário"); compose.onNode(hasSetTextAction()).assertTextContains("Rascunho preservado")
        compose.onNodeWithText("Salvar comentário").performClick()
        compose.runOnIdle { assertEquals(3 to "Rascunho preservado", salvo) }
    }
}
