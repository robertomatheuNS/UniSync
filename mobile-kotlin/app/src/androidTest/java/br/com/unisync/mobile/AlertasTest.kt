package br.com.unisync.mobile

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.data.SessaoAutenticada
import br.com.unisync.mobile.data.local.*
import br.com.unisync.mobile.notificacoes.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.Base64

class AlertasTest {
    private class Saida : SaidaAlertas {
        var permitido = true; var proximo: Long? = null
        val publicados = mutableListOf<Alerta>()
        var ativos = setOf<String>()
        override fun permitido() = permitido
        override fun agendar(horario: Long?) { proximo = horario }
        override fun publicar(alerta: Alerta, usuarioId: String): Boolean { publicados += alerta; return true }
        override fun removerExceto(chaves: Set<String>) { ativos = chaves }
        override fun limpar() { proximo = null; ativos = emptySet() }
    }
    private fun sessao(usuario: String, expira: Long): SessaoAutenticada {
        val json = """{"exp":$expira,"http://schemas.microsoft.com/ws/2008/06/identity/claims/role":"Aluno","nameid":"$usuario"}"""
        return SessaoAutenticada.doToken("h.${Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())}.s", 0)
    }
    @Test fun entregaUmaVezCancelaConcluidosEIsolaTrocaDeContaESessaoExpirada() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, BancoUniSync::class.java).build()
        val saida = Saida()
        var agora = 1000L
        var atual: SessaoAutenticada? = sessao("eu", 100)
        val gerenciador = GerenciadorAlertas(db, { atual }, { OpcoesAlertas(lembretes = true) }, saida, { agora })
        val item = LembreteLocal("a", "eu", "Entrega", false, Instant.ofEpochMilli(5000).toString())
        try {
            db.lembretes().salvar(listOf(item, item.copy(id = "b", usuarioId = "outra")))
            gerenciador.atualizar(); assertEquals(5000L, saida.proximo)
            agora = 6000
            gerenciador.atualizar(entregar = true); gerenciador.atualizar(entregar = true)
            assertEquals(1, saida.publicados.size)
            db.lembretes().salvar(listOf(item.copy(concluido = true)))
            gerenciador.atualizar(); assertNull(saida.proximo); assertTrue(saida.ativos.isEmpty())
            atual = sessao("outra", 100)
            gerenciador.atualizar(entregar = true); assertEquals(1, saida.publicados.size)
            atual = null
            gerenciador.atualizar(); assertTrue(db.alertas().listar("outra").isEmpty()); assertNull(saida.proximo)
            atual = sessao("eu", 1); gerenciador.atualizar(); assertNull(saida.proximo)
        } finally { db.close() }
    }
    @Test fun permissaoNegadaNaoPublicaELogoutCancelaAlarmes() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, BancoUniSync::class.java).build()
        val saida = Saida().apply { permitido = false }
        val gerenciador = GerenciadorAlertas(db, { sessao("eu", 100) }, { OpcoesAlertas(lembretes = true) }, saida, { 1000 })
        try {
            db.lembretes().salvar(listOf(LembreteLocal("a", "eu", "Entrega", false, Instant.ofEpochMilli(5000).toString())))
            gerenciador.atualizar(); assertNull(saida.proximo); assertTrue(saida.publicados.isEmpty())
            saida.permitido = true; gerenciador.atualizar(); assertEquals(5000L, saida.proximo)
            gerenciador.desativar(); assertNull(saida.proximo); assertTrue(db.alertas().listar("eu").isEmpty())
            gerenciador.atualizar(); assertNull(saida.proximo)
        } finally { db.close() }
    }
    @Test fun notificacaoNativaPublicaComPrivacidadeERemoveAoConcluir() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val contexto = instrumentation.targetContext
        val tinhaPermissao = Build.VERSION.SDK_INT < 33 || contexto.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= 33 && !tinhaPermissao) instrumentation.uiAutomation.grantRuntimePermission(contexto.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val saida = AlertasAndroid(contexto)
        val manager = contexto.getSystemService(NotificationManager::class.java)
        try {
            assertTrue(saida.permitido())
            assertTrue(saida.publicar(Alerta("lembrete:teste", "Entrega técnica", "Lembrete agendado", System.currentTimeMillis(), "lembretes", "teste"), "tecnico"))
            val limite = android.os.SystemClock.elapsedRealtime() + 3000
            while (manager.activeNotifications.none { it.tag == "lembrete:teste" } && android.os.SystemClock.elapsedRealtime() < limite) Thread.sleep(50)
            val registro = manager.activeNotifications.single { it.tag == "lembrete:teste" }
            assertEquals(android.app.Notification.VISIBILITY_PRIVATE, registro.notification.visibility)
            assertNotNull(registro.notification.contentIntent)
            assertNotNull(registro.notification.publicVersion)
            saida.removerExceto(emptySet())
            val limiteRemocao = android.os.SystemClock.elapsedRealtime() + 3000
            while (manager.activeNotifications.any { it.tag == "lembrete:teste" } && android.os.SystemClock.elapsedRealtime() < limiteRemocao) Thread.sleep(50)
            assertTrue(manager.activeNotifications.none { it.tag == "lembrete:teste" })
        } finally {
            saida.limpar()
        }
    }
}
