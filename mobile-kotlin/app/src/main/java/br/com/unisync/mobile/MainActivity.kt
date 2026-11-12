package br.com.unisync.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import br.com.unisync.mobile.data.AuthRepository
import br.com.unisync.mobile.ui.auth.AuthScreen
import br.com.unisync.mobile.ui.home.HomeScreen
import br.com.unisync.mobile.ui.theme.UniSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniSyncTheme {
                var screen by rememberSaveable { mutableStateOf("home") }
                val repository = remember { AuthRepository() }
                key(screen) {
                    when (screen) {
                        "home" -> HomeScreen(
                            onLogin = { screen = "login" },
                            onRegister = { screen = "register" }
                        )
                        else -> AuthScreen(
                            registration = screen == "register",
                            repository = repository,
                            onBack = { screen = "home" },
                            onSwitchMode = { screen = if (screen == "register") "login" else "register" }
                        )
                    }
                }
            }
        }
    }
}
