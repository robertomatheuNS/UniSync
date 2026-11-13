package br.com.unisync.mobile

import android.app.Application
import androidx.room.Room
import br.com.unisync.mobile.data.local.BancoUniSync
import br.com.unisync.mobile.data.local.PreferenciasUniSync
import br.com.unisync.mobile.data.RepositorioTarefas
import br.com.unisync.mobile.data.RepositorioOportunidades
import br.com.unisync.mobile.data.remote.ClienteAcademico

class UniSyncApplication : Application() {
    val banco by lazy { Room.databaseBuilder(this, BancoUniSync::class.java, "unisync.db").addMigrations(BancoUniSync.MIGRACAO_1_2).build() }
    val tarefas by lazy { RepositorioTarefas(banco.tarefas(), ClienteAcademico()) }
    val oportunidades by lazy { RepositorioOportunidades(banco.oportunidades(), ClienteAcademico()) }
    val preferencias by lazy { PreferenciasUniSync(this) }
}
