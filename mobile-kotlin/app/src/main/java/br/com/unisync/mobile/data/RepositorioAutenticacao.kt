package br.com.unisync.mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.io.IOException

class RepositorioAutenticacao(private val urlBase: String = "http://127.0.0.1:5040") {
    suspend fun entrar(email: String, senha: String): SessaoAutenticada {
        val resposta = enviarRequisicao("login", JSONObject().put("email", email).put("senha", senha))
        val token = resposta.optString("token").takeIf { it.isNotBlank() }
            ?: throw ExcecaoAutenticacao("Resposta de autenticação inválida.")
        return SessaoAutenticada.doToken(token)
    }

    suspend fun cadastrar(nome: String, email: String, senha: String, codigoProfessor: String?) {
        val corpo = JSONObject().put("nome", nome).put("email", email).put("senha", senha)
        if (codigoProfessor != null) corpo.put("codigoProfessor", codigoProfessor)
        enviarRequisicao(if (codigoProfessor == null) "" else "professores", corpo)
    }

    private suspend fun enviarRequisicao(caminho: String, corpo: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val conexao = URI("$urlBase/api/usuarios/$caminho").toURL().openConnection() as HttpURLConnection
        try {
            conexao.requestMethod = "POST"
            conexao.connectTimeout = 10000
            conexao.readTimeout = 15000
            conexao.doOutput = true
            conexao.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conexao.outputStream.use { it.write(corpo.toString().toByteArray(Charsets.UTF_8)) }
            val status = conexao.responseCode
            val fluxo = if (status in 200..299) conexao.inputStream else conexao.errorStream
            val texto = fluxo?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val mensagem = when (status) {
                    401 -> "E-mail ou senha inválidos."
                    409 -> "O e-mail informado já está cadastrado."
                    400 -> "Confira os dados informados e o código de professor."
                    else -> "O serviço está indisponível. Tente novamente."
                }
                throw ExcecaoAutenticacao(mensagem)
            }
            JSONObject(texto)
        } finally {
            conexao.disconnect()
        }
    }
}

class ExcecaoAutenticacao(mensagem: String) : IOException(mensagem)
