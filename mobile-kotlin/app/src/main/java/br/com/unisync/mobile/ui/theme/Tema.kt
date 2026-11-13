package br.com.unisync.mobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val CoresEscuras = darkColorScheme(
    primary = AzulNoturno,
    onPrimary = Color(0xFF002A9D),
    primaryContainer = Color(0xFF193B9C),
    onPrimaryContainer = Color(0xFFDEE4FF),
    secondary = CianoUniSync,
    onSecondary = TintaUniSync,
    secondaryContainer = Color(0xFF203D50),
    onSecondaryContainer = AzulSuave,
    background = FundoEscuro,
    onBackground = TextoEscuro,
    surface = SuperficieEscura,
    surfaceTint = AzulNoturno,
    surfaceContainerLowest = FundoEscuro,
    surfaceContainerLow = SuperficieEscura,
    surfaceContainer = Color(0xFF202839),
    surfaceContainerHigh = Color(0xFF293243),
    surfaceContainerHighest = Color(0xFF333E52),
    onSurface = TextoEscuro,
    surfaceVariant = Color(0xFF293243),
    onSurfaceVariant = TextoAuxiliarEscuro,
    outline = Color(0xFF8A96AA),
    outlineVariant = Color(0xFF475569),
    error = ErroEscuro,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val CoresClaras = lightColorScheme(
    primary = AzulUniSync,
    onPrimary = Color.White,
    primaryContainer = CianoUniSync,
    onPrimaryContainer = TintaUniSync,
    secondary = Color(0xFF1769AA),
    onSecondary = Color.White,
    secondaryContainer = AzulSuave,
    onSecondaryContainer = TintaUniSync,
    background = FundoClaro,
    onBackground = TextoClaro,
    surface = Color.White,
    surfaceTint = AzulUniSync,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = FundoClaro,
    surfaceContainer = Color(0xFFF0F5FA),
    surfaceContainerHigh = Color(0xFFE9F1F8),
    surfaceContainerHighest = Color(0xFFE2ECF5),
    onSurface = TextoClaro,
    surfaceVariant = Color(0xFFEEF2F6),
    onSurfaceVariant = TextoAuxiliarClaro,
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFCBD5E1),
    error = ErroClaro,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun TemaUniSync(
    temaEscuro: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    corDinamica: Boolean = false,
    conteudo: @Composable () -> Unit
) {
    val esquemaCores = when {
        corDinamica && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val contexto = LocalContext.current
            if (temaEscuro) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto)
        }

        temaEscuro -> CoresEscuras
        else -> CoresClaras
    }

    MaterialTheme(
        colorScheme = esquemaCores,
        typography = Tipografia,
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = conteudo
    )
}
