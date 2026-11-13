package br.com.unisync.mobile.ui.lembretes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.domain.lembretes.*
import br.com.unisync.mobile.domain.tarefas.formatoPrazo
import br.com.unisync.mobile.ui.componentes.*
import java.text.DateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")
fun Lembrete.horarioLocal(): String = agendadoEm?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("dd/MM/uuuu 'às' HH:mm")) ?: "Sem data e horário"

@Composable
fun TelaLembretes(estado: EstadoLembretes, aoAtualizar: () -> Unit, aoSalvar: (String?, DadosLembrete) -> Unit,
    aoConcluir: (Lembrete) -> Unit, aoExcluir: (Lembrete) -> Unit, aoConfigurarAlertas: () -> Unit = {},
    lembreteInicial: String? = null, aoConsumirLembrete: () -> Unit = {}) {
    var formulario by rememberSaveable { mutableStateOf(false) }
    var id by rememberSaveable { mutableStateOf<String?>(null) }
    var concluidos by rememberSaveable { mutableStateOf(false) }
    var excluirId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmacaoVista by rememberSaveable { mutableIntStateOf(estado.confirmacao) }
    val ocupado = estado.salvando || estado.atualizando
    LaunchedEffect(estado.confirmacao) { if (confirmacaoVista != estado.confirmacao) {
        confirmacaoVista = estado.confirmacao; formulario = false; excluirId = null
    } }
    LaunchedEffect(lembreteInicial, estado.itens) { estado.itens.find { it.id == lembreteInicial }?.let {
        id = it.id; concluidos = it.concluido; formulario = true; aoConsumirLembrete()
    } }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = aoAtualizar, enabled = !ocupado) { Text("Atualizar") }
            Button(onClick = { id = null; formulario = true }, enabled = !ocupado) { Text("Novo lembrete") }
        } }
        item { Text("Organize seus lembretes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text("Defina uma data e horário para incluir na agenda. Ative os alertas no perfil para receber notificações.")
            TextButton(onClick = aoConfigurarAlertas) { Text("Configurar alertas") }
        }
        item { StatusLembretes(estado) }
        estado.erro?.let { item { MensagemDeErro(it) } }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !concluidos, onClick = { concluidos = false }, label = { Text("Pendentes (${estado.itens.count { !it.concluido }})") })
            FilterChip(selected = concluidos, onClick = { concluidos = true }, label = { Text("Concluídos (${estado.itens.count { it.concluido }})") })
        } }
        if (estado.lendo) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        val itens = estado.itens.filter { it.concluido == concluidos }
            .sortedWith(compareBy<Lembrete> { it.agendadoEm ?: Instant.MAX }.thenBy { it.titulo.lowercase() })
        if (!estado.lendo && itens.isEmpty()) item { ConteudoVazio("Nenhum lembrete neste filtro", "Crie um lembrete ou selecione outro filtro.") }
        items(itens, key = { it.id }) { item -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.titulo, style = MaterialTheme.typography.titleMedium)
                Text(item.horarioLocal())
                Text(if (item.concluido) "Concluído" else "Pendente", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { aoConcluir(item) }, enabled = !ocupado) { Text(if (item.concluido) "Reabrir" else "Concluir") }
                    TextButton(onClick = { id = item.id; formulario = true }, enabled = !ocupado) { Text("Editar") }
                    TextButton(onClick = { excluirId = item.id }, enabled = !ocupado) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                }
            }
        } }
    }
    if (formulario) {
        val item = estado.itens.find { it.id == id }
        if (id == null || item != null) key(id) { FormularioLembrete(item, estado, { formulario = false }, aoSalvar) }
        else if (!estado.lendo) AlertDialog(onDismissRequest = { formulario = false }, title = { Text("Lembrete indisponível") },
            text = { Text("Atualize a lista e tente novamente.") }, confirmButton = { TextButton(onClick = { formulario = false }) { Text("Fechar") } })
    }
    estado.itens.find { it.id == excluirId }?.let { item -> AlertDialog(onDismissRequest = { if (!estado.salvando) excluirId = null },
        title = { Text("Excluir lembrete?") }, text = { Column { Text(item.titulo); estado.erro?.let { MensagemDeErro(it) } } },
        confirmButton = { TextButton(enabled = !ocupado, onClick = { aoExcluir(item) }) { Text("Excluir") } },
        dismissButton = { TextButton(enabled = !estado.salvando, onClick = { excluirId = null }) { Text("Cancelar") } }) }
}

