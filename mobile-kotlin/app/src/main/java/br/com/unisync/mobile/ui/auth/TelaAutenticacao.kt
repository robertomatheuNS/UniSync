package br.com.unisync.mobile.ui.auth

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import br.com.unisync.mobile.ui.theme.TemaUniSync
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.com.unisync.mobile.R
import br.com.unisync.mobile.ui.componentes.MensagemDeErro
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

@Composable
fun TelaAutenticacao(
    cadastro: Boolean,
    estado: EstadoAutenticacao,
    aoEntrar: (String, String) -> Unit,
    aoCadastrar: (String, String, String, String?) -> Unit,
    aoVoltar: () -> Unit,
    aoAlternarFormulario: () -> Unit,
    modificador: Modifier = Modifier
) {
    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var professor by rememberSaveable { mutableStateOf(false) }
    var mostrarSenha by remember { mutableStateOf(false) }
    var enviado by remember { mutableStateOf(false) }
    var confirmarSaida by remember { mutableStateOf(false) }
    val carregando = estado.carregando
    val mensagem = estado.mensagem
    val cadastrado = estado.cadastrado
    val focoNome = remember { FocusRequester() }
    val focoEmail = remember { FocusRequester() }
    val focoSenha = remember { FocusRequester() }
    val focoCodigo = remember { FocusRequester() }
    val nomeInvalido = cadastro && nome.trim().length !in 2..100
    val emailInvalido = !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || email.trim().length > 180
    val senhaInvalida = if (cadastro) senha.length !in 8..128 else senha.isBlank()
    val codigoInvalido = cadastro && professor && codigo.isBlank()
    LaunchedEffect(cadastrado) {
        if (cadastrado) { senha = ""; codigo = "" }
    }
    val voltar: () -> Unit = {
        if (cadastro && listOf(nome, email, senha, codigo).any { it.isNotBlank() }) confirmarSaida = true
        else aoVoltar()
    }
    BackHandler(onBack = voltar)

    Box(
        modifier = modificador.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding().imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)
                .widthIn(max = 480.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = voltar) { Text(stringResource(R.string.auth_back)) }
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                    Text(
                        stringResource(if (cadastro) R.string.auth_create else R.string.auth_enter),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() }
                    )
                    if (cadastro) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(selected = !professor, onClick = { professor = false }, enabled = !carregando,
                                label = { Text(stringResource(R.string.auth_student)) })
                            FilterChip(selected = professor, onClick = { professor = true }, enabled = !carregando,
                                label = { Text(stringResource(R.string.auth_teacher)) })
                        }
                        CampoAutenticacao(nome, { nome = it }, R.string.auth_name, carregando,
                            enviado && nomeInvalido, R.string.auth_name_error, foco = focoNome)
                    }
                    CampoAutenticacao(email, { email = it }, R.string.auth_email, carregando,
                        enviado && emailInvalido, R.string.auth_email_error, KeyboardType.Email, focoEmail)
                    OutlinedTextField(
                        value = senha, onValueChange = { senha = it },
                        label = { Text(stringResource(R.string.auth_password)) },
                        enabled = !carregando, singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(focoSenha),
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
                            enviado && codigoInvalido, R.string.auth_required, foco = focoCodigo)
                    }
                    mensagem?.let { MensagemDeErro(it) }
                    Button(
                        enabled = !carregando,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        onClick = {
                            enviado = true
                            if (!nomeInvalido && !emailInvalido && !senhaInvalida && !codigoInvalido) {
                                if (cadastro) aoCadastrar(nome, email, senha, if (professor) codigo else null)
                                else aoEntrar(email, senha)
                            } else {
                                when {
                                    nomeInvalido -> focoNome
                                    emailInvalido -> focoEmail
                                    senhaInvalida -> focoSenha
                                    else -> focoCodigo
                                }.requestFocus()
                            }
                        }
                    ) {
                        if (carregando) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
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
    if (confirmarSaida) {
        AlertDialog(
            onDismissRequest = { confirmarSaida = false },
            title = { Text("Descartar cadastro?") },
            text = { Text("Os dados preenchidos serão perdidos.") },
            confirmButton = { TextButton(onClick = aoVoltar) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = { confirmarSaida = false }) { Text("Continuar preenchendo") } }
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 640, heightDp = 360, fontScale = 1.5f)
@Composable
private fun PreviaAutenticacao() {
    TemaUniSync {
        TelaAutenticacao(false, EstadoAutenticacao(restaurando = false), { _, _ -> }, { _, _, _, _ -> }, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PreviaAutenticacaoEscura() {
    TemaUniSync(temaEscuro = true) {
        TelaAutenticacao(true, EstadoAutenticacao(restaurando = false), { _, _ -> }, { _, _, _, _ -> }, {}, {})
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
    tipoTeclado: KeyboardType = KeyboardType.Text,
    foco: FocusRequester = remember { FocusRequester() }
) {
    OutlinedTextField(
        value = valor, onValueChange = aoAlterar,
        label = { Text(stringResource(rotulo)) },
        enabled = !carregando, isError = erro, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado),
        supportingText = { if (erro) Text(stringResource(textoErro)) },
        modifier = Modifier.fillMaxWidth().focusRequester(foco)
    )
}
