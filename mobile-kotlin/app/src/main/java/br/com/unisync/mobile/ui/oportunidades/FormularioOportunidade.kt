package br.com.unisync.mobile.ui.oportunidades

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
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
    var url by rememberSaveable(chave) { mutableStateOf(inicial?.urlInscricao.orEmpty()) }
    var prazo by rememberSaveable(chave) { mutableStateOf(inicial?.prazoInscricao?.toString()) }
    var enviado by rememberSaveable(chave) { mutableStateOf(false) }
    var descartar by remember { mutableStateOf(false) }
    var erroHorarioPrazo by rememberSaveable(chave) { mutableStateOf<String?>(null) }
    val data = LocalDateTime.parse(horario)
    val zona = ZoneId.systemDefault()
    val instantePrazo = prazo?.let(Instant::parse)
    val erroUrl = if (enviado) runCatching { validarUrlInscricao(url) }.exceptionOrNull()?.message else null
    val erroPrazo = if (enviado && instantePrazo?.isAfter(data.atZone(zona).toInstant()) == true)
        "O prazo de inscrição não pode ser posterior ao evento." else erroHorarioPrazo
    val erroHorario = enviado && data.atZone(zona).toLocalDateTime() != data
    val contexto = LocalContext.current
    val alterado = titulo != inicial?.titulo.orEmpty() || materia != inicial?.materia.orEmpty() ||
        tipo != inicial?.tipo.orEmpty() || descricao != inicial?.descricao.orEmpty() || horario != horarioInicial ||
        url != inicial?.urlInscricao.orEmpty() || instantePrazo != inicial?.prazoInscricao
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
                if (erroHorario) MensagemDeErro("Escolha um horário válido no fuso do aparelho.")
                OutlinedTextField(url, { url = it }, enabled = !salvando, singleLine = true,
                    label = { Text("Link de inscrição (opcional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = erroUrl != null, supportingText = { Text(erroUrl ?: "Use o endereço HTTPS da inscrição.") }, modifier = Modifier.fillMaxWidth())
                Text(if (instantePrazo == null) "Prazo de inscrição não informado" else
                    "Prazo de inscrição: ${instantePrazo.atZone(zona).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !salvando, onClick = {
                        val inicio = instantePrazo?.atZone(zona)?.toLocalDateTime() ?: data
                        DatePickerDialog(contexto, { _, ano, mes, dia ->
                            val diaPrazo = LocalDate.of(ano, mes + 1, dia)
                            TimePickerDialog(contexto, { _, hora, minuto ->
                                val local = diaPrazo.atTime(hora, minuto)
                                val comFuso = local.atZone(zona)
                                if (local == comFuso.toLocalDateTime()) {
                                    prazo = comFuso.toInstant().toString()
                                    erroHorarioPrazo = null
                                } else erroHorarioPrazo = "Escolha um horário de inscrição válido no fuso do aparelho."
                            }, inicio.hour, inicio.minute, true).show()
                        }, inicio.year, inicio.monthValue - 1, inicio.dayOfMonth).show()
                    }) { Text(if (prazo == null) "Definir prazo de inscrição" else "Alterar prazo de inscrição") }
                    if (prazo != null || erroHorarioPrazo != null) TextButton(enabled = !salvando, onClick = {
                        prazo = null; erroHorarioPrazo = null
                    }) { Text("Remover prazo") }
                }
                erroPrazo?.let { MensagemDeErro(it) }
                erro?.let { MensagemDeErro(it) }
                if (salvando) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = !salvando, onClick = {
            enviado = true
            val dados = DadosOportunidade(titulo, materia, descricao, tipo, data, url, instantePrazo)
            val validos = runCatching { dados.validar(zona) }.getOrNull()
            if (validos != null && erroHorarioPrazo == null) aoSalvar(validos)
        }) { Text(if (inicial == null) "Publicar" else "Salvar alterações") } },
        dismissButton = { TextButton(enabled = !salvando, onClick = fechar) { Text("Cancelar") } })
    if (descartar) AlertDialog(onDismissRequest = { descartar = false }, title = { Text("Descartar alterações?") },
        text = { Text("As alterações ainda não foram salvas.") },
        confirmButton = { TextButton(onClick = { descartar = false; aoFechar() }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { descartar = false }) { Text("Continuar editando") } })
}
