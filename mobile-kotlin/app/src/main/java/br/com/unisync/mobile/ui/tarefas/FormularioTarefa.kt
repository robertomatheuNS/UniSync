package br.com.unisync.mobile.ui.tarefas

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.domain.academico.Turma
import br.com.unisync.mobile.domain.tarefas.*
import br.com.unisync.mobile.ui.componentes.MensagemDeErro
import java.time.LocalDate

@Composable
internal fun FormularioTarefa(tarefa: Tarefa?, turmas: List<Turma>, estado: EstadoTarefas,
    aoFechar: () -> Unit, aoSalvar: (String?, DadosTarefa) -> Unit) {
    val inicial = remember { tarefa?.dados() ?: DadosTarefa("") }
    var titulo by rememberSaveable { mutableStateOf(inicial.titulo) }
    var status by rememberSaveable { mutableStateOf(inicial.status.name) }
    var prioridade by rememberSaveable { mutableStateOf(inicial.prioridade.name) }
    var prazo by rememberSaveable { mutableStateOf(inicial.prazo?.format(formatoPrazo) ?: "") }
    var turmaId by rememberSaveable { mutableStateOf(inicial.turmaId) }
    var enviado by rememberSaveable { mutableStateOf(false) }
    var descartar by rememberSaveable { mutableStateOf(false) }
    val focoTitulo = remember { FocusRequester() }
    val focoPrazo = remember { FocusRequester() }
    val data = runCatching { lerPrazo(prazo) }
    val erroTitulo = titulo.trim().length !in 1..100
    val erroPrazo = data.isFailure
    val contexto = LocalContext.current
    val ocupado = estado.salvando || estado.atualizando
    val fechar = {
        if (titulo != inicial.titulo || status != inicial.status.name || prioridade != inicial.prioridade.name ||
            prazo != (inicial.prazo?.format(formatoPrazo) ?: "") || turmaId != inicial.turmaId) descartar = true
        else aoFechar()
    }
    AlertDialog(onDismissRequest = { if (!estado.salvando) fechar() },
        title = { Text(if (tarefa == null) "Nova tarefa" else "Editar tarefa") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, enabled = !estado.salvando,
                    isError = enviado && erroTitulo, supportingText = {
                        Text(if (enviado && erroTitulo) "Informe entre 1 e 100 caracteres." else "${titulo.trim().length}/100 caracteres")
                    }, modifier = Modifier.fillMaxWidth().focusRequester(focoTitulo))
                Text("Prioridade", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrioridadeTarefa.entries.forEach { opcao -> FilterChip(selected = prioridade == opcao.name,
                        onClick = { prioridade = opcao.name }, enabled = !estado.salvando, label = { Text(opcao.rotulo) }) }
                }
                OutlinedTextField(prazo, { prazo = it }, label = { Text("Prazo (opcional)") },
                    singleLine = true, enabled = !estado.salvando, isError = enviado && erroPrazo,
                    supportingText = { Text(if (enviado && erroPrazo) "Data inválida. Use dd/mm/aaaa." else "dd/mm/aaaa • deixe vazio para não definir prazo") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focoPrazo))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = !estado.salvando, onClick = {
                        val atual = data.getOrNull() ?: LocalDate.now()
                        DatePickerDialog(contexto, { _, ano, mes, dia -> prazo = LocalDate.of(ano, mes + 1, dia).format(formatoPrazo) },
                            atual.year, atual.monthValue - 1, atual.dayOfMonth).show()
                    }) { Text("Escolher data") }
                    if (prazo.isNotBlank()) TextButton(enabled = !estado.salvando, onClick = { prazo = "" }) { Text("Remover prazo") }
                }
                Text("Disciplina (opcional)", style = MaterialTheme.typography.titleSmall)
                Text("Vincule a uma das suas turmas.", style = MaterialTheme.typography.bodySmall)
                FilterChip(selected = turmaId == null, onClick = { turmaId = null }, enabled = !estado.salvando,
                    label = { Text("Sem disciplina") })
                if (turmaId != null && turmas.none { it.id == turmaId }) {
                    Text("Vínculo atual: ${tarefa?.disciplina ?: "turma indisponível"}. Será mantido ao salvar.", style = MaterialTheme.typography.bodySmall)
                }
                turmas.forEach { turma -> FilterChip(selected = turmaId == turma.id, onClick = { turmaId = turma.id },
                    enabled = !estado.salvando, label = { Text("${turma.disciplina} • ${turma.nome}") }) }
                if (turmas.isEmpty()) Text("Suas turmas aparecem após a atualização em Minhas turmas.", style = MaterialTheme.typography.bodySmall)
                if (tarefa != null) {
                    Text("Status", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusTarefa.entries.forEach { opcao -> FilterChip(selected = status == opcao.name,
                            onClick = { status = opcao.name }, enabled = !estado.salvando, label = { Text(opcao.rotulo) }) }
                    }
                }
                estado.erro?.let { MensagemDeErro(it) }
                if (estado.salvando) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }, confirmButton = { TextButton(enabled = !ocupado, onClick = {
            enviado = true
            when { erroTitulo -> focoTitulo.requestFocus(); erroPrazo -> focoPrazo.requestFocus()
                else -> aoSalvar(tarefa?.id, DadosTarefa(titulo, StatusTarefa.valueOf(status), PrioridadeTarefa.valueOf(prioridade), data.getOrNull(), turmaId)) }
        }) { Text(if (estado.salvando) "Salvando…" else "Salvar") } },
        dismissButton = { TextButton(enabled = !estado.salvando, onClick = fechar) { Text("Cancelar") } })
    if (descartar) AlertDialog(onDismissRequest = { descartar = false }, title = { Text("Descartar alterações?") },
        text = { Text("As alterações ainda não foram salvas.") },
        confirmButton = { TextButton(onClick = aoFechar) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { descartar = false }) { Text("Continuar editando") } })
}
