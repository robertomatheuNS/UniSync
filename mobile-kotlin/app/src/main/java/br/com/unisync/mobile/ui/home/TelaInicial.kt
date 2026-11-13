package br.com.unisync.mobile.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.unisync.mobile.R
import br.com.unisync.mobile.ui.theme.TemaUniSync

@Composable
fun TelaInicial(
    aoEntrar: () -> Unit,
    aoCadastrar: () -> Unit,
    modificador: Modifier = Modifier
) {
    val cores = MaterialTheme.colorScheme
    val fundoInicial = Brush.verticalGradient(listOf(cores.surface, cores.secondaryContainer, cores.primaryContainer))
    BoxWithConstraints(
        modifier = modificador
            .fillMaxSize()
            .background(fundoInicial)
            .safeDrawingPadding()
    ) {
        val alturaDisponivel = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = alturaDisponivel)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                Text(
                    text = stringResource(R.string.home_title),
                    color = cores.onSurface,
                    fontSize = 56.sp,
                    lineHeight = 56.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-2).sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BotaoInicial(
                        texto = stringResource(R.string.home_login),
                        aoClicar = aoEntrar,
                        modificador = Modifier.weight(1f)
                    )
                    BotaoInicial(
                        texto = stringResource(R.string.home_register),
                        aoClicar = aoCadastrar,
                        modificador = Modifier.weight(1f)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(top = 64.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.group_study),
                        contentDescription = stringResource(R.string.home_group_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Image(
                        painter = painterResource(R.drawable.students_looking_down),
                        contentDescription = stringResource(R.string.home_students_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
                Text(
                    text = stringResource(R.string.home_brand),
                    color = cores.onPrimaryContainer,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }
    }
}

@Composable
private fun BotaoInicial(texto: String, aoClicar: () -> Unit, modificador: Modifier = Modifier) {
    Button(
        onClick = aoClicar,
        modifier = modificador.heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(text = texto, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 640, heightDp = 360)
@Composable
private fun PreviaTelaInicial() {
    TemaUniSync {
        TelaInicial(aoEntrar = {}, aoCadastrar = {})
    }
}
