package br.com.unisync.mobile.data

import android.util.Base64
import org.json.JSONObject

enum class PerfilUsuario {
    ALUNO,
    PROFESSOR
}

class SessaoAutenticada private constructor(
    val token: String,
    val nome: String,
    val perfil: PerfilUsuario
) {
    companion object {
        fun doToken(token: String): SessaoAutenticada {
            val partes = token.split('.')
            if (partes.size != 3) throw ExcecaoAutenticacao("Resposta de autenticação inválida.")

            // As declarações personalizam a interface. A autorização permanece na API.
            val dados = try {
                Base64.decode(partes[1], Base64.URL_SAFE)
            } catch (_: IllegalArgumentException) {
                throw ExcecaoAutenticacao("Resposta de autenticação inválida.")
            }
            val declaracoes = JSONObject(String(dados, Charsets.UTF_8))
            val perfil = when (
                declaracoes.optString("http://schemas.microsoft.com/ws/2008/06/identity/claims/role")
            ) {
                "Aluno" -> PerfilUsuario.ALUNO
                "Professor" -> PerfilUsuario.PROFESSOR
                else -> throw ExcecaoAutenticacao("Perfil de usuário não reconhecido.")
            }
            val nome = declaracoes.optString("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name")
            return SessaoAutenticada(token, nome, perfil)
        }
    }
}
