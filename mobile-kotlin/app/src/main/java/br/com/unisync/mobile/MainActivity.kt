package br.com.unisync.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelProvider
import br.com.unisync.mobile.data.RepositorioAutenticacao
import br.com.unisync.mobile.ui.auth.ModeloSessao
import br.com.unisync.mobile.ui.auth.TelaAutenticacao
import br.com.unisync.mobile.ui.home.TelaInicial
import br.com.unisync.mobile.ui.painel.TelaPainel
import br.com.unisync.mobile.ui.theme.TemaUniSync

class MainActivity : ComponentActivity() {
    override fun onCreate(estadoSalvo: Bundle?) {
        super.onCreate(estadoSalvo)
        enableEdgeToEdge()
        val modeloSessao = ViewModelProvider(this)[ModeloSessao::class.java]
        setContent {
            TemaUniSync {
                var tela by rememberSaveable { mutableStateOf("inicio") }
                val repositorio = remember { RepositorioAutenticacao() }
                val sessao = modeloSessao.sessao
                if (sessao != null) {
                    TelaPainel(
                        nome = sessao.nome,
                        perfil = sessao.perfil,
                        aoSair = {
                            modeloSessao.encerrar()
                            tela = "inicio"
                        }
                    )
                } else key(tela) {
                    when (tela) {
                        "inicio" -> TelaInicial(
                            aoEntrar = { tela = "login" },
                            aoCadastrar = { tela = "cadastro" }
                        )
                        else -> TelaAutenticacao(
                            cadastro = tela == "cadastro",
                            repositorio = repositorio,
                            aoAutenticar = modeloSessao::iniciar,
                            aoVoltar = { tela = "inicio" },
                            aoAlternarFormulario = { tela = if (tela == "cadastro") "login" else "cadastro" }
                        )
                    }
                }
            }
        }
    }
}
