package br.com.unisync.mobile

import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.domain.academico.*
import br.com.unisync.mobile.domain.oportunidades.Oportunidade
import br.com.unisync.mobile.ui.academico.*
import br.com.unisync.mobile.ui.lembretes.EstadoLembretes
import br.com.unisync.mobile.ui.oportunidades.*
import br.com.unisync.mobile.ui.painel.*
import br.com.unisync.mobile.ui.theme.TemaUniSync
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class InicioUiTest {
    @get:Rule val compose = createAndroidComposeRule<AtividadeTeste>()
    private val turma = Turma("turma", "Turma A", "Engenharia de Software", "", "prof", "Ana", null, 1)
    private val aviso = Aviso("aviso", turma.id, turma.nome, turma.disciplina, "prof", "Ana", "Entrega do projeto", "Apresentação completa na próxima aula.", "2026-11-17T10:00:00Z", "2026-11-17T10:00:00Z", false)
    private val oportunidade = Oportunidade("700000000000000000000001", "Oficina de Kotlin", "Ana", turma.disciplina, "Descrição completa da oficina.", "Workshop", "2030-01-01T12:00:00Z", "prof")
    private val academico = EstadoAcademico(listOf(turma), listOf(aviso), lendo = false)
    private val oportunidades = EstadoOportunidades(listOf(oportunidade), lendo = false)
    private val lembretes = EstadoLembretes(lendo = false)
    private fun rolar(texto: String) { compose.onNode(hasScrollAction()).performScrollToNode(hasText(texto)) }
    @Test fun painelAbreAvisoEOportunidadePeloIdentificadorSemMarcarLeitura() {
        var abriuAviso: String? = null; var abriuOportunidade: String? = null
        compose.setContent { TemaUniSync { TelaInicio("Bruno", false, academico, oportunidades, lembretes,
            acoes = AcoesInicio(abrirAviso = { abriuAviso = it }, abrirOportunidade = { abriuOportunidade = it })) } }
        rolar(aviso.titulo); compose.onNodeWithTag("inicio-aviso:${aviso.id}").performClick()
        compose.runOnIdle { assertEquals(aviso.id, abriuAviso); assertFalse(aviso.lido) }
        rolar(oportunidade.titulo); compose.onNodeWithTag("inicio-oportunidade:${oportunidade.id}").performClick()
        compose.runOnIdle { assertEquals(oportunidade.id, abriuOportunidade) }
    }
    @Test fun falhaMantemDadosEMostraAtualizacaoIndependente() {
        var atualizouAvisos = false; var atualizouOportunidades = false
        compose.setContent { TemaUniSync { TelaInicio("Bruno", false, academico.copy(erro = "Falha nos avisos"), oportunidades.copy(erro = "Falha nas oportunidades"), lembretes,
            acoes = AcoesInicio(atualizarAcademico = { atualizouAvisos = true }, atualizarOportunidades = { atualizouOportunidades = true })) } }
        rolar("Falha nos avisos"); compose.onNodeWithText("Falha nos avisos").assertExists()
        compose.onNodeWithText("Atualizar avisos").performScrollTo().performClick()
        rolar(aviso.titulo); compose.onNodeWithTag("inicio-aviso:${aviso.id}").assertExists()
        rolar("Atualizar oportunidades"); compose.onNodeWithText("Atualizar oportunidades").performClick()
        rolar(oportunidade.titulo); compose.onNodeWithTag("inicio-oportunidade:${oportunidade.id}").assertExists()
        compose.runOnIdle { assertTrue(atualizouAvisos); assertTrue(atualizouOportunidades) }
    }
    @Test fun vazioNaoSeConfundeComCarregamentoOuFalha() {
        var a by mutableStateOf(EstadoAcademico())
        var o by mutableStateOf(EstadoOportunidades())
        compose.setContent { TemaUniSync { TelaInicio("Bruno", false, a, o, lembretes) } }
        compose.onNodeWithText("Entre em uma turma ou crie uma para acompanhar os avisos.").assertDoesNotExist()
        compose.runOnIdle { a = EstadoAcademico(lendo = false); o = EstadoOportunidades(lendo = false) }
        rolar("Entre em uma turma ou crie uma para acompanhar os avisos.")
        rolar("Nenhuma oportunidade disponível por enquanto.")
        compose.runOnIdle { o = o.copy(erro = "Falha de rede") }
        compose.onNodeWithText("Nenhuma oportunidade disponível por enquanto.").assertDoesNotExist()
    }
    @Test fun avisoInicialSobreviveRestauracaoEFechadoNaoReabre() {
        val restauracao = StateRestorationTester(compose)
        var inicial by mutableStateOf<String?>(aviso.id)
        var leituras = 0
        restauracao.setContent { TemaUniSync { TelaAvisos(academico, null, "outra", {}, { _, _ -> }, {}, { leituras++ }, {}, {},
            avisoInicial = inicial, aoConsumirAviso = { inicial = null }) } }
        compose.onNode(hasText(aviso.conteudo) and hasAnyAncestor(isDialog())).assertExists()
        restauracao.emulateSavedInstanceStateRestore()
        compose.onNode(hasText(aviso.conteudo) and hasAnyAncestor(isDialog())).assertExists()
        compose.runOnIdle { assertNull(inicial); assertEquals(0, leituras) }
        compose.onNodeWithText("Fechar").performClick()
        restauracao.emulateSavedInstanceStateRestore(); compose.onNode(isDialog()).assertDoesNotExist()
    }
    @Test fun oportunidadeInicialEsperaCargaEItemRemovidoPermiteTentarOuFechar() {
        var estado by mutableStateOf(EstadoOportunidades())
        var inicial by mutableStateOf<String?>(oportunidade.id)
        compose.setContent { TemaUniSync { TelaOportunidades(estado, {}, oportunidadeInicial = inicial, aoConsumirOportunidade = { inicial = null }) } }
        compose.onNodeWithText("Oportunidade em consulta").assertExists()
        compose.runOnIdle { estado = oportunidades }
        compose.onNodeWithText(oportunidade.descricao!!).assertExists()
        compose.runOnIdle { estado = EstadoOportunidades(lendo = false) }
        compose.onNodeWithText("Oportunidade indisponível").assertExists()
        compose.onNodeWithText("Tentar novamente").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Fechar").performClick(); compose.onNode(isDialog()).assertDoesNotExist()
    }
    @Test fun inicioEscuroComConfiguracaoDoSistemaMantemCardsEAcoesAcessiveis() {
        var avisoAberto = false; var oportunidadeAberta = false
        compose.setContent { TemaUniSync(temaEscuro = true) { Surface { TelaInicio("Bruno", false, academico, oportunidades, lembretes,
            acoes = AcoesInicio(abrirAviso = { avisoAberto = true }, abrirOportunidade = { oportunidadeAberta = true })) } } }
        rolar(aviso.titulo); compose.onNodeWithTag("inicio-aviso:${aviso.id}").performClick()
        capturar("inicio-avisos-escuro.png")
        rolar(oportunidade.titulo); compose.onNodeWithTag("inicio-oportunidade:${oportunidade.id}").performClick()
        capturar("inicio-oportunidades-escuro.png")
        compose.runOnIdle { assertTrue(avisoAberto); assertTrue(oportunidadeAberta) }
    }
    private fun capturar(nome: String) {
        compose.mainClock.advanceTimeBy(500)
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, nome).outputStream().use {
        compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
    } }
}
