package br.com.unisync.mobile

import br.com.unisync.mobile.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class SessaoAutenticadaTest {
    private fun token(perfil: String = "Aluno", exp: Long? = 2000): String {
        val dados = JSONObject()
            .put("http://schemas.microsoft.com/ws/2008/06/identity/claims/role", perfil)
            .put("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name", "João")
        if (exp != null) dados.put("exp", exp)
        return "header.${Base64.getUrlEncoder().withoutPadding().encodeToString(dados.toString().toByteArray())}.signature"
    }

    @Test fun interpretaAlunoComNomeAcentuado() {
        val sessao = SessaoAutenticada.doToken(token(), 1000)
        assertEquals("João", sessao.nome)
        assertEquals(PerfilUsuario.ALUNO, sessao.perfil)
        assertFalse(sessao.expirada(1999))
        assertTrue(sessao.expirada(2000))
    }

    @Test fun interpretaProfessor() {
        assertEquals(PerfilUsuario.PROFESSOR, SessaoAutenticada.doToken(token("Professor"), 1000).perfil)
    }

    @Test(expected = ExcecaoAutenticacao::class)
    fun rejeitaPerfilDesconhecido() { SessaoAutenticada.doToken(token("Admin"), 1000) }

    @Test(expected = ExcecaoAutenticacao::class)
    fun rejeitaTokenExpirado() { SessaoAutenticada.doToken(token(exp = 1000), 1000) }

    @Test(expected = ExcecaoAutenticacao::class)
    fun rejeitaTokenSemExpiracao() { SessaoAutenticada.doToken(token(exp = null), 1000) }

    @Test(expected = ExcecaoAutenticacao::class)
    fun rejeitaTokenIncompleto() { SessaoAutenticada.doToken("token") }

    @Test(expected = ExcecaoAutenticacao::class)
    fun rejeitaPayloadInvalido() { SessaoAutenticada.doToken("header.%%%.signature") }

    @Test(expected = ExcecaoAutenticacao::class)
    fun rejeitaJsonInvalido() { SessaoAutenticada.doToken("header.bm90anNvbg.signature") }
}
