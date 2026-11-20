package br.com.unisync.mobile.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "lembretes", primaryKeys = ["usuarioId", "id"])
data class LembreteLocal(val id: String, val usuarioId: String, val titulo: String,
    val concluido: Boolean, val agendadoEm: String?)

@Entity(tableName = "sincronizacoes_lembretes")
data class SincronizacaoLembretes(@PrimaryKey val usuarioId: String, val atualizadoEm: Long)

@Entity(tableName = "alertas", primaryKeys = ["usuarioId", "chave"])
data class AlertaLocal(val usuarioId: String, val chave: String, val horario: Long, val entregue: Boolean)

@Dao
interface LembreteDao {
    @Query("SELECT * FROM lembretes WHERE usuarioId = :usuarioId ORDER BY concluido, agendadoEm IS NULL, agendadoEm, titulo COLLATE NOCASE")
    fun observar(usuarioId: String): Flow<List<LembreteLocal>>
    @Query("SELECT * FROM lembretes") fun observarTodos(): Flow<List<LembreteLocal>>
    @Query("SELECT * FROM lembretes WHERE usuarioId = :usuarioId") suspend fun listar(usuarioId: String): List<LembreteLocal>
    @Query("SELECT atualizadoEm FROM sincronizacoes_lembretes WHERE usuarioId = :usuarioId") suspend fun atualizadoEm(usuarioId: String): Long?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun salvar(itens: List<LembreteLocal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun marcar(item: SincronizacaoLembretes)
    @Query("DELETE FROM lembretes WHERE usuarioId = :usuarioId") suspend fun limparUsuario(usuarioId: String)
    @Query("DELETE FROM lembretes WHERE usuarioId = :usuarioId AND id = :id") suspend fun excluir(usuarioId: String, id: String)
    @Query("DELETE FROM lembretes") suspend fun limparItens()
    @Query("DELETE FROM sincronizacoes_lembretes") suspend fun limparDatas()
    @Transaction suspend fun substituir(usuarioId: String, itens: List<LembreteLocal>, agora: Long) {
        limparUsuario(usuarioId); salvar(itens); marcar(SincronizacaoLembretes(usuarioId, agora))
    }
    @Transaction suspend fun limparTudo() { limparItens(); limparDatas() }
}

@Dao
interface AlertaDao {
    @Query("SELECT * FROM alertas WHERE usuarioId = :usuarioId") suspend fun listar(usuarioId: String): List<AlertaLocal>
    @Query("DELETE FROM alertas") suspend fun limpar()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun salvar(itens: List<AlertaLocal>)
    @Transaction suspend fun substituir(itens: List<AlertaLocal>) { limpar(); salvar(itens) }
}
