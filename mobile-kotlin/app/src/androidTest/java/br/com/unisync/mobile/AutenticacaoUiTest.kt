package br.com.unisync.mobile

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.com.unisync.mobile.ui.auth.EstadoAutenticacao
import br.com.unisync.mobile.ui.auth.TelaAutenticacao
import br.com.unisync.mobile.ui.theme.TemaUniSync
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AutenticacaoUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loginEnviaCredenciaisEImpedeEnvioInvalido() {
        var credenciais: Pair<String, String>? = null
        compose.setContent {
            TemaUniSync {
                TelaAutenticacao(false, EstadoAutenticacao(restaurando = false),
                    { email, senha -> credenciais = email to senha }, { _, _, _, _ -> }, {}, {})
            }
        }
        compose.onNode(hasText("Entrar") and hasClickAction()).performScrollTo().performClick()
        compose.runOnIdle { assertNull(credenciais) }
        compose.onNodeWithText("Informe um e-mail válido com até 180 caracteres.").assertExists()
        compose.onNodeWithText("E-mail").performTextInput("aluno@example.com")
        compose.onNodeWithText("Senha").performTextInput("senha123")
        compose.onNode(hasText("Entrar") and hasClickAction()).performScrollTo().performClick()
        compose.runOnIdle { assertEquals("aluno@example.com" to "senha123", credenciais) }
    }

    @Test fun cadastroProfessorExigeCodigoEEnviaPerfilCorreto() {
        var codigo: String? = null
        compose.setContent {
            TemaUniSync(temaEscuro = true) {
                TelaAutenticacao(true, EstadoAutenticacao(restaurando = false), { _, _ -> },
                    { _, _, _, recebido -> codigo = recebido }, {}, {})
            }
        }
        compose.onNodeWithText("Professor").performScrollTo().performClick()
        compose.onNodeWithText("Nome completo").performTextInput("Professor Teste")
        compose.onNodeWithText("E-mail").performTextInput("professor@example.com")
        compose.onNodeWithText("Senha").performTextInput("senha123")
        compose.onAllNodesWithText("Criar cadastro").filter(hasClickAction()).onFirst()
            .performScrollTo().performClick()
        compose.runOnIdle { assertNull(codigo) }
        compose.onNodeWithText("Código do professor").performScrollTo().performTextInput("CODIGO")
        compose.onAllNodesWithText("Criar cadastro").filter(hasClickAction()).onFirst()
            .performScrollTo().performClick()
        compose.runOnIdle { assertEquals("CODIGO", codigo) }
    }

    @Test fun falhaDeRedeMantemFormularioEPermiteNovaTentativa() {
        val estado = mutableStateOf(EstadoAutenticacao(restaurando = false))
        compose.setContent {
            TemaUniSync {
                TelaAutenticacao(false, estado.value, { _, _ -> }, { _, _, _, _ -> }, {}, {})
            }
        }
        compose.onNodeWithText("E-mail").performTextInput("aluno@example.com")
        compose.runOnIdle { estado.value = estado.value.copy(mensagem = "Sem conexão. Tente novamente.") }
        compose.onNodeWithText("aluno@example.com").assertExists()
        compose.onNodeWithText("Sem conexão. Tente novamente.").assertExists()
        compose.onNode(hasText("Entrar") and hasClickAction()).assertIsEnabled()
    }
}
