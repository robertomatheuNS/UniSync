package br.com.unisync.mobile

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TarefasRoomTest {
    @Test fun migracaoPreservaTarefasDaVersaoAnterior() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val nome = "migracao-${java.util.UUID.randomUUID()}.db"
        contexto.openOrCreateDatabase(nome, 0, null).use { antigo ->
            antigo.execSQL("CREATE TABLE tarefas (id TEXT NOT NULL, alunoId TEXT NOT NULL, titulo TEXT NOT NULL, status TEXT NOT NULL, PRIMARY KEY(alunoId, id))")
            antigo.execSQL("CREATE TABLE sincronizacoes (alunoId TEXT NOT NULL PRIMARY KEY, atualizadoEm INTEGER NOT NULL)")
            antigo.execSQL("INSERT INTO tarefas VALUES ('id', 'aluno', 'Tarefa preservada', 'A_FAZER')")
            antigo.version = 1
        }
        val banco = Room.databaseBuilder(contexto, BancoUniSync::class.java, nome).addMigrations(BancoUniSync.MIGRACAO_1_2).build()
        try {
            assertEquals("Tarefa preservada", banco.tarefas().observar("aluno").first().single().titulo)
            assertTrue(banco.oportunidades().observar().first().isEmpty())
        } finally { banco.close(); contexto.deleteDatabase(nome) }
    }

    @Test fun cacheIsolaAlunosESubstituiListaInclusiveQuandoVazia() = runBlocking {
        val banco = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, BancoUniSync::class.java).build()
        try {
            val dao = banco.tarefas()
            dao.substituir("aluno-a", listOf(TarefaLocal("id-a", "aluno-a", "Estudar", "A_FAZER")), 100)
            dao.substituir("aluno-b", listOf(TarefaLocal("id-b", "aluno-b", "Revisar", "CONCLUIDO")), 200)
            assertEquals(listOf("id-a"), dao.observar("aluno-a").first().map { it.id })
            assertEquals(listOf("id-b"), dao.observar("aluno-b").first().map { it.id })
            dao.substituir("aluno-a", emptyList(), 300)
            assertTrue(dao.observar("aluno-a").first().isEmpty())
            assertEquals(300L, dao.atualizadoEm("aluno-a"))
            assertEquals(1, dao.observar("aluno-b").first().size)
            dao.limparTudo()
            assertTrue(dao.observar("aluno-b").first().isEmpty())
            assertNull(dao.atualizadoEm("aluno-b"))
        } finally { banco.close() }
    }
}
