package br.com.unisync.mobile.ui.lembretes

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.unisync.mobile.notificacoes.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun PreferenciasAlertas(opcoes: OpcoesAlertas, ocupado: Boolean, aoSalvar: (OpcoesAlertas) -> Unit, aoRevisar: () -> Unit) {
    val contexto = LocalContext.current
    var permitido by remember { mutableStateOf(AlertasAndroid(contexto).permitido()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permitido = AlertasAndroid(contexto).permitido(); aoRevisar()
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, evento -> if (evento == Lifecycle.Event.ON_RESUME) {
            permitido = AlertasAndroid(contexto).permitido(); aoRevisar()
        } }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Alertas de prazos e lembretes", style = MaterialTheme.typography.titleMedium)
        Text("Os alertas usam os dados sincronizados neste aparelho e param ao sair da conta ou expirar a sessão. O Android pode atrasar a entrega para economizar bateria.")
        FilterChip(selected = opcoes.lembretes, enabled = !ocupado, onClick = { aoSalvar(opcoes.copy(lembretes = !opcoes.lembretes)) },
            label = { Text(if (opcoes.lembretes) "Lembretes: ativados" else "Lembretes: desativados") })
        FilterChip(selected = opcoes.tarefas, enabled = !ocupado, onClick = { aoSalvar(opcoes.copy(tarefas = !opcoes.tarefas)) },
            label = { Text(if (opcoes.tarefas) "Prazos de tarefas: ativados" else "Prazos de tarefas: desativados") })
        TextButton(enabled = !ocupado, onClick = { TimePickerDialog(contexto,
            { _, h, m -> aoSalvar(opcoes.copy(horaPrazos = LocalTime.of(h, m))) }, opcoes.horaPrazos.hour, opcoes.horaPrazos.minute, true).show() }) {
            Text("Horário dos prazos: ${opcoes.horaPrazos.format(DateTimeFormatter.ofPattern("HH:mm"))}")
        }
        Text(if (permitido) "Notificações permitidas pelo Android." else "As notificações estão bloqueadas no Android. Os lembretes continuam disponíveis na lista e na agenda.")
        if (!permitido && Build.VERSION.SDK_INT >= 33) OutlinedButton(enabled = !ocupado, onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Permitir notificações") }
        TextButton(onClick = { contexto.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)) }) { Text("Abrir ajustes de notificações") }
    }
}
