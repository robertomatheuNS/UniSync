package br.com.unisync.mobile.domain.oportunidades

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import java.net.URI

data class Oportunidade(val id: String, val titulo: String, val professor: String,
    val materia: String, val descricao: String?, val tipo: String, val dataEvento: String,
    val professorId: String? = null, val urlInscricao: String? = null, val prazoInscricao: Instant? = null) {
    /** Datas com offset são convertidas; sem offset, preservamos o horário informado. */
    fun horario(zona: ZoneId = ZoneId.systemDefault()): LocalDateTime? =
        runCatching { OffsetDateTime.parse(dataEvento).atZoneSameInstant(zona).toLocalDateTime() }.getOrElse {
            runCatching { LocalDateTime.parse(dataEvento) }.getOrNull()
        }
    fun dataFormatada(): String = horario()?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.forLanguageTag("pt-BR"))) ?: dataEvento
    fun encerrada(agora: LocalDateTime = LocalDateTime.now()): Boolean = horario()?.isBefore(agora) == true
    fun inscricoesEncerradas(agora: Instant = Instant.now()) = prazoInscricao?.let { !agora.isBefore(it) } ?: false
    private fun instanteEvento(zona: ZoneId): Instant? = runCatching { OffsetDateTime.parse(dataEvento).toInstant() }
        .getOrElse { horario(zona)?.atZone(zona)?.toInstant() }
    fun podeInscrever(agora: Instant = Instant.now(), zona: ZoneId = ZoneId.systemDefault()): Boolean =
        instanteEvento(zona)?.let { agora.isBefore(it) } == true && !inscricoesEncerradas(agora) &&
            runCatching { validarUrlInscricao(urlInscricao) }.getOrNull() != null
    fun prazoFormatado(): String? = prazoInscricao?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.forLanguageTag("pt-BR")))
    fun situacaoInscricao(agora: Instant = Instant.now(), zona: ZoneId = ZoneId.systemDefault()): String = when {
        instanteEvento(zona)?.let { !agora.isBefore(it) } == true -> "Evento já realizado"
        inscricoesEncerradas(agora) -> "Inscrições encerradas"
        prazoInscricao == null -> if (urlInscricao.isNullOrBlank()) "Inscrição não informada" else "Prazo de inscrição não informado"
        prazoInscricao <= agora.plus(Duration.ofDays(3)) -> "Prazo de inscrição próximo"
        else -> "Inscrições abertas"
    }
}

data class DadosOportunidade(val titulo: String, val materia: String, val descricao: String,
    val tipo: String, val horarioLocal: LocalDateTime, val urlInscricao: String? = null, val prazoInscricao: Instant? = null) {
    fun validar(zona: ZoneId = ZoneId.systemDefault()): DadosOportunidade {
        require(titulo.trim().length in 5..100) { "O título deve ter entre 5 e 100 caracteres." }
        require(materia.isNotBlank()) { "Informe a matéria." }
        require(tipo.isNotBlank()) { "Informe a categoria." }
        require(horarioLocal.atZone(zona).toLocalDateTime() == horarioLocal) { "Esse horário não existe no fuso selecionado. Escolha outro horário." }
        require(prazoInscricao == null || !prazoInscricao.isAfter(horarioLocal.atZone(zona).toInstant())) { "O prazo de inscrição não pode ser posterior ao evento." }
        return copy(titulo = titulo.trim(), materia = materia.trim(), descricao = descricao.trim(), tipo = tipo.trim(), urlInscricao = validarUrlInscricao(urlInscricao))
    }
    fun dataParaApi(zona: ZoneId = ZoneId.systemDefault()): String = horarioLocal.atZone(zona).toOffsetDateTime().toString()
}

fun validarUrlInscricao(valor: String?): String? {
    val texto = valor?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val erro = "Informe um endereço HTTPS sem usuário ou senha."
    require(texto.length <= 2048 && texto.none { it.isWhitespace() || it.isISOControl() }) { erro }
    val uri = runCatching { URI(texto) }.getOrNull() ?: throw IllegalArgumentException(erro)
    require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
        (uri.port == -1 || uri.port in 1..65535)) { erro }
    return "https" + uri.toASCIIString().substringAfter(uri.scheme)
}

fun Oportunidade.textoParaCompartilhar(): String = buildString {
    append(titulo); append("\n"); append("${tipo} • $materia\n${dataFormatada()}\nProfessor: $professor")
    prazoFormatado()?.let { append("\nPrazo de inscrição: $it") }
    append("\n${situacaoInscricao()}")
    runCatching { validarUrlInscricao(urlInscricao) }.getOrNull()?.let { append("\nInscrição: $it") }
}

interface Oportunidades {
    val itens: Flow<List<Oportunidade>>
    suspend fun atualizar(token: String)
    suspend fun publicar(token: String, nomeProfessor: String, dados: DadosOportunidade)
    suspend fun editar(token: String, id: String, dados: DadosOportunidade)
    suspend fun excluir(token: String, id: String)
}
