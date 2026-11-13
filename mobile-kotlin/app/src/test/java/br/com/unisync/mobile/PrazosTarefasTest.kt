package br.com.unisync.mobile

import br.com.unisync.mobile.domain.tarefas.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PrazosTarefasTest {
    private val hoje = LocalDate.of(2027, 3, 1)
    private fun tarefa(id: String, prazo: LocalDate?, prioridade: PrioridadeTarefa = PrioridadeTarefa.MEDIA) =
        Tarefa(id, "aluno", id, StatusTarefa.A_FAZER, prioridade, prazo)

    @Test fun atrasoConsideraDiaInteiroENaoMarcaConcluidas() {
        val anterior = tarefa("a", hoje.minusDays(1))
        assertTrue(anterior.atrasada(hoje))
        assertFalse(anterior.copy(status = StatusTarefa.CONCLUIDO).atrasada(hoje))
        assertFalse(tarefa("b", hoje).atrasada(hoje))
        assertFalse(tarefa("c", null).atrasada(hoje))
    }
    @Test fun ordenacaoUsaPrazoPrioridadeESemPrazoNoFinal() {
        val itens = listOf(tarefa("sem", null, PrioridadeTarefa.ALTA), tarefa("baixa", hoje, PrioridadeTarefa.BAIXA),
            tarefa("alta", hoje, PrioridadeTarefa.ALTA), tarefa("antes", hoje.minusDays(1)))
        assertEquals(listOf("antes", "alta", "baixa", "sem"), itens.ordenadas().map { it.id })
        assertEquals(listOf("alta", "sem", "antes", "baixa"), itens.ordenadas(OrdemTarefa.PRIORIDADE).map { it.id })
    }
    @Test fun dataValidaAnoBissextoERejeitaDatasImpossiveis() {
        assertEquals(LocalDate.of(2028, 2, 29), lerPrazo("29/02/2028"))
        assertNull(lerPrazo("  "))
        listOf("29/02/2027", "31/04/2027", "1/2/2027", "01/01/0000", "2027-01-01").forEach {
            assertTrue(it, runCatching { lerPrazo(it) }.isFailure)
        }
    }
}
