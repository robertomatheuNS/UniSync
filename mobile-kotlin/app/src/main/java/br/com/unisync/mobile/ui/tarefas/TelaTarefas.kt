package br.com.unisync.mobile.ui.tarefas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.domain.academico.Turma
import br.com.unisync.mobile.domain.tarefas.*
import br.com.unisync.mobile.ui.componentes.*
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelaTarefas(estado: EstadoTarefas, aoAtualizar: () -> Unit,
    aoSalvar: (String?, DadosTarefa) -> Unit, aoExcluir: (Tarefa) -> Unit,
    turmas: List<Turma> = emptyList(), tarefaInicial: String? = null, aoConsumirTarefa: () -> Unit = {}) {
    var filtro by rememberSaveable { mutableStateOf(StatusTarefa.A_FAZER.name) }
    var ordem by rememberSaveable { mutableStateOf(OrdemTarefa.PRAZO.name) }
    var formulario by rememberSaveable { mutableStateOf(false) }
    var id by rememberSaveable { mutableStateOf<String?>(null) }
    var excluir by remember { mutableStateOf<Tarefa?>(null) }
    var confirmacaoVista by rememberSaveable { mutableIntStateOf(estado.confirmacao) }
    LaunchedEffect(estado.confirmacao) { if (estado.confirmacao != confirmacaoVista) {
        confirmacaoVista = estado.confirmacao; formulario = false; excluir = null
    } }
    LaunchedEffect(tarefaInicial, estado.itens) {
        estado.itens.find { it.id == tarefaInicial }?.let {
            id = it.id; filtro = it.status.name; formulario = true; aoConsumirTarefa()
        }
    }
    val ocupado = estado.salvando || estado.atualizando
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = aoAtualizar, enabled = !ocupado) { Text("Atualizar") }
            Button(enabled = !ocupado, onClick = { id = null; formulario = true }) { Text("Nova tarefa") }
        } }
        item { StatusSincronizacao(estado) }
        estado.erro?.let { erro -> item { MensagemDeErro(erro) } }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusTarefa.entries.forEach { item -> FilterChip(selected = filtro == item.name,
                onClick = { filtro = item.name }, label = { Text("${item.rotulo} (${estado.itens.count { it.status == item }})") },
                modifier = Modifier.heightIn(min = 48.dp)) }
            FilterChip(selected = filtro == "ATRASADAS", onClick = { filtro = "ATRASADAS" },
                label = { Text("Atrasadas (${estado.itens.count { it.atrasada() }})") }, modifier = Modifier.heightIn(min = 48.dp))
        } }
        item { Text("Ordenar por", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OrdemTarefa.entries.forEach { opcao ->
                FilterChip(selected = ordem == opcao.name, onClick = { ordem = opcao.name }, label = { Text(opcao.rotulo) })
            } }
        }
        if (estado.lendo) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        val filtradas = estado.itens.filter { if (filtro == "ATRASADAS") it.atrasada() else it.status.name == filtro }
            .ordenadas(OrdemTarefa.valueOf(ordem))
        if (!estado.lendo && filtradas.isEmpty()) item { ConteudoVazio("Nenhuma tarefa neste filtro", "Crie uma tarefa ou selecione outro status.") }
        items(filtradas, key = { it.id }) { tarefa -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tarefa.titulo, style = MaterialTheme.typography.titleMedium)
                ResumoTarefa(tarefa)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = !ocupado, onClick = { id = tarefa.id; formulario = true }) { Text("Editar") }
                    TextButton(enabled = !ocupado, onClick = { excluir = tarefa }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                }
            }
        } }
    }
    if (formulario) {
        val tarefa = estado.itens.find { it.id == id }
        // Aguarda o cache ao restaurar um formulário de edição após recriar a Activity.
        if (id == null || tarefa != null) key(id) { FormularioTarefa(tarefa, turmas, estado, { formulario = false }, aoSalvar) }
        else if (!estado.lendo) AlertDialog(onDismissRequest = { formulario = false }, title = { Text("Tarefa indisponível") },
            text = { Text("Atualize as tarefas e tente novamente.") }, confirmButton = { TextButton(onClick = { formulario = false }) { Text("Fechar") } })
    }
    excluir?.let { tarefa -> AlertDialog(onDismissRequest = { if (!estado.salvando) excluir = null }, title = { Text("Excluir tarefa?") },
        text = { Column { Text(tarefa.titulo); estado.erro?.let { MensagemDeErro(it) } } },
        confirmButton = { TextButton(enabled = !ocupado, onClick = { aoExcluir(tarefa) }) { Text("Excluir") } },
        dismissButton = { TextButton(enabled = !estado.salvando, onClick = { excluir = null }) { Text("Cancelar") } }) }
}

@Composable
fun ResumoTarefa(tarefa: Tarefa) {
    Text("${tarefa.status.rotulo} • Prioridade ${tarefa.prioridade.rotulo.lowercase()}", style = MaterialTheme.typography.labelLarge)
    tarefa.disciplina?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    Text(tarefa.prazo?.let { "Prazo: ${it.format(formatoPrazo)}" } ?: "Sem prazo", style = MaterialTheme.typography.bodyMedium)
    if (tarefa.atrasada()) Text("Atrasada", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
}

@Composable
fun StatusSincronizacao(estado: EstadoTarefas) {
    val texto = when {
        estado.atualizando -> "Atualizando tarefas…"
        estado.atualizadoEm != null -> "Última atualização: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.forLanguageTag("pt-BR")).format(Date(estado.atualizadoEm))}"
        else -> "Nenhuma atualização recebida ainda"
    }
    Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}
