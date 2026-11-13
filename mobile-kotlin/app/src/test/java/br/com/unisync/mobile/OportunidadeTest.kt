package br.com.unisync.mobile

import br.com.unisync.mobile.domain.oportunidades.Oportunidade
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
}
