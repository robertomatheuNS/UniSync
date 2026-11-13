package br.com.unisync.mobile

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.data.*
import br.com.unisync.mobile.data.local.*
import br.com.unisync.mobile.data.remote.ClienteAcademico
import br.com.unisync.mobile.domain.tarefas.StatusTarefa
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

/** Opt-in: cria uma conta técnica no backend local e remove as tarefas ao finalizar. */
class FluxoLocalE2ETest {
    @Test fun cadastroLoginSessaoPersistidaECrudTarefaNasApisReais() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("e2eLocal") == "true")
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val banco = Room.inMemoryDatabaseBuilder(contexto, BancoUniSync::class.java).build()
        val cofre = SessaoSegura(contexto, "sessao_e2e")
        val api = RepositorioAutenticacao()
        val identificador = UUID.randomUUID().toString()
        val email = "mobile-$identificador@example.invalid"
        val senha = "Teste-$identificador-42!"
        val tarefas = RepositorioTarefas(banco.tarefas(), ClienteAcademico())
        var idCriada: String? = null
        var sessao: SessaoAutenticada? = null
        try {
            api.cadastrar("Teste Android", email, senha, null)
            sessao = api.entrar(email, senha)
            assertTrue(sessao.usuarioId.isNotBlank())
            cofre.salvarToken(sessao.token)
            val restaurada = SessaoAutenticada.doToken(SessaoSegura(contexto, "sessao_e2e").lerToken()!!)
            assertEquals(sessao.usuarioId, restaurada.usuarioId)
            tarefas.criar(restaurada.usuarioId, restaurada.token, "Tarefa E2E Android")
            idCriada = tarefas.observar(restaurada.usuarioId).first().single().id
            tarefas.editar(restaurada.usuarioId, restaurada.token, idCriada, "Tarefa revisada", StatusTarefa.EM_ANDAMENTO)
            // Nova consulta à API comprova que a escrita saiu do cache local.
            tarefas.atualizar(restaurada.usuarioId, restaurada.token)
            val atualizada = tarefas.observar(restaurada.usuarioId).first().single()
            assertEquals("Tarefa revisada", atualizada.titulo)
            assertEquals(StatusTarefa.EM_ANDAMENTO, atualizada.status)
            tarefas.excluir(restaurada.usuarioId, restaurada.token, idCriada)
            idCriada = null
            tarefas.atualizar(restaurada.usuarioId, restaurada.token)
            assertTrue(tarefas.observar(restaurada.usuarioId).first().isEmpty())
            tarefas.limparCache()
            cofre.limpar()
            assertNull(cofre.lerToken())
        } finally {
            if (idCriada != null && sessao != null) tarefas.excluir(sessao.usuarioId, sessao.token, idCriada)
            cofre.limpar()
            banco.close()
        }
    }
}
