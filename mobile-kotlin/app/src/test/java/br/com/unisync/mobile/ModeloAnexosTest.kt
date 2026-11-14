package br.com.unisync.mobile

import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.mural.*
import br.com.unisync.mobile.ui.mural.ModeloAnexos
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloAnexosTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun encerrar() { Dispatchers.resetMain() }
    private val publicacaoId = "111111111111111111111111"
    private val arquivo = ArquivoEnvio("roteiro.pdf", "application/pdf", "PDF".toByteArray())
    private val anexo = Anexo("222222222222222222222222", publicacaoId, arquivo.nome, arquivo.tipo, 3, "a".repeat(64), Instant.EPOCH, true)
    private inner class Fake : AnexosMural {
        var lista = ListaAnexos(listOf(anexo), true)
        var erro: Exception? = null
        var erroLeitura: Exception? = null
        var envios = mutableListOf<String>(); var exclusoes = 0; var adiamento = false
        override suspend fun listar(token: String, publicacaoId: String): ListaAnexos { erroLeitura?.let { throw it }; return lista }
        override suspend fun enviar(token: String, publicacaoId: String, envioId: String, arquivo: ArquivoEnvio, progresso: (Int) -> Unit): Anexo {
            envios.add(envioId); progresso(50); if (adiamento) delay(1000); erro?.let { throw it }; progresso(100); return anexo.copy(id = envioId)
        }
        override suspend fun baixar(token: String, anexo: Anexo): ByteArray { erro?.let { throw it }; return arquivo.dados }
        override suspend fun excluir(token: String, publicacaoId: String, anexoId: String) { exclusoes++; erro?.let { throw it } }
    }
    @Test fun respostaPerdidaMantemMesmoEnvioENaoDuplicaLista() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloAnexos(repo, "token", publicacaoId, {}); runCurrent()
        repo.erro = IOException(); modelo.enviar(anexo.id) { arquivo }; runCurrent()
        assertEquals(0, modelo.estado.value.confirmacaoEnvio); assertEquals(1, modelo.estado.value.itens.size)
        repo.erro = null; modelo.enviar(anexo.id) { arquivo }; runCurrent()
        assertEquals(listOf(anexo.id, anexo.id), repo.envios)
        assertEquals(1, modelo.estado.value.confirmacaoEnvio); assertEquals(1, modelo.estado.value.itens.size); modelo.fechar()
    }
    @Test fun toquesRepetidosEAtualizacaoDuranteEnvioNaoDisparamOperacoesExtras() = runTest(dispatcher) {
        val repo = Fake().also { it.adiamento = true }; val modelo = ModeloAnexos(repo, "token", publicacaoId, {}); runCurrent()
        modelo.enviar(anexo.id) { arquivo }; modelo.enviar(anexo.id) { arquivo }; modelo.atualizar(); runCurrent()
        assertEquals(1, repo.envios.size); assertEquals(50, modelo.estado.value.progresso)
        advanceUntilIdle(); assertFalse(modelo.estado.value.enviando); assertEquals(1, modelo.estado.value.confirmacaoEnvio); modelo.fechar()
    }
    @Test fun leitorNaoEnviaNemRemoveArquivosDeTerceiros() = runTest(dispatcher) {
        val repo = Fake().also { it.lista = ListaAnexos(listOf(anexo.copy(podeExcluir = false)), false) }
        val modelo = ModeloAnexos(repo, "token", publicacaoId, {}); runCurrent()
        modelo.enviar(anexo.id) { arquivo }; modelo.excluir(anexo); runCurrent()
        assertTrue(repo.envios.isEmpty()); assertEquals(0, repo.exclusoes); modelo.fechar()
    }
    @Test fun downloadNegadoApagaMetadadosENaoAbreArquivo() = runTest(dispatcher) {
        val repo = Fake(); var abriu = false; var perdeu = false
        val modelo = ModeloAnexos(repo, "token", publicacaoId, {}, { perdeu = true }); runCurrent()
        repo.erro = FalhaApi(403, "Sem acesso"); modelo.abrir(anexo) { abriu = true }; runCurrent()
        assertFalse(abriu); assertTrue(perdeu); assertTrue(modelo.estado.value.itens.isEmpty()); assertFalse(modelo.estado.value.podeAdicionar); modelo.fechar()
    }
    @Test fun anexoRemovidoAtualizaListaSemOcultarPublicacaoDisponivel() = runTest(dispatcher) {
        val repo = Fake(); var perdeu = false
        val modelo = ModeloAnexos(repo, "token", publicacaoId, {}, { perdeu = true }); runCurrent()
        repo.erro = FalhaApi(404, "Removido"); repo.lista = ListaAnexos(emptyList(), true)
        modelo.abrir(anexo) { fail("Não deve abrir") }; runCurrent()
        assertFalse(perdeu); assertTrue(modelo.estado.value.itens.isEmpty()); assertNotNull(modelo.estado.value.erro); modelo.fechar()
    }
    @Test fun exclusaoSoRemoveDepoisDaConfirmacaoDoServidor() = runTest(dispatcher) {
        val repo = Fake(); var limpou = false
        val modelo = ModeloAnexos(repo, "token", publicacaoId, {}); runCurrent()
        repo.erro = IOException(); modelo.excluir(anexo) { limpou = true }; runCurrent()
        assertFalse(limpou); assertEquals(1, modelo.estado.value.itens.size)
        repo.erro = null; modelo.excluir(anexo) { limpou = true }; runCurrent()
        assertTrue(limpou); assertTrue(modelo.estado.value.itens.isEmpty()); assertEquals(1, modelo.estado.value.confirmacaoExclusao); modelo.fechar()
    }
    @Test fun sessaoExpiradaNaoAbreArquivo() = runTest(dispatcher) {
        val repo = Fake(); var expirou = false
        val modelo = ModeloAnexos(repo, "token", publicacaoId, { expirou = true }); runCurrent()
        repo.erro = FalhaApi(401, "Expirou"); modelo.abrir(anexo) { fail("Não deve abrir") }; runCurrent()
        assertTrue(expirou); modelo.fechar()
    }
    @Test fun validacaoRejeitaArquivoVazioGrandeExtensaoDivergenteENomeInseguro() {
        assertEquals(arquivo, validarArquivoEnvio(arquivo))
        listOf(arquivo.copy(dados = byteArrayOf()), arquivo.copy(dados = ByteArray(TAMANHO_MAXIMO_ANEXO + 1)),
            arquivo.copy(nome = "arquivo.html"), arquivo.copy(nome = "../arquivo.pdf"), arquivo.copy(nome = "a\u202e.pdf"),
            arquivo.copy(tipo = "text/html")).forEach { assertTrue(runCatching { validarArquivoEnvio(it) }.isFailure) }
        assertEquals(24, ModeloAnexos.novoEnvioId().length)
    }
}