@Composable
fun StatusLembretes(estado: EstadoLembretes) {
    Text(when {
        estado.atualizando -> "Atualizando lembretes…"
        estado.atualizadoEm != null -> "Lembretes atualizados em ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.forLanguageTag("pt-BR")).format(Date(estado.atualizadoEm))}"
        else -> "Nenhuma atualização de lembretes recebida ainda"
    }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun FormularioLembrete(item: Lembrete?, estado: EstadoLembretes, aoFechar: () -> Unit, aoSalvar: (String?, DadosLembrete) -> Unit) {
    val inicial = remember { item?.dados() ?: DadosLembrete("") }
    val horarioInicial = remember { inicial.agendadoEm?.atZone(ZoneId.systemDefault()) }
    val dataInicial = horarioInicial?.format(formatoPrazo) ?: ""
    val horaInicial = horarioInicial?.format(formatoHora) ?: ""
    var titulo by rememberSaveable { mutableStateOf(inicial.titulo) }
    var data by rememberSaveable { mutableStateOf(dataInicial) }
    var hora by rememberSaveable { mutableStateOf(horaInicial) }
    var enviado by rememberSaveable { mutableStateOf(false) }
    var descartar by rememberSaveable { mutableStateOf(false) }
    val focoTitulo = remember { FocusRequester() }
    val focoData = remember { FocusRequester() }
    val agendamento = runCatching { if (data == dataInicial && hora == horaInicial) inicial.agendadoEm else lerAgendamento(data, hora) }
    val erroTitulo = titulo.trim().length !in 1..200
    val contexto = LocalContext.current
    val ocupado = estado.salvando || estado.atualizando
    val fechar = { if (titulo != inicial.titulo || data != dataInicial || hora != horaInicial) descartar = true else aoFechar() }
    AlertDialog(onDismissRequest = { if (!estado.salvando) fechar() }, title = { Text(if (item == null) "Novo lembrete" else "Editar lembrete") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(titulo, { titulo = it }, label = { Text("Título do lembrete") }, enabled = !estado.salvando,
                isError = enviado && erroTitulo, supportingText = { Text(if (enviado && erroTitulo) "Informe entre 1 e 200 caracteres." else "${titulo.trim().length}/200 caracteres") },
                modifier = Modifier.fillMaxWidth().focusRequester(focoTitulo))
            OutlinedTextField(data, { data = it }, label = { Text("Data (opcional)") }, singleLine = true, enabled = !estado.salvando,
                isError = enviado && agendamento.isFailure, supportingText = { Text("dd/mm/aaaa") }, modifier = Modifier.fillMaxWidth().focusRequester(focoData))
            OutlinedTextField(hora, { hora = it }, label = { Text("Horário (opcional)") }, singleLine = true, enabled = !estado.salvando,
                isError = enviado && agendamento.isFailure, supportingText = { Text("hh:mm • horário do aparelho") }, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !estado.salvando, onClick = {
                    val atual = agendamento.getOrNull()?.atZone(ZoneId.systemDefault()) ?: ZonedDateTime.now()
                    DatePickerDialog(contexto, { _, ano, mes, dia -> data = LocalDate.of(ano, mes + 1, dia).format(formatoPrazo)
                        if (hora.isBlank()) hora = "09:00" }, atual.year, atual.monthValue - 1, atual.dayOfMonth).show()
                }) { Text("Escolher data") }
                TextButton(enabled = !estado.salvando, onClick = {
                    val atual = agendamento.getOrNull()?.atZone(ZoneId.systemDefault()) ?: ZonedDateTime.now()
                    TimePickerDialog(contexto, { _, h, m -> hora = LocalTime.of(h, m).format(formatoHora)
                        if (data.isBlank()) data = LocalDate.now().format(formatoPrazo) }, atual.hour, atual.minute, true).show()
                }) { Text("Escolher horário") }
                if (data.isNotBlank() || hora.isNotBlank()) TextButton(enabled = !estado.salvando, onClick = { data = ""; hora = "" }) { Text("Remover agendamento") }
            }
            Text("Sem data e horário, o lembrete fica apenas na lista. Alertas dependem da permissão de notificações e podem chegar após o horário escolhido.", style = MaterialTheme.typography.bodySmall)
            if (enviado) agendamento.exceptionOrNull()?.message?.let { MensagemDeErro(it) }
            estado.erro?.let { MensagemDeErro(it) }
            if (estado.salvando) LinearProgressIndicator(Modifier.fillMaxWidth())
        } }, confirmButton = { TextButton(enabled = !ocupado, onClick = {
            enviado = true
            when { erroTitulo -> focoTitulo.requestFocus(); agendamento.isFailure -> focoData.requestFocus()
                else -> aoSalvar(item?.id, DadosLembrete(titulo, inicial.concluido, agendamento.getOrNull())) }
        }) { Text(if (estado.salvando) "Salvando…" else "Salvar lembrete") } },
        dismissButton = { TextButton(enabled = !estado.salvando, onClick = fechar) { Text("Cancelar") } })
    if (descartar) AlertDialog(onDismissRequest = { descartar = false }, title = { Text("Descartar alterações?") },
        text = { Text("As alterações ainda não foram salvas.") }, confirmButton = { TextButton(onClick = aoFechar) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { descartar = false }) { Text("Continuar editando") } })
}
