package br.com.unisync.mobile.ui.principal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import br.com.unisync.mobile.UniSyncApplication
import br.com.unisync.mobile.data.*
import br.com.unisync.mobile.data.local.ModoTema
import br.com.unisync.mobile.domain.tarefas.StatusTarefa
import br.com.unisync.mobile.ui.componentes.*
import br.com.unisync.mobile.ui.tarefas.*
import br.com.unisync.mobile.ui.oportunidades.*
import br.com.unisync.mobile.ui.academico.*
import kotlinx.coroutines.launch

private data class Destino(val rota: String, val titulo: String, val rotulo: String = titulo)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaPrincipal(sessao: SessaoAutenticada, aplicacao: UniSyncApplication, modoTema: ModoTema,
    aoExpirar: () -> Unit, aoSair: () -> Unit, mensagem: String?, carregando: Boolean) {
    val nav = rememberNavController()
    val destinos = remember(sessao.perfil) { if (sessao.perfil == PerfilUsuario.ALUNO)
        listOf(Destino("inicio", "Início"), Destino("calendario", "Calendário"), Destino("tarefas", "Tarefas"),
            Destino("oportunidades", "Oportunidades", "Explorar"), Destino("perfil", "Perfil"))
        else listOf(Destino("inicio", "Início"), Destino("turmas", "Turmas"), Destino("avisos", "Avisos"),
            Destino("oportunidades", "Oportunidades", "Explorar"), Destino("perfil", "Perfil")) }
    val entrada by nav.currentBackStackEntryAsState()
    val rota = entrada?.destination?.route ?: "inicio"
    val modelo = remember(sessao.token) { if (sessao.perfil == PerfilUsuario.ALUNO)
        ModeloTarefas(sessao, aplicacao.tarefas, aoExpirar) else null }
    DisposableEffect(modelo) { onDispose { modelo?.fechar() } }
    val modeloOportunidades = remember(sessao.token) { ModeloOportunidades(aplicacao.oportunidades, sessao.token, aoExpirar,
        usuarioId = sessao.usuarioId, nomeProfessor = sessao.nome, professor = sessao.perfil == PerfilUsuario.PROFESSOR) }
    val oportunidades by modeloOportunidades.estado.collectAsStateWithLifecycle()
    DisposableEffect(modeloOportunidades) { onDispose { modeloOportunidades.fechar() } }
    val professor = sessao.perfil == PerfilUsuario.PROFESSOR
    val professorId = sessao.usuarioId.takeIf { professor }
    val modeloAcademico = remember(sessao.token) { ModeloAcademico(aplicacao.academico, sessao.token,
        sessao.usuarioId, professor, aoExpirar) }
    val academico by modeloAcademico.estado.collectAsStateWithLifecycle()
    DisposableEffect(modeloAcademico) { onDispose { modeloAcademico.fechar() } }
    var filtroAvisos by rememberSaveable { mutableStateOf<String?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, modelo, modeloOportunidades, modeloAcademico) {
        val observador = LifecycleEventObserver { _, evento -> if (evento == Lifecycle.Event.ON_START) {
            modelo?.atualizar(); modeloOportunidades.atualizar(); modeloAcademico.atualizar()
        } }
        lifecycle.addObserver(observador)
        onDispose { lifecycle.removeObserver(observador) }
    }
    val estado = modelo?.estado?.collectAsStateWithLifecycle()?.value ?: EstadoTarefas(lendo = false)
    val escopo = rememberCoroutineScope()
    var erroPreferencia by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(rota) { if (rota == "turmas" || rota == "avisos") modeloAcademico.atualizar() }
    Scaffold(topBar = { TopAppBar(title = { Text(when (rota) {
        "turmas" -> "Turmas"; "avisos" -> "Avisos"; else -> destinos.find { it.rota == rota }?.titulo ?: "UniSync"
    }) }, navigationIcon = {
        if (!professor && (rota == "turmas" || rota == "avisos")) TextButton(onClick = { nav.popBackStack() }) { Text("Voltar") }
    }) },
        bottomBar = { NavigationBar { destinos.forEachIndexed { indice, destino ->
            NavigationBarItem(selected = rota == destino.rota, onClick = {
                nav.navigate(destino.rota) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true; restoreState = true
                }
            }, icon = { Icon(iconeDestino(indice), contentDescription = null) }, label = { Text(destino.rotulo) },
                modifier = Modifier.semantics { contentDescription = destino.titulo })
        } } }) { padding ->
        NavHost(navController = nav, startDestination = "inicio", modifier = Modifier.padding(padding)) {
            composable("inicio") {
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { Text("Olá, ${sessao.nome}", style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() }) }
                    item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Vida acadêmica", style = MaterialTheme.typography.titleLarge)
                        Text("${academico.turmas.size} turmas • ${if (professor) academico.avisos.size.toString() + " avisos publicados" else academico.avisos.count { !it.lido }.toString() + " avisos não lidos"}")
                        if (academico.lendo) LinearProgressIndicator(Modifier.fillMaxWidth())
                        academico.erro?.let { MensagemDeErro(it) }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { nav.navigate("turmas") }) { Text("Minhas turmas") }
                            OutlinedButton(onClick = { filtroAvisos = null; nav.navigate("avisos") }) { Text("Ver avisos") }
                        }
                    } } }
                    if (modelo != null) {
                        item { StatusSincronizacao(estado) }
                        estado.erro?.let { item { MensagemDeErro(it) } }
                        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Suas tarefas", style = MaterialTheme.typography.titleLarge)
                            StatusTarefa.entries.forEach { status -> Text("${status.rotulo}: ${estado.itens.count { it.status == status }}") }
                            Button(onClick = { nav.navigate("tarefas") }) { Text("Organizar tarefas") }
                            TextButton(onClick = modelo::atualizar, enabled = !estado.atualizando) { Text("Atualizar") }
                        } } }
                    }
                    item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Oportunidades", style = MaterialTheme.typography.titleLarge)
                        Text("${oportunidades.itens.count { !it.encerrada() }} próximos eventos entre os itens carregados")
                        Button(onClick = { nav.navigate("oportunidades") }) { Text("Explorar oportunidades") }
                    } } }
                }
            }
            composable("tarefas") { modelo?.let { TelaTarefas(estado, it::atualizar, it::salvar, it::excluir) } }
            composable("oportunidades") { TelaOportunidades(oportunidades, modeloOportunidades::atualizar,
                professorId = sessao.usuarioId.takeIf { sessao.perfil == PerfilUsuario.PROFESSOR },
                aoSalvar = modeloOportunidades::salvar, aoExcluir = modeloOportunidades::excluir) }
            composable("calendario") { TelaCalendario(oportunidades, modeloOportunidades::atualizar) }
            composable("perfil") {
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { Text(sessao.nome, style = MaterialTheme.typography.titleLarge); Text(sessao.perfil.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    item { Text("Aparência", style = MaterialTheme.typography.titleMedium) }
                    items(ModoTema.entries.size) { indice -> val modo = ModoTema.entries[indice]
                        FilterChip(selected = modo == modoTema, onClick = { escopo.launch {
                            try { aplicacao.preferencias.definirTema(modo); erroPreferencia = null }
                            catch (erro: kotlinx.coroutines.CancellationException) { throw erro }
                            catch (_: Exception) { erroPreferencia = "Não foi possível salvar a preferência. Tente novamente." }
                        } }, label = { Text(when(modo) { ModoTema.SISTEMA -> "Usar tema do sistema"; ModoTema.CLARO -> "Claro"; ModoTema.ESCURO -> "Escuro" }) })
                    }
                    erroPreferencia?.let { item { MensagemDeErro(it) } }
                    mensagem?.let { item { MensagemDeErro(it) } }
                    item { OutlinedButton(onClick = aoSair, enabled = !carregando && !estado.salvando && !estado.atualizando && !oportunidades.salvando && !oportunidades.atualizando && !academico.salvando && !academico.atualizando) { Text("Sair da conta") } }
                }
            }
            composable("turmas") { TelaTurmas(academico, professorId, modeloAcademico::atualizar,
                modeloAcademico::salvarTurma, modeloAcademico::entrar, modeloAcademico::arquivar,
                aoVerAvisos = { filtroAvisos = it; nav.navigate("avisos") }, aoLimparMensagem = modeloAcademico::limparMensagem) }
            composable("avisos") { TelaAvisos(academico, professorId, filtroAvisos, modeloAcademico::atualizar,
                modeloAcademico::salvarAviso, modeloAcademico::excluirAviso, modeloAcademico::marcarLido,
                aoAbrirTurmas = { nav.navigate("turmas") }, aoLimparMensagem = modeloAcademico::limparMensagem) }
        }
    }
}

