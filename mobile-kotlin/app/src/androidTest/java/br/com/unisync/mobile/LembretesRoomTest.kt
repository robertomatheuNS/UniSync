package br.com.unisync.mobile

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LembretesRoomTest {
    @Test fun migracaoQuatroParaCincoPreservaTarefaESincronizacao() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val nome = "migracao-lembretes-${java.util.UUID.randomUUID()}.db"
        contexto.openOrCreateDatabase(nome, 0, null).use { antigo ->
            antigo.execSQL("CREATE TABLE tarefas (id TEXT NOT NULL, alunoId TEXT NOT NULL, titulo TEXT NOT NULL, status TEXT NOT NULL, prioridade TEXT NOT NULL DEFAULT 'MEDIA', prazo TEXT, turmaId TEXT, disciplina TEXT, PRIMARY KEY(alunoId, id))")
            antigo.execSQL("CREATE TABLE sincronizacoes (alunoId TEXT NOT NULL PRIMARY KEY, atualizadoEm INTEGER NOT NULL)")
            antigo.execSQL("CREATE TABLE oportunidades (id TEXT NOT NULL PRIMARY KEY, titulo TEXT NOT NULL, professor TEXT NOT NULL, materia TEXT NOT NULL, descricao TEXT, tipo TEXT NOT NULL, dataEvento TEXT NOT NULL, professorId TEXT)")
            antigo.execSQL("INSERT INTO tarefas VALUES ('id', 'eu', 'Preservada', 'A_FAZER', 'ALTA', '2028-02-29', 'turma', 'Kotlin')")
            antigo.execSQL("INSERT INTO sincronizacoes VALUES ('eu', 123)")
            antigo.version = 4
        }
        val banco = Room.databaseBuilder(contexto, BancoUniSync::class.java, nome).addMigrations(BancoUniSync.MIGRACAO_4_5).build()
        try {
            assertEquals("2028-02-29", banco.tarefas().listar("eu").single().prazo)
            assertEquals(123L, banco.tarefas().atualizadoEm("eu"))
            val item = LembreteLocal("a", "eu", "Entrega", false, "2028-02-29T12:30:00Z")
            banco.lembretes().substituir("eu", listOf(item), 456)
            banco.lembretes().substituir("outra", listOf(item.copy(usuarioId = "outra")), 789)
            assertEquals(item, banco.lembretes().observar("eu").first().single())
            banco.lembretes().substituir("eu", emptyList(), 1000)
            assertTrue(banco.lembretes().listar("eu").isEmpty())
            assertEquals(1, banco.lembretes().listar("outra").size)
            assertEquals(1000L, banco.lembretes().atualizadoEm("eu"))
            banco.lembretes().limparTudo(); assertTrue(banco.lembretes().listar("outra").isEmpty())
        } finally { banco.close(); contexto.deleteDatabase(nome) }
    }
}
