package br.com.unisync.mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import br.com.unisync.mobile.BuildConfig
import br.com.unisync.mobile.domain.auth.Autenticacao
import org.json.JSONException
import java.net.SocketTimeoutException
import java.net.ConnectException
import java.net.UnknownHostException

class RepositorioAutenticacao(private val urlBase: String = BuildConfig.AUTH_BASE_URL) : Autenticacao {
    override suspend fun entrar(email: String, senha: String): SessaoAutenticada {
        val resposta = enviarRequisicao("login", JSONObject().put("email", email).put("senha", senha))
        val token = resposta.optString("token").takeIf { it.isNotBlank() }
            ?: throw ExcecaoAutenticacao("Resposta de autenticação inválida.")
        return SessaoAutenticada.doToken(token)
    }

    override suspend fun cadastrar(nome: String, email: String, senha: String, codigoProfessor: String?) {
        val corpo = JSONObject().put("nome", nome).put("email", email).put("senha", senha)
        if (codigoProfessor != null) corpo.put("codigoProfessor", codigoProfessor)
        enviarRequisicao(if (codigoProfessor == null) "" else "professores", corpo)
    }

    private suspend fun enviarRequisicao(caminho: String, corpo: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        if (urlBase.isBlank()) throw ExcecaoAutenticacao("Configure o endereço do serviço para esta versão do aplicativo.")
        val base = URI(urlBase)
        if (base.host.isNullOrBlank() || base.scheme !in listOf("http", "https") ||
            (!BuildConfig.DEBUG && base.scheme != "https")) {
            throw ExcecaoAutenticacao("O endereço do serviço é inválido. A versão de produção exige HTTPS.")
        }
        val conexao = URI("${urlBase.trimEnd('/')}/api/usuarios${if (caminho.isEmpty()) "" else "/$caminho"}").toURL().openConnection() as HttpURLConnection
        try {
            conexao.requestMethod = "POST"
            conexao.instanceFollowRedirects = false
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
            try {
                if (texto.isBlank()) JSONObject() else JSONObject(texto)
            } catch (_: JSONException) {
                throw ExcecaoAutenticacao("O serviço retornou uma resposta inválida. Tente novamente.")
            }
        } catch (_: SocketTimeoutException) {
            throw ExcecaoAutenticacao("O serviço demorou para responder. Tente novamente.")
        } catch (_: UnknownHostException) {
            throw ExcecaoAutenticacao("Não foi possível localizar o serviço. Confira a conexão e tente novamente.")
        } catch (_: ConnectException) {
            throw ExcecaoAutenticacao("Não foi possível conectar ao serviço. Confira a conexão e tente novamente.")
        } finally {
            conexao.disconnect()
        }
    }
}
