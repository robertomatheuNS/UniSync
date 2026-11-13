package br.com.unisync.mobile.ui.academico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.domain.academico.*
import br.com.unisync.mobile.ui.componentes.MensagemDeErro

@Composable
private fun DialogoAcademico(titulo: String, acao: String, ocupado: Boolean, alterado: Boolean,
    erro: String?, aoFechar: () -> Unit, aoConfirmar: () -> Unit, conteudo: @Composable ColumnScope.() -> Unit) {
    var descartar by remember { mutableStateOf(false) }
    val fechar = { if (alterado) descartar = true else aoFechar() }
    AlertDialog(onDismissRequest = { if (!ocupado) fechar() }, title = { Text(titulo) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            conteudo()
            erro?.let { MensagemDeErro(it) }
            if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
        } },
        confirmButton = { TextButton(enabled = !ocupado, onClick = aoConfirmar) { Text(acao) } },
        dismissButton = { TextButton(enabled = !ocupado, onClick = fechar) { Text("Cancelar") } })
    if (descartar) AlertDialog(onDismissRequest = { descartar = false }, title = { Text("Descartar alterações?") },
        text = { Text("As alterações ainda não foram salvas.") },
        confirmButton = { TextButton(onClick = { descartar = false; aoFechar() }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { descartar = false }) { Text("Continuar editando") } })
}

@Composable
fun FormularioTurma(inicial: Turma?, ocupado: Boolean, erro: String?, aoSalvar: (DadosTurma) -> Unit, aoFechar: () -> Unit) {
    val chave = inicial?.id ?: "nova"
    var nome by rememberSaveable(chave) { mutableStateOf(inicial?.nome.orEmpty()) }
    var disciplina by rememberSaveable(chave) { mutableStateOf(inicial?.disciplina.orEmpty()) }
    var periodo by rememberSaveable(chave) { mutableStateOf(inicial?.periodo.orEmpty()) }
    var enviado by rememberSaveable(chave) { mutableStateOf(false) }
    val focoNome = remember { FocusRequester() }
    val focoDisciplina = remember { FocusRequester() }
    val focoPeriodo = remember { FocusRequester() }
    val nomeInvalido = nome.trim().length !in 3..80
    val disciplinaInvalida = disciplina.trim().length !in 2..100
    val periodoInvalido = periodo.trim().length > 30
    val alterado = nome != inicial?.nome.orEmpty() || disciplina != inicial?.disciplina.orEmpty() || periodo != inicial?.periodo.orEmpty()
    DialogoAcademico(if (inicial == null) "Nova turma" else "Editar turma", "Salvar turma", ocupado, alterado,
        erro, aoFechar, aoConfirmar = {
            enviado = true
            when {
                nomeInvalido -> focoNome.requestFocus()
                disciplinaInvalida -> focoDisciplina.requestFocus()
                periodoInvalido -> focoPeriodo.requestFocus()
                else -> aoSalvar(DadosTurma(nome, disciplina, periodo))
            }
        }) {
        OutlinedTextField(nome, { nome = it }, label = { Text("Nome da turma") }, enabled = !ocupado,
            isError = enviado && nomeInvalido, supportingText = { Text(if (enviado && nomeInvalido) "Use entre 3 e 80 caracteres." else "Ex.: Turma A") },
            modifier = Modifier.fillMaxWidth().focusRequester(focoNome))
        OutlinedTextField(disciplina, { disciplina = it }, label = { Text("Disciplina") }, enabled = !ocupado,
            isError = enviado && disciplinaInvalida, supportingText = { Text(if (enviado && disciplinaInvalida) "Use entre 2 e 100 caracteres." else "Ex.: Engenharia de Software") },
            modifier = Modifier.fillMaxWidth().focusRequester(focoDisciplina))
        OutlinedTextField(periodo, { periodo = it }, label = { Text("Período (opcional)") }, enabled = !ocupado,
            isError = enviado && periodoInvalido, supportingText = { Text(if (enviado && periodoInvalido) "Use até 30 caracteres." else "Ex.: 2026.2") },
            modifier = Modifier.fillMaxWidth().focusRequester(focoPeriodo))
    }
}

