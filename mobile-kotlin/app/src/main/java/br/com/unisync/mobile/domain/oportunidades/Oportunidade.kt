package br.com.unisync.mobile.domain.oportunidades

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

data class Oportunidade(val id: String, val titulo: String, val professor: String,
    val materia: String, val descricao: String?, val tipo: String, val dataEvento: String) {
    /** Datas com offset são convertidas; sem offset, preservamos o horário informado. */
    fun horario(zona: ZoneId = ZoneId.systemDefault()): LocalDateTime? =
        runCatching { OffsetDateTime.parse(dataEvento).atZoneSameInstant(zona).toLocalDateTime() }.getOrElse {
            runCatching { LocalDateTime.parse(dataEvento) }.getOrNull()
        }
    fun dataFormatada(): String = horario()?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.forLanguageTag("pt-BR"))) ?: dataEvento
    fun encerrada(agora: LocalDateTime = LocalDateTime.now()): Boolean = horario()?.isBefore(agora) == true
}
