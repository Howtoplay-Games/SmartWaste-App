package sg.smartwaste.app.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sg.smartwaste.app.AppContainer
import sg.smartwaste.app.R
import sg.smartwaste.app.data.auth.AuthException
import sg.smartwaste.app.ui.components.AuthField
import sg.smartwaste.app.ui.components.CircleIconButton
import sg.smartwaste.app.ui.components.GlowOrb
import sg.smartwaste.app.ui.components.OrDivider
import sg.smartwaste.app.ui.components.PrimaryButton
import sg.smartwaste.app.ui.components.SecondaryButton
import sg.smartwaste.app.ui.components.appViewModel
import sg.smartwaste.app.ui.theme.SW

enum class AuthMode { LOGIN, REGISTER, FORGOT, RESET }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val code: String = "",
    val remember: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    /** Demo mode: the reset code is shown in a dialog because there is no mail server. */
    val demoCode: String? = null,
)

class AuthViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val last = c.auth.lastEmail.first()
            if (last.isNotBlank() && _state.value.email.isBlank()) _state.update { it.copy(email = last) }
        }
    }

    fun setMode(mode: AuthMode) = _state.update { it.copy(mode = mode, error = null, info = null, password = "", code = "") }
    fun setName(v: String) = _state.update { it.copy(name = v.take(40), error = null) }
    fun setEmail(v: String) = _state.update { it.copy(email = v.trim().take(80), error = null) }
    fun setPassword(v: String) = _state.update { it.copy(password = v.take(64), error = null) }
    fun setCode(v: String) = _state.update { it.copy(code = v.filter(Char::isDigit).take(5), error = null) }
    fun toggleRemember() = _state.update { it.copy(remember = !it.remember) }
    fun dismissCode() = _state.update { it.copy(demoCode = null) }

    private fun launchAuth(block: suspend () -> Unit) {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: AuthException) {
                _state.update { it.copy(error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Something went wrong: ${e.message}") }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun login() = launchAuth { val s = _state.value; c.auth.login(s.email, s.password, s.remember) }

    fun register() = launchAuth { val s = _state.value; c.auth.register(s.name, s.email, s.password, s.remember) }

    fun sendCode() = launchAuth {
        val code = c.auth.requestResetCode(_state.value.email)
        _state.update { it.copy(mode = AuthMode.RESET, demoCode = code, code = "", password = "", info = null) }
    }

    fun resetPassword() = launchAuth {
        val s = _state.value
        c.auth.resetPassword(s.email, s.code, s.password)
        _state.update { it.copy(mode = AuthMode.LOGIN, password = "", code = "", info = "Password updated. Sign in with your new password.") }
    }

    fun google(activity: Activity?) = launchAuth {
        if (activity == null) throw AuthException("Google sign-in needs an activity.")
        c.auth.signInWithGoogle(activity)
    }

    fun apple() = _state.update {
        it.copy(error = "Sign in with Apple needs a server-side Apple Services ID, which this build doesn't have yet. Use email or Google for now.")
    }
}

@Composable
fun AuthScreen() {
    val vm = appViewModel { AuthViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()

    BackHandler(enabled = s.mode != AuthMode.LOGIN) {
        vm.setMode(if (s.mode == AuthMode.RESET) AuthMode.FORGOT else AuthMode.LOGIN)
    }

    Box(Modifier.fillMaxSize().background(SW.Bg)) {
        // Olive glow + dot texture behind the header (login reference).
        Canvas(Modifier.fillMaxWidth().height(440.dp)) {
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFF2E3A0E), Color.Transparent),
                    center = Offset(size.width / 2, size.height * 0.25f), radius = size.width * 0.9f,
                )
            )
        }
        Canvas(Modifier.fillMaxWidth().height(300.dp)) {
            val step = 18.dp.toPx()
            var y = step
            while (y < size.height) {
                var x = step / 2
                while (x < size.width) {
                    drawCircle(SW.Lime.copy(alpha = 0.07f * (1 - y / size.height)), 1.dp.toPx(), Offset(x, y)); x += step
                }
                y += step
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (s.mode != AuthMode.LOGIN) {
                    CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", {
                        vm.setMode(if (s.mode == AuthMode.RESET) AuthMode.FORGOT else AuthMode.LOGIN)
                    }, container = Color.White.copy(alpha = 0.06f))
                } else Spacer(Modifier.size(44.dp))
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).border(1.dp, SW.Line, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(R.drawable.ic_recycle), "SmartWaste", colorFilter = ColorFilter.tint(SW.Text), modifier = Modifier.size(20.dp))
                }
            }

            AnimatedContent(s.mode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "auth") { mode ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(if (mode == AuthMode.FORGOT || mode == AuthMode.RESET) 4.dp else 18.dp))
                    GlowOrb(orbSize = 92.dp, rings = mode == AuthMode.FORGOT || mode == AuthMode.RESET)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        when (mode) {
                            AuthMode.LOGIN -> "Welcome Back!"
                            AuthMode.REGISTER -> "Create Your Account"
                            AuthMode.FORGOT -> "Forgot Password?"
                            AuthMode.RESET -> "Enter Verification Code"
                        },
                        color = SW.Text, fontSize = 27.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (mode) {
                            AuthMode.LOGIN -> "Sign in to track your recycling, earn points and redeem rewards."
                            AuthMode.REGISTER -> "Create your account to start recycling smarter and earning rewards."
                            AuthMode.FORGOT -> "Enter your email and we'll send you a 5-digit verification code instantly."
                            AuthMode.RESET -> "Enter the 5-digit code sent to ${s.email}, then choose a new password."
                        },
                        color = SW.Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(26.dp))

                    s.info?.let { Banner(it, ok = true); Spacer(Modifier.height(14.dp)) }
                    s.error?.let { Banner(it, ok = false); Spacer(Modifier.height(14.dp)) }

                    when (mode) {
                        AuthMode.LOGIN -> LoginForm(s, vm, activity)
                        AuthMode.REGISTER -> RegisterForm(s, vm, activity)
                        AuthMode.FORGOT -> ForgotForm(s, vm)
                        AuthMode.RESET -> ResetForm(s, vm)
                    }
                }
            }
        }
    }

    s.demoCode?.let { code ->
        AlertDialog(
            onDismissRequest = vm::dismissCode,
            containerColor = SW.Card,
            shape = RoundedCornerShape(26.dp),
            icon = { Icon(Icons.Rounded.Email, null, tint = SW.Lime) },
            title = { Text("Verification code", color = SW.Text) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(code, color = SW.Lime, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Demo mode: this build has no mail server, so the code is shown here instead of being emailed. Valid for 10 minutes.",
                        color = SW.Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.setCode(code); vm.dismissCode() }) { Text("Use code", color = SW.Lime, fontWeight = FontWeight.Bold) }
            },
        )
    }
}

