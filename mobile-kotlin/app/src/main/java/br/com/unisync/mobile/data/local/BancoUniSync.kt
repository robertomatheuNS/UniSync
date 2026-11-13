package br.com.unisync.mobile.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "tarefas", primaryKeys = ["alunoId", "id"])
data class TarefaLocal(val id: String, val alunoId: String, val titulo: String, val status: String,
    @ColumnInfo(defaultValue = "'MEDIA'") val prioridade: String = "MEDIA", val prazo: String? = null,
    val turmaId: String? = null, val disciplina: String? = null)

@Entity(tableName = "sincronizacoes")
data class SincronizacaoLocal(@PrimaryKey val alunoId: String, val atualizadoEm: Long)

@Entity(tableName = "oportunidades")
data class OportunidadeLocal(@PrimaryKey val id: String, val titulo: String, val professor: String,
    val materia: String, val descricao: String?, val tipo: String, val dataEvento: String,
    val professorId: String? = null)

@Dao
interface OportunidadeDao {
    @Query("SELECT * FROM oportunidades ORDER BY dataEvento")
    fun observar(): Flow<List<OportunidadeLocal>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(itens: List<OportunidadeLocal>)
    @Query("DELETE FROM oportunidades")
    suspend fun limpar()
    @Query("DELETE FROM oportunidades WHERE id = :id")
    suspend fun excluir(id: String)
    @Transaction
    suspend fun substituir(itens: List<OportunidadeLocal>) { limpar(); salvar(itens) }
}

@Dao
interface TarefaDao {
    @Query("SELECT * FROM tarefas WHERE alunoId = :alunoId ORDER BY titulo COLLATE NOCASE")
    fun observar(alunoId: String): Flow<List<TarefaLocal>>
    @Query("SELECT * FROM tarefas") fun observarTodas(): Flow<List<TarefaLocal>>
    @Query("SELECT * FROM tarefas WHERE alunoId = :alunoId") suspend fun listar(alunoId: String): List<TarefaLocal>
    @Query("SELECT atualizadoEm FROM sincronizacoes WHERE alunoId = :alunoId")
    suspend fun atualizadoEm(alunoId: String): Long?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(itens: List<TarefaLocal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun marcar(sincronizacao: SincronizacaoLocal)
    @Query("DELETE FROM tarefas WHERE alunoId = :alunoId")
    suspend fun limparAluno(alunoId: String)
    @Query("DELETE FROM tarefas WHERE alunoId = :alunoId AND id = :id")
    suspend fun excluir(alunoId: String, id: String)
    @Query("DELETE FROM tarefas")
    suspend fun limparTarefas()
    @Query("DELETE FROM sincronizacoes")
    suspend fun limparSincronizacoes()
    @Transaction
    suspend fun limparTudo() { limparTarefas(); limparSincronizacoes() }
    @Transaction
    suspend fun substituir(alunoId: String, itens: List<TarefaLocal>, agora: Long) {
        limparAluno(alunoId)
        salvar(itens)
        marcar(SincronizacaoLocal(alunoId, agora))
    }
}

@Database(entities = [TarefaLocal::class, SincronizacaoLocal::class, OportunidadeLocal::class, LembreteLocal::class, SincronizacaoLembretes::class, AlertaLocal::class], version = 5, exportSchema = true)
abstract class BancoUniSync : RoomDatabase() {
    abstract fun tarefas(): TarefaDao
    abstract fun oportunidades(): OportunidadeDao
    abstract fun lembretes(): LembreteDao
    abstract fun alertas(): AlertaDao
    companion object {
        val MIGRACAO_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS lembretes (id TEXT NOT NULL, usuarioId TEXT NOT NULL, titulo TEXT NOT NULL, concluido INTEGER NOT NULL, agendadoEm TEXT, PRIMARY KEY(usuarioId, id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS sincronizacoes_lembretes (usuarioId TEXT NOT NULL PRIMARY KEY, atualizadoEm INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS alertas (usuarioId TEXT NOT NULL, chave TEXT NOT NULL, horario INTEGER NOT NULL, entregue INTEGER NOT NULL, PRIMARY KEY(usuarioId, chave))")
            }
        }
        val MIGRACAO_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tarefas ADD COLUMN prioridade TEXT NOT NULL DEFAULT 'MEDIA'")
                db.execSQL("ALTER TABLE tarefas ADD COLUMN prazo TEXT")
                db.execSQL("ALTER TABLE tarefas ADD COLUMN turmaId TEXT")
                db.execSQL("ALTER TABLE tarefas ADD COLUMN disciplina TEXT")
            }
        }
        val MIGRACAO_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE oportunidades ADD COLUMN professorId TEXT")
            }
        }
        val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS oportunidades (id TEXT NOT NULL PRIMARY KEY, titulo TEXT NOT NULL, professor TEXT NOT NULL, materia TEXT NOT NULL, descricao TEXT, tipo TEXT NOT NULL, dataEvento TEXT NOT NULL)")
            }
        }
    }
}
