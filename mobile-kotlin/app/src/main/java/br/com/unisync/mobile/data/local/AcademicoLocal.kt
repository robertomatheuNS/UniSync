package br.com.unisync.mobile.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.PrimaryKey

@Entity(tableName = "turmas_cache", primaryKeys = ["usuarioId", "id"])
data class TurmaCache(val usuarioId: String, val id: String, val json: String)

@Entity(tableName = "avisos_cache", primaryKeys = ["usuarioId", "id"])
data class AvisoCache(val usuarioId: String, val id: String, val json: String)

@Entity(tableName = "sincronizacoes_academico")
data class SincronizacaoAcademico(@PrimaryKey val usuarioId: String, val atualizadoEm: Long)

@Dao
interface AcademicoDao {
    @Query("SELECT atualizadoEm FROM sincronizacoes_academico WHERE usuarioId = :usuarioId")
    suspend fun atualizadoEm(usuarioId: String): Long?
    @Query("SELECT * FROM turmas_cache WHERE usuarioId = :usuarioId")
    suspend fun listarTurmas(usuarioId: String): List<TurmaCache>
    @Query("SELECT * FROM avisos_cache WHERE usuarioId = :usuarioId")
    suspend fun listarAvisos(usuarioId: String): List<AvisoCache>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarTurmas(itens: List<TurmaCache>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarAvisos(itens: List<AvisoCache>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun marcar(item: SincronizacaoAcademico)
    @Query("DELETE FROM turmas_cache WHERE usuarioId = :usuarioId")
    suspend fun limparTurmas(usuarioId: String)
    @Query("DELETE FROM avisos_cache WHERE usuarioId = :usuarioId")
    suspend fun limparAvisos(usuarioId: String)
    @Query("DELETE FROM turmas_cache")
    suspend fun limparTodasTurmas()
    @Query("DELETE FROM avisos_cache")
    suspend fun limparTodosAvisos()
    @Query("DELETE FROM sincronizacoes_academico")
    suspend fun limparDatas()

    @Transaction
    suspend fun substituir(usuarioId: String, turmas: List<TurmaCache>, avisos: List<AvisoCache>) {
        limparTurmas(usuarioId)
        limparAvisos(usuarioId)
        salvarTurmas(turmas)
        salvarAvisos(avisos)
        marcar(SincronizacaoAcademico(usuarioId, System.currentTimeMillis()))
    }

    @Transaction
    suspend fun limparTudo() {
        limparTodasTurmas()
        limparTodosAvisos()
        limparDatas()
    }
}
