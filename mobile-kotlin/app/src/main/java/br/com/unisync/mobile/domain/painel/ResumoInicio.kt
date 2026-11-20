package br.com.unisync.mobile.domain.painel

import br.com.unisync.mobile.domain.academico.Aviso
import br.com.unisync.mobile.domain.academico.Turma
import br.com.unisync.mobile.domain.oportunidades.Oportunidade
import java.time.Instant

fun avisosRecentes(avisos: List<Aviso>, turmas: List<Turma>): List<Aviso> {
    val autorizadas = turmas.map { it.id }.toSet()
    fun data(item: Aviso) = runCatching { Instant.parse(item.atualizadoEm) }.getOrElse {
        runCatching { Instant.parse(item.criadoEm) }.getOrDefault(Instant.MIN)
    }
    return avisos.filter { it.turmaId in autorizadas }
        .sortedWith(compareByDescending<Aviso> { data(it) }.thenByDescending { it.id })
        .distinctBy { it.id }.take(3)
}

fun oportunidadesRecentes(itens: List<Oportunidade>): List<Oportunidade> =
    // A API usa ObjectId: sua ordem indica criação, independentemente da data do evento.
    // O recorte continua limitado aos itens retornados pela API e disponíveis no cache.
    itens.sortedByDescending { it.id }.distinctBy { it.id }.take(3)
