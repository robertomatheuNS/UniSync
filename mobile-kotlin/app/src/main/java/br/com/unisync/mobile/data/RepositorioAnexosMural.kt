package br.com.unisync.mobile.data

import br.com.unisync.mobile.data.remote.ClienteAcademico
import br.com.unisync.mobile.domain.mural.*
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.Instant

class RepositorioAnexosMural(private val api: ClienteAcademico = ClienteAcademico()) : AnexosMural {
    private fun id(valor: String) = valor.also { require(it.matches(Regex("[a-fA-F0-9]{24}"))) { "Identificador de anexo inválido." } }
    private fun caminho(publicacaoId: String) = "/mural/${id(publicacaoId)}/anexos"
    override suspend fun listar(token: String, publicacaoId: String): ListaAnexos {
        val json = JSONObject(api.executar("GET", caminho(publicacaoId), token))
        require(json.getInt("limite") == LIMITE_ANEXOS && json.getInt("tamanho_maximo") == TAMANHO_MAXIMO_ANEXO) { "Atualize o aplicativo para enviar anexos." }
        val lista = json.getJSONArray("itens")
        require(lista.length() <= LIMITE_ANEXOS)
        val itens = (0 until lista.length()).map { anexo(lista.getJSONObject(it), publicacaoId) }
        require(itens.distinctBy { it.id }.size == itens.size)
        return ListaAnexos(itens, json.getBoolean("pode_adicionar"))
    }
    override suspend fun enviar(token: String, publicacaoId: String, envioId: String, arquivo: ArquivoEnvio, progresso: (Int) -> Unit): Anexo {
        validarArquivoEnvio(arquivo)
        val nome = URLEncoder.encode(arquivo.nome, "UTF-8")
        val json = api.enviarArquivo(caminho(publicacaoId) + "/${id(envioId)}?nome=$nome", token, arquivo.tipo, arquivo.dados, progresso)
        return anexo(JSONObject(json), publicacaoId).also {
            require(it.id == envioId && it.nome == arquivo.nome && it.tipo == arquivo.tipo &&
                it.tamanho == arquivo.dados.size && it.sha256 == sha256(arquivo.dados)) { "Não foi possível conferir o arquivo enviado. Atualize os anexos." }
        }
    }
    override suspend fun baixar(token: String, anexo: Anexo): ByteArray {
        val dados = api.baixarArquivo(caminho(anexo.publicacaoId) + "/${id(anexo.id)}/conteudo", token, TAMANHO_MAXIMO_ANEXO)
        require(dados.size == anexo.tamanho && sha256(dados) == anexo.sha256) { "O arquivo recebido ficou incompleto. Tente abrir novamente." }
        return dados
    }
    override suspend fun excluir(token: String, publicacaoId: String, anexoId: String) {
        api.executar("DELETE", caminho(publicacaoId) + "/${id(anexoId)}", token)
    }
    private fun anexo(json: JSONObject, publicacaoId: String): Anexo {
        require(json.getString("publicacao_id") == publicacaoId)
        return Anexo(id(json.getString("id")), publicacaoId, json.getString("nome"),
            json.getString("tipo").also { require(it in TIPOS_ANEXO) },
            json.getInt("tamanho").also { require(it in 1..TAMANHO_MAXIMO_ANEXO) },
            json.getString("sha256").also { require(it.matches(Regex("[a-f0-9]{64}"))) },
            Instant.parse(json.getString("criado_em")), json.getBoolean("pode_excluir"))
    }
}

internal fun sha256(dados: ByteArray) = MessageDigest.getInstance("SHA-256").digest(dados).joinToString("") { "%02x".format(it) }
