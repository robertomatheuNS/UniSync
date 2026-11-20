package br.com.unisync.mobile

import br.com.unisync.mobile.domain.oportunidades.Oportunidade
import br.com.unisync.mobile.domain.oportunidades.validarUrlInscricao
import java.time.*
import org.junit.Test
import org.junit.Assert.*

class OportunidadeTest {
    private fun evento(data: String) = Oportunidade("id", "Palestra", "Professor", "Kotlin", null, "Palestra", data)
    @Test fun converteOffsetParaFusoDoUsuarioSemPerderDia() {
        assertEquals(LocalDateTime.of(2026, 11, 12, 22, 0),
            evento("2026-11-13T01:00:00Z").horario(ZoneId.of("America/Maceio")))
    }
    @Test fun preservaHorarioQuandoContratoNaoInformaOffset() {
        assertEquals(LocalDateTime.of(2026, 11, 13, 14, 0),
            evento("2026-11-13T14:00:00").horario(ZoneId.of("America/Maceio")))
    }
    @Test fun encerramentoUsaDataDoEventoEDataInvalidaNaoDerrubaTela() {
        assertTrue(evento("2026-11-13T14:00:00").encerrada(LocalDateTime.of(2026,11,13,15,0)))
        assertNull(evento("invalido").horario())
        assertEquals("invalido", evento("invalido").dataFormatada())
    }

    @Test fun inscricaoEncerraNoInstanteExatoMesmoComEventoEmOutroFuso() {
        val limite = Instant.parse("2028-12-10T15:00:00Z")
        val item = evento("2028-12-10T20:00:00+01:00").copy(urlInscricao = "https://example.org/inscricao", prazoInscricao = limite)
        assertTrue(item.podeInscrever(limite.minusSeconds(1), ZoneId.of("America/Maceio")))
        assertFalse(item.podeInscrever(limite, ZoneId.of("Europe/Paris")))
        assertEquals("Inscrições encerradas", item.situacaoInscricao(limite))
        assertFalse(item.copy(prazoInscricao = null).podeInscrever(Instant.parse("2028-12-10T19:00:00Z")))
    }

    @Test fun abrirAceitaApenasHttpsComHostSemCredenciais() {
        assertEquals("https://example.org/a", validarUrlInscricao(" HTTPS://example.org/a "))
        listOf("http://example.org", "javascript:alert(1)", "https://user@example.org", "https://", "https://exemplo.com:65536", "https://x.org/a b").forEach {
            assertTrue(it, runCatching { validarUrlInscricao(it) }.isFailure)
        }
    }
}
