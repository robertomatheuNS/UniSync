package br.com.unisync.mobile.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Mantém a abertura por identificador visível enquanto a lista é restaurada/atualizada. */
@Composable
fun DetalheIndisponivel(tipo: String, carregando: Boolean, erro: String?, aoAtualizar: () -> Unit, aoFechar: () -> Unit) {
    AlertDialog(onDismissRequest = aoFechar, title = { Text("$tipo ${if (carregando) "em consulta" else "indisponível"}") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (carregando) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Consultando os dados disponíveis…") }
            else if (erro != null) MensagemDeErro(erro)
            else Text("Este item não está entre os dados disponíveis. Pode ter sido removido ou deixado de estar acessível. Atualize para conferir.")
            if (!carregando) OutlinedButton(onClick = aoAtualizar) { Text("Tentar novamente") }
        } }, confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar") } })
}
