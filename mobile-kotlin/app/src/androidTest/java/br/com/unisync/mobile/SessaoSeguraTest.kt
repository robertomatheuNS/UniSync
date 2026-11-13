package br.com.unisync.mobile

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.data.local.SessaoSegura
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SessaoSeguraTest {
    @Test fun persisteCifradoRestauraELimpaToken() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val cofre = SessaoSegura(contexto, "sessao_teste")
        val token = "token-de-teste-nao-e-uma-credencial"
        try {
            cofre.salvarToken(token)
            val registro = contexto.getSharedPreferences("sessao_teste", Context.MODE_PRIVATE).getString("token", null)
            assertNotNull(registro)
            assertFalse(registro!!.contains(token))
            assertEquals(token, SessaoSegura(contexto, "sessao_teste").lerToken())
            cofre.limpar()
            assertNull(SessaoSegura(contexto, "sessao_teste").lerToken())
        } finally {
            cofre.limpar()
        }
    }
}
