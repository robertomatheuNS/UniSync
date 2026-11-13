package br.com.unisync.mobile.ui.auth

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.R
import br.com.unisync.mobile.data.ExcecaoAutenticacao
import br.com.unisync.mobile.data.RepositorioAutenticacao
import br.com.unisync.mobile.data.SessaoAutenticada
import kotlinx.coroutines.launch
import java.io.IOException
import org.json.JSONException

@Composable
fun TelaAutenticacao(
    cadastro: Boolean,
    repositorio: RepositorioAutenticacao,
    aoVoltar: () -> Unit,
    aoAlternarFormulario: () -> Unit,
    aoAutenticar: (SessaoAutenticada) -> Unit,
    modificador: Modifier = Modifier
) {
    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var professor by rememberSaveable { mutableStateOf(false) }
    var mostrarSenha by remember { mutableStateOf(false) }
    var enviado by remember { mutableStateOf(false) }
    var carregando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var cadastrado by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()
    val nomeInvalido = cadastro && nome.trim().length !in 2..100
    val emailInvalido = !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || email.trim().length > 180
    val senhaInvalida = if (cadastro) senha.length !in 8..128 else senha.isBlank()
    val codigoInvalido = cadastro && professor && codigo.isBlank()
    val erroConexao = stringResource(R.string.auth_connection_error)
    val respostaInvalida = stringResource(R.string.auth_invalid_response)
    BackHandler(onBack = aoVoltar)

    Box(
        modifier = modificador.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE9FBFF), Color(0xFF315CFF))))
            .safeDrawingPadding().imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)
                .widthIn(max = 480.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = aoVoltar) { Text(stringResource(R.string.auth_back)) }
            Surface(shape = RoundedCornerShape(28.dp), color = Color.White, contentColor = Color.Black) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                    Text(
                        stringResource(if (cadastro) R.string.auth_create else R.string.auth_enter),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    if (cadastro) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(selected = !professor, onClick = { professor = false }, enabled = !carregando,
                                label = { Text(stringResource(R.string.auth_student)) })
                            FilterChip(selected = professor, onClick = { professor = true }, enabled = !carregando,
                                label = { Text(stringResource(R.string.auth_teacher)) })
                        }
                        CampoAutenticacao(nome, { nome = it }, R.string.auth_name, carregando,
                            enviado && nomeInvalido, R.string.auth_name_error)
                    }
                    CampoAutenticacao(email, { email = it }, R.string.auth_email, carregando,
                        enviado && emailInvalido, R.string.auth_email_error, KeyboardType.Email)
                    OutlinedTextField(
                        value = senha, onValueChange = { senha = it },
                        label = { Text(stringResource(R.string.auth_password)) },
                        enabled = !carregando, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        isError = enviado && senhaInvalida,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (mostrarSenha) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { mostrarSenha = !mostrarSenha }) {
                                Text(stringResource(if (mostrarSenha) R.string.auth_hide else R.string.auth_show))
                            }
                        },
                        supportingText = {
                            if (enviado && senhaInvalida) Text(stringResource(
                                if (cadastro) R.string.auth_password_error else R.string.auth_required))
                        }
                    )
                    if (cadastro && professor) {
                        CampoAutenticacao(codigo, { codigo = it }, R.string.auth_code, carregando,
                            enviado && codigoInvalido, R.string.auth_required)
                    }
                    mensagem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        enabled = !carregando,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                        onClick = {
                            enviado = true
                            mensagem = null
                            if (!nomeInvalido && !emailInvalido && !senhaInvalida && !codigoInvalido) {
                                carregando = true
                                escopo.launch {
                                    try {
                                        if (cadastro) {
                                            repositorio.cadastrar(nome.trim(), email.trim(), senha,
                                                if (professor) codigo.trim() else null)
                                            senha = ""
                                            codigo = ""
                                            cadastrado = true
                                        } else {
                                            val sessao = repositorio.entrar(email.trim(), senha)
                                            senha = ""
                                            aoAutenticar(sessao)
                                        }
                                    } catch (erro: ExcecaoAutenticacao) {
                                        mensagem = erro.message
                                    } catch (_: IOException) {
                                        mensagem = erroConexao
                                    } catch (_: JSONException) {
                                        mensagem = respostaInvalida
                                    } finally {
                                        carregando = false
                                    }
                                }
                            }
                        }
                    ) {
                        if (carregando) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text(stringResource(if (cadastro) R.string.auth_create else R.string.auth_enter))
                    }
                    TextButton(onClick = aoAlternarFormulario, enabled = !carregando) {
                        Text(stringResource(if (cadastro) R.string.auth_have_account else R.string.auth_new_account))
                    }
                }
            }
        }
    }
    if (cadastrado) {
        AlertDialog(
            onDismissRequest = aoAlternarFormulario,
            title = { Text(stringResource(R.string.auth_registered)) },
            text = { Text(stringResource(R.string.auth_login_next)) },
            confirmButton = {
                TextButton(onClick = aoAlternarFormulario) {
                    Text(stringResource(R.string.auth_enter))
                }
            }
        )
    }
}

@Composable
private fun CampoAutenticacao(
    valor: String,
    aoAlterar: (String) -> Unit,
    rotulo: Int,
    carregando: Boolean,
    erro: Boolean,
    textoErro: Int,
    tipoTeclado: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = valor, onValueChange = aoAlterar,
        label = { Text(stringResource(rotulo)) },
        enabled = !carregando, isError = erro, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado),
        supportingText = { if (erro) Text(stringResource(textoErro)) },
        modifier = Modifier.fillMaxWidth()
    )
}
