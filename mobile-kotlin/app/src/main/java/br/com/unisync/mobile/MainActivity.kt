package br.com.unisync.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import br.com.unisync.mobile.ui.home.HomeScreen
import br.com.unisync.mobile.ui.theme.UniSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniSyncTheme {
                var selectedAction by rememberSaveable { mutableStateOf<Int?>(null) }
                HomeScreen(
                    onLogin = { selectedAction = R.string.home_login },
                    onRegister = { selectedAction = R.string.home_register }
                )
                selectedAction?.let { title ->
                    AlertDialog(
                        onDismissRequest = { selectedAction = null },
                        title = { Text(stringResource(title)) },
                        text = { Text(stringResource(R.string.home_auth_pending)) },
                        confirmButton = {
                            TextButton(onClick = { selectedAction = null }) {
                                Text(stringResource(R.string.home_close))
                            }
                        }
                    )
                }
            }
        }
    }
}
