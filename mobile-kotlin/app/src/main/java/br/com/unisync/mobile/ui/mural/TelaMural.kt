package br.com.unisync.mobile.ui.mural

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
import br.com.unisync.mobile.domain.academico.Turma
import br.com.unisync.mobile.domain.mural.*
import br.com.unisync.mobile.ui.componentes.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun TelaMural(estado: EstadoMural, turmas: List<Turma>, aoAtualizar: () -> Unit, aoFiltrar: (String?) -> Unit,
    aoCarregarMais: () -> Unit, aoAbrir: (String) -> Unit, aoFecharDetalhe: () -> Unit,
    aoSalvar: (String?, Int?, DadosPublicacao) -> Unit, aoExcluir: (Publicacao) -> Unit, aoVerTurmas: () -> Unit) {
    var alvoDetalhe by rememberSaveable { mutableStateOf<String?>(null) }
    var formulario by rememberSaveable { mutableStateOf(false) }
    var editando by rememberSaveable { mutableStateOf(false) }
    var excluir by remember { mutableStateOf<Publicacao?>(null) }
    var confirmacaoVista by rememberSaveable { mutableIntStateOf(estado.confirmacao) }
    LaunchedEffect(estado.confirmacao) { if (estado.confirmacao != confirmacaoVista) {
        confirmacaoVista = estado.confirmacao; formulario = false; editando = false; excluir = null
        alvoDetalhe = estado.abrindoId
    } }
    LaunchedEffect(Unit) { if (alvoDetalhe != null && estado.abrindoId == null) alvoDetalhe?.let(aoAbrir) }
    LaunchedEffect(estado.abrindoId) { if (estado.abrindoId != null) alvoDetalhe = estado.abrindoId }
    LaunchedEffect(estado.erro, estado.detalhe) {
        if (editando && estado.detalhe == null && estado.erro != null) { formulario = false; editando = false }
    }
    val abrir = { id: String -> alvoDetalhe = id; aoAbrir(id) }
    val fecharDetalhe = { alvoDetalhe = null; aoFecharDetalhe() }
    val filtrar = { turmaId: String? -> alvoDetalhe = null; aoFiltrar(turmaId) }
    val ocupado = estado.salvando || estado.atualizando || estado.carregandoMais
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Mural da comunidade", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text("Compartilhe ideias, dúvidas e novidades com suas turmas.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !ocupado, onClick = aoAtualizar) { Text("Atualizar mural") }
                Button(enabled = !ocupado && turmas.isNotEmpty(), onClick = { editando = false; formulario = true }) { Text("Nova publicação") }
            }
        }
        if (turmas.isEmpty()) item { Text("Para publicar, crie uma turma ou entre com o código do professor.")
            TextButton(onClick = aoVerTurmas) { Text("Ver turmas") }
        }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = estado.filtroTurma == null, enabled = !ocupado, onClick = { filtrar(null) }, label = { Text("Todas as turmas") })
            turmas.forEach { turma -> FilterChip(selected = estado.filtroTurma == turma.id, enabled = !ocupado,
                onClick = { filtrar(turma.id) }, label = { Text("${turma.disciplina} • ${turma.nome}") }) }
            if (estado.filtroTurma != null && turmas.none { it.id == estado.filtroTurma }) FilterChip(selected = true, enabled = !ocupado,
                onClick = { filtrar(null) }, label = { Text("Turma indisponível • limpar filtro") })
        } }
        estado.erro?.let { item { MensagemDeErro(it) } }
        estado.mensagem?.let { item { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) } }
        if (estado.lendo || estado.atualizando) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item { Text("${estado.itens.size} publicações carregadas", style = MaterialTheme.typography.bodySmall) }
        if (!estado.lendo && !estado.atualizando && estado.itens.isEmpty() && estado.erro == null) item {
            ConteudoVazio("Nenhuma publicação neste filtro", "Selecione outra turma ou publique a primeira mensagem.")
        }
        items(estado.itens, key = { it.id }) { item -> Card(onClick = { abrir(item.id) }, enabled = !estado.salvando, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.titulo, style = MaterialTheme.typography.titleMedium)
                MetadadosPublicacao(item)
                Text(item.resumo, style = MaterialTheme.typography.bodyMedium)
                Text("Ver publicação", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        } }
        if (estado.proximaPagina != null) item { OutlinedButton(enabled = !ocupado, onClick = aoCarregarMais, modifier = Modifier.fillMaxWidth()) {
            Text(if (estado.carregandoMais) "Carregando…" else "Carregar mais publicações")
        } }
    }
    val detalhe = estado.detalhe
    if (formulario && (!editando || detalhe != null)) key(if (editando) detalhe?.id else "nova") {
        FormularioPublicacao(if (editando) detalhe else null, turmas, estado.filtroTurma, ocupado,
            estado.erro, { formulario = false; editando = false }, aoSalvar)
    }
    if (estado.abrindoId != null && !formulario && excluir == null) AlertDialog(onDismissRequest = { if (!estado.salvando) fecharDetalhe() },
        title = { Text(detalhe?.titulo ?: "Publicação") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (estado.carregandoDetalhe) LinearProgressIndicator(Modifier.fillMaxWidth())
                detalhe?.let { item ->
                    MetadadosPublicacao(item)
                    Text(item.conteudo.orEmpty())
                    if (item.podeEditar) OutlinedButton(enabled = !ocupado, onClick = { editando = true; formulario = true }) { Text("Editar publicação") }
                    if (item.podeExcluir) {
                        if (!item.podeEditar) Text("Como professor responsável, você pode remover esta publicação da turma.", style = MaterialTheme.typography.bodySmall)
                        TextButton(enabled = !ocupado, onClick = { excluir = item }) { Text("Excluir publicação", color = MaterialTheme.colorScheme.error) }
                    }
                }
                (estado.erroDetalhe ?: estado.erro)?.let { MensagemDeErro(it) }
                if (!estado.carregandoDetalhe && detalhe == null) TextButton(onClick = { estado.abrindoId?.let(abrir) }) { Text("Tentar novamente") }
            }
        }, confirmButton = { TextButton(enabled = !estado.salvando, onClick = fecharDetalhe) { Text("Fechar publicação") } })
    excluir?.let { item -> AlertDialog(onDismissRequest = { if (!estado.salvando) excluir = null }, title = { Text("Excluir publicação?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.titulo); Text("A publicação deixará de aparecer no mural da turma."); estado.erro?.let { MensagemDeErro(it) }
            if (estado.salvando) LinearProgressIndicator(Modifier.fillMaxWidth())
        } }, confirmButton = { TextButton(enabled = !ocupado, onClick = { aoExcluir(item) }) { Text("Excluir") } },
        dismissButton = { TextButton(enabled = !estado.salvando, onClick = { excluir = null }) { Text("Cancelar") } }) }
}

@Composable
private fun MetadadosPublicacao(item: Publicacao) {
    val formato = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.forLanguageTag("pt-BR")).withZone(ZoneId.systemDefault()) }
    Text("${item.disciplina} • ${item.turmaNome}", style = MaterialTheme.typography.labelLarge)
    Text("${item.autor} • ${item.papelAutor}", style = MaterialTheme.typography.bodySmall)
    Text("Publicado em ${formato.format(item.criadoEm)}", style = MaterialTheme.typography.bodySmall)
    if (item.versao > 1) Text("Editado em ${formato.format(item.atualizadoEm)}", style = MaterialTheme.typography.bodySmall)
}
