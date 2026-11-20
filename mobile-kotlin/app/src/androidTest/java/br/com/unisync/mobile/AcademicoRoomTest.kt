package br.com.unisync.mobile

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.unisync.mobile.data.local.AvisoCache
import br.com.unisync.mobile.data.local.BancoUniSync
import br.com.unisync.mobile.data.local.TurmaCache
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AcademicoRoomTest {
    @Test fun cacheIsolaContasSubstituiListasVaziasELimpaNoLogout() = runBlocking {
        val banco = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            BancoUniSync::class.java).build()
        try {
            val dao = banco.academico()
            dao.substituir("aluno", listOf(TurmaCache("aluno", "turma", "{}")),
                listOf(AvisoCache("aluno", "aviso", "{}")))
            dao.substituir("professor", listOf(TurmaCache("professor", "outra", "{}")), emptyList())
            assertEquals(listOf("turma"), dao.listarTurmas("aluno").map { it.id })
            assertEquals(listOf("aviso"), dao.listarAvisos("aluno").map { it.id })
            assertEquals(listOf("outra"), dao.listarTurmas("professor").map { it.id })
            val anterior = dao.atualizadoEm("aluno")
            assertNotNull(anterior)
            dao.substituir("aluno", emptyList(), emptyList())
            assertTrue(dao.listarTurmas("aluno").isEmpty())
            assertTrue(dao.listarAvisos("aluno").isEmpty())
            assertNotNull(dao.atualizadoEm("aluno"))
            assertEquals(1, dao.listarTurmas("professor").size)
            dao.limparTudo()
            assertTrue(dao.listarTurmas("professor").isEmpty())
            assertNull(dao.atualizadoEm("aluno"))
            assertNull(dao.atualizadoEm("professor"))
        } finally { banco.close() }
    }
}
