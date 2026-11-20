package br.com.unisync.mobile.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.unisync.mobile.UniSyncApplication
import br.com.unisync.mobile.data.local.SessaoSegura
import br.com.unisync.mobile.data.remote.FalhaApi
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Revalida a sessão em cada execução; nenhum token é colocado nos argumentos do WorkManager. */
class SincronizacaoPeriodica(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        val aplicacao = applicationContext as UniSyncApplication
        val token = try { SessaoSegura(applicationContext).lerToken() ?: return Result.success() }
            catch (erro: CancellationException) { throw erro }
            catch (_: Exception) { return Result.success() }
        val sessao = try { SessaoAutenticada.doToken(token) }
            catch (_: Exception) { return Result.success() }
        return try {
            val academico = aplicacao.academico.carregar(token)
            if (academico.origemCache) return Result.retry()
            if (sessao.perfil == PerfilUsuario.ALUNO) aplicacao.tarefas.atualizar(sessao.usuarioId, token)
            aplicacao.lembretes.atualizar(sessao.usuarioId, token)
            aplicacao.oportunidades.atualizar(token)
            aplicacao.atualizarAlertas()
            Result.success()
        } catch (erro: CancellationException) { throw erro }
        catch (erro: FalhaApi) { if (erro.status == 401 || erro.status == 403) Result.success() else Result.retry() }
        catch (_: IOException) { Result.retry() }
        catch (_: Exception) { Result.failure() }
    }

    companion object {
        private const val NOME = "unisync.sincronizacao.periodica"
        fun agendar(contexto: Context) {
            val restricoes = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val trabalho = PeriodicWorkRequestBuilder<SincronizacaoPeriodica>(30, TimeUnit.MINUTES)
                .setConstraints(restricoes).build()
            WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(NOME, ExistingPeriodicWorkPolicy.KEEP, trabalho)
        }
        fun cancelar(contexto: Context) { WorkManager.getInstance(contexto).cancelUniqueWork(NOME) }
    }
}
