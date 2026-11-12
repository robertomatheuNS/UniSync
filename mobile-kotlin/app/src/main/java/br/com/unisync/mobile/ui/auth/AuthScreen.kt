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
import br.com.unisync.mobile.data.AuthException
import br.com.unisync.mobile.data.AuthRepository
import kotlinx.coroutines.launch
import java.io.IOException
import org.json.JSONException

@Composable
fun AuthScreen(
    registration: Boolean,
    repository: AuthRepository,
    onBack: () -> Unit,
    onSwitchMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var professor by rememberSaveable { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var token by remember { mutableStateOf<String?>(null) }
    var registered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val invalidName = registration && name.trim().length !in 2..100
    val invalidEmail = !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || email.trim().length > 180
    val invalidPassword = if (registration) password.length !in 8..128 else password.isBlank()
    val invalidCode = registration && professor && code.isBlank()
    val connectionError = stringResource(R.string.auth_connection_error)
    BackHandler(onBack = onBack)

    Box(
        modifier = modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE9FBFF), Color(0xFF315CFF))))
            .safeDrawingPadding().imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)
                .widthIn(max = 480.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.auth_back)) }
            Surface(shape = RoundedCornerShape(28.dp), color = Color.White, contentColor = Color.Black) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                    Text(
                        stringResource(if (registration) R.string.auth_create else R.string.auth_enter),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    if (registration) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(selected = !professor, onClick = { professor = false }, enabled = !busy,
                                label = { Text(stringResource(R.string.auth_student)) })
                            FilterChip(selected = professor, onClick = { professor = true }, enabled = !busy,
                                label = { Text(stringResource(R.string.auth_teacher)) })
                        }
                        AuthField(name, { name = it }, R.string.auth_name, busy,
                            submitted && invalidName, R.string.auth_name_error)
                    }
                    AuthField(email, { email = it }, R.string.auth_email, busy,
                        submitted && invalidEmail, R.string.auth_email_error, KeyboardType.Email)
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text(stringResource(R.string.auth_password)) },
                        enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        isError = submitted && invalidPassword,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(stringResource(if (showPassword) R.string.auth_hide else R.string.auth_show))
                            }
                        },
                        supportingText = {
                            if (submitted && invalidPassword) Text(stringResource(
                                if (registration) R.string.auth_password_error else R.string.auth_required))
                        }
                    )
                    if (registration && professor) {
                        AuthField(code, { code = it }, R.string.auth_code, busy,
                            submitted && invalidCode, R.string.auth_required)
                    }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                        onClick = {
                            submitted = true
                            message = null
                            if (!invalidName && !invalidEmail && !invalidPassword && !invalidCode) {
                                busy = true
                                scope.launch {
                                    try {
                                        if (registration) {
                                            repository.register(name.trim(), email.trim(), password,
                                                if (professor) code.trim() else null)
                                            password = ""
                                            code = ""
                                            registered = true
                                        } else {
                                            token = repository.login(email.trim(), password)
                                            password = ""
                                        }
                                    } catch (error: AuthException) {
                                        message = error.message
                                    } catch (_: IOException) {
                                        message = connectionError
                                    } catch (_: JSONException) {
                                        message = connectionError
                                    } finally {
                                        busy = false
                                    }
                                }
                            }
                        }
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text(stringResource(if (registration) R.string.auth_create else R.string.auth_enter))
                    }
                    TextButton(onClick = onSwitchMode, enabled = !busy) {
                        Text(stringResource(if (registration) R.string.auth_have_account else R.string.auth_new_account))
                    }
                }
            }
        }
    }
    if (registered || token != null) {
        AlertDialog(
            onDismissRequest = {
                if (registered) onSwitchMode() else onBack()
            },
            title = { Text(stringResource(if (registered) R.string.auth_registered else R.string.auth_logged_in)) },
            text = { Text(stringResource(if (registered) R.string.auth_login_next else R.string.auth_dashboard_pending)) },
            confirmButton = {
                TextButton(onClick = { if (registered) onSwitchMode() else onBack() }) {
                    Text(stringResource(R.string.home_close))
                }
            }
        )
    }
}

@Composable
private fun AuthField(
    value: String,
    onChange: (String) -> Unit,
    label: Int,
    busy: Boolean,
    error: Boolean,
    errorText: Int,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(stringResource(label)) },
        enabled = !busy, isError = error, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = { if (error) Text(stringResource(errorText)) },
        modifier = Modifier.fillMaxWidth()
    )
}
