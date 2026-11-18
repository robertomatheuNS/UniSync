package br.com.unisync.mobile.ui.painel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.domain.painel.*
import br.com.unisync.mobile.domain.tarefas.*
import br.com.unisync.mobile.ui.academico.EstadoAcademico
import br.com.unisync.mobile.ui.componentes.MensagemDeErro
import br.com.unisync.mobile.ui.lembretes.*
import br.com.unisync.mobile.ui.oportunidades.EstadoOportunidades
import br.com.unisync.mobile.ui.tarefas.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

data class AcoesInicio(val atualizarAcademico: () -> Unit = {}, val atualizarOportunidades: () -> Unit = {},
    val atualizarTarefas: () -> Unit = {}, val atualizarLembretes: () -> Unit = {},
    val verTurmas: () -> Unit = {}, val verAvisos: () -> Unit = {}, val abrirAviso: (String) -> Unit = {},
    val verMural: () -> Unit = {}, val verTarefas: () -> Unit = {}, val verLembretes: () -> Unit = {},
    val verAgenda: () -> Unit = {}, val verOportunidades: () -> Unit = {}, val abrirOportunidade: (String) -> Unit = {},
    val abrirLembrete: (String) -> Unit = {})

@Composable
fun TelaInicio(nome: String, professor: Boolean, academico: EstadoAcademico, oportunidades: EstadoOportunidades,
    lembretes: EstadoLembretes, tarefas: EstadoTarefas? = null, acoes: AcoesInicio = AcoesInicio()) {
    val avisos = avisosRecentes(academico.avisos, academico.turmas)
    val recentes = oportunidadesRecentes(oportunidades.itens)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "saudacao") { Text("Olá, $nome", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() }) }
        item(key = "academico") { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TituloInicio("Vida acadêmica")
            if (!academico.lendo || academico.turmas.isNotEmpty()) Text("${academico.turmas.size} turmas • ${if (professor) academico.avisos.size.toString() + " avisos publicados" else academico.avisos.count { !it.lido }.toString() + " avisos não lidos"}")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = acoes.verTurmas) { Text("Minhas turmas") }
                OutlinedButton(onClick = acoes.verAvisos) { Text("Ver avisos") }
                OutlinedButton(onClick = acoes.verMural) { Text("Abrir mural") }
            }
        } } }
        item(key = "avisos") { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TituloInicio("Avisos recentes")
            if (academico.lendo || academico.atualizando) LinearProgressIndicator(Modifier.fillMaxWidth())
            academico.erro?.let { MensagemDeErro(it)
                if (avisos.isNotEmpty()) Text("Exibindo a última consulta desta sessão.", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(enabled = !academico.atualizando && !academico.salvando, onClick = acoes.atualizarAcademico) { Text("Atualizar avisos") }
            if (!academico.lendo && !academico.atualizando && academico.erro == null && avisos.isEmpty())
                Text(if (academico.turmas.isEmpty()) "Entre em uma turma ou crie uma para acompanhar os avisos." else "Nenhum aviso publicado nas suas turmas.")
        } }
        items(avisos, key = { "aviso:${it.id}" }) { aviso ->
            OutlinedCard(onClick = { acoes.abrirAviso(aviso.id) }, modifier = Modifier.fillMaxWidth().testTag("inicio-aviso:${aviso.id}")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!professor) Text(if (aviso.lido) "Lido" else "Não lido", style = MaterialTheme.typography.labelLarge)
                    Text(aviso.titulo, style = MaterialTheme.typography.titleMedium)
                    Text("${aviso.disciplina} • ${aviso.turmaNome}", style = MaterialTheme.typography.bodySmall)
                    Text("${aviso.professor} • ${dataAvisoInicio(aviso.atualizadoEm)}", style = MaterialTheme.typography.bodySmall)
                    Text(aviso.conteudo, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Ver aviso", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        item(key = "oportunidades") { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TituloInicio("Oportunidades recentes")
            Text("Até três entre as oportunidades carregadas.", style = MaterialTheme.typography.bodySmall)
            if (oportunidades.lendo || oportunidades.atualizando) LinearProgressIndicator(Modifier.fillMaxWidth())
            oportunidades.erro?.let { MensagemDeErro(it)
                if (recentes.isNotEmpty()) Text("Exibindo as oportunidades salvas no aparelho.", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(enabled = !oportunidades.atualizando && !oportunidades.salvando, onClick = acoes.atualizarOportunidades) { Text("Atualizar oportunidades") }
            if (!oportunidades.lendo && !oportunidades.atualizando && oportunidades.erro == null && recentes.isEmpty()) Text("Nenhuma oportunidade disponível por enquanto.")
            Button(onClick = acoes.verOportunidades) { Text("Explorar oportunidades") }
        } }
        items(recentes, key = { "oportunidade:${it.id}" }) { item ->
            OutlinedCard(onClick = { acoes.abrirOportunidade(item.id) }, modifier = Modifier.fillMaxWidth().testTag("inicio-oportunidade:${item.id}")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.tipo, style = MaterialTheme.typography.labelLarge)
                    Text(item.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(item.dataFormatada())
                    Text("${item.materia} • ${item.professor}", style = MaterialTheme.typography.bodySmall)
                    if (item.encerrada()) Text("Evento já realizado", style = MaterialTheme.typography.bodySmall)
                    Text("Ver oportunidade", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        tarefas?.let { estado ->
            item(key = "status-tarefas") { StatusSincronizacao(estado); estado.erro?.let { MensagemDeErro(it) } }
            item(key = "tarefas") { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TituloInicio("Suas tarefas")
                StatusTarefa.entries.forEach { status -> Text("${status.rotulo}: ${estado.itens.count { it.status == status }}") }
                Text("${estado.itens.count { it.atrasada() }} tarefas atrasadas")
                estado.itens.filter { it.status != StatusTarefa.CONCLUIDO && it.prazo != null }.ordenadas().take(3).forEach { tarefa -> Text("${tarefa.prazo?.format(formatoPrazo)} • ${tarefa.titulo}") }
                Button(onClick = acoes.verTarefas) { Text("Organizar tarefas") }
                TextButton(enabled = !estado.atualizando && !estado.salvando, onClick = acoes.atualizarTarefas) { Text("Atualizar tarefas") }
            } } }
        }
        item(key = "lembretes") { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TituloInicio("Seus lembretes")
            Text("${lembretes.itens.count { !it.concluido }} lembretes pendentes")
            if (lembretes.lendo || lembretes.atualizando) LinearProgressIndicator(Modifier.fillMaxWidth())
            lembretes.erro?.let { MensagemDeErro(it) }
            lembretes.itens.filter { !it.concluido && it.agendadoEm != null }.sortedBy { it.agendadoEm }.take(3).forEach { item ->
                TextButton(onClick = { acoes.abrirLembrete(item.id) }) { Text("${item.horarioLocal()} • ${item.titulo}") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = acoes.verLembretes) { Text("Meus lembretes") }
                OutlinedButton(onClick = acoes.verAgenda) { Text("Ver agenda") }
            }
            TextButton(enabled = !lembretes.atualizando && !lembretes.salvando, onClick = acoes.atualizarLembretes) { Text("Atualizar lembretes") }
        } } }
    }
}

@Composable
private fun TituloInicio(texto: String) = Text(texto, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })

private fun dataAvisoInicio(valor: String) = runCatching {
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.systemDefault()).format(Instant.parse(valor))
}.getOrElse { valor }