@Composable
private fun LoginForm(s: AuthUiState, vm: AuthViewModel, activity: Activity?) {
    Column(Modifier.fillMaxWidth()) {
        AuthField("Email address*", s.email, vm::setEmail, "example@gmail.com", keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(16.dp))
        AuthField("Password*", s.password, vm::setPassword, "Enter your password", password = true, imeAction = ImeAction.Done, onIme = vm::login)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = vm::toggleRemember), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(20.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (s.remember) SW.Lime else Color.Transparent)
                        .border(1.5.dp, if (s.remember) SW.Lime else SW.Muted, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) { if (s.remember) Icon(Icons.Rounded.Check, null, tint = SW.OnLime, modifier = Modifier.size(14.dp)) }
                Spacer(Modifier.width(8.dp))
                Text("Remember me", color = SW.Subtle, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Forgot Password?", color = SW.Text, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { vm.setMode(AuthMode.FORGOT) }.padding(4.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        PrimaryButton("Sign in", vm::login, Modifier.fillMaxWidth(), loading = s.loading, leading = Icons.Rounded.AutoFixHigh)
        Socials(vm, activity)
        Spacer(Modifier.height(28.dp))
        FooterLink("Don't have an account? ", "Sign up") { vm.setMode(AuthMode.REGISTER) }
    }
}

