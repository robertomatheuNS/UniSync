package br.com.unisync.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
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
    private val chaveTema = stringPreferencesKey("tema")
    val tema = dados.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> ModoTema.entries.find { it.name == prefs[chaveTema] } ?: ModoTema.SISTEMA }
    suspend fun definirTema(modo: ModoTema) { dados.edit { it[chaveTema] = modo.name } }
}
