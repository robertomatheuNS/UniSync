package br.com.unisync.mobile.ui.mural

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.domain.mural.*
import br.com.unisync.mobile.ui.componentes.MensagemDeErro
import java.util.Locale

@Composable
internal fun SecaoAnexos(estado: EstadoAnexos, ocupado: Boolean, nomeSelecionado: String?,
    aoSelecionar: () -> Unit, aoCancelar: () -> Unit, aoEnviar: () -> Unit, aoAtualizar: () -> Unit,
    aoAbrir: (Anexo) -> Unit, aoExcluir: (Anexo) -> Unit) {
    var excluir by remember { mutableStateOf<Anexo?>(null) }
    var confirmacaoVista by remember { mutableIntStateOf(estado.confirmacaoExclusao) }
    LaunchedEffect(estado.confirmacaoExclusao) {
        if (confirmacaoVista != estado.confirmacaoExclusao) { confirmacaoVista = estado.confirmacaoExclusao; excluir = null }
    }
    Column(Modifier.fillMaxWidth().testTag("anexos"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text("Anexos (${estado.itens.size}/$LIMITE_ANEXOS)", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        if (estado.podeAdicionar) {
            Text("Até três arquivos: PDF, PNG ou JPEG, com até 4 MiB cada.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = !ocupado && estado.itens.size < LIMITE_ANEXOS, onClick = aoSelecionar) { Text("Selecionar arquivo") }
            if (nomeSelecionado != null) {
                Text("Selecionado: $nomeSelecionado")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !ocupado, onClick = aoEnviar) { Text("Enviar anexo") }
                    TextButton(enabled = !ocupado, onClick = aoCancelar) { Text("Cancelar anexo") }
                }
            }
        }
        if (estado.enviando) {
            Text(if (estado.progresso == 100) "Confirmando envio…" else "Enviando arquivo… ${estado.progresso}%", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            LinearProgressIndicator(progress = { estado.progresso / 100f }, modifier = Modifier.fillMaxWidth())
        } else if (estado.atualizando) LinearProgressIndicator(Modifier.fillMaxWidth())
        estado.itens.forEach { anexo ->
            OutlinedCard(Modifier.fillMaxWidth().testTag("anexo:${anexo.id}")) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(anexo.nome, style = MaterialTheme.typography.titleSmall)
                    Text("${when (anexo.tipo) { "application/pdf" -> "PDF"; "image/png" -> "PNG"; else -> "JPEG" }} • ${formatarTamanhoAnexo(anexo.tamanho)}", style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = !ocupado, onClick = { aoAbrir(anexo) }) { Text(if (estado.abrindoId == anexo.id) "Abrindo…" else "Abrir anexo") }
                        if (anexo.podeExcluir) TextButton(enabled = !ocupado, onClick = { excluir = anexo }) { Text("Remover anexo", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        if (!estado.atualizando && estado.itens.isEmpty() && estado.erro == null) Text("Nenhum anexo nesta publicação.")
        estado.erro?.let { MensagemDeErro(it) }
        estado.mensagem?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        TextButton(enabled = !ocupado, onClick = aoAtualizar) { Text("Atualizar anexos") }
    }
    excluir?.let { anexo ->
        AlertDialog(onDismissRequest = { if (!estado.operando) excluir = null }, title = { Text("Remover anexo?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(anexo.nome); Text("O arquivo deixará de estar disponível nesta publicação."); estado.erro?.let { MensagemDeErro(it) }
            } }, confirmButton = { TextButton(enabled = !ocupado, onClick = { aoExcluir(anexo) }) { Text("Remover") } },
            dismissButton = { TextButton(enabled = !estado.operando, onClick = { excluir = null }) { Text("Cancelar") } })
    }
}

internal fun formatarTamanhoAnexo(bytes: Int) = if (bytes >= 1024 * 1024)
    String.format(Locale.forLanguageTag("pt-BR"), "%.1f MiB", bytes / (1024.0 * 1024)) else "${(bytes + 1023) / 1024} KiB"
