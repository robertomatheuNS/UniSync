package br.com.unisync.mobile.notificacoes

import br.com.unisync.mobile.data.SessaoAutenticada
import br.com.unisync.mobile.data.local.BancoUniSync
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface SaidaAlertas {
    fun permitido(): Boolean
    fun agendar(horario: Long?)
    fun publicar(alerta: Alerta, usuarioId: String): Boolean
    fun removerExceto(chaves: Set<String>)
    fun limpar()
}

/** O receiver consulta a sessão e o cache; nunca transporta token em intents. */
class GerenciadorAlertas(private val banco: BancoUniSync, private val sessaoAtual: suspend () -> SessaoAutenticada?,
    private val opcoes: suspend (String) -> OpcoesAlertas, private val saida: SaidaAlertas,
    private val agora: () -> Long = System::currentTimeMillis) {
    private val operacao = Mutex()
    private var suspenso = false
    suspend fun ativar() { operacao.withLock { suspenso = false }; atualizar() }
    suspend fun desativar() = operacao.withLock {
        suspenso = true; saida.limpar(); banco.alertas().limpar()
    }
    suspend fun atualizar(entregar: Boolean = false) = operacao.withLock {
        val instante = agora()
        val sessao = sessaoAtual()
        if (suspenso || sessao == null || sessao.expirada(instante / 1000)) {
            saida.limpar(); banco.alertas().limpar(); return@withLock
        }
        val usuarioId = sessao.usuarioId
        val config = opcoes(usuarioId)
        val alertas = alertasDoUsuario(usuarioId, config, banco.lembretes().listar(usuarioId), banco.tarefas().listar(usuarioId))
        val registros = registrosAtualizados(usuarioId, alertas, banco.alertas().listar(usuarioId), instante).toMutableList()
        saida.removerExceto(alertas.map { it.chave }.toSet())
        if (entregar && saida.permitido()) registros.indices.forEach { indice ->
            val registro = registros[indice]
            if (!registro.entregue && registro.horario <= instante) {
                val recente = instante - registro.horario <= 24 * 60 * 60 * 1000L
                if (!recente || saida.publicar(alertas.first { it.chave == registro.chave }, usuarioId))
                    registros[indice] = registro.copy(entregue = true)
            }
        }
        banco.alertas().substituir(registros)
        val proximo = registros.filter { !it.entregue }.minOfOrNull { it.horario }?.coerceAtLeast(instante + 1000)
        val validade = sessao.expiraEm * 1000
        saida.agendar(if (!saida.permitido() || alertas.isEmpty()) null else minOf(proximo ?: validade, validade))
    }
}
