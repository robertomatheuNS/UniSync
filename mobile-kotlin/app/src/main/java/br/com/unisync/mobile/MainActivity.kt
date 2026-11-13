package br.com.unisync.mobile

import android.os.Bundle
import android.content.Intent
import br.com.unisync.mobile.notificacoes.AberturaAlerta
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import br.com.unisync.mobile.data.RepositorioAutenticacao
import br.com.unisync.mobile.ui.auth.ModeloSessao
import br.com.unisync.mobile.ui.auth.TelaAutenticacao
import br.com.unisync.mobile.ui.home.TelaInicial
import br.com.unisync.mobile.ui.theme.TemaUniSync
import br.com.unisync.mobile.data.local.SessaoSegura
import br.com.unisync.mobile.ui.componentes.CarregandoConteudo
import br.com.unisync.mobile.ui.principal.AreaPrincipal
import br.com.unisync.mobile.data.local.ModoTema
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
    private var aberturaAlerta by mutableStateOf<AberturaAlerta?>(null)
    private fun lerAlerta(intent: Intent): AberturaAlerta? {
        val usuario = intent.getStringExtra("usuario_alerta") ?: return null
        val destino = intent.getStringExtra("destino_alerta") ?: return null
        val item = intent.getStringExtra("item_alerta") ?: return null
        return AberturaAlerta(usuario, destino, item)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        aberturaAlerta = lerAlerta(intent)
    }
    private val modeloSessao by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ModeloSessao::class.java))
                @Suppress("UNCHECKED_CAST")
                return ModeloSessao(RepositorioAutenticacao(), SessaoSegura(applicationContext),
                    limparCache = { (application as UniSyncApplication).limparDadosPrivados() }) as T
            }
        })[ModeloSessao::class.java]
    }

    override fun onStart() {
        super.onStart()
        modeloSessao.verificarValidade()
    }

    override fun onCreate(estadoSalvo: Bundle?) {
        super.onCreate(estadoSalvo)
        aberturaAlerta = if (estadoSalvo == null) lerAlerta(intent) else null
        enableEdgeToEdge()
        setContent {
            val aplicacao = application as UniSyncApplication
            val modoTema by aplicacao.preferencias.tema.collectAsState(initial = ModoTema.SISTEMA)
            TemaUniSync(temaEscuro = when (modoTema) {
                ModoTema.SISTEMA -> isSystemInDarkTheme()
                ModoTema.CLARO -> false
                ModoTema.ESCURO -> true
            }) {
                var tela by rememberSaveable { mutableStateOf("inicio") }
                val sessao = modeloSessao.sessao
                val estado = modeloSessao.estado
                LaunchedEffect(estado.solicitarLogin) {
                    if (estado.solicitarLogin) tela = "login"
                }
                if (estado.restaurando) {
                    CarregandoConteudo("Recuperando sessão…")
                } else if (sessao != null) {
                    key(sessao.token) { AreaPrincipal(
                        sessao = sessao,
                        aplicacao = aplicacao,
                        modoTema = modoTema,
                        aoExpirar = modeloSessao::sessaoRecusada,
                        mensagem = estado.mensagem,
                        carregando = estado.carregando,
                        aberturaAlerta = aberturaAlerta, aoConsumirAlerta = { aberturaAlerta = null; intent.removeExtra("usuario_alerta"); intent.removeExtra("destino_alerta"); intent.removeExtra("item_alerta") },
                        aoSair = {
                            modeloSessao.encerrar()
                            tela = "inicio"
                        }
                    ) }
                } else key(tela) {
                    when (tela) {
                        "inicio" -> TelaInicial(
                            aoEntrar = { modeloSessao.limparMensagem(); tela = "login" },
                            aoCadastrar = { modeloSessao.limparMensagem(); tela = "cadastro" }
                        )
                        else -> TelaAutenticacao(
                            cadastro = tela == "cadastro",
                            estado = estado,
                            aoEntrar = modeloSessao::entrar,
                            aoCadastrar = modeloSessao::cadastrar,
                            aoVoltar = { tela = "inicio" },
                            aoAlternarFormulario = {
                                modeloSessao.limparMensagem()
                                tela = if (tela == "cadastro") "login" else "cadastro"
                            }
                        )
                    }
                }
            }
        }
    }
}
