package br.com.unisync.mobile.ui.oportunidades

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import br.com.unisync.mobile.domain.oportunidades.*
import br.com.unisync.mobile.ui.componentes.*
import br.com.unisync.mobile.domain.lembretes.Lembrete
import br.com.unisync.mobile.ui.lembretes.*
import java.time.ZoneId
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun TelaOportunidades(estado: EstadoOportunidades, aoAtualizar: () -> Unit,
    professorId: String? = null, aoSalvar: (String?, DadosOportunidade) -> Unit = { _, _ -> },
    aoExcluir: (String) -> Unit = {}, oportunidadeInicial: String? = null, aoConsumirOportunidade: () -> Unit = {}) {
    var busca by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf("") }
    var apenasFuturas by rememberSaveable { mutableStateOf(false) }
    var detalhe by rememberSaveable { mutableStateOf<String?>(null) }
    var formulario by rememberSaveable { mutableStateOf(false) }
    var edicaoId by rememberSaveable { mutableStateOf<String?>(null) }
    var excluirId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmacaoInicial by remember { mutableIntStateOf(estado.confirmacao) }
    LaunchedEffect(oportunidadeInicial) { oportunidadeInicial?.let { id ->
        detalhe = id; busca = ""; tipo = ""; apenasFuturas = false; formulario = false; edicaoId = null; excluirId = null
        aoConsumirOportunidade()
    } }
    LaunchedEffect(estado.confirmacao) {
        if (estado.confirmacao > confirmacaoInicial) {
            formulario = false; excluirId = null; detalhe = null; confirmacaoInicial = estado.confirmacao
        }
    }
    val gerenciando = !professorId.isNullOrBlank()
    val ocupado = estado.salvando || estado.atualizando
    val categorias = estado.itens.map { it.tipo }.distinct().sorted()
    val filtradas = estado.itens.filter {
        (tipo.isEmpty() || it.tipo == tipo) && (!apenasFuturas || !it.encerrada()) &&
            listOf(it.titulo, it.materia, it.professor, it.descricao.orEmpty()).any { texto -> texto.contains(busca, ignoreCase = true) }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CabecalhoOportunidades(estado, aoAtualizar) }
        if (gerenciando) item { Button(enabled = !ocupado, onClick = {
            edicaoId = null; formulario = true
        }) { Text("Publicar oportunidade") } }
        item { OutlinedTextField(busca, { busca = it }, label = { Text("Buscar oportunidades") }, modifier = Modifier.fillMaxWidth()) }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tipo.isEmpty(), onClick = { tipo = "" }, label = { Text("Todas as categorias") })
            categorias.forEach { categoria -> FilterChip(selected = tipo == categoria, onClick = { tipo = categoria }, label = { Text(categoria) }) }
            FilterChip(selected = apenasFuturas, onClick = { apenasFuturas = !apenasFuturas }, label = { Text("Somente próximos eventos") })
        } }
        if (!estado.lendo && filtradas.isEmpty()) item {
            ConteudoVazio("Nenhuma oportunidade encontrada", "Atualize a lista ou altere a busca e os filtros.")
        }
        items(filtradas, key = { it.id }) { oportunidade -> CartaoOportunidade(oportunidade) { detalhe = oportunidade.id } }
    }
    estado.itens.find { it.id == detalhe }?.let { oportunidade ->
        DetalheOportunidade(oportunidade, podeEditar = gerenciando && oportunidade.professorId == professorId,
            ocupado = ocupado,
            aoEditar = { edicaoId = oportunidade.id; detalhe = null; formulario = true },
            aoExcluir = { excluirId = oportunidade.id; detalhe = null }, aoFechar = { detalhe = null })
    }
    if (detalhe != null && estado.itens.none { it.id == detalhe }) {
        DetalheIndisponivel("Oportunidade", estado.lendo || estado.atualizando, estado.erro, aoAtualizar) { detalhe = null }
    }
    if (formulario) FormularioOportunidade(estado.itens.find { it.id == edicaoId }, estado.salvando, estado.erro,
        aoSalvar = { aoSalvar(edicaoId, it) }, aoFechar = { formulario = false })
    estado.itens.find { it.id == excluirId }?.let { oportunidade ->
        AlertDialog(onDismissRequest = { if (!estado.salvando) excluirId = null },
            title = { Text("Excluir oportunidade?") },
            text = { Column { Text("${oportunidade.titulo}\nA publicação será removida para todos os usuários.")
                estado.erro?.let { MensagemDeErro(it) } } },
            confirmButton = { TextButton(enabled = !ocupado, onClick = { aoExcluir(oportunidade.id) }) { Text("Excluir", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(enabled = !estado.salvando, onClick = { excluirId = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun CabecalhoOportunidades(estado: EstadoOportunidades, aoAtualizar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = aoAtualizar, enabled = !estado.atualizando && !estado.salvando) { Text("Atualizar oportunidades") }
        Text("Até 100 oportunidades. Busca e filtros aplicados aos itens carregados.", style = MaterialTheme.typography.bodySmall)
        if (estado.atualizando || estado.lendo) LinearProgressIndicator(Modifier.fillMaxWidth())
        estado.erro?.let { MensagemDeErro(it) }
    }
}

@Composable
private fun CartaoOportunidade(item: Oportunidade, aoAbrir: () -> Unit) {
    Card(onClick = aoAbrir, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.tipo, style = MaterialTheme.typography.labelLarge)
            Text(item.titulo, style = MaterialTheme.typography.titleMedium)
            Text(item.dataFormatada())
            Text("${item.materia} • ${item.professor}", style = MaterialTheme.typography.bodyMedium)
            if (item.prazoInscricao != null || item.encerrada()) {
                Text(item.situacaoInscricao(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DetalheOportunidade(item: Oportunidade, podeEditar: Boolean = false, ocupado: Boolean = false,
    aoEditar: () -> Unit = {}, aoExcluir: () -> Unit = {}, aoFechar: () -> Unit) {
    val contexto = LocalContext.current
    val link = remember(item.urlInscricao) { runCatching { validarUrlInscricao(item.urlInscricao) }.getOrNull() }
    var erroAcao by remember(item.id) { mutableStateOf<String?>(null) }
    val agora by produceState(Instant.now(), item.id, item.prazoInscricao) {
        while (true) {
            value = Instant.now()
            delay(1000)
        }
    }
    AlertDialog(onDismissRequest = aoFechar, title = { Text(item.titulo) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(item.tipo)
            Text("Evento: ${item.dataFormatada()}")
            Text(item.materia)
            Text("Professor: ${item.professor}")
            Text(item.descricao?.takeIf { it.isNotBlank() } ?: "Nenhuma descrição informada.")
            item.prazoFormatado()?.let { Text("Prazo de inscrição: $it") }
            Text(item.situacaoInscricao(agora))
            if (link != null && item.podeInscrever(agora)) {
                val uri = link.toUri()
                Text("Site da inscrição: ${uri.host}", style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    erroAcao = null
                    if (item.podeInscrever()) {
                        try {
                            contexto.startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
                        } catch (_: ActivityNotFoundException) {
                            erroAcao = "Nenhum aplicativo pode abrir o site da inscrição."
                        }
                    } else erroAcao = "O prazo de inscrição ou do evento já passou."
                }) { Text("Abrir site de inscrição") }
            } else if (link == null && item.prazoInscricao != null && !item.inscricoesEncerradas(agora)) {
                Text("Link de inscrição não informado.")
            }
            OutlinedButton(onClick = {
                erroAcao = null
                val enviar = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, item.textoParaCompartilhar())
                try { contexto.startActivity(Intent.createChooser(enviar, "Compartilhar oportunidade")) }
                catch (_: ActivityNotFoundException) { erroAcao = "O compartilhamento está indisponível neste aparelho." }
            }) { Text("Compartilhar oportunidade") }
            erroAcao?.let { MensagemDeErro(it) }
            if (podeEditar) {
                OutlinedButton(enabled = !ocupado, onClick = aoEditar) { Text("Editar oportunidade") }
                TextButton(enabled = !ocupado, onClick = aoExcluir) { Text("Excluir oportunidade", color = MaterialTheme.colorScheme.error) }
            }
        } }, confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar") } })
}

@Composable
fun TelaCalendario(estado: EstadoOportunidades, aoAtualizar: () -> Unit,
    tarefas: br.com.unisync.mobile.ui.tarefas.EstadoTarefas? = null,
    aoAbrirTarefa: (br.com.unisync.mobile.domain.tarefas.Tarefa) -> Unit = {},
    lembretes: EstadoLembretes? = null, aoAbrirLembrete: (Lembrete) -> Unit = {}) {
    var mes by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var modo by rememberSaveable { mutableStateOf("Mês") }
    var dia by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var detalhe by rememberSaveable { mutableStateOf<String?>(null) }
    val selecionado = LocalDate.parse(dia)
    val periodo = YearMonth.parse(mes)
    fun noPeriodo(data: LocalDate) = when (modo) {
        "Dia" -> data == selecionado
        "Semana" -> { val inicio = selecionado.minusDays(selecionado.dayOfWeek.value.toLong() - 1)
            data >= inicio && data < inicio.plusDays(7) }
        else -> YearMonth.from(data) == periodo
    }
    val eventos = estado.itens.filter { it.horario()?.toLocalDate()?.let(::noPeriodo) == true }.sortedBy { it.horario() }
    val zona = ZoneId.systemDefault()
    val inscricoes = estado.itens.mapNotNull { item ->
        item.prazoInscricao?.atZone(zona)?.takeIf { noPeriodo(it.toLocalDate()) }?.let { it to item }
    }.sortedBy { it.first.toInstant() }.groupBy { it.first.toLocalDate() }
    val prazos = tarefas?.itens.orEmpty().filter { it.prazo?.let(::noPeriodo) == true }
    fun diaLembrete(item: Lembrete) = item.agendadoEm?.atZone(ZoneId.systemDefault())?.toLocalDate()
    val agendados = lembretes?.itens.orEmpty().filter { diaLembrete(it)?.let(::noPeriodo) == true }.sortedBy { it.agendadoEm }
    val datas = (eventos.mapNotNull { it.horario()?.toLocalDate() } + prazos.mapNotNull { it.prazo } + agendados.mapNotNull(::diaLembrete) + inscricoes.keys).distinct().sorted()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CabecalhoOportunidades(estado, aoAtualizar) }
        item { Text(if (tarefas == null && lembretes == null) "Agenda dos eventos publicados" else "Sua agenda acadêmica", style = MaterialTheme.typography.titleLarge)
            Text(if (tarefas == null && lembretes == null) "Consulte eventos e prazos de inscrição." else "Prazos, lembretes e eventos publicados, em um só lugar.", style = MaterialTheme.typography.bodyMedium) }
        tarefas?.let { estadoTarefas ->
            item { br.com.unisync.mobile.ui.tarefas.StatusSincronizacao(estadoTarefas) }
            estadoTarefas.erro?.let { mensagem -> item { MensagemDeErro(mensagem) } }
        }
        lembretes?.let { estadoLembretes ->
            item { StatusLembretes(estadoLembretes) }
            estadoLembretes.erro?.let { mensagem -> item { MensagemDeErro(mensagem) } }
        }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Mês", "Semana", "Dia").forEach { opcao -> FilterChip(selected = modo == opcao,
                onClick = { modo = opcao }, label = { Text(opcao) }) }
        } }
        item {
            val voltar = { if (modo == "Mês") mes = periodo.minusMonths(1).toString()
                else dia = selecionado.minusDays(if (modo == "Semana") 7 else 1).toString() }
            val avancar = { if (modo == "Mês") mes = periodo.plusMonths(1).toString()
                else dia = selecionado.plusDays(if (modo == "Semana") 7 else 1).toString() }
            Column {
                Text(if (modo == "Mês") periodo.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-BR")))
                    else selecionado.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag("pt-BR"))), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = voltar) { Text("Anterior") }
                    TextButton(onClick = { mes = YearMonth.now().toString(); dia = LocalDate.now().toString() }) { Text("Hoje") }
                    TextButton(onClick = avancar) { Text("Próximo") }
                }
            }
        }
        if (datas.isEmpty() && !estado.lendo && tarefas?.lendo != true && lembretes?.lendo != true) item {
            ConteudoVazio("Nenhum compromisso neste período", "Tarefas com prazo, lembretes agendados e eventos aparecem aqui. Selecione outro período ou atualize.")
        }
        items(datas, key = { it.toString() }) { data ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), style = MaterialTheme.typography.titleMedium)
                prazos.filter { it.prazo == data }.sortedByDescending { it.prioridade.ordinal }.forEach { tarefa ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tarefa.titulo, style = MaterialTheme.typography.titleMedium)
                        br.com.unisync.mobile.ui.tarefas.ResumoTarefa(tarefa)
                        TextButton(onClick = { aoAbrirTarefa(tarefa) }) { Text("Abrir tarefa") }
                    } }
                }
                agendados.filter { diaLembrete(it) == data }.forEach { lembrete ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(lembrete.titulo, style = MaterialTheme.typography.titleMedium)
                        Text("Lembrete • ${if (lembrete.concluido) "Concluído" else "Pendente"}")
                        Text(lembrete.horarioLocal())
                        TextButton(onClick = { aoAbrirLembrete(lembrete) }) { Text("Abrir lembrete") }
                    } }
                }
                eventos.filter { it.horario()?.toLocalDate() == data }.forEach { item -> CartaoOportunidade(item) { detalhe = item.id } }
                inscricoes[data].orEmpty().forEach { (_, item) ->
                    OutlinedCard(onClick = { detalhe = item.id }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Prazo de inscrição", style = MaterialTheme.typography.labelLarge)
                            Text(item.titulo, style = MaterialTheme.typography.titleMedium)
                            item.prazoFormatado()?.let { Text(it) }
                            Text(item.situacaoInscricao(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    estado.itens.find { it.id == detalhe }?.let { DetalheOportunidade(it) { detalhe = null } }
}
