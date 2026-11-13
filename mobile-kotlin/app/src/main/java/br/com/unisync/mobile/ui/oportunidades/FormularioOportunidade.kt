package br.com.unisync.mobile.ui.oportunidades

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.domain.oportunidades.*
import br.com.unisync.mobile.ui.componentes.MensagemDeErro
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun FormularioOportunidade(inicial: Oportunidade?, salvando: Boolean, erro: String?,
    aoSalvar: (DadosOportunidade) -> Unit, aoFechar: () -> Unit) {
    val chave = inicial?.id ?: "nova"
    val horarioInicial = rememberSaveable(chave) {
        (inicial?.horario() ?: LocalDateTime.now().plusDays(1).withSecond(0).withNano(0)).toString()
    }
    var titulo by rememberSaveable(chave) { mutableStateOf(inicial?.titulo.orEmpty()) }
    var materia by rememberSaveable(chave) { mutableStateOf(inicial?.materia.orEmpty()) }
    var tipo by rememberSaveable(chave) { mutableStateOf(inicial?.tipo.orEmpty()) }
    var descricao by rememberSaveable(chave) { mutableStateOf(inicial?.descricao.orEmpty()) }
    var horario by rememberSaveable(chave) { mutableStateOf(horarioInicial) }
    var enviado by rememberSaveable(chave) { mutableStateOf(false) }
    var descartar by remember { mutableStateOf(false) }
    val data = LocalDateTime.parse(horario)
    val contexto = LocalContext.current
    val alterado = titulo != inicial?.titulo.orEmpty() || materia != inicial?.materia.orEmpty() ||
        tipo != inicial?.tipo.orEmpty() || descricao != inicial?.descricao.orEmpty() || horario != horarioInicial
    val fechar = { if (alterado) descartar = true else aoFechar() }
    AlertDialog(onDismissRequest = { if (!salvando) fechar() },
        title = { Text(if (inicial == null) "Publicar oportunidade" else "Editar oportunidade") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(titulo, { titulo = it }, enabled = !salvando,
                    label = { Text("Título") }, isError = enviado && titulo.trim().length !in 5..100,
                    supportingText = { Text(if (enviado && titulo.trim().length !in 5..100) "Use entre 5 e 100 caracteres." else "${titulo.trim().length}/100 caracteres") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(materia, { materia = it }, enabled = !salvando, singleLine = true,
                    label = { Text("Matéria") }, isError = enviado && materia.isBlank(),
                    supportingText = { if (enviado && materia.isBlank()) Text("Informe a matéria.") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(tipo, { tipo = it }, enabled = !salvando, singleLine = true,
                    label = { Text("Categoria") }, placeholder = { Text("Ex.: Palestra ou Estágio") }, isError = enviado && tipo.isBlank(),
                    supportingText = { if (enviado && tipo.isBlank()) Text("Informe a categoria.") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(descricao, { descricao = it }, enabled = !salvando, minLines = 3,
                    label = { Text("Descrição (opcional)") }, modifier = Modifier.fillMaxWidth())
                Text("Data: ${data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                OutlinedButton(enabled = !salvando, onClick = {
                    DatePickerDialog(contexto, { _, ano, mes, dia ->
                        horario = data.withYear(ano).withDayOfMonth(1).withMonth(mes + 1).withDayOfMonth(dia).toString()
                    }, data.year, data.monthValue - 1, data.dayOfMonth).show()
                }) { Text("Selecionar data") }
                Text("Horário: ${data.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                OutlinedButton(enabled = !salvando, onClick = {
                    TimePickerDialog(contexto, { _, hora, minuto -> horario = data.withHour(hora).withMinute(minuto).toString() },
                        data.hour, data.minute, true).show()
                }) { Text("Selecionar horário") }
                Text("Fuso horário: ${ZoneId.systemDefault().id}", style = MaterialTheme.typography.bodySmall)
                erro?.let { MensagemDeErro(it) }
                if (salvando) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = !salvando, onClick = {
            enviado = true
            if (titulo.trim().length in 5..100 && materia.isNotBlank() && tipo.isNotBlank()) {
                aoSalvar(DadosOportunidade(titulo, materia, descricao, tipo, data))
            }
        }) { Text(if (inicial == null) "Publicar" else "Salvar alterações") } },
        dismissButton = { TextButton(enabled = !salvando, onClick = fechar) { Text("Cancelar") } })
    if (descartar) AlertDialog(onDismissRequest = { descartar = false }, title = { Text("Descartar alterações?") },
        text = { Text("As alterações ainda não foram salvas.") },
        confirmButton = { TextButton(onClick = { descartar = false; aoFechar() }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { descartar = false }) { Text("Continuar editando") } })
}
