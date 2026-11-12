package br.com.unisync.mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.io.IOException

class AuthRepository(private val baseUrl: String = "http://127.0.0.1:5040") {
    suspend fun login(email: String, password: String): String {
        val response = post("login", JSONObject().put("email", email).put("senha", password))
        return response.optString("token").takeIf { it.isNotBlank() }
            ?: throw IOException("Resposta de autenticação inválida.")
    }

    suspend fun register(name: String, email: String, password: String, professorCode: String?) {
        val body = JSONObject().put("nome", name).put("email", email).put("senha", password)
        if (professorCode != null) body.put("codigoProfessor", professorCode)
        post(if (professorCode == null) "" else "professores", body)
    }

    private suspend fun post(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val connection = URI("$baseUrl/api/usuarios/$path").toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = when (status) {
                    401 -> "E-mail ou senha inválidos."
                    409 -> "O e-mail informado já está cadastrado."
                    400 -> "Confira os dados informados e o código de professor."
                    else -> "O serviço está indisponível. Tente novamente."
                }
                throw AuthException(message)
            }
            JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}

class AuthException(message: String) : IOException(message)
