package br.com.unisync.mobile

import br.com.unisync.mobile.domain.academico.*
import br.com.unisync.mobile.domain.oportunidades.Oportunidade
import br.com.unisync.mobile.domain.painel.*
import org.junit.Assert.*
import org.junit.Test

class ResumoInicioTest {
    private val turma = Turma("turma", "Turma A", "Kotlin", "", "prof", "Ana", null, 1)
    private val aviso = Aviso("1", turma.id, turma.nome, turma.disciplina, "prof", "Ana", "Título", "Conteúdo", "2026-11-17T10:00:00Z", "2026-11-17T10:00:00Z", false)
    @Test fun avisoRevisadoApareceAntesDePublicadoMaisRecentemente() {
        val publicado = aviso.copy(id = "2", criadoEm = "2026-11-17T11:00:00Z", atualizadoEm = "2026-11-17T11:00:00Z")
        val revisado = aviso.copy(atualizadoEm = "2026-11-17T09:00:00-03:00")
        assertEquals(listOf(revisado, publicado), avisosRecentes(listOf(publicado, revisado), listOf(turma)))
    }
    @Test fun limitaTresAvisosComDesempateEstavelSemRepetirIds() {
        val itens = (1..4).map { aviso.copy(id = "$it") }
        assertEquals(listOf("4", "3", "2"), avisosRecentes(itens + itens.last(), listOf(turma)).map { it.id })
    }
    @Test fun dataInvalidaUsaCriacaoEItemSemDataValidaFicaPorUltimo() {
        val recuperado = aviso.copy(id = "2", atualizadoEm = "inválido", criadoEm = "2026-11-17T11:00:00Z")
        val invalido = aviso.copy(id = "3", atualizadoEm = "inválido", criadoEm = "inválido")
        assertEquals(listOf("2", "1", "3"), avisosRecentes(listOf(invalido, aviso, recuperado), listOf(turma)).map { it.id })
    }
    @Test fun avisosDeTurmaAusenteNaoEntramNoResumo() {
        assertEquals(listOf(aviso), avisosRecentes(listOf(aviso, aviso.copy(id = "2", turmaId = "outra")), listOf(turma)))
        assertTrue(avisosRecentes(listOf(aviso), emptyList()).isEmpty())
    }
    @Test fun oportunidadesRecentesSeguemCriacaoPeloObjectIdEDataDoEventoNaoAlteraOrdem() {
        val antiga = Oportunidade("600000000000000000000001", "Evento futuro", "Ana", "Kotlin", null, "Workshop", "2030-01-01T12:00:00Z")
        val nova = antiga.copy(id = "700000000000000000000001", titulo = "Publicação nova", dataEvento = "2020-01-01T12:00:00Z")
        val terceira = antiga.copy(id = "650000000000000000000001")
        val quarta = antiga.copy(id = "610000000000000000000001")
        assertEquals(listOf(nova, terceira, quarta), oportunidadesRecentes(listOf(antiga, nova, terceira, quarta, nova)))
    }
    @Test fun listasVaziasNaoCriamConteudoArtificial() {
        assertTrue(avisosRecentes(emptyList(), listOf(turma)).isEmpty())
        assertTrue(oportunidadesRecentes(emptyList()).isEmpty())
    }
}
