package br.com.unisync.mobile.data.remote

import br.com.unisync.mobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

class FalhaApi(val status: Int, mensagem: String) : IOException(mensagem)

class ClienteAcademico(private val urlBase: String = BuildConfig.ACADEMIC_BASE_URL) {
    suspend fun executar(metodo: String, caminho: String, token: String, corpo: String? = null): String = withContext(Dispatchers.IO) {
        val base = runCatching { URI(urlBase) }.getOrNull()
        if (base?.host.isNullOrBlank() || base?.scheme !in listOf("http", "https") ||
            (!BuildConfig.DEBUG && base?.scheme != "https")) throw IOException("Endereço do serviço acadêmico indisponível.")
        val conexao = URI("${urlBase.trimEnd('/')}$caminho").toURL().openConnection() as HttpURLConnection
        try {
            conexao.requestMethod = metodo
            conexao.instanceFollowRedirects = false
            conexao.connectTimeout = 10000
            conexao.readTimeout = 15000
            conexao.setRequestProperty("Authorization", "Bearer $token")
            conexao.setRequestProperty("Accept", "application/json")
            if (corpo != null) {
                conexao.doOutput = true
                conexao.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conexao.outputStream.use { it.write(corpo.toByteArray(Charsets.UTF_8)) }
            }
            val status = conexao.responseCode
            if (status !in 200..299) throw FalhaApi(status, when (status) {
                401 -> "Sua sessão expirou. Entre novamente."
                403 -> "Você não tem permissão para realizar esta ação."
                404 -> "O item não está mais disponível. Atualize a lista."
                409 -> "O item foi alterado. Atualize a lista e tente novamente."
                400, 422 -> "Confira os dados e tente novamente."
                else -> "O serviço está indisponível. Tente novamente."
            })
            conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally { conexao.disconnect() }
    }
}
