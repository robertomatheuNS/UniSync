package br.com.unisync.mobile.ui.mural

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.domain.academico.Turma
import br.com.unisync.mobile.domain.mural.*
import br.com.unisync.mobile.ui.componentes.MensagemDeErro

@Composable
internal fun FormularioPublicacao(inicial: Publicacao?, turmas: List<Turma>, turmaInicial: String?, ocupado: Boolean,
    erro: String?, aoFechar: () -> Unit, aoSalvar: (String?, Int?, DadosPublicacao) -> Unit) {
    val original = remember { inicial }
    val selecaoInicial = remember { original?.turmaId ?: turmaInicial ?: turmas.singleOrNull()?.id.orEmpty() }
    var turmaId by rememberSaveable { mutableStateOf(selecaoInicial) }
    var titulo by rememberSaveable { mutableStateOf(original?.titulo.orEmpty()) }
    var conteudo by rememberSaveable { mutableStateOf(original?.conteudo.orEmpty()) }
    val versao by rememberSaveable { mutableStateOf(original?.versao) }
    var enviado by rememberSaveable { mutableStateOf(false) }
    var descartar by rememberSaveable { mutableStateOf(false) }
    val focoTitulo = remember { FocusRequester() }
    val focoConteudo = remember { FocusRequester() }
    val focoTurma = remember { FocusRequester() }
    val tituloInvalido = titulo.trim().length !in 3..100
    val conteudoInvalido = conteudo.trim().length !in 1..5000
    val turmaInvalida = turmaId.isBlank()
    val fechar = { if (turmaId != selecaoInicial || titulo != original?.titulo.orEmpty() || conteudo != original?.conteudo.orEmpty()) descartar = true else aoFechar() }
    AlertDialog(onDismissRequest = { if (!ocupado) fechar() }, title = { Text(if (original == null) "Nova publicação" else "Editar publicação") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("A publicação será visível aos participantes da turma.", style = MaterialTheme.typography.bodyMedium)
            if (original != null) Text("${original.disciplina} • ${original.turmaNome}")
            else {
                Text("Turma", style = MaterialTheme.typography.titleSmall)
                turmas.forEachIndexed { indice, turma -> FilterChip(selected = turmaId == turma.id, onClick = { turmaId = turma.id }, enabled = !ocupado,
                    label = { Text("${turma.disciplina} • ${turma.nome}") }, modifier = if (indice == 0) Modifier.focusRequester(focoTurma) else Modifier) }
                if (enviado && turmaInvalida) MensagemDeErro("Selecione uma turma.")
            }
            OutlinedTextField(titulo, { titulo = it }, label = { Text("Título da publicação") }, enabled = !ocupado,
                isError = enviado && tituloInvalido, supportingText = { Text(if (enviado && tituloInvalido) "Use entre 3 e 100 caracteres." else "${titulo.trim().length}/100 caracteres") },
                modifier = Modifier.fillMaxWidth().focusRequester(focoTitulo))
            OutlinedTextField(conteudo, { conteudo = it }, label = { Text("Conteúdo da publicação") }, enabled = !ocupado,
                minLines = 2, isError = enviado && conteudoInvalido,
                supportingText = { Text(if (enviado && conteudoInvalido) "Use entre 1 e 5000 caracteres." else "${conteudo.trim().length}/5000 caracteres") },
                modifier = Modifier.fillMaxWidth().focusRequester(focoConteudo))
            erro?.let { MensagemDeErro(it) }
            if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
        } }, confirmButton = { TextButton(enabled = !ocupado, onClick = {
            enviado = true
            when {
                turmaInvalida -> if (turmas.isNotEmpty()) focoTurma.requestFocus()
                tituloInvalido -> focoTitulo.requestFocus()
                conteudoInvalido -> focoConteudo.requestFocus()
                else -> aoSalvar(original?.id, versao, DadosPublicacao(turmaId, titulo, conteudo))
            }
        }) { Text(if (ocupado) "Salvando…" else "Salvar publicação") } },
        dismissButton = { TextButton(enabled = !ocupado, onClick = fechar) { Text("Cancelar") } })
    if (descartar) AlertDialog(onDismissRequest = { descartar = false }, title = { Text("Descartar alterações?") },
        text = { Text("As alterações ainda não foram salvas.") }, confirmButton = { TextButton(onClick = aoFechar) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { descartar = false }) { Text("Continuar editando") } })
}
