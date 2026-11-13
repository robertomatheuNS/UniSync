package br.com.unisync.mobile.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.ui.theme.TemaUniSync

@Composable
fun CarregandoConteudo(mensagem: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(mensagem, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}

@Composable
fun MensagemDeErro(mensagem: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Text(mensagem, Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite })
    }
}

@Composable
fun ConteudoVazio(titulo: String, mensagem: String, acao: String? = null, aoAgir: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(mensagem, style = MaterialTheme.typography.bodyLarge)
        if (acao != null) Button(onClick = aoAgir, modifier = Modifier.heightIn(min = 48.dp)) { Text(acao) }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviaErro() = TemaUniSync { MensagemDeErro("Não foi possível atualizar. Tente novamente.") }
