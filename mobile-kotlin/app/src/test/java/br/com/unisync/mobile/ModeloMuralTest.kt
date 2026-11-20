package br.com.unisync.mobile

import br.com.unisync.mobile.data.remote.FalhaApi
import br.com.unisync.mobile.domain.mural.*
import br.com.unisync.mobile.ui.mural.ModeloMural
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloMuralTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun encerrar() { Dispatchers.resetMain() }
    private val turma = "111111111111111111111111"
    private fun item(id: String = "222222222222222222222222", autor: String = "eu") = Publicacao(id, turma, "Turma A", "Kotlin", autor,
        "Ana", "Aluno", "Título", "Resumo", Instant.EPOCH, Instant.EPOCH, 1, autor == "eu", autor == "eu", "Conteúdo completo")
    private inner class Fake : Mural {
        var pagina = PaginaPublicacoes(listOf(item().copy(conteudo = null)), "333333333333333333333333")
        var detalhe = item()
        var falha: Exception? = null
        var cria = 0; var edita = 0; var exclui = 0
        var versaoRecebida: Int? = null
        var adiarEscrita = false
        var lista: (suspend (String?, String?) -> PaginaPublicacoes)? = null
        override suspend fun listar(token: String, turmaId: String?, antes: String?): PaginaPublicacoes {
            falha?.let { throw it }; return lista?.invoke(turmaId, antes) ?: pagina
        }
        override suspend fun buscar(token: String, id: String): Publicacao { falha?.let { throw it }; return detalhe }
        override suspend fun publicar(token: String, dados: DadosPublicacao): Publicacao {
            cria++; if (adiarEscrita) delay(1000); falha?.let { throw it }; return detalhe.copy(titulo = dados.titulo, conteudo = dados.conteudo)
        }
        override suspend fun editar(token: String, id: String, versao: Int, dados: DadosPublicacao): Publicacao {
            edita++; versaoRecebida = versao; falha?.let { throw it }; return detalhe.copy(versao = versao + 1, titulo = dados.titulo, conteudo = dados.conteudo)
        }
        override suspend fun excluir(token: String, id: String, versao: Int) { exclui++; versaoRecebida = versao; falha?.let { throw it } }
    }
    @Test fun falhaPreservaPaginaECursorMasAcessoNegadoLimpaDadosPrivados() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloMural(repo, "token", "eu") {}; runCurrent()
        val anterior = modelo.estado.value.atualizadoEm
        repo.falha = IOException(); modelo.atualizar(); runCurrent()
        assertEquals(1, modelo.estado.value.itens.size); assertEquals(anterior, modelo.estado.value.atualizadoEm)
        assertNotNull(modelo.estado.value.proximaPagina); assertNotNull(modelo.estado.value.erro)
        repo.falha = FalhaApi(403, "Sem acesso"); modelo.atualizar(); runCurrent()
        assertTrue(modelo.estado.value.itens.isEmpty()); assertNull(modelo.estado.value.proximaPagina)
        modelo.fechar()
    }
    @Test fun paginaSeguinteDeduplicaSemPerderPrimeiraPaginaEFalhaPermiteTentarNovamente() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloMural(repo, "token", "eu") {}; runCurrent()
        repo.falha = IOException(); modelo.carregarMais(); runCurrent()
        assertEquals(1, modelo.estado.value.itens.size); assertNotNull(modelo.estado.value.proximaPagina)
        repo.falha = null; repo.pagina = PaginaPublicacoes(listOf(item(), item("444444444444444444444444")), null)
        modelo.carregarMais(); runCurrent()
        assertEquals(2, modelo.estado.value.itens.size); assertNull(modelo.estado.value.proximaPagina)
        modelo.fechar()
    }
    @Test fun consultaAntigaNaoMisturaPublicacoesAposTrocarFiltro() = runTest(dispatcher) {
        val antiga = CompletableDeferred<PaginaPublicacoes>()
        val novaTurma = "555555555555555555555555"
        val repo = Fake().apply { lista = { filtro, _ ->
            if (filtro == null) withContext(NonCancellable) { antiga.await() }
            else PaginaPublicacoes(listOf(item().copy(turmaId = novaTurma)), null)
        } }
        val modelo = ModeloMural(repo, "token", "eu") {}; runCurrent()
        modelo.atualizar(novaTurma); runCurrent()
        antiga.complete(PaginaPublicacoes(listOf(item()), null)); runCurrent()
        assertEquals(novaTurma, modelo.estado.value.filtroTurma)
        assertTrue(modelo.estado.value.itens.all { it.turmaId == novaTurma })
        modelo.fechar()
    }
    @Test fun conflitoPreservaRascunhoSemConfirmarEEnviaVersaoDoFormulario() = runTest(dispatcher) {
        val repo = Fake(); val modelo = ModeloMural(repo, "token", "eu") {}; runCurrent()
        modelo.abrir(repo.detalhe.id); runCurrent()
        repo.falha = FalhaApi(409, "Conflito")
        modelo.salvar(repo.detalhe.id, 1, DadosPublicacao(turma, "Revisado", "Meu rascunho")); runCurrent()
        assertEquals(1, repo.versaoRecebida); assertEquals(0, modelo.estado.value.confirmacao)
        assertEquals("Conteúdo completo", modelo.estado.value.detalhe?.conteudo)
        assertTrue(modelo.estado.value.erro!!.contains("publicação mudou"))
        modelo.fechar()
    }
    @Test fun naoEditaConteudoDeOutroAutorEBloqueiaDuploEnvio() = runTest(dispatcher) {
        val repo = Fake().apply { detalhe = item(autor = "outro") }
        val modelo = ModeloMural(repo, "token", "eu") {}; runCurrent()
        modelo.abrir(repo.detalhe.id); runCurrent()
        modelo.salvar(repo.detalhe.id, 1, DadosPublicacao(turma, "Tentativa", "Texto")); runCurrent()
        assertEquals(0, repo.edita)
        repo.adiarEscrita = true
        modelo.salvar(null, null, DadosPublicacao(turma, "Título novo", "Texto"))
        modelo.salvar(null, null, DadosPublicacao(turma, "Duplicado", "Texto")); runCurrent()
        assertEquals(1, repo.cria)
        advanceUntilIdle(); assertEquals(1, modelo.estado.value.confirmacao)
        modelo.fechar()
    }
    @Test fun tokenRecusadoSolicitaNovaSessao() = runTest(dispatcher) {
        var expirou = false
        val modelo = ModeloMural(Fake().apply { falha = FalhaApi(401, "Expirou") }, "token", "eu") { expirou = true }
        runCurrent(); assertTrue(expirou); modelo.fechar()
    }
    @Test fun dadosSaoNormalizadosEExigemTurmaTituloEConteudo() {
        assertEquals("Título", DadosPublicacao(turma, " Título ", " Texto ").validar().titulo)
        assertTrue(runCatching { DadosPublicacao("", "Título", "Texto").validar() }.isFailure)
        assertTrue(runCatching { DadosPublicacao(turma, "  ", "Texto").validar() }.isFailure)
        assertTrue(runCatching { DadosPublicacao(turma, "Título", "a".repeat(5001)).validar() }.isFailure)
    }
}