// Vetores nativos: uma única linguagem visual, sem fontes de ícones externas.
private fun iconeDestino(indice: Int): ImageVector = ImageVector.Builder("Destino$indice", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black)) {
        when (indice) {
            0 -> { moveTo(3f,10f); lineTo(12f,2f); lineTo(21f,10f); lineTo(21f,22f); lineTo(14f,22f); lineTo(14f,15f); lineTo(10f,15f); lineTo(10f,22f); lineTo(3f,22f); close() }
            1 -> { moveTo(4f,4f); lineTo(20f,4f); lineTo(20f,21f); lineTo(4f,21f); close(); moveTo(6f,9f); lineTo(6f,19f); lineTo(18f,19f); lineTo(18f,9f); close() }
            2 -> { moveTo(4f,4f); lineTo(20f,4f); lineTo(20f,7f); lineTo(4f,7f); close(); moveTo(4f,11f); lineTo(20f,11f); lineTo(20f,14f); lineTo(4f,14f); close(); moveTo(4f,18f); lineTo(20f,18f); lineTo(20f,21f); lineTo(4f,21f); close() }
            3 -> { moveTo(12f,2f); lineTo(22f,12f); lineTo(12f,22f); lineTo(2f,12f); close() }
            else -> { moveTo(12f,3f); curveTo(5f,3f,5f,12f,12f,12f); curveTo(19f,12f,19f,3f,12f,3f); close(); moveTo(3f,22f); curveTo(3f,10f,21f,10f,21f,22f); close() }
        }
    }
}.build()
