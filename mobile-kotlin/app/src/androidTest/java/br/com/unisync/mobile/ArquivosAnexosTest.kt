package br.com.unisync.mobile

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.arquivos.ArquivosAnexos
import br.com.unisync.mobile.domain.mural.Anexo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class ArquivosAnexosTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val arquivos = ArquivosAnexos(contexto)
    private val bytes = "%PDF-1.4\n%%EOF".toByteArray()
    private val anexo = Anexo("444444444444444444444444", "555555555555555555555555", "roteiro.pdf", "application/pdf", bytes.size, "a".repeat(64), Instant.EPOCH, true)
    @Test fun arquivoFicaNoCachePrivadoComUriDeLeituraELimpezaRemoveCopia() = runBlocking {
        val provider = contexto.packageManager.resolveContentProvider("${contexto.packageName}.anexos", 0)!!
        assertFalse(provider.exported); assertTrue(provider.grantUriPermissions)
        val intent = arquivos.prepararAbertura(anexo, bytes)
        val uri = intent.data!!
        assertEquals("content", uri.scheme)
        assertEquals(Intent.ACTION_VIEW, intent.action); assertEquals("application/pdf", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertEquals(uri, intent.clipData!!.getItemAt(0).uri)
        assertArrayEquals(bytes, contexto.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
        arquivos.limparAnexo(anexo.id)
        assertTrue(runCatching { contexto.contentResolver.openInputStream(uri)!!.close() }.isFailure)
    }
    @Test fun limpezaDaSessaoRemoveTodasAsCopias() = runBlocking {
        val primeira = arquivos.prepararAbertura(anexo, bytes).data!!
        val segunda = arquivos.prepararAbertura(anexo.copy(id = "666666666666666666666666"), bytes).data!!
        arquivos.limpar()
        assertTrue(runCatching { contexto.contentResolver.openInputStream(primeira)!!.close() }.isFailure)
        assertTrue(runCatching { contexto.contentResolver.openInputStream(segunda)!!.close() }.isFailure)
    }
}
