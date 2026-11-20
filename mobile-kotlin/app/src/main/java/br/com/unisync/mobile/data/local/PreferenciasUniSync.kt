package br.com.unisync.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import br.com.unisync.mobile.notificacoes.OpcoesAlertas
import java.time.LocalTime
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException

private val Context.preferencias by preferencesDataStore("preferencias_unisync")
enum class ModoTema { SISTEMA, CLARO, ESCURO }
class PreferenciasUniSync(contexto: Context) {
    private val dados = contexto.applicationContext.preferencias
    private val leitura get() = dados.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
    private val chaveTema = stringPreferencesKey("tema")
    val tema = dados.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> ModoTema.entries.find { it.name == prefs[chaveTema] } ?: ModoTema.SISTEMA }
    val mudancas = leitura.map { it.hashCode() }
    fun alertas(usuarioId: String) = leitura.map { prefs -> OpcoesAlertas(
        prefs[booleanPreferencesKey("lembretes_$usuarioId")] ?: false,
        prefs[booleanPreferencesKey("tarefas_$usuarioId")] ?: false,
        prefs[stringPreferencesKey("hora_prazos_$usuarioId")]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: LocalTime.of(9, 0)) }
    suspend fun definirAlertas(usuarioId: String, opcoes: OpcoesAlertas) {
        dados.edit { prefs ->
            prefs[booleanPreferencesKey("lembretes_$usuarioId")] = opcoes.lembretes
            prefs[booleanPreferencesKey("tarefas_$usuarioId")] = opcoes.tarefas
            prefs[stringPreferencesKey("hora_prazos_$usuarioId")] = opcoes.horaPrazos.toString()
        }
    }
    suspend fun definirTema(modo: ModoTema) { dados.edit { it[chaveTema] = modo.name } }
}