@Composable
private fun RegisterForm(s: AuthUiState, vm: AuthViewModel, activity: Activity?) {
    Column(Modifier.fillMaxWidth()) {
        AuthField("Full Name*", s.name, vm::setName, "Alex Tan")
        Spacer(Modifier.height(16.dp))
        AuthField("Email address*", s.email, vm::setEmail, "example@gmail.com", keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(16.dp))
        AuthField("Password*", s.password, vm::setPassword, "At least 8 characters, 1 number", password = true, imeAction = ImeAction.Done, onIme = vm::register)
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Register", vm::register, Modifier.fillMaxWidth(), loading = s.loading, leading = Icons.Rounded.AutoFixHigh)
        Socials(vm, activity)
        Spacer(Modifier.height(28.dp))
        FooterLink("Already have an account? ", "Sign In") { vm.setMode(AuthMode.LOGIN) }
    }
}

@Composable
private fun ForgotForm(s: AuthUiState, vm: AuthViewModel) {
    Column(Modifier.fillMaxWidth()) {
        AuthField("Email address*", s.email, vm::setEmail, "example@gmail.com", keyboardType = KeyboardType.Email, imeAction = ImeAction.Send, onIme = vm::sendCode)
        Spacer(Modifier.height(22.dp))
        PrimaryButton("Send Code", vm::sendCode, Modifier.fillMaxWidth(), loading = s.loading, leading = Icons.Rounded.AutoFixHigh)
        Spacer(Modifier.height(120.dp))
        FooterLink("Already have an account? ", "Sign In") { vm.setMode(AuthMode.LOGIN) }
    }
}

@Composable
private fun ResetForm(s: AuthUiState, vm: AuthViewModel) {
    Column(Modifier.fillMaxWidth()) {
        Text("Verification code*", color = SW.Text, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        CodeInput(s.code, vm::setCode)
        Spacer(Modifier.height(16.dp))
        AuthField("New password*", s.password, vm::setPassword, "At least 8 characters, 1 number", password = true, imeAction = ImeAction.Done, onIme = vm::resetPassword)
        Spacer(Modifier.height(22.dp))
        PrimaryButton(
            "Reset Password", vm::resetPassword, Modifier.fillMaxWidth(), loading = s.loading,
            enabled = s.code.length == 5, leading = Icons.Rounded.AutoFixHigh,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Didn't get it? Resend code", color = SW.Lime, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(6.dp)).clickable(onClick = vm::sendCode).padding(6.dp),
        )
        Spacer(Modifier.height(60.dp))
        FooterLink("Already have an account? ", "Sign In") { vm.setMode(AuthMode.LOGIN) }
    }
}

/** Five boxed digits for the verification code. */
@Composable
private fun CodeInput(code: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = code,
        onValueChange = onChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
        singleLine = true,
        decorationBox = { innerTextField -> Box {
            // The real text field stays composed (invisible) so focus, IME and paste work.
            Box(Modifier.size(1.dp).alpha(0f)) { innerTextField() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(5) { i ->
                    val active = i == code.length
                    Box(
                        Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(14.dp)).background(SW.Field)
                            .border(if (active) 1.5.dp else 1.dp, if (active) SW.Lime else SW.Line, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(code.getOrNull(i)?.toString().orEmpty(), color = SW.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } },
    )
}

@Composable
private fun Socials(vm: AuthViewModel, activity: Activity?) {
    Spacer(Modifier.height(22.dp))
    OrDivider("Or continue with")
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SecondaryButton("Google", { vm.google(activity) }, Modifier.weight(1f), leadingPainter = R.drawable.ic_google)
        SecondaryButton("Apple", vm::apple, Modifier.weight(1f), leadingPainter = R.drawable.ic_apple)
    }
}

@Composable
private fun FooterLink(prefix: String, action: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(bottom = 18.dp), contentAlignment = Alignment.Center) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = SW.Muted)) { append(prefix) }
                withStyle(SpanStyle(color = SW.Text, fontWeight = FontWeight.SemiBold)) { append(action) }
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(6.dp),
        )
    }
}

@Composable
private fun Banner(text: String, ok: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (ok) SW.LimeSoft else SW.DangerSoft).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (ok) Icons.Rounded.Check else Icons.Rounded.Error, null, tint = if (ok) SW.Lime else SW.Danger, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = SW.Text, style = MaterialTheme.typography.bodySmall)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
