package br.com.unisync.mobile.notificacoes

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.unisync.mobile.MainActivity
import br.com.unisync.mobile.R
import br.com.unisync.mobile.UniSyncApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class AberturaAlerta(val usuarioId: String, val destino: String, val itemId: String, val chave: String = java.util.UUID.randomUUID().toString())

class AlertasAndroid(private val contexto: Context) : SaidaAlertas {
    private val notificacoes = contexto.getSystemService(NotificationManager::class.java)
    private val alarmes = contexto.getSystemService(AlarmManager::class.java)
    init {
        notificacoes.createNotificationChannel(NotificationChannel(CANAL, "Prazos e lembretes", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Alertas dos lembretes agendados e prazos das tarefas sincronizadas"
        })
    }
    override fun permitido() = (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(contexto).areNotificationsEnabled() && notificacoes.getNotificationChannel(CANAL)?.importance != NotificationManager.IMPORTANCE_NONE
    private fun alarme() = PendingIntent.getBroadcast(contexto, 0, Intent(contexto, RecebedorAlertas::class.java).setAction(ACAO),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    override fun agendar(horario: Long?) {
        if (horario == null) alarmes.cancel(alarme())
        else alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, horario, alarme())
    }
    override fun publicar(alerta: Alerta, usuarioId: String): Boolean {
        if (!permitido()) return false
        val intent = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = Uri.Builder().scheme("unisync").authority("alerta").appendPath(usuarioId).appendPath(alerta.chave).build()
            putExtra("usuario_alerta", usuarioId); putExtra("destino_alerta", alerta.destino); putExtra("item_alerta", alerta.itemId)
        }
        val abrir = PendingIntent.getActivity(contexto, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val publico = NotificationCompat.Builder(contexto, CANAL).setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle("UniSync").setContentText("Você tem um prazo ou lembrete.").build()
        val notificacao = NotificationCompat.Builder(contexto, CANAL).setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(alerta.titulo).setContentText(alerta.texto).setContentIntent(abrir).setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(publico).setOnlyAlertOnce(true).build()
        return try { notificacoes.notify(alerta.chave, 0, notificacao); true } catch (_: SecurityException) { false }
    }
    override fun removerExceto(chaves: Set<String>) {
        notificacoes.activeNotifications.filter { it.tag !in chaves }.forEach { notificacoes.cancel(it.tag, it.id) }
    }
    override fun limpar() { agendar(null); notificacoes.cancelAll() }
    companion object { const val CANAL = "prazos_lembretes"; const val ACAO = "br.com.unisync.mobile.ALERTA" }
}

class RecebedorAlertas : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        if (intent.action !in setOf(AlertasAndroid.ACAO, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val pendente = goAsync()
        val app = contexto.applicationContext as UniSyncApplication
        app.escopoAplicacao.launch {
            try { withTimeout(8000) { app.alertas.atualizar(entregar = true) } }
            catch (erro: kotlinx.coroutines.CancellationException) { throw erro }
            catch (_: Exception) { android.util.Log.w("UniSync", "Não foi possível entregar os alertas locais.") }
            finally { pendente.finish() }
        }
    }
}
