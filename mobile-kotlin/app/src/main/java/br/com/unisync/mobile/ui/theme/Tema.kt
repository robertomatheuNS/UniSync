package br.com.unisync.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val CoresEscuras = darkColorScheme(
    primary = Roxo80,
    secondary = CinzaRoxo80,
    tertiary = Rosa80
)

private val CoresClaras = lightColorScheme(
    primary = Roxo40,
    secondary = CinzaRoxo40,
    tertiary = Rosa40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun TemaUniSync(
    temaEscuro: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    corDinamica: Boolean = true,
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
        content = conteudo
    )
}