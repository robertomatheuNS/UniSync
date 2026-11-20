package br.com.unisync.mobile

import br.com.unisync.mobile.data.local.*
import br.com.unisync.mobile.domain.lembretes.*
import br.com.unisync.mobile.notificacoes.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class LembretesTest {
    @Test fun horarioExigeDataCompletaHoraValidaEFusoConsistente() {
        val zona = ZoneId.of("America/Maceio")
        assertEquals(Instant.parse("2028-02-29T12:30:00Z"), lerAgendamento("29/02/2028", "09:30", zona))
        assertNull(lerAgendamento("", "", zona))
        listOf("24:00", "9:00", "12:60", "").forEach { assertTrue(runCatching { lerAgendamento("29/02/2028", it, zona) }.isFailure) }
        assertTrue(runCatching { lerAgendamento("29/02/2027", "09:00", zona) }.isFailure)
        assertTrue(runCatching { lerAgendamento("10/03/2024", "02:30", ZoneId.of("America/New_York")) }.isFailure)
        assertEquals("Estudar", DadosLembrete("  Estudar  ").validar().titulo)
        assertTrue(runCatching { DadosLembrete(" ").validar() }.isFailure)
    }
    @Test fun planoIsolaUsuariosEIgnoraConcluidosOuSemHorario() {
        val data = "2028-02-29T12:30:00Z"
        val lembretes = listOf(LembreteLocal("a", "eu", "Entrega", false, data), LembreteLocal("b", "outra", "Privado", false, data),
            LembreteLocal("c", "eu", "Concluído", true, data), LembreteLocal("d", "eu", "Sem data", false, null))
        val tarefas = listOf(TarefaLocal("t", "eu", "Prova", "A_FAZER", prazo = "2028-02-29"),
            TarefaLocal("u", "eu", "Concluída", "CONCLUIDO", prazo = "2028-02-29"))
        val plano = alertasDoUsuario("eu", OpcoesAlertas(true, true), lembretes, tarefas, ZoneId.of("America/Maceio"))
        assertEquals(listOf("tarefa:t", "lembrete:a"), plano.map { it.chave })
        assertEquals(Instant.parse("2028-02-29T12:00:00Z").toEpochMilli(), plano.first().horario)
        assertTrue(alertasDoUsuario("eu", OpcoesAlertas(), lembretes, tarefas).isEmpty())
    }
    @Test fun sincronizarNaoDisparaPassadoEReagendarCriaNovaEntrega() {
        val alerta = Alerta("id", "Título", "Texto", 2000, "lembretes", "id")
        assertTrue(registrosAtualizados("eu", listOf(alerta), emptyList(), 3000).single().entregue)
        val futuro = registrosAtualizados("eu", listOf(alerta), emptyList(), 1000).single()
        assertFalse(futuro.entregue)
        assertEquals(futuro, registrosAtualizados("eu", listOf(alerta), listOf(futuro), 3000).single())
        assertFalse(registrosAtualizados("eu", listOf(alerta.copy(horario = 4000)), listOf(futuro.copy(entregue = true)), 3000).single().entregue)
        assertFalse(registrosAtualizados("outra", listOf(alerta), listOf(futuro.copy(entregue = true)), 1000).single().entregue)
    }
}
