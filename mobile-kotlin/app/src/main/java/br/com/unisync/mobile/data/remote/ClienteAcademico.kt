package br.com.unisync.mobile.data.remote

import br.com.unisync.mobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

class FalhaApi(val status: Int, mensagem: String) : IOException(mensagem)

class ClienteAcademico(private val urlBase: String = BuildConfig.ACADEMIC_BASE_URL) {
    private fun conectar(caminho: String, token: String): HttpURLConnection {
        val base = runCatching { URI(urlBase) }.getOrNull()
        if (base?.host.isNullOrBlank() || base?.scheme !in listOf("http", "https") ||
            (!BuildConfig.DEBUG && base?.scheme != "https")) throw IOException("Endereço do serviço acadêmico indisponível.")
        require(caminho.startsWith("/") && !caminho.startsWith("//"))
        return (URI("${urlBase.trimEnd('/')}$caminho").toURL().openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 10000
            readTimeout = 30000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Cache-Control", "no-store")
            useCaches = false
        }
    }
    private fun conferir(conexao: HttpURLConnection) {
        val status = conexao.responseCode
        if (status !in 200..299) throw FalhaApi(status, when (status) {
            401 -> "Sua sessão expirou. Entre novamente."
            403 -> "Você não tem permissão para realizar esta ação."
            404 -> "O item não está mais disponível. Atualize a lista."
            409 -> "O item foi alterado. Atualize a lista e tente novamente."
            413 -> "O arquivo deve ter até 4 MiB."
            415 -> "Selecione um PDF, PNG ou JPEG com conteúdo e extensão correspondentes."
            400, 422 -> "Confira os dados e tente novamente."
            else -> "O serviço está indisponível. Tente novamente."
        })
    }
    suspend fun enviarArquivo(caminho: String, token: String, tipo: String, dados: ByteArray, progresso: (Int) -> Unit): String = withContext(Dispatchers.IO) {
        val conexao = conectar(caminho, token)
        try {
            conexao.requestMethod = "PUT"
            conexao.doOutput = true
            conexao.setRequestProperty("Content-Type", tipo)
            conexao.setRequestProperty("Accept", "application/json")
            conexao.setFixedLengthStreamingMode(dados.size)
            conexao.outputStream.use { saida ->
                var enviado = 0
                while (enviado < dados.size) {
                    coroutineContext.ensureActive()
                    val tamanho = minOf(32768, dados.size - enviado)
                    saida.write(dados, enviado, tamanho)
                    enviado += tamanho
                    progresso((enviado.toLong() * 100 / dados.size).toInt())
                }
            }
            conferir(conexao)
            conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally { conexao.disconnect() }
    }
    suspend fun baixarArquivo(caminho: String, token: String, limite: Int): ByteArray = withContext(Dispatchers.IO) {
        val conexao = conectar(caminho, token)
        try {
            conexao.setRequestProperty("Accept", "application/pdf, image/png, image/jpeg")
            conferir(conexao)
            require(conexao.contentLengthLong <= limite) { "O arquivo excede o tamanho permitido." }
            conexao.inputStream.use { entrada ->
                val saida = ByteArrayOutputStream()
                val buffer = ByteArray(32768)
                while (true) {
                    coroutineContext.ensureActive()
                    val lidos = entrada.read(buffer)
                    if (lidos < 0) break
                    require(saida.size() + lidos <= limite) { "O arquivo excede o tamanho permitido." }
                    saida.write(buffer, 0, lidos)
                }
                saida.toByteArray()
            }
        } finally { conexao.disconnect() }
    }
    suspend fun executar(metodo: String, caminho: String, token: String, corpo: String? = null): String = withContext(Dispatchers.IO) {
        val conexao = conectar(caminho, token)
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
            conferir(conexao)
            conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally { conexao.disconnect() }
    }
}
