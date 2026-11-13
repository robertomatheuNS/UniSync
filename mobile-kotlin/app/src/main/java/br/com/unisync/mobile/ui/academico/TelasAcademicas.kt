package br.com.unisync.mobile.ui.academico

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.domain.academico.*
import br.com.unisync.mobile.ui.componentes.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
private fun CabecalhoAcademico(estado: EstadoAcademico, aoAtualizar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(enabled = !estado.atualizando && !estado.salvando, onClick = aoAtualizar) { Text("Atualizar turmas e avisos") }
        if (estado.lendo || estado.atualizando || estado.salvando) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (estado.atualizadoEm == null) Text("Atualize para consultar suas turmas e avisos.", style = MaterialTheme.typography.bodySmall)
        else Text("Última consulta: ${formatarData(Instant.ofEpochMilli(estado.atualizadoEm).toString())}", style = MaterialTheme.typography.bodySmall)
        estado.erro?.let { MensagemDeErro(it) }
        estado.mensagem?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    }
}

@Composable
fun TelaTurmas(estado: EstadoAcademico, professorId: String?, aoAtualizar: () -> Unit,
    aoSalvar: (String?, DadosTurma) -> Unit, aoEntrar: (String) -> Unit, aoArquivar: (String) -> Unit,
    aoVerAvisos: (String) -> Unit, aoLimparMensagem: () -> Unit) {
    var formulario by rememberSaveable { mutableStateOf(false) }
    var edicaoId by rememberSaveable { mutableStateOf<String?>(null) }
    var entrada by rememberSaveable { mutableStateOf(false) }
    var arquivarId by rememberSaveable { mutableStateOf<String?>(null) }
    var copiado by remember { mutableStateOf<String?>(null) }
    var confirmacaoInicial by remember { mutableIntStateOf(estado.confirmacao) }
    LaunchedEffect(estado.confirmacao) { if (estado.confirmacao > confirmacaoInicial) {
        formulario = false; entrada = false; arquivarId = null; confirmacaoInicial = estado.confirmacao
    } }
    val ocupado = estado.atualizando || estado.salvando
    val professor = !professorId.isNullOrBlank()
    val contexto = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CabecalhoAcademico(estado, aoAtualizar) }
        item {
            Text("Turmas e disciplinas", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(if (professor) "Crie suas turmas e compartilhe o código de entrada com os alunos." else "Entre com o código do professor para acompanhar os avisos de cada disciplina.")
        }
        item { Button(enabled = !ocupado, onClick = {
            aoLimparMensagem()
            if (professor) { edicaoId = null; formulario = true } else entrada = true
        }) { Text(if (professor) "Criar turma" else "Entrar em uma turma") } }
        copiado?.let { item { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) } }
        if (!estado.lendo && estado.turmas.isEmpty()) item {
            ConteudoVazio("Nenhuma turma por enquanto", if (professor) "Crie a primeira turma para começar a publicar avisos." else "Solicite o código de uma turma ao seu professor.")
        }
        items(estado.turmas.sortedBy { it.nome }, key = { it.id }) { turma ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(turma.nome, style = MaterialTheme.typography.titleMedium)
                    Text(turma.disciplina)
                    if (turma.periodo.isNotBlank()) Text("Período: ${turma.periodo}")
                    Text("Professor: ${turma.professor}")
                    Text("Alunos: ${turma.totalAlunos}")
                    if (professorId == turma.professorId) turma.codigo?.let { codigo ->
                        SelectionContainer { Text("Código de entrada: $codigo", style = MaterialTheme.typography.labelLarge) }
                        OutlinedButton(onClick = {
                            (contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                .setPrimaryClip(ClipData.newPlainText("Código da turma", codigo))
                            copiado = "Código de ${turma.nome} copiado."
                        }) { Text("Copiar código") }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { aoVerAvisos(turma.id) }) { Text("Ver avisos") }
                        if (professorId == turma.professorId) {
                            TextButton(enabled = !ocupado, onClick = { aoLimparMensagem(); edicaoId = turma.id; formulario = true }) { Text("Editar turma") }
                            TextButton(enabled = !ocupado, onClick = { aoLimparMensagem(); arquivarId = turma.id }) { Text("Arquivar turma") }
                        }
                    }
                }
            }
        }
    }
    val turmaEditada = estado.turmas.find { it.id == edicaoId }
    if (formulario && (edicaoId == null || turmaEditada != null)) FormularioTurma(turmaEditada, ocupado, estado.erro,
        aoSalvar = { aoSalvar(edicaoId, it) }, aoFechar = { formulario = false })
    if (entrada) FormularioEntradaTurma(ocupado, estado.erro, aoEntrar, aoFechar = { entrada = false })
    estado.turmas.find { it.id == arquivarId }?.let { turma ->
        ConfirmacaoAcademica("Arquivar turma?", "${turma.nome} e seus avisos deixarão de aparecer para os alunos. Novas entradas serão bloqueadas.",
            "Arquivar", ocupado, estado.erro, { aoArquivar(turma.id) }, { arquivarId = null })
    }
}

