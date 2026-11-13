package br.com.unisync.mobile.ui.tarefas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.domain.tarefas.*
import br.com.unisync.mobile.ui.componentes.*
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelaTarefas(estado: EstadoTarefas, aoAtualizar: () -> Unit,
    aoSalvar: (String?, String, StatusTarefa) -> Unit, aoExcluir: (Tarefa) -> Unit) {
    var filtro by rememberSaveable { mutableStateOf(StatusTarefa.A_FAZER.name) }
    var formulario by rememberSaveable { mutableStateOf(false) }
    var id by rememberSaveable { mutableStateOf<String?>(null) }
    var titulo by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf(StatusTarefa.A_FAZER.name) }
    var excluir by remember { mutableStateOf<Tarefa?>(null) }
    var descartar by remember { mutableStateOf(false) }
    var tituloInicial by rememberSaveable { mutableStateOf("") }
    var statusInicial by rememberSaveable { mutableStateOf(StatusTarefa.A_FAZER.name) }
    var enviado by remember { mutableStateOf(false) }
    LaunchedEffect(estado.confirmacao) { if (estado.confirmacao > 0) { formulario = false; excluir = null } }
    val ocupado = estado.salvando || estado.atualizando
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = aoAtualizar, enabled = !ocupado) { Text("Atualizar") }
                Button(enabled = !ocupado, onClick = {
                    id = null; titulo = ""; tituloInicial = ""; status = StatusTarefa.A_FAZER.name
                    statusInicial = status; enviado = false; formulario = true
                }) { Text("Nova tarefa") }
            }
        }
        item { StatusSincronizacao(estado) }
        estado.erro?.let { erro -> item { MensagemDeErro(erro) } }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusTarefa.entries.forEach { item ->
                    FilterChip(selected = filtro == item.name, onClick = { filtro = item.name },
                        label = { Text("${item.rotulo} (${estado.itens.count { it.status == item }})") },
                        modifier = Modifier.heightIn(min = 48.dp))
                }
            }
        }
        if (estado.lendo) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        val filtradas = estado.itens.filter { it.status.name == filtro }
        if (!estado.lendo && filtradas.isEmpty()) item {
            ConteudoVazio(when (StatusTarefa.valueOf(filtro)) {
                StatusTarefa.A_FAZER -> "Nenhuma tarefa a fazer"
                StatusTarefa.EM_ANDAMENTO -> "Nenhuma tarefa em andamento"
                StatusTarefa.CONCLUIDO -> "Nenhuma tarefa concluída"
            },
                "Crie uma tarefa ou selecione outro status.")
        }
        items(filtradas, key = { it.id }) { tarefa ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tarefa.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(tarefa.status.rotulo, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled = !ocupado, onClick = {
                            id = tarefa.id; titulo = tarefa.titulo; tituloInicial = titulo
                            status = tarefa.status.name; statusInicial = status; enviado = false; formulario = true
                        }) { Text("Editar") }
                        TextButton(enabled = !ocupado, onClick = { excluir = tarefa }) {
                            Text("Excluir", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    val fechar = {
        if (titulo != tituloInicial || status != statusInicial) descartar = true else formulario = false
    }
    if (formulario) AlertDialog(
        onDismissRequest = { if (!estado.salvando) fechar() },
        title = { Text(if (id == null) "Nova tarefa" else "Editar tarefa") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, enabled = !estado.salvando,
                    isError = enviado && titulo.trim().length !in 1..100,
                    supportingText = { Text("${titulo.trim().length}/100 caracteres") }, modifier = Modifier.fillMaxWidth())
                if (id != null) StatusTarefa.entries.forEach { opcao ->
                    FilterChip(selected = status == opcao.name, onClick = { status = opcao.name },
                        enabled = !estado.salvando, label = { Text(opcao.rotulo) })
                }
                estado.erro?.let { MensagemDeErro(it) }
                if (estado.salvando) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = !ocupado, onClick = {
            enviado = true
            if (titulo.trim().length in 1..100) aoSalvar(id, titulo, StatusTarefa.valueOf(status))
        }) { Text("Salvar") } },
        dismissButton = { TextButton(enabled = !estado.salvando, onClick = fechar) { Text("Cancelar") } }
    )
    if (descartar) AlertDialog(onDismissRequest = { descartar = false }, title = { Text("Descartar alterações?") },
        text = { Text("As alterações ainda não foram salvas.") },
        confirmButton = { TextButton(onClick = { descartar = false; formulario = false }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { descartar = false }) { Text("Continuar editando") } })
    excluir?.let { tarefa -> AlertDialog(onDismissRequest = { if (!estado.salvando) excluir = null },
        title = { Text("Excluir tarefa?") }, text = { Column { Text(tarefa.titulo); estado.erro?.let { MensagemDeErro(it) } } },
        confirmButton = { TextButton(enabled = !ocupado, onClick = { aoExcluir(tarefa) }) { Text("Excluir") } },
        dismissButton = { TextButton(enabled = !estado.salvando, onClick = { excluir = null }) { Text("Cancelar") } }) }
}

@Composable
fun StatusSincronizacao(estado: EstadoTarefas) {
    val texto = when {
        estado.atualizando -> "Atualizando tarefas…"
        estado.atualizadoEm != null -> "Última atualização: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.forLanguageTag("pt-BR")).format(Date(estado.atualizadoEm))}"
        else -> "Nenhuma atualização recebida ainda"
    }
    Text(texto, style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}
