package br.com.unisync.mobile.notificacoes

import br.com.unisync.mobile.data.local.*
import java.time.*

data class OpcoesAlertas(val lembretes: Boolean = false, val tarefas: Boolean = false, val horaPrazos: LocalTime = LocalTime.of(9, 0))
data class Alerta(val chave: String, val titulo: String, val texto: String, val horario: Long, val destino: String, val itemId: String)

fun alertasDoUsuario(usuarioId: String, opcoes: OpcoesAlertas, lembretes: List<LembreteLocal>, tarefas: List<TarefaLocal>,
    zona: ZoneId = ZoneId.systemDefault()): List<Alerta> = buildList {
    if (opcoes.lembretes) lembretes.filter { it.usuarioId == usuarioId && !it.concluido }.forEach { item ->
        item.agendadoEm?.let { add(Alerta("lembrete:${item.id}", item.titulo, "Lembrete agendado", Instant.parse(it).toEpochMilli(), "lembretes", item.id)) }
    }
    if (opcoes.tarefas) tarefas.filter { it.alunoId == usuarioId && it.status != "CONCLUIDO" }.forEach { item ->
        item.prazo?.let { data -> add(Alerta("tarefa:${item.id}", item.titulo, "Prazo da tarefa: ${LocalDate.parse(data).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"))}", LocalDate.parse(data).atTime(opcoes.horaPrazos).atZone(zona).toInstant().toEpochMilli(), "tarefas", item.id)) }
    }
}.sortedWith(compareBy<Alerta> { it.horario }.thenBy { it.chave })

/** Novos itens já vencidos não geram uma rajada de notificações ao sincronizar. */
fun registrosAtualizados(usuarioId: String, alertas: List<Alerta>, anteriores: List<AlertaLocal>, agora: Long): List<AlertaLocal> = alertas.map { alerta ->
    anteriores.find { it.usuarioId == usuarioId && it.chave == alerta.chave && it.horario == alerta.horario }
        ?: AlertaLocal(usuarioId, alerta.chave, alerta.horario, entregue = alerta.horario <= agora)
}
