package br.com.unisync.mobile

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.data.*
import br.com.unisync.mobile.data.local.BancoUniSync
import br.com.unisync.mobile.data.remote.*
import br.com.unisync.mobile.ui.lembretes.*
import br.com.unisync.mobile.ui.theme.TemaUniSync
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.time.ZoneId
import java.util.UUID

/** Contas técnicas, banco em memória e Activity dedicada preservam a sessão pessoal. */
class LembretesLocalE2ETest {
    @get:Rule val compose = createAndroidComposeRule<AtividadeTeste>()
    @Test fun androidCriaComHorarioWebEditaSemPerderDataEOutraContaNaoAcessa() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("e2eLembretes") == "true")
        val auth = RepositorioAutenticacao()
        val api = ClienteAcademico()
        val id = UUID.randomUUID().toString()
        val senha = "Teste-$id-42!"
        val usuario = runBlocking {
            auth.cadastrar("Aluno Lembretes", "lembretes-$id@example.invalid", senha, null)
            auth.entrar("lembretes-$id@example.invalid", senha)
        }
        val banco = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, BancoUniSync::class.java).build()
        val repo = RepositorioLembretes(banco.lembretes(), api)
        var expirou = false
        val modelo = ModeloLembretes(usuario, repo) { expirou = true }
        var lembreteId: String? = null
        try {
            compose.setContent { TemaUniSync { val estado by modelo.estado.collectAsState()
                TelaLembretes(estado, modelo::atualizar, modelo::salvar, modelo::concluir, modelo::excluir)
            } }
            esperar(modelo)
            compose.onNodeWithText("Novo lembrete").performScrollTo().performClick()
            compose.onNodeWithText("Título do lembrete").performTextInput("Apresentar projeto Android")
            compose.onNodeWithText("Data (opcional)").performScrollTo().performTextInput("29/02/2028")
            compose.onNodeWithText("Horário (opcional)").performScrollTo().performTextInput("09:30")
            compose.onNodeWithText("Salvar lembrete").performClick()
            compose.waitUntil(15000) { !modelo.estado.value.salvando && modelo.estado.value.itens.size == 1 }
            assertNull(modelo.estado.value.erro)
            val criado = modelo.estado.value.itens.single()
            lembreteId = criado.id
            assertEquals(java.time.LocalDateTime.of(2028, 2, 29, 9, 30).atZone(ZoneId.systemDefault()).toInstant(), criado.agendadoEm)
            runBlocking {
                // Contrato utilizado pelo painel Web: PATCH de título sem enviar agendamento.
                api.executar("PATCH", "/lembretes/atualizar/${criado.id}", usuario.token, """{"titulo":"Título revisado pela Web"}""")
                repo.atualizar(usuario.usuarioId, usuario.token)
                val atualizado = repo.observar(usuario.usuarioId).first().single()
                assertEquals("Título revisado pela Web", atualizado.titulo)
                assertEquals(criado.agendadoEm, atualizado.agendadoEm)
                auth.cadastrar("Outra conta", "lembretes-fora-$id@example.invalid", senha, null)
                val fora = auth.entrar("lembretes-fora-$id@example.invalid", senha)
                assertEquals(0, JSONArray(api.executar("GET", "/lembretes/listar", fora.token)).length())
                assertEquals(404, (runCatching { api.executar("PATCH", "/lembretes/atualizar/${criado.id}", fora.token, """{"concluido":true}""") }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(404, (runCatching { api.executar("DELETE", "/lembretes/excluir/${criado.id}", fora.token) }.exceptionOrNull() as? FalhaApi)?.status)
            }
            compose.onNodeWithText("Título revisado pela Web").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Concluir").performScrollTo().performClick()
            compose.waitUntil(15000) { !modelo.estado.value.salvando && modelo.estado.value.itens.single().concluido }
            compose.onNodeWithText("Concluídos (1)").performScrollTo().performClick()
            compose.onNodeWithText("Editar").performScrollTo().performClick()
            compose.onNodeWithText("Remover agendamento").performScrollTo().performClick()
            compose.onNodeWithText("Salvar lembrete").performClick()
            compose.waitUntil(15000) { !modelo.estado.value.salvando && modelo.estado.value.itens.single().agendadoEm == null }
            assertTrue(modelo.estado.value.itens.single().concluido)
            runBlocking { repo.atualizar(usuario.usuarioId, usuario.token); assertNull(repo.observar(usuario.usuarioId).first().single().agendadoEm) }
            compose.onNodeWithText("Excluir").performScrollTo().performClick()
            compose.onNode(hasText("Excluir") and hasAnyAncestor(isDialog())).performClick()
            compose.waitUntil(15000) { !modelo.estado.value.salvando && modelo.estado.value.itens.isEmpty() }
            lembreteId = null
            assertFalse(expirou)
        } finally {
            modelo.fechar()
            try { runBlocking { lembreteId?.let { repo.excluir(usuario.usuarioId, usuario.token, it) } } }
            finally { banco.close() }
        }
    }
    private fun esperar(modelo: ModeloLembretes) {
        compose.waitUntil(15000) { !modelo.estado.value.lendo && !modelo.estado.value.atualizando }
        assertNull(modelo.estado.value.erro)
    }
}
