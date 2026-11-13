package br.com.unisync.mobile.ui.oportunidades

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.domain.oportunidades.Oportunidade
import br.com.unisync.mobile.ui.componentes.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun TelaOportunidades(estado: EstadoOportunidades, aoAtualizar: () -> Unit) {
    var busca by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf("") }
    var apenasFuturas by rememberSaveable { mutableStateOf(false) }
    var detalhe by rememberSaveable { mutableStateOf<String?>(null) }
    val categorias = estado.itens.map { it.tipo }.distinct().sorted()
    val filtradas = estado.itens.filter {
        (tipo.isEmpty() || it.tipo == tipo) && (!apenasFuturas || !it.encerrada()) &&
            listOf(it.titulo, it.materia, it.professor, it.descricao.orEmpty()).any { texto -> texto.contains(busca, ignoreCase = true) }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CabecalhoOportunidades(estado, aoAtualizar) }
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
    estado.itens.find { it.id == detalhe }?.let { oportunidade -> DetalheOportunidade(oportunidade) { detalhe = null } }
}

@Composable
private fun CabecalhoOportunidades(estado: EstadoOportunidades, aoAtualizar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = aoAtualizar, enabled = !estado.atualizando) { Text("Atualizar oportunidades") }
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
            if (item.encerrada()) Text("Evento já realizado", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetalheOportunidade(item: Oportunidade, aoFechar: () -> Unit) {
    AlertDialog(onDismissRequest = aoFechar, title = { Text(item.titulo) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(item.tipo); Text(item.dataFormatada()); Text(item.materia); Text("Professor: ${item.professor}")
            Text(item.descricao?.takeIf { it.isNotBlank() } ?: "Nenhuma descrição informada.")
            if (item.encerrada()) Text("Evento já realizado")
        } }, confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar") } })
}

@Composable
fun TelaCalendario(estado: EstadoOportunidades, aoAtualizar: () -> Unit) {
    var mes by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var modo by rememberSaveable { mutableStateOf("Mês") }
    var dia by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var detalhe by rememberSaveable { mutableStateOf<String?>(null) }
    val selecionado = LocalDate.parse(dia)
    val periodo = YearMonth.parse(mes)
    val eventos = estado.itens.mapNotNull { item -> item.horario()?.let { it.toLocalDate() to item } }
        .filter { (data, _) -> when (modo) {
            "Dia" -> data == selecionado
            "Semana" -> { val inicio = selecionado.minusDays(selecionado.dayOfWeek.value.toLong() - 1); data >= inicio && data < inicio.plusDays(7) }
            else -> YearMonth.from(data) == periodo
        } }.sortedBy { it.first }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CabecalhoOportunidades(estado, aoAtualizar) }
        item { Text("Agenda dos eventos publicados", style = MaterialTheme.typography.titleLarge)
            Text("Consulte as datas dos eventos publicados.", style = MaterialTheme.typography.bodyMedium) }
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
        if (eventos.isEmpty() && !estado.lendo) item { ConteudoVazio("Nenhum evento neste período", "Selecione outro período ou atualize as oportunidades.") }
        items(eventos, key = { it.second.id }) { (_, item) -> CartaoOportunidade(item) { detalhe = item.id } }
    }
    estado.itens.find { it.id == detalhe }?.let { DetalheOportunidade(it) { detalhe = null } }
}
