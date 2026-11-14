package br.com.unisync.mobile

import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.mural.*
import br.com.unisync.mobile.ui.mural.ModeloConversa
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloConversaTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun encerrar() { Dispatchers.resetMain() }
    private val publicacao = Publicacao("111111111111111111111111", "222222222222222222222222", "A", "Kotlin", "eu", "Ana", "Aluno", "Título", "Resumo", Instant.EPOCH, Instant.EPOCH, 1, true, true, "Texto")
    private fun comentario(id: String = "333333333333333333333333", proprio: Boolean = true) = Comentario(id, publicacao.id, if (proprio) "eu" else "outro", "Bruno", "Aluno", "Original", Instant.EPOCH, Instant.EPOCH, 1, proprio, proprio)
    private inner class Fake : InteracoesMural {
        var pagina = PaginaComentarios(listOf(comentario()), "444444444444444444444444")
        var falha: Exception? = null
        var cria = 0; var edita = 0; var exclui = 0; var reage = 0
        var versao: Int? = null
        var adiar = false
        override suspend fun listarComentarios(token: String, publicacaoId: String, antes: String?): PaginaComentarios { falha?.let { throw it }; return pagina }
        override suspend fun consultarCurtida(token: String, publicacaoId: String): Curtida { falha?.let { throw it }; return Curtida(publicacaoId, 0, false) }
        override suspend fun comentar(token: String, publicacaoId: String, conteudo: String): Comentario { cria++; if (adiar) delay(1000); falha?.let { throw it }; return comentario("555555555555555555555555").copy(conteudo = conteudo) }
        override suspend fun editarComentario(token: String, publicacaoId: String, id: String, versao: Int, conteudo: String): Comentario { edita++; this.versao = versao; falha?.let { throw it }; return comentario(id).copy(versao = versao + 1, conteudo = conteudo) }
        override suspend fun excluirComentario(token: String, publicacaoId: String, id: String, versao: Int) { exclui++; this.versao = versao; falha?.let { throw it } }
        override suspend fun curtir(token: String, publicacaoId: String, curtiu: Boolean): Curtida { reage++; falha?.let { throw it }; return Curtida(publicacaoId, if (curtiu) 1 else 0, curtiu) }
    }
    @Test fun erroDeRedePreservaComentariosECurtidaSemConfirmarEscrita() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloConversa(repo, "token", "eu", publicacao, {}); runCurrent()
        repo.falha = IOException(); modelo.salvar(null, null, "Rascunho"); runCurrent()
        assertEquals(0, modelo.estado.value.confirmacaoSalvo); assertEquals("Original", modelo.estado.value.comentarios.single().conteudo)
        modelo.curtir(true); runCurrent()
        assertFalse(modelo.estado.value.curtiu); assertEquals(0, modelo.estado.value.totalCurtidas)
        assertNotNull(modelo.estado.value.erro); modelo.fechar()
    }
    @Test fun curtidaNaoConfirmaNemLimpaComentarioENaoEAplicadaAntesDoServidor() = runTest(dispatcher) {
        var atualizada: Curtida? = null
        val modelo = ModeloConversa(Fake(), "token", "eu", publicacao, {}, { atualizada = it }); runCurrent()
        modelo.curtir(true)
        assertFalse(modelo.estado.value.curtiu)
        runCurrent(); assertTrue(modelo.estado.value.curtiu); assertEquals(1, atualizada?.total)
        assertEquals(0, modelo.estado.value.confirmacaoSalvo)
        modelo.fechar()
    }
    @Test fun evitaDuplicacaoEPreservaVersaoDoRascunhoEmConflito() = runTest(dispatcher) {
        val repo = Fake().apply { adiar = true }; val modelo = ModeloConversa(repo, "token", "eu", publicacao, {}); runCurrent()
        modelo.salvar(null, null, "Novo"); modelo.salvar(null, null, "Duplicado"); runCurrent()
        assertEquals(1, repo.cria); advanceUntilIdle(); assertEquals(1, modelo.estado.value.confirmacaoSalvo)
        repo.falha = FalhaApi(409, "Conflito")
        modelo.salvar(comentario().id, 1, "Rascunho em conflito"); runCurrent()
        assertEquals(1, repo.versao); assertEquals(1, modelo.estado.value.confirmacaoSalvo)
        assertTrue(modelo.estado.value.erro!!.contains("comentário mudou")); modelo.fechar()
    }
    @Test fun naoEditaNemExcluiComentarioSemPermissao() = runTest(dispatcher) {
        val repo = Fake().apply { pagina = PaginaComentarios(listOf(comentario(proprio = false)), null) }
        val modelo = ModeloConversa(repo, "token", "eu", publicacao, {}); runCurrent()
        modelo.salvar(comentario().id, 1, "Tentativa"); runCurrent()
        modelo.excluir(comentario(proprio = false)); runCurrent()
        assertEquals(0, repo.edita); assertEquals(0, repo.exclui); modelo.fechar()
    }
    @Test fun paginacaoDeduplicaERedeIndisponivelMantemCursorParaTentarNovamente() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloConversa(repo, "token", "eu", publicacao, {}); runCurrent()
        repo.falha = IOException(); modelo.carregarMais(); runCurrent()
        assertEquals(1, modelo.estado.value.comentarios.size); assertNotNull(modelo.estado.value.proximaPagina)
        repo.falha = null; repo.pagina = PaginaComentarios(listOf(comentario(), comentario("666666666666666666666666")), null)
        modelo.carregarMais(); runCurrent()
        assertEquals(2, modelo.estado.value.comentarios.size); assertNull(modelo.estado.value.proximaPagina); modelo.fechar()
    }
    @Test fun perdaDeAcessoRemoveDadosDaConversaE401EncerraSessao() = runTest(dispatcher) {
        val repo = Fake(); var perdeu = false; var expirou = false
        val modelo = ModeloConversa(repo, "token", "eu", publicacao, { expirou = true }, aoPerderAcesso = { perdeu = true }); runCurrent()
        repo.falha = FalhaApi(403, "Sem acesso"); modelo.atualizar(); runCurrent()
        assertTrue(perdeu); assertTrue(modelo.estado.value.comentarios.isEmpty())
        repo.falha = FalhaApi(401, "Expirou"); modelo.atualizar(); runCurrent(); assertTrue(expirou); modelo.fechar()
    }
    @Test fun comentarioExigeTextoERejeitaMaisDe2000Caracteres() {
        assertEquals("Texto", validarComentario(" Texto "))
        assertTrue(runCatching { validarComentario(" ") }.isFailure)
        assertTrue(runCatching { validarComentario("a".repeat(2001)) }.isFailure)
    }
}
