package br.com.unisync.mobile.ui.painel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.R
import br.com.unisync.mobile.data.PerfilUsuario

@Composable
fun TelaPainel(nome: String, perfil: PerfilUsuario, aoSair: () -> Unit) {
    val professor = perfil == PerfilUsuario.PROFESSOR
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding()) {
            Surface(color = Color(0xFF8FD3EC), contentColor = Color(0xFF142637)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = aoSair) {
                        Text(stringResource(R.string.painel_sair), color = Color(0xFF142637))
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE6F7FF),
                        contentColor = Color(0xFF142637)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                if (professor) R.string.painel_professor else R.string.painel_aluno
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() }
                        )
                        if (nome.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.painel_saudacao, nome),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                if (professor) R.string.painel_avisos_turma else R.string.painel_proximos_avisos
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = stringResource(R.string.painel_avisos_pendentes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
