package br.com.unisync.mobile.domain.mural

import java.time.Instant

const val TAMANHO_MAXIMO_ANEXO = 4 * 1024 * 1024
const val LIMITE_ANEXOS = 3
val TIPOS_ANEXO = listOf("application/pdf", "image/png", "image/jpeg")

data class Anexo(val id: String, val publicacaoId: String, val nome: String, val tipo: String,
    val tamanho: Int, val sha256: String, val criadoEm: Instant, val podeExcluir: Boolean)
data class ListaAnexos(val itens: List<Anexo>, val podeAdicionar: Boolean)
data class ArquivoEnvio(val nome: String, val tipo: String, val dados: ByteArray)

fun validarArquivoEnvio(arquivo: ArquivoEnvio): ArquivoEnvio {
    require(arquivo.dados.isNotEmpty()) { "O arquivo está vazio." }
    require(arquivo.dados.size <= TAMANHO_MAXIMO_ANEXO) { "O arquivo deve ter até 4 MiB." }
    require(arquivo.nome.length in 1..150 && arquivo.nome == arquivo.nome.trim() &&
        arquivo.nome.none { it == '/' || it == '\\' } && arquivo.nome.codePoints().noneMatch { Character.getType(it).let { tipo -> tipo in listOf(
            Character.CONTROL.toInt(), Character.FORMAT.toInt(), Character.SURROGATE.toInt(), Character.PRIVATE_USE.toInt(), Character.UNASSIGNED.toInt()) } }) {
        "Use um nome de arquivo simples, com até 150 caracteres."
    }
    val extensoes = when (arquivo.tipo) {
        "application/pdf" -> listOf(".pdf")
        "image/png" -> listOf(".png")
        "image/jpeg" -> listOf(".jpg", ".jpeg")
        else -> emptyList()
    }
    require(extensoes.any { arquivo.nome.endsWith(it, ignoreCase = true) }) { "Selecione um PDF, PNG ou JPEG com extensão correspondente ao tipo." }
    return arquivo
}

interface AnexosMural {
    suspend fun listar(token: String, publicacaoId: String): ListaAnexos
    suspend fun enviar(token: String, publicacaoId: String, envioId: String, arquivo: ArquivoEnvio, progresso: (Int) -> Unit): Anexo
    suspend fun baixar(token: String, anexo: Anexo): ByteArray
    suspend fun excluir(token: String, publicacaoId: String, anexoId: String)
}
