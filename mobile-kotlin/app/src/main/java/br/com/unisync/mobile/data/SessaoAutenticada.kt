package br.com.unisync.mobile.data

import java.util.Base64
import org.json.JSONObject

enum class PerfilUsuario {
    ALUNO,
    PROFESSOR
}

class SessaoAutenticada private constructor(
    val token: String,
    val nome: String,
    val perfil: PerfilUsuario,
    val expiraEm: Long,
    val usuarioId: String
) {
    fun expirada(agora: Long = System.currentTimeMillis() / 1000): Boolean = agora >= expiraEm

    companion object {
        fun doToken(token: String, agora: Long = System.currentTimeMillis() / 1000): SessaoAutenticada {
            try {
                val partes = token.split('.')
                if (partes.size != 3 || partes.any { it.isBlank() }) {
                    throw ExcecaoAutenticacao("Resposta de autenticação inválida.")
                }
                // As declarações personalizam a interface. A autorização permanece na API.
                val dados = Base64.getUrlDecoder().decode(partes[1])
                val declaracoes = JSONObject(String(dados, Charsets.UTF_8))
                val perfil = when (
                    declaracoes.optString("http://schemas.microsoft.com/ws/2008/06/identity/claims/role")
                ) {
                    "Aluno" -> PerfilUsuario.ALUNO
                    "Professor" -> PerfilUsuario.PROFESSOR
                    else -> throw ExcecaoAutenticacao("Perfil de usuário não reconhecido.")
                }
                val nome = declaracoes.optString("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name")
                val expiraEm = declaracoes.getLong("exp")
                if (expiraEm <= agora) throw ExcecaoAutenticacao("Sua sessão expirou. Entre novamente.")
                val usuarioId = declaracoes.optString("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier",
                    declaracoes.optString("nameid"))
                return SessaoAutenticada(token, nome, perfil, expiraEm, usuarioId)
            } catch (erro: ExcecaoAutenticacao) {
                throw erro
            } catch (_: Exception) {
                throw ExcecaoAutenticacao("Resposta de autenticação inválida.")
            }
        }
    }
}
