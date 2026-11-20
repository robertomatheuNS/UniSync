package br.com.unisync.mobile.ui.mural

import androidx.compose.foundation.layout.*
import androidx.core.net.toUri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.unisync.mobile.domain.mural.*
import br.com.unisync.mobile.ui.componentes.MensagemDeErro
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
internal fun DetalhePublicacao(estado: EstadoMural, conversa: EstadoConversa?, aoFechar: () -> Unit,
    aoTentar: () -> Unit, aoEditar: () -> Unit, aoExcluir: () -> Unit,
    aoAtualizarComentarios: () -> Unit, aoCarregarMaisComentarios: () -> Unit,
    aoSalvarComentario: (String?, Int?, String) -> Unit, aoExcluirComentario: (Comentario) -> Unit, aoCurtir: (Boolean) -> Unit,
    anexos: EstadoAnexos? = null, aoAtualizarAnexos: () -> Unit = {}, aoEnviarAnexo: (String, String) -> Unit = { _, _ -> },
    aoAbrirAnexo: (Anexo) -> Unit = {}, aoExcluirAnexo: (Anexo) -> Unit = {}) {
    val id = estado.abrindoId.orEmpty()
    var texto by rememberSaveable(id) { mutableStateOf("") }
    var textoInicial by rememberSaveable(id) { mutableStateOf("") }
    var editandoId by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var versao by rememberSaveable(id) { mutableStateOf<Int?>(null) }
    var enviado by rememberSaveable(id) { mutableStateOf(false) }
    var acaoPendente by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var excluirComentario by remember { mutableStateOf<Comentario?>(null) }
    var salvoVisto by rememberSaveable(id) { mutableIntStateOf(conversa?.confirmacaoSalvo ?: 0) }
    var exclusaoVista by rememberSaveable(id) { mutableIntStateOf(conversa?.confirmacaoExcluido ?: 0) }
    var uriArquivo by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var nomeArquivo by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var envioId by rememberSaveable(id) { mutableStateOf(ModeloAnexos.novoEnvioId()) }
    var envioVisto by rememberSaveable(id) { mutableIntStateOf(anexos?.confirmacaoEnvio ?: 0) }
    var erroSelecao by remember { mutableStateOf<String?>(null) }
    val contexto = LocalContext.current
    val seletor = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { uriArquivo = uri.toString(); nomeArquivo = "Arquivo selecionado"; envioId = ModeloAnexos.novoEnvioId(); erroSelecao = null }
    }
    LaunchedEffect(uriArquivo) {
        uriArquivo?.let { endereco ->
            try {
                nomeArquivo = withContext(Dispatchers.IO) {
                    contexto.contentResolver.query(endereco.toUri(), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: "Arquivo selecionado"
                }
            } catch (erro: kotlinx.coroutines.CancellationException) { throw erro }
            catch (_: Exception) { erroSelecao = "Não foi possível ler o arquivo. Selecione-o novamente." }
        }
    }
    LaunchedEffect(anexos?.confirmacaoEnvio) {
        if (anexos != null && envioVisto != anexos.confirmacaoEnvio) {
            envioVisto = anexos.confirmacaoEnvio; uriArquivo = null; nomeArquivo = null; erroSelecao = null
        }
    }
    val lista = rememberLazyListState()
    val foco = remember { FocusRequester() }
    val focoManager = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    val escopo = rememberCoroutineScope()
    val escrevendo = estado.salvando || conversa?.salvando == true || conversa?.curtindo == true || anexos?.operando == true
    val alturaJanela = LocalWindowInfo.current.containerSize.height
    val compacto = with(LocalDensity.current) { alturaJanela.toDp() < 480.dp }
    val ocupado = estado.atualizando || escrevendo || conversa?.ocupado == true || anexos?.ocupado == true
    val limpar = { texto = ""; textoInicial = ""; editandoId = null; versao = null; enviado = false }
    LaunchedEffect(conversa?.confirmacaoSalvo) {
        if (conversa != null && salvoVisto != conversa.confirmacaoSalvo) {
            salvoVisto = conversa.confirmacaoSalvo; limpar(); focoManager.clearFocus(); teclado?.hide()
        }
    }
    LaunchedEffect(conversa?.confirmacaoExcluido) {
        if (conversa != null && exclusaoVista != conversa.confirmacaoExcluido) {
            exclusaoVista = conversa.confirmacaoExcluido; excluirComentario = null
            if (editandoId == conversa.ultimoExcluidoId) limpar()
        }
    }
    val executar = { acao: String ->
        when {
            acao == "fechar" -> aoFechar()
            acao == "editarPublicacao" -> aoEditar()
            acao == "excluirPublicacao" -> aoExcluir()
            acao == "cancelarComentario" -> limpar()
            acao.startsWith("editarComentario:") -> conversa?.comentarios?.find { it.id == acao.substringAfter(":") }?.let { comentario ->
                editandoId = comentario.id; versao = comentario.versao; texto = comentario.conteudo; textoInicial = comentario.conteudo; enviado = false
                escopo.launch { lista.animateScrollToItem(if (anexos != null) 2 else 1); foco.requestFocus() }
            }
        }
    }
    val solicitar = { acao: String ->
        if (texto != textoInicial || (uriArquivo != null && acao in listOf("fechar", "editarPublicacao", "excluirPublicacao"))) acaoPendente = acao else executar(acao)
    }
    Dialog(onDismissRequest = { if (!escrevendo) solicitar("fechar") }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(12.dp), contentAlignment = Alignment.Center) {
            Surface(Modifier.widthIn(max = 640.dp).fillMaxWidth().fillMaxHeight(), shape = MaterialTheme.shapes.extraLarge) {
                Column {
                    LazyColumn(state = lista, modifier = Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item(key = "publicacao") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(estado.detalhe?.titulo ?: "Publicação", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                                if (estado.carregandoDetalhe) LinearProgressIndicator(Modifier.fillMaxWidth())
                                estado.detalhe?.let { item ->
                                    MetadadosPublicacao(item)
                                    Text(item.conteudo.orEmpty())
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (item.podeEditar) OutlinedButton(enabled = !ocupado, onClick = { solicitar("editarPublicacao") }) { Text("Editar publicação") }
                                        if (item.podeExcluir) TextButton(enabled = !ocupado, onClick = { solicitar("excluirPublicacao") }) { Text("Excluir publicação", color = MaterialTheme.colorScheme.error) }
                                    }
                                    if (item.podeExcluir && !item.podeEditar) Text("Como professor responsável, você pode remover esta publicação da turma.", style = MaterialTheme.typography.bodySmall)
                                }
                                (estado.erroDetalhe ?: estado.erro)?.let { MensagemDeErro(it) }
                                if (!estado.carregandoDetalhe && estado.detalhe == null) TextButton(onClick = aoTentar) { Text("Tentar novamente") }
                            }
                        }
                        if (anexos != null && estado.detalhe != null) item(key = "anexos") {
                            SecaoAnexos(anexos, ocupado, nomeArquivo, {
                                try { seletor.launch(TIPOS_ANEXO.toTypedArray()) }
                                catch (_: android.content.ActivityNotFoundException) { erroSelecao = "O seletor de arquivos está indisponível neste aparelho." }
                            }, { uriArquivo = null; nomeArquivo = null; erroSelecao = null },
                                { uriArquivo?.let { aoEnviarAnexo(it, envioId) } }, aoAtualizarAnexos, aoAbrirAnexo, aoExcluirAnexo)
                            erroSelecao?.let { MensagemDeErro(it) }
                        }
                        if (conversa != null && estado.detalhe != null) {
                            item(key = "compor") {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HorizontalDivider()
                                    FilterChip(selected = conversa.curtiu, enabled = !ocupado, onClick = { aoCurtir(!conversa.curtiu) },
                                        label = { Text(if (conversa.curtiu) "Curtido (${conversa.totalCurtidas})" else "Curtir (${conversa.totalCurtidas})") })
                                    if (conversa.curtindo) LinearProgressIndicator(Modifier.fillMaxWidth())
                                    Text("Comentários", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                                    TextButton(enabled = !ocupado, onClick = aoAtualizarComentarios) { Text("Atualizar conversa") }
                                    OutlinedTextField(texto, { texto = it }, enabled = !escrevendo, label = { Text(if (editandoId == null) "Seu comentário" else "Editar comentário") },
                                        minLines = if (compacto) 1 else 2, maxLines = if (compacto) 3 else 5, isError = enviado && texto.trim().length !in 1..2000,
                                        supportingText = { Text(if (enviado && texto.trim().length !in 1..2000) "Use entre 1 e 2000 caracteres." else "${texto.trim().length}/2000 caracteres") },
                                        modifier = Modifier.fillMaxWidth().focusRequester(foco))
                                    if (editandoId != null) TextButton(enabled = !escrevendo, onClick = { solicitar("cancelarComentario") }) { Text("Cancelar edição") }
                                    conversa.erro?.let { MensagemDeErro(it) }
                                    conversa.mensagem?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                                    if (conversa.lendo || conversa.atualizando) LinearProgressIndicator(Modifier.fillMaxWidth())
                                    Text("${conversa.comentarios.size} comentários carregados", style = MaterialTheme.typography.bodySmall)
                                    if (conversa.consultaConfirmada && conversa.comentarios.isEmpty() && !conversa.atualizando && conversa.erro == null)
                                        Text("Seja o primeiro a comentar.")
                                }
                            }
                            items(conversa.comentarios, key = { "comentario:${it.id}" }) { comentario ->
                                OutlinedCard(Modifier.fillMaxWidth().testTag("comentario:${comentario.id}")) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${comentario.autor} • ${comentario.papelAutor}", style = MaterialTheme.typography.titleSmall)
                                    Text(formatarDataComentario(comentario), style = MaterialTheme.typography.bodySmall)
                                    Text(comentario.conteudo)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (comentario.podeEditar) TextButton(enabled = !ocupado, onClick = { solicitar("editarComentario:${comentario.id}") }) { Text("Editar comentário") }
                                        if (comentario.podeExcluir) TextButton(enabled = !ocupado, onClick = { excluirComentario = comentario }) { Text("Excluir comentário", color = MaterialTheme.colorScheme.error) }
                                    }
                                } }
                            }
                            if (conversa.proximaPagina != null) item(key = "mais") {
                                OutlinedButton(enabled = !ocupado, modifier = Modifier.fillMaxWidth(), onClick = aoCarregarMaisComentarios) {
                                    Text(if (conversa.carregandoMais) "Carregando…" else "Carregar mais comentários")
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    if (conversa != null && estado.detalhe != null) {
                        FlowRow(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(enabled = !ocupado, onClick = {
                                enviado = true
                                if (texto.trim().length in 1..2000) aoSalvarComentario(editandoId, versao, texto)
                                else escopo.launch { lista.animateScrollToItem(if (anexos != null) 2 else 1); foco.requestFocus() }
                            }) { Text(if (conversa.salvando) "Salvando…" else if (editandoId == null) "Enviar comentário" else "Salvar comentário") }
                            TextButton(enabled = !escrevendo, onClick = { solicitar("fechar") }) { Text("Fechar publicação") }
                        }
                    } else TextButton(enabled = !escrevendo, modifier = Modifier.align(Alignment.End).padding(12.dp), onClick = { solicitar("fechar") }) { Text("Fechar publicação") }
                }
            }
        }
    }
    if (acaoPendente != null) AlertDialog(onDismissRequest = { acaoPendente = null }, title = { Text(if (uriArquivo != null) "Descartar alterações não enviadas?" else "Descartar comentário não salvo?") },
        text = { Text(if (uriArquivo != null) "Há um arquivo selecionado ou um comentário que ainda não foi enviado." else "Seu texto ainda não foi enviado.") }, confirmButton = { TextButton(onClick = {
            val acao = acaoPendente!!; acaoPendente = null; limpar()
            if (acao in listOf("fechar", "editarPublicacao", "excluirPublicacao")) { uriArquivo = null; nomeArquivo = null }
            executar(acao)
        }) { Text("Descartar") } }, dismissButton = { TextButton(onClick = { acaoPendente = null }) { Text("Continuar escrevendo") } })
    excluirComentario?.let { comentario -> AlertDialog(onDismissRequest = { if (!escrevendo) excluirComentario = null }, title = { Text("Excluir comentário?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(comentario.conteudo); conversa?.erro?.let { MensagemDeErro(it) } } },
        confirmButton = { TextButton(enabled = !ocupado, onClick = { aoExcluirComentario(comentario) }) { Text("Excluir") } },
        dismissButton = { TextButton(enabled = !escrevendo, onClick = { excluirComentario = null }) { Text("Cancelar") } }) }
}

private fun formatarDataComentario(item: Comentario): String {
    val formato = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.forLanguageTag("pt-BR")).withZone(ZoneId.systemDefault())
    return if (item.versao > 1) "Editado em ${formato.format(item.atualizadoEm)}" else "Publicado em ${formato.format(item.criadoEm)}"
}
