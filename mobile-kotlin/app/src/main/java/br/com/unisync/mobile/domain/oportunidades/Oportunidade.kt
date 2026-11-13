package br.com.unisync.mobile.domain.oportunidades

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.flow.Flow

data class Oportunidade(val id: String, val titulo: String, val professor: String,
    val materia: String, val descricao: String?, val tipo: String, val dataEvento: String,
    val professorId: String? = null) {
    /** Datas com offset são convertidas; sem offset, preservamos o horário informado. */
    fun horario(zona: ZoneId = ZoneId.systemDefault()): LocalDateTime? =
        runCatching { OffsetDateTime.parse(dataEvento).atZoneSameInstant(zona).toLocalDateTime() }.getOrElse {
            runCatching { LocalDateTime.parse(dataEvento) }.getOrNull()
        }
    fun dataFormatada(): String = horario()?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.forLanguageTag("pt-BR"))) ?: dataEvento
    fun encerrada(agora: LocalDateTime = LocalDateTime.now()): Boolean = horario()?.isBefore(agora) == true
}

data class DadosOportunidade(val titulo: String, val materia: String, val descricao: String,
    val tipo: String, val horarioLocal: LocalDateTime) {
    fun validar(zona: ZoneId = ZoneId.systemDefault()): DadosOportunidade {
        require(titulo.trim().length in 5..100) { "O título deve ter entre 5 e 100 caracteres." }
        require(materia.isNotBlank()) { "Informe a matéria." }
        require(tipo.isNotBlank()) { "Informe a categoria." }
        require(horarioLocal.atZone(zona).toLocalDateTime() == horarioLocal) { "Esse horário não existe no fuso selecionado. Escolha outro horário." }
        return copy(titulo = titulo.trim(), materia = materia.trim(), descricao = descricao.trim(), tipo = tipo.trim())
    }
    fun dataParaApi(zona: ZoneId = ZoneId.systemDefault()): String = horarioLocal.atZone(zona).toOffsetDateTime().toString()
}

interface Oportunidades {
    val itens: Flow<List<Oportunidade>>
    suspend fun atualizar(token: String)
    suspend fun publicar(token: String, nomeProfessor: String, dados: DadosOportunidade)
    suspend fun editar(token: String, id: String, dados: DadosOportunidade)
    suspend fun excluir(token: String, id: String)
}
