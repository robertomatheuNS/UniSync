package br.com.unisync.mobile

import android.app.Application
import android.util.Log
import br.com.unisync.mobile.data.RepositorioMural
import br.com.unisync.mobile.data.RepositorioLembretes
import br.com.unisync.mobile.data.SessaoAutenticada
import br.com.unisync.mobile.data.local.SessaoSegura
import br.com.unisync.mobile.notificacoes.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import androidx.room.Room
import br.com.unisync.mobile.data.local.BancoUniSync
import br.com.unisync.mobile.data.local.PreferenciasUniSync
import br.com.unisync.mobile.data.RepositorioTarefas
import br.com.unisync.mobile.data.RepositorioOportunidades
import br.com.unisync.mobile.data.RepositorioAcademico
import br.com.unisync.mobile.data.remote.ClienteAcademico

class UniSyncApplication : Application() {
    val banco by lazy { Room.databaseBuilder(this, BancoUniSync::class.java, "unisync.db")
        .addMigrations(BancoUniSync.MIGRACAO_1_2, BancoUniSync.MIGRACAO_2_3, BancoUniSync.MIGRACAO_3_4, BancoUniSync.MIGRACAO_4_5).build() }
    val lembretes by lazy { RepositorioLembretes(banco.lembretes(), ClienteAcademico()) }
    val tarefas by lazy { RepositorioTarefas(banco.tarefas(), ClienteAcademico()) }
    val oportunidades by lazy { RepositorioOportunidades(banco.oportunidades(), ClienteAcademico()) }
    val mural by lazy { RepositorioMural() }
    val academico by lazy { RepositorioAcademico() }
    val preferencias by lazy { PreferenciasUniSync(this) }
    val escopoAplicacao = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val alertas by lazy { GerenciadorAlertas(banco,
        { try { SessaoSegura(this).lerToken()?.let { SessaoAutenticada.doToken(it) } }
          catch (erro: CancellationException) { throw erro }
          catch (_: Exception) { null } },
        { preferencias.alertas(it).first() }, AlertasAndroid(this)) }
    override fun onCreate() {
        super.onCreate()
        escopoAplicacao.launch {
            combine(banco.tarefas().observarTodas(), banco.lembretes().observarTodos(), preferencias.mudancas) { _, _, _ -> Unit }
                .collect { atualizarAlertas() }
        }
    }
    suspend fun atualizarAlertas(ativar: Boolean = false) {
        try { if (ativar) alertas.ativar() else alertas.atualizar() }
        catch (erro: CancellationException) { throw erro }
        catch (_: Exception) { Log.w("UniSync", "Não foi possível atualizar os alertas locais.") }
    }
    suspend fun limparDadosPrivados() {
        alertas.desativar()
        tarefas.limparCache(); lembretes.limparCache()
    }
}