@Composable
fun FormularioEntradaTurma(ocupado: Boolean, erro: String?, aoEntrar: (String) -> Unit, aoFechar: () -> Unit) {
    var codigo by rememberSaveable { mutableStateOf("") }
    var enviado by rememberSaveable { mutableStateOf(false) }
    val foco = remember { FocusRequester() }
    val invalido = runCatching { validarCodigoTurma(codigo) }.isFailure
    DialogoAcademico("Entrar em uma turma", "Entrar na turma", ocupado, codigo.isNotBlank(), erro, aoFechar,
        aoConfirmar = { enviado = true; if (invalido) foco.requestFocus() else aoEntrar(codigo) }) {
        Text("Peça ao professor o código da turma. Você terá acesso aos avisos dessa disciplina.")
        OutlinedTextField(codigo, { codigo = it.uppercase() }, label = { Text("Código da turma") }, enabled = !ocupado,
            singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            isError = enviado && invalido, supportingText = { Text("Informe os 12 caracteres fornecidos pelo professor.") },
            modifier = Modifier.fillMaxWidth().focusRequester(foco))
    }
}

@Composable
fun FormularioAviso(inicial: Aviso?, turmas: List<Turma>, turmaInicial: String?, ocupado: Boolean,
    erro: String?, aoSalvar: (DadosAviso) -> Unit, aoFechar: () -> Unit) {
    val chave = inicial?.id ?: "novo"
    val selecaoInicial = inicial?.turmaId ?: turmaInicial ?: turmas.singleOrNull()?.id.orEmpty()
    var turmaId by rememberSaveable(chave) { mutableStateOf(selecaoInicial) }
    var titulo by rememberSaveable(chave) { mutableStateOf(inicial?.titulo.orEmpty()) }
    var conteudo by rememberSaveable(chave) { mutableStateOf(inicial?.conteudo.orEmpty()) }
    var enviado by rememberSaveable(chave) { mutableStateOf(false) }
    val focoTitulo = remember { FocusRequester() }
    val focoConteudo = remember { FocusRequester() }
    val tituloInvalido = titulo.trim().length !in 3..100
    val conteudoInvalido = conteudo.trim().length !in 1..5000
    val turmaInvalida = turmas.none { it.id == turmaId }
    val alterado = titulo != inicial?.titulo.orEmpty() || conteudo != inicial?.conteudo.orEmpty() || turmaId != selecaoInicial
    DialogoAcademico(if (inicial == null) "Novo aviso" else "Editar aviso", "Publicar aviso", ocupado, alterado,
        erro, aoFechar, aoConfirmar = {
            enviado = true
            when {
                turmaInvalida -> Unit
                tituloInvalido -> focoTitulo.requestFocus()
                conteudoInvalido -> focoConteudo.requestFocus()
                else -> aoSalvar(DadosAviso(turmaId, titulo, conteudo))
            }
        }) {
        Text("Turma e disciplina", style = MaterialTheme.typography.titleSmall)
        if (inicial != null) Text("${inicial.turmaNome} • ${inicial.disciplina}")
        else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            turmas.forEach { turma -> FilterChip(selected = turmaId == turma.id, enabled = !ocupado,
                onClick = { turmaId = turma.id }, label = { Text("${turma.nome} • ${turma.disciplina}") }) }
        }
        if (enviado && turmaInvalida) MensagemDeErro("Selecione uma turma para publicar.")
        OutlinedTextField(titulo, { titulo = it }, label = { Text("Título do aviso") }, enabled = !ocupado,
            isError = enviado && tituloInvalido, supportingText = { Text(if (enviado && tituloInvalido) "Use entre 3 e 100 caracteres." else "${titulo.trim().length}/100 caracteres") },
            modifier = Modifier.fillMaxWidth().focusRequester(focoTitulo))
        OutlinedTextField(conteudo, { conteudo = it }, label = { Text("Conteúdo do aviso") }, enabled = !ocupado,
            minLines = 4, isError = enviado && conteudoInvalido,
            supportingText = { Text(if (enviado && conteudoInvalido) "Use entre 1 e 5000 caracteres." else "${conteudo.trim().length}/5000 caracteres") },
            modifier = Modifier.fillMaxWidth().focusRequester(focoConteudo))
        if (inicial != null) Text("Uma alteração fará este aviso aparecer novamente como não lido para os alunos.")
    }
}
