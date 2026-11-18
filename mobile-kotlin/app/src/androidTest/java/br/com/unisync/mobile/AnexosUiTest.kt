package br.com.unisync.mobile

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
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

internal class SeletorArquivoTeste(private val resultado: Uri?) : ActivityResultRegistryOwner {
    override val activityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            dispatchResult(requestCode, resultado)
        }
    }
}

class AnexosUiTest {
    @get:Rule val compose = createAndroidComposeRule<AtividadeTeste>()
    private val turma = Turma("111111111111111111111111", "Turma A", "Kotlin", "", "prof", "Ana", null, 2)
    private val pub = Publicacao("222222222222222222222222", turma.id, turma.nome, turma.disciplina, "aluno", "Bruno", "Aluno", "Material da atividade", "Resumo", Instant.EPOCH, Instant.EPOCH, 1, true, true, "Consulte o roteiro da turma.")
    private val mural = EstadoMural(lendo = false, abrindoId = pub.id, detalhe = pub)
    private val anexo = Anexo("333333333333333333333333", pub.id, "roteiro.pdf", "application/pdf", 2048, "a".repeat(64), Instant.EPOCH, true)
    private fun rolar(texto: String) { compose.onNode(hasScrollAction() and hasAnyAncestor(isDialog())).performScrollToNode(hasText(texto)) }
    private fun uri(): Uri {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "anexos/roteiro.pdf").apply { parentFile!!.mkdirs(); writeText("%PDF-1.4\n%%EOF") }
        return FileProvider.getUriForFile(context, "${context.packageName}.anexos", file)
    }
    @Test fun selecaoFalhaERestauracaoMantemArquivoEMesmoIdDeReenvio() {
        val seletor = SeletorArquivoTeste(uri())
        val restauracao = StateRestorationTester(compose)
        var estado by mutableStateOf(EstadoAnexos(podeAdicionar = true))
        val enviados = mutableListOf<Pair<String, String>>()
        restauracao.setContent { CompositionLocalProvider(LocalActivityResultRegistryOwner provides seletor) { TemaUniSync { Surface {
            TelaMural(mural, listOf(turma), {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {},
                anexos = estado, aoEnviarAnexo = { uri, id -> enviados.add(uri to id); estado = estado.copy(erro = "Falha de conexão") })
        } } } }
        rolar("Selecionar arquivo"); compose.onNodeWithText("Selecionar arquivo").performClick()
        compose.waitUntil { compose.onAllNodesWithText("Selecionado: roteiro.pdf").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Enviar anexo").performScrollTo().performClick()
        restauracao.emulateSavedInstanceStateRestore()
        rolar("Enviar anexo"); compose.onNodeWithText("Enviar anexo").performClick()
        compose.runOnIdle { assertEquals(2, enviados.size); assertEquals(enviados[0], enviados[1]) }
        compose.onNodeWithText("Fechar publicação").performClick()
        compose.onNodeWithText("Descartar alterações não enviadas?").assertExists()
        compose.onNodeWithText("Continuar escrevendo").performClick()
        compose.onNodeWithText("Selecionado: roteiro.pdf").assertExists()
    }
    @Test fun envioConfirmadoLimpaArquivoSemApagarRascunhoDeComentario() {
        val seletor = SeletorArquivoTeste(uri())
        var estado by mutableStateOf(EstadoAnexos(podeAdicionar = true))
        compose.setContent { CompositionLocalProvider(LocalActivityResultRegistryOwner provides seletor) { TemaUniSync {
            TelaMural(mural, listOf(turma), {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {}, EstadoConversa(lendo = false),
                anexos = estado, aoEnviarAnexo = { _, _ -> estado = estado.copy(confirmacaoEnvio = 1, itens = listOf(anexo)) })
        } } }
        rolar("Seu comentário"); compose.onNodeWithText("Seu comentário").performTextInput("Meu rascunho permanece")
        // O seletor controlado não abre outra Activity para fechar o teclado como o seletor real.
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rolar("Selecionar arquivo"); compose.onNodeWithText("Selecionar arquivo").performClick()
        compose.waitUntil { compose.onAllNodesWithText("Selecionado: roteiro.pdf").fetchSemanticsNodes().isNotEmpty() }
        rolar("Enviar anexo"); compose.onNodeWithText("Enviar anexo").performClick()
        compose.onNodeWithText("Selecionado: roteiro.pdf").assertDoesNotExist()
        rolar("Seu comentário"); compose.onNodeWithText("Seu comentário").assertTextContains("Meu rascunho permanece")
    }
    @Test fun leitorAbreSemAdicionarOuRemoverEProfessorConfirmaRemocao() {
        var estado by mutableStateOf(EstadoAnexos(listOf(anexo.copy(podeExcluir = false))))
        var abriu = false; var removeu = false
        compose.setContent { TemaUniSync { TelaMural(mural, listOf(turma), {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {},
            anexos = estado, aoAbrirAnexo = { abriu = true }, aoExcluirAnexo = { removeu = true }) } }
        rolar("Abrir anexo"); compose.onNodeWithText("Abrir anexo").performClick()
        compose.onNodeWithText("Selecionar arquivo").assertDoesNotExist(); compose.onNodeWithText("Remover anexo").assertDoesNotExist()
        compose.runOnIdle { assertTrue(abriu); estado = EstadoAnexos(listOf(anexo)) }
        rolar("Remover anexo"); compose.onNodeWithText("Remover anexo").performClick()
        compose.onNodeWithText("Remover anexo?").assertExists()
        compose.runOnIdle { assertFalse(removeu) }
        compose.onNodeWithText("Remover").performClick(); compose.runOnIdle { assertTrue(removeu) }
    }
    @Test fun envioEmAndamentoBloqueiaSairERepeteProgressoAcessivel() {
        var fechou = false
        compose.setContent { TemaUniSync { TelaMural(mural, listOf(turma), {}, {}, {}, {}, { fechou = true }, { _, _, _ -> }, {}, {},
            anexos = EstadoAnexos(podeAdicionar = true, enviando = true, progresso = 50)) } }
        rolar("Enviando arquivo… 50%"); compose.onNodeWithText("Enviando arquivo… 50%").assertIsDisplayed()
        compose.onNodeWithText("Fechar publicação").assertIsNotEnabled()
        compose.onNodeWithText("Selecionar arquivo").assertIsNotEnabled(); compose.runOnIdle { assertFalse(fechou) }
    }
    @Test fun anexosEscurosComConfiguracaoDoSistemaPermitemAbrirERemover() {
        var abriu = false
        compose.setContent { TemaUniSync(temaEscuro = true) { TelaMural(mural, listOf(turma), {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {},
            conversa = EstadoConversa(lendo = false, consultaConfirmada = true),
            anexos = EstadoAnexos(listOf(anexo), podeAdicionar = true), aoAbrirAnexo = { abriu = true }) } }
        rolar("Abrir anexo"); compose.onNodeWithText("Abrir anexo").performClick()
        compose.runOnIdle { assertTrue(abriu) }
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "mural-anexos-escuro.png").outputStream().use {
            compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Remover anexo").performScrollTo().performClick(); compose.onNodeWithText("Cancelar").performClick()
    }
}
