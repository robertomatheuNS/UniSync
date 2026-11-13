package br.com.unisync.mobile.domain.lembretes

import br.com.unisync.mobile.domain.tarefas.lerPrazo
import kotlinx.coroutines.flow.Flow
import java.time.*
import java.time.format.DateTimeFormatter

data class Lembrete(val id: String, val usuarioId: String, val titulo: String,
    val concluido: Boolean = false, val agendadoEm: Instant? = null) {
    fun dados() = DadosLembrete(titulo, concluido, agendadoEm)
}

data class DadosLembrete(val titulo: String, val concluido: Boolean = false, val agendadoEm: Instant? = null) {
    fun validar(): DadosLembrete {
        require(titulo.trim().length in 1..200) { "Informe um título entre 1 e 200 caracteres." }
        require(agendadoEm == null || agendadoEm.atOffset(ZoneOffset.UTC).year in 1..9999) { "Informe uma data válida." }
        return copy(titulo = titulo.trim())
    }
}

fun lerAgendamento(data: String, hora: String, zona: ZoneId = ZoneId.systemDefault()): Instant? {
    if (data.isBlank() && hora.isBlank()) return null
    val dia = lerPrazo(data) ?: throw IllegalArgumentException("Informe a data do lembrete.")
    require(hora.matches(Regex("[0-9]{2}:[0-9]{2}"))) { "Informe uma hora válida no formato hh:mm." }
    val horario = runCatching { LocalTime.parse(hora) }.getOrNull()
        ?: throw IllegalArgumentException("Informe uma hora válida no formato hh:mm.")
    val local = LocalDateTime.of(dia, horario)
    val offsets = zona.rules.getValidOffsets(local)
    require(offsets.isNotEmpty()) { "Esse horário não existe neste fuso. Escolha outro horário." }
    // Em transições com horário repetido, usa a primeira ocorrência.
    return local.toInstant(offsets.first())
}

interface Lembretes {
    fun observar(usuarioId: String): Flow<List<Lembrete>>
    suspend fun ultimaAtualizacao(usuarioId: String): Long?
    suspend fun atualizar(usuarioId: String, token: String)
    suspend fun criar(usuarioId: String, token: String, dados: DadosLembrete)
    suspend fun editar(usuarioId: String, token: String, id: String, dados: DadosLembrete)
    suspend fun concluir(usuarioId: String, token: String, id: String, concluido: Boolean)
    suspend fun excluir(usuarioId: String, token: String, id: String)
}