@Composable
fun TelaAvisos(estado: EstadoAcademico, professorId: String?, turmaInicial: String?, aoAtualizar: () -> Unit,
    aoSalvar: (String?, DadosAviso) -> Unit, aoExcluir: (String) -> Unit, aoLer: (String) -> Unit,
    aoAbrirTurmas: () -> Unit, aoLimparMensagem: () -> Unit) {
    var turmaId by rememberSaveable(turmaInicial) { mutableStateOf(turmaInicial.orEmpty()) }
    var apenasNaoLidos by rememberSaveable { mutableStateOf(false) }
    var detalheId by rememberSaveable { mutableStateOf<String?>(null) }
    var formulario by rememberSaveable { mutableStateOf(false) }
    var edicaoId by rememberSaveable { mutableStateOf<String?>(null) }
    var excluirId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmacaoInicial by remember { mutableIntStateOf(estado.confirmacao) }
    LaunchedEffect(estado.confirmacao) { if (estado.confirmacao > confirmacaoInicial) {
        formulario = false; excluirId = null; detalheId = null; confirmacaoInicial = estado.confirmacao
    } }
    val professor = !professorId.isNullOrBlank()
    val ocupado = estado.atualizando || estado.salvando
    val turmasProprias = estado.turmas.filter { it.professorId == professorId }
    val filtrados = estado.avisos.filter { (turmaId.isBlank() || it.turmaId == turmaId) && (!apenasNaoLidos || !it.lido) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CabecalhoAcademico(estado, aoAtualizar) }
        if (professor && turmasProprias.isNotEmpty()) item {
            Button(enabled = !ocupado, onClick = { aoLimparMensagem(); edicaoId = null; formulario = true }) { Text("Publicar aviso") }
        }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = turmaId.isBlank(), onClick = { turmaId = "" }, label = { Text("Todas as turmas") })
            estado.turmas.forEach { turma -> FilterChip(selected = turmaId == turma.id, onClick = { turmaId = turma.id },
                label = { Text("${turma.nome} • ${turma.disciplina}") }) }
            if (!professor) FilterChip(selected = apenasNaoLidos, onClick = { apenasNaoLidos = !apenasNaoLidos }, label = { Text("Somente não lidos") })
        } }
        if (!estado.lendo && filtrados.isEmpty()) item {
            ConteudoVazio("Nenhum aviso encontrado", if (estado.turmas.isEmpty()) "Acesse Turmas para começar." else "Atualize a lista ou altere os filtros.")
            if (estado.turmas.isEmpty()) TextButton(onClick = aoAbrirTurmas) { Text("Abrir turmas") }
        }
        items(filtrados, key = { it.id }) { aviso ->
            Card(onClick = { aoLimparMensagem(); detalheId = aviso.id }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!professor) Text(if (aviso.lido) "Lido" else "Não lido", style = MaterialTheme.typography.labelLarge)
                    Text(aviso.titulo, style = MaterialTheme.typography.titleMedium)
                    Text("${aviso.turmaNome} • ${aviso.disciplina}")
                    Text("${aviso.professor} • ${formatarData(aviso.atualizadoEm)}", style = MaterialTheme.typography.bodySmall)
                    Text(aviso.conteudo, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
    }
    estado.avisos.find { it.id == detalheId }?.let { aviso ->
        AlertDialog(onDismissRequest = { if (!estado.salvando) detalheId = null }, title = { Text(aviso.titulo) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${aviso.turmaNome} • ${aviso.disciplina}")
                Text("Professor: ${aviso.professor}")
                Text("Atualizado em ${formatarData(aviso.atualizadoEm)}", style = MaterialTheme.typography.bodySmall)
                SelectionContainer { Text(aviso.conteudo) }
                if (!professor) {
                    Text(if (aviso.lido) "Leitura confirmada" else "Você ainda não marcou este aviso como lido.")
                    if (!aviso.lido) Button(enabled = !ocupado, onClick = { aoLer(aviso.id) }) { Text("Marcar como lido") }
                }
                if (aviso.professorId == professorId) {
                    OutlinedButton(enabled = !ocupado, onClick = { edicaoId = aviso.id; detalheId = null; formulario = true }) { Text("Editar aviso") }
                    TextButton(enabled = !ocupado, onClick = { excluirId = aviso.id; detalheId = null }) { Text("Excluir aviso", color = MaterialTheme.colorScheme.error) }
                }
                estado.erro?.let { MensagemDeErro(it) }
                if (estado.salvando) LinearProgressIndicator(Modifier.fillMaxWidth())
            } }, confirmButton = { TextButton(enabled = !estado.salvando, onClick = { detalheId = null }) { Text("Fechar") } })
    }
    val avisoEditado = estado.avisos.find { it.id == edicaoId }
    if (formulario && (edicaoId == null || avisoEditado != null)) FormularioAviso(avisoEditado, turmasProprias,
        turmaId.takeIf { it.isNotBlank() }, ocupado, estado.erro, aoSalvar = { aoSalvar(edicaoId, it) }, aoFechar = { formulario = false })
    estado.avisos.find { it.id == excluirId }?.let { aviso ->
        ConfirmacaoAcademica("Excluir aviso?", "${aviso.titulo}\nO aviso será removido para todos os alunos da turma.",
            "Excluir", ocupado, estado.erro, { aoExcluir(aviso.id) }, { excluirId = null })
    }
}

@Composable
private fun ConfirmacaoAcademica(titulo: String, texto: String, acao: String, ocupado: Boolean,
    erro: String?, aoConfirmar: () -> Unit, aoCancelar: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!ocupado) aoCancelar() }, title = { Text(titulo) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(texto); erro?.let { MensagemDeErro(it) }; if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
        } }, confirmButton = { TextButton(enabled = !ocupado, onClick = aoConfirmar) { Text(acao, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(enabled = !ocupado, onClick = aoCancelar) { Text("Cancelar") } })
}

private fun formatarData(valor: String): String = runCatching {
    Instant.parse(valor).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.forLanguageTag("pt-BR")))
}.getOrElse { valor }
