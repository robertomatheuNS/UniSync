package br.com.unisync.mobile

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.core.content.FileProvider
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.data.*
import br.com.unisync.mobile.data.remote.*
import br.com.unisync.mobile.domain.academico.DadosTurma
import br.com.unisync.mobile.domain.mural.DadosPublicacao
import br.com.unisync.mobile.domain.mural.ArquivoEnvio
import br.com.unisync.mobile.domain.mural.TAMANHO_MAXIMO_ANEXO
import br.com.unisync.mobile.arquivos.ArquivosAnexos
import br.com.unisync.mobile.ui.mural.*
import br.com.unisync.mobile.ui.theme.TemaUniSync
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class MuralLocalE2ETest {
    @get:Rule val compose = createAndroidComposeRule<AtividadeTeste>()
    @Test fun alunoPublicaEditaComConflitoProfessorModeraEPaginasIsolamTurma() {
        val args = InstrumentationRegistry.getArguments()
        val codigo = args.getString("codigoProfessorMural").orEmpty()
        assumeTrue(args.getString("e2eMural") == "true" && codigo.isNotBlank())
        val id = UUID.randomUUID().toString()
        val senha = "Teste-$id-42!"
        val auth = RepositorioAutenticacao()
        fun cadastrar(nome: String, prefixo: String, codigoProfessor: String? = null) = runBlocking {
            val email = "$prefixo-$id@example.invalid"
            auth.cadastrar(nome, email, senha, codigoProfessor); auth.entrar(email, senha)
        }
        val professor = cadastrar("Professora Mural", "prof-mural", codigo)
        val aluno = cadastrar("Aluno Mural", "aluno-mural")
        val colega = cadastrar("Colega Mural", "colega-mural")
        val fora = cadastrar("Conta sem matrícula", "fora-mural")
        val academico = RepositorioAcademico()
        val repo = RepositorioMural()
        val interacoes = RepositorioInteracoesMural()
        val anexosRepo = RepositorioAnexosMural()
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val arquivos = ArquivosAnexos(contexto)
        val pdf = "%PDF-1.4\n1 0 obj << /Type /Catalog >> endobj\n%%EOF\n".toByteArray()
        val arquivoLocal = File(contexto.cacheDir, "anexos/roteiro-e2e.pdf").apply { parentFile!!.mkdirs(); writeBytes(pdf) }
        val uriArquivo = FileProvider.getUriForFile(contexto, "${contexto.packageName}.anexos", arquivoLocal)
        val seletor = SeletorArquivoTeste(uriArquivo)
        val api = ClienteAcademico()
        val turma = runBlocking {
            academico.criarTurma(professor.token, DadosTurma("Turma Mural", "Engenharia de Software")).also {
                academico.entrarTurma(aluno.token, it.codigo!!); academico.entrarTurma(colega.token, it.codigo)
            }
        }
        var arquivada = false
        var expirou = false
        val modeloAluno = ModeloMural(repo, aluno.token, aluno.usuarioId) { expirou = true }
        val modeloProfessor = ModeloMural(repo, professor.token, professor.usuarioId) { expirou = true }
        var professorAtivo by mutableStateOf(false)
        val conversaAtual = AtomicReference<ModeloConversa?>()
        val anexosAtuais = AtomicReference<ModeloAnexos?>()
        var arquivoAberto: ByteArray? = null
        fun esperarConversa() {
            compose.waitUntil(15000) { conversaAtual.get()?.estado?.value?.let { !it.lendo && !it.ocupado } == true }
            compose.waitUntil(15000) { anexosAtuais.get()?.estado?.value?.ocupado == false }
            assertNull(conversaAtual.get()!!.estado.value.erro)
            assertNull(anexosAtuais.get()!!.estado.value.erro)
        }
        try {
            compose.setContent { CompositionLocalProvider(LocalActivityResultRegistryOwner provides seletor) { TemaUniSync {
                val modelo = if (professorAtivo) modeloProfessor else modeloAluno
                val estado by modelo.estado.collectAsState()
                val sessao = if (professorAtivo) professor else aluno
                val conversa = remember(professorAtivo, estado.detalhe?.id) { estado.detalhe?.let { pub ->
                    ModeloConversa(interacoes, sessao.token, sessao.usuarioId, pub, { expirou = true }, modelo::atualizarCurtida,
                        { erro -> modelo.interacaoIndisponivel(pub.id, erro) })
                } }
                DisposableEffect(conversa) { onDispose { conversa?.fechar() } }
                SideEffect { conversaAtual.set(conversa) }
                val estadoConversa = conversa?.estado?.collectAsState()?.value
                val modeloAnexos = remember(professorAtivo, estado.detalhe?.id) { estado.detalhe?.let { pub ->
                    ModeloAnexos(anexosRepo, sessao.token, pub.id, { expirou = true }, { erro -> modelo.interacaoIndisponivel(pub.id, erro) })
                } }
                DisposableEffect(modeloAnexos) { onDispose { modeloAnexos?.fechar() } }
                SideEffect { anexosAtuais.set(modeloAnexos) }
                val estadoAnexos = modeloAnexos?.estado?.collectAsState()?.value
                key(professorAtivo) { TelaMural(estado, listOf(turma), { modelo.atualizar() }, modelo::atualizar, modelo::carregarMais,
                    modelo::abrir, modelo::fecharDetalhe, modelo::salvar, modelo::excluir, {}, estadoConversa,
                    { conversa?.atualizar() }, { conversa?.carregarMais() }, { id, versao, texto -> conversa?.salvar(id, versao, texto) },
                    { conversa?.excluir(it) }, { conversa?.curtir(it) }, estadoAnexos, { modeloAnexos?.atualizar() },
                    { uri, envioId -> modeloAnexos?.enviar(envioId) { arquivos.ler(Uri.parse(uri)) } },
                    { anexo -> modeloAnexos?.abrir(anexo) { arquivoAberto = it } }, { modeloAnexos?.excluir(it) }) }
            } } }
            esperar(modeloAluno); esperar(modeloProfessor)
            compose.onNode(hasText("Nova publicação") and hasClickAction()).performClick()
            compose.onNodeWithText("Título da publicação").performTextInput("Projeto colaborativo Android")
            compose.onNodeWithText("Conteúdo da publicação").performScrollTo().performTextInput("Vamos compartilhar as dúvidas da atividade.")
            compose.onNodeWithText("Salvar publicação").performClick()
            compose.waitUntil(15000) { !modeloAluno.estado.value.salvando && modeloAluno.estado.value.confirmacao == 1 }
            assertNull(modeloAluno.estado.value.erro)
            val item = modeloAluno.estado.value.detalhe!!
            assertEquals(aluno.usuarioId, item.autorId)
            esperarConversa()
            rolarConversa("Selecionar arquivo"); compose.onNodeWithText("Selecionar arquivo").performClick()
            rolarConversa("Enviar anexo"); compose.onNodeWithText("Enviar anexo").performClick()
            compose.waitUntil(15000) { anexosAtuais.get()?.estado?.value?.confirmacaoEnvio == 1 && anexosAtuais.get()?.estado?.value?.enviando == false }
            assertNull(anexosAtuais.get()!!.estado.value.erro)
            val anexoEnviado = anexosAtuais.get()!!.estado.value.itens.single()
            rolarConversa("Abrir anexo"); compose.onNodeWithText("Abrir anexo").performClick()
            compose.waitUntil(15000) { arquivoAberto != null && anexosAtuais.get()?.estado?.value?.abrindoId == null }
            assertArrayEquals(pdf, arquivoAberto)
            capturar("mural-anexos-aluno.png")
            runBlocking {
                assertArrayEquals(pdf, anexosRepo.baixar(colega.token, anexoEnviado))
                assertEquals(403, (runCatching { anexosRepo.baixar(fora.token, anexoEnviado) }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(403, (runCatching { anexosRepo.enviar(colega.token, item.id, ModeloAnexos.novoEnvioId(), ArquivoEnvio("a.pdf", "application/pdf", pdf)) {} }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(403, (runCatching { anexosRepo.excluir(colega.token, item.id, anexoEnviado.id) }.exceptionOrNull() as? FalhaApi)?.status)
                val grande = ByteArray(TAMANHO_MAXIMO_ANEXO).apply {
                    "%PDF-1.4\n".toByteArray().copyInto(this)
                    "\n%%EOF\n".toByteArray().copyInto(this, size - 7)
                }
                // Aproxima o limite de BSON com três arquivos de 4 MiB e verifica a projeção leve.
                anexosRepo.excluir(aluno.token, item.id, anexoEnviado.id)
                val ids = (1..3).map { ModeloAnexos.novoEnvioId() }
                ids.forEach { envio -> anexosRepo.enviar(aluno.token, item.id, envio, ArquivoEnvio("limite.pdf", "application/pdf", grande)) {} }
                assertEquals(3, anexosRepo.listar(aluno.token, item.id).itens.size)
                assertTrue(api.executar("GET", "/mural/listar", aluno.token).length < 5000)
                assertEquals(409, (runCatching { anexosRepo.enviar(aluno.token, item.id, ModeloAnexos.novoEnvioId(), ArquivoEnvio("extra.pdf", "application/pdf", pdf)) {} }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(ids.first(), anexosRepo.enviar(aluno.token, item.id, ids.first(), ArquivoEnvio("limite.pdf", "application/pdf", grande)) {}.id)
                ids.forEach { anexosRepo.excluir(aluno.token, item.id, it) }
                anexosRepo.enviar(aluno.token, item.id, anexoEnviado.id, ArquivoEnvio("roteiro-e2e.pdf", "application/pdf", pdf)) {}
            }
            capturar("mural-publicacao-aluno.png")
            rolarConversa("Curtir (0)"); compose.onNodeWithText("Curtir (0)").performClick()
            compose.waitUntil(15000) { conversaAtual.get()?.estado?.value?.curtiu == true && conversaAtual.get()?.estado?.value?.curtindo == false }
            runBlocking {
                assertEquals(1, interacoes.curtir(aluno.token, item.id, true).total)
                assertEquals(1, interacoes.curtir(aluno.token, item.id, true).total)
                assertEquals(1, repo.buscar(aluno.token, item.id).versao)
            }
            rolarConversa("Seu comentário"); compose.onNodeWithText("Seu comentário").performTextInput("Minha dúvida inicial")
            compose.onNodeWithText("Enviar comentário").assertIsDisplayed().performClick()
            compose.waitUntil(15000) { conversaAtual.get()?.estado?.value?.confirmacaoSalvo == 1 && conversaAtual.get()?.estado?.value?.salvando == false }
            val comentarioOriginal = conversaAtual.get()!!.estado.value.comentarios.single()
            runBlocking {
                assertEquals("Minha dúvida inicial", interacoes.listarComentarios(aluno.token, item.id, null).itens.single().conteudo)
                assertEquals(403, (runCatching { interacoes.editarComentario(colega.token, item.id, comentarioOriginal.id, 1, "Indevido") }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(403, (runCatching { interacoes.curtir(fora.token, item.id, true) }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(403, (runCatching { interacoes.listarComentarios(fora.token, item.id, null) }.exceptionOrNull() as? FalhaApi)?.status)
                repeat(20) { numero -> interacoes.comentar(colega.token, item.id, "Comentário paginado $numero") }
            }
            rolarConversa("Atualizar conversa"); compose.onNodeWithText("Atualizar conversa").performClick(); esperarConversa()
            assertEquals(20, conversaAtual.get()!!.estado.value.comentarios.size)
            rolarConversa("Carregar mais comentários"); compose.onNodeWithText("Carregar mais comentários").performClick()
            compose.waitUntil(15000) { conversaAtual.get()?.estado?.value?.comentarios?.size == 21 && conversaAtual.get()?.estado?.value?.carregandoMais == false }
            assertEquals(21, conversaAtual.get()!!.estado.value.comentarios.map { it.id }.distinct().size)
            runBlocking { interacoes.editarComentario(aluno.token, item.id, comentarioOriginal.id, 1, "Revisão de outro cliente") }
            rolarConversa("Editar comentário"); compose.onNode(hasText("Editar comentário") and hasClickAction()).performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement("Meu comentário em conflito")
            compose.onNodeWithText("Salvar comentário").assertIsDisplayed().performClick()
            compose.waitUntil(15000) { conversaAtual.get()?.estado?.value?.erro != null && conversaAtual.get()?.estado?.value?.salvando == false }
            compose.onNode(hasSetTextAction()).assertTextContains("Meu comentário em conflito")
            assertEquals(1, conversaAtual.get()!!.estado.value.confirmacaoSalvo)
            compose.onNodeWithText("Cancelar edição").performScrollTo().performClick(); compose.onNodeWithText("Descartar").performClick()
            rolarConversa("Atualizar conversa"); compose.onNodeWithText("Atualizar conversa").performClick(); esperarConversa()
            rolarConversa("Carregar mais comentários"); compose.onNodeWithText("Carregar mais comentários").performClick()
            compose.waitUntil(15000) { conversaAtual.get()?.estado?.value?.comentarios?.size == 21 && conversaAtual.get()?.estado?.value?.carregandoMais == false }
            rolarConversa("Editar comentário"); compose.onNode(hasText("Editar comentário") and hasClickAction()).performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement("Minha dúvida revisada")
            compose.onNodeWithText("Salvar comentário").assertIsDisplayed().performClick()
            compose.waitUntil(15000) { conversaAtual.get()?.estado?.value?.confirmacaoSalvo == 2 && conversaAtual.get()?.estado?.value?.salvando == false }
            assertEquals(3, conversaAtual.get()!!.estado.value.comentarios.first { it.id == comentarioOriginal.id }.versao)
            capturar("mural-conversa-aluno.png")
            runBlocking {
                assertFalse(repo.buscar(colega.token, item.id).podeEditar)
                assertFalse(repo.buscar(colega.token, item.id).podeExcluir)
                assertEquals(403, (runCatching { repo.editar(colega.token, item.id, 1, DadosPublicacao(turma.id, "Tentativa", "Texto")) }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(403, (runCatching { repo.excluir(colega.token, item.id, 1) }.exceptionOrNull() as? FalhaApi)?.status)
                assertTrue(repo.listar(fora.token, null, null).itens.isEmpty())
                assertEquals(403, (runCatching { repo.buscar(fora.token, item.id) }.exceptionOrNull() as? FalhaApi)?.status)
                // Simula outro cliente usando a API com uma versão mais recente.
                val corpo = JSONObject().put("titulo", item.titulo).put("conteudo", "Revisão feita por outro cliente").put("versao", 1)
                api.executar("PATCH", "/mural/atualizar/${item.id}", aluno.token, corpo.toString())
            }
            esperarConversa()
            rolarConversa("Editar publicação"); compose.onNodeWithText("Editar publicação").performClick()
            compose.onNodeWithText("Título da publicação").performTextReplacement("Rascunho em conflito")
            compose.onNodeWithText("Salvar publicação").performClick()
            compose.waitUntil(15000) { !modeloAluno.estado.value.salvando && modeloAluno.estado.value.erro != null }
            assertEquals(1, modeloAluno.estado.value.confirmacao)
            compose.onNodeWithText("Título da publicação").assertTextContains("Rascunho em conflito")
            compose.onNodeWithText("Cancelar").performClick(); compose.onNodeWithText("Descartar").performClick()
            compose.onNodeWithText("Fechar publicação").performClick()
            modeloAluno.atualizar(); esperar(modeloAluno)
            modeloAluno.abrir(item.id)
            compose.waitUntil(15000) { !modeloAluno.estado.value.carregandoDetalhe && modeloAluno.estado.value.detalhe?.versao == 2 }
            esperarConversa()
            rolarConversa("Editar publicação"); compose.onNodeWithText("Editar publicação").performClick()
            compose.onNodeWithText("Título da publicação").performTextReplacement("Projeto revisado Android")
            compose.onNodeWithText("Salvar publicação").performClick()
            compose.waitUntil(15000) { !modeloAluno.estado.value.salvando && modeloAluno.estado.value.detalhe?.versao == 3 }
            compose.onNodeWithText("Fechar publicação").performClick()
            runBlocking { repeat(20) { numero -> repo.publicar(colega.token, DadosPublicacao(turma.id, "Dúvida de paginação $numero", "Conteúdo de teste da turma.")) } }
            modeloAluno.atualizar(); esperar(modeloAluno)
            assertEquals(20, modeloAluno.estado.value.itens.size); assertNotNull(modeloAluno.estado.value.proximaPagina)
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Carregar mais publicações"))
            compose.onNodeWithText("Carregar mais publicações").performClick()
            compose.waitUntil(15000) { !modeloAluno.estado.value.carregandoMais && modeloAluno.estado.value.itens.size == 21 }
            assertEquals(21, modeloAluno.estado.value.itens.map { it.id }.distinct().size)
            assertNull(modeloAluno.estado.value.proximaPagina)
            modeloProfessor.abrir(item.id)
            compose.waitUntil(15000) { !modeloProfessor.estado.value.carregandoDetalhe && modeloProfessor.estado.value.detalhe != null }
            compose.runOnIdle { professorAtivo = true }
            esperarConversa()
            compose.onNodeWithText("Editar publicação").assertDoesNotExist()
            compose.onNodeWithText("Selecionar arquivo").assertDoesNotExist()
            rolarConversa("Remover anexo"); compose.onNodeWithText("Remover anexo").performClick()
            compose.onNodeWithText("Remover").performClick()
            compose.waitUntil(15000) { anexosAtuais.get()?.estado?.value?.confirmacaoExclusao == 1 && anexosAtuais.get()?.estado?.value?.excluindoId == null }
            runBlocking {
                assertEquals(404, (runCatching { anexosRepo.baixar(aluno.token, anexoEnviado) }.exceptionOrNull() as? FalhaApi)?.status)
                anexosRepo.enviar(aluno.token, item.id, anexoEnviado.id, ArquivoEnvio("roteiro-e2e.pdf", "application/pdf", pdf)) {}
            }
            val moderado = conversaAtual.get()!!.estado.value.comentarios.first()
            assertTrue(moderado.podeExcluir); assertFalse(moderado.podeEditar)
            compose.onNode(hasScrollAction() and hasAnyAncestor(isDialog())).performScrollToNode(hasTestTag("comentario:${moderado.id}"))
            compose.onNode(hasText("Excluir comentário") and hasAnyAncestor(hasTestTag("comentario:${moderado.id}"))).performScrollTo().performClick()
            compose.onNodeWithText("Excluir").performClick()
            compose.waitUntil(15000) { conversaAtual.get()?.estado?.value?.confirmacaoExcluido == 1 && conversaAtual.get()?.estado?.value?.salvando == false }
            runBlocking { assertTrue(interacoes.listarComentarios(aluno.token, item.id, null).itens.none { it.id == moderado.id }) }
            rolarConversa("Excluir publicação"); compose.onNodeWithText("Excluir publicação").performClick()
            compose.onNodeWithText("Excluir").performClick()
            compose.waitUntil(15000) { !modeloProfessor.estado.value.salvando && modeloProfessor.estado.value.confirmacao == 1 }
            runBlocking {
                assertEquals(404, (runCatching { repo.buscar(aluno.token, item.id) }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(404, (runCatching { interacoes.curtir(aluno.token, item.id, true) }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(404, (runCatching { interacoes.listarComentarios(aluno.token, item.id, null) }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(404, (runCatching { anexosRepo.baixar(aluno.token, anexoEnviado) }.exceptionOrNull() as? FalhaApi)?.status)
            }
            compose.onNode(hasText("Nova publicação") and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText("Título da publicação").performTextInput("Organização da apresentação")
            compose.onNodeWithText("Conteúdo da publicação").performScrollTo().performTextInput("Compartilhem suas ideias para a próxima aula.")
            compose.onNodeWithText("Salvar publicação").performClick()
            compose.waitUntil(15000) { !modeloProfessor.estado.value.salvando && modeloProfessor.estado.value.confirmacao == 2 }
            val publicacaoProfessor = modeloProfessor.estado.value.detalhe!!
            esperarConversa()
            capturar("mural-publicacao-professor.png")
            runBlocking {
                val leitura = repo.buscar(aluno.token, publicacaoProfessor.id)
                assertEquals("Professor", leitura.papelAutor); assertFalse(leitura.podeEditar)
                val anexoProfessor = anexosRepo.enviar(professor.token, publicacaoProfessor.id, ModeloAnexos.novoEnvioId(), ArquivoEnvio("a.pdf", "application/pdf", pdf)) {}
                academico.arquivarTurma(professor.token, turma.id); arquivada = true
                assertTrue(repo.listar(aluno.token, null, null).itens.isEmpty())
                assertEquals(404, (runCatching { repo.buscar(aluno.token, publicacaoProfessor.id) }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(404, (runCatching { interacoes.comentar(aluno.token, publicacaoProfessor.id, "Arquivada") }.exceptionOrNull() as? FalhaApi)?.status)
                assertEquals(404, (runCatching { anexosRepo.baixar(aluno.token, anexoProfessor) }.exceptionOrNull() as? FalhaApi)?.status)
            }
            assertFalse(expirou)
        } finally {
            modeloAluno.fechar(); modeloProfessor.fechar()
            arquivos.limpar()
            if (!arquivada) runBlocking { academico.arquivarTurma(professor.token, turma.id) }
        }
    }
    private fun esperar(modelo: ModeloMural) {
        compose.waitUntil(15000) { !modelo.estado.value.lendo && !modelo.estado.value.atualizando }
        assertNull(modelo.estado.value.erro)
    }
    private fun rolarConversa(texto: String) { compose.onNode(hasScrollAction() and hasAnyAncestor(isDialog())).performScrollToNode(hasText(texto)) }
    private fun capturar(nome: String) {
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, nome).outputStream().use {
            compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
