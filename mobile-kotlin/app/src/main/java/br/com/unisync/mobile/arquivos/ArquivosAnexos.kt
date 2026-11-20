package br.com.unisync.mobile.arquivos

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import br.com.unisync.mobile.domain.mural.*
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID

class ProvedorAnexos : FileProvider()

class ArquivosAnexos(private val contexto: Context) {
    private val diretorio get() = File(contexto.cacheDir, "anexos")
    suspend fun ler(uri: Uri): ArquivoEnvio = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") { "Selecione o arquivo pelo seletor do Android." }
        val resolver = contexto.contentResolver
        var nome = ""
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val colunaNome = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (colunaNome >= 0 && !cursor.isNull(colunaNome)) nome = cursor.getString(colunaNome)
                val colunaTamanho = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (colunaTamanho >= 0 && !cursor.isNull(colunaTamanho))
                    require(cursor.getLong(colunaTamanho) <= TAMANHO_MAXIMO_ANEXO) { "O arquivo deve ter até 4 MiB." }
            }
        }
        val tipo = resolver.getType(uri).orEmpty()
        require(tipo in TIPOS_ANEXO) { "Selecione um PDF, PNG ou JPEG." }
        val dados = (resolver.openInputStream(uri) ?: throw IOException("Selecione o arquivo novamente.")).use { entrada ->
            val saida = ByteArrayOutputStream()
            val buffer = ByteArray(32768)
            while (true) {
                coroutineContext.ensureActive()
                val lidos = entrada.read(buffer)
                if (lidos < 0) break
                require(saida.size() + lidos <= TAMANHO_MAXIMO_ANEXO) { "O arquivo deve ter até 4 MiB." }
                saida.write(buffer, 0, lidos)
            }
            saida.toByteArray()
        }
        validarArquivoEnvio(ArquivoEnvio(nome, tipo, dados))
    }
    internal suspend fun prepararAbertura(anexo: Anexo, dados: ByteArray): Intent {
        val arquivo = withContext(Dispatchers.IO) {
            require(anexo.id.matches(Regex("[a-fA-F0-9]{24}")) && dados.size in 1..TAMANHO_MAXIMO_ANEXO && dados.size == anexo.tamanho)
            diretorio.mkdirs()
            // Limita cópias transitórias; cada abertura consulta novamente a API autenticada.
            diretorio.listFiles()?.sortedByDescending { it.lastModified() }?.drop(19)?.forEach(::remover)
            val extensao = when (anexo.tipo) { "application/pdf" -> ".pdf"; "image/png" -> ".png"; "image/jpeg" -> ".jpg"; else -> error("Tipo de anexo inválido.") }
            File(diretorio, "${anexo.id}-${UUID.randomUUID()}$extensao").also { it.writeBytes(dados) }
        }
        val endereco = uri(arquivo)
        return Intent(Intent.ACTION_VIEW).setDataAndType(endereco, anexo.tipo)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = ClipData.newRawUri("Anexo", endereco) }
    }
    suspend fun abrir(anexo: Anexo, dados: ByteArray) {
        val intent = prepararAbertura(anexo, dados)
        try {
            withContext(Dispatchers.Main) {
                ensureActive()
                contexto.startActivity(Intent.createChooser(intent, "Abrir ${anexo.nome}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } catch (erro: ActivityNotFoundException) {
            limparAnexo(anexo.id)
            throw IllegalArgumentException("Instale um aplicativo que abra esse tipo de arquivo e tente novamente.")
        } catch (erro: Exception) { limparAnexo(anexo.id); throw erro }
    }
    private fun uri(arquivo: File) = FileProvider.getUriForFile(contexto, "${contexto.packageName}.anexos", arquivo)
    private fun remover(arquivo: File) {
        contexto.revokeUriPermission(uri(arquivo), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        arquivo.delete()
    }
    fun limparAnexo(id: String) { diretorio.listFiles()?.filter { it.name.startsWith("$id-") }?.forEach(::remover) }
    fun limpar() { diretorio.listFiles()?.forEach(::remover) }
}
