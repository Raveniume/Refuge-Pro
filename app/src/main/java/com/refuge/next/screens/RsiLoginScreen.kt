package com.refuge.next.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.data.RsiAuthRepository
import com.refuge.next.data.RsiLoginStep
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeTypography
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeLiquidGlassField
import com.refuge.next.material.RefugeLiquidSheet
import com.refuge.next.reference.OfficialLiquidButtonPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Native editing and the catalog LiquidButton share one optical layer.
 * Credentials stay in composition memory; no preview authentication path exists.
 */
@Composable
fun RsiLoginScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    auth: RsiAuthRepository,
    loginEmailDraft: String = "",
    onLoginEmailChanged: (String) -> Unit = {},
    allowClose: Boolean = true,
    onAuthenticated: () -> Unit,
    onClose: () -> Unit,
) {
    var email by remember { mutableStateOf(auth.session()?.email ?: loginEmailDraft) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var captcha by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var needCode by remember { mutableStateOf(false) }
    var needCaptcha by remember { mutableStateOf(false) }
    var captchaImage by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showCaptcha by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val passwordFocus = remember { FocusRequester() }
    val codeFocus = remember { FocusRequester() }
    val captchaFocus = remember { FocusRequester() }
    val ready = if (needCode) code.isNotBlank() else
        email.isNotBlank() && password.isNotBlank() && (!needCaptcha || captcha.isNotBlank())

    suspend fun fetchCaptcha() {
        captcha = ""
        captchaImage = auth.captcha()?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        showCaptcha = captchaImage != null
        if (captchaImage == null) { message = "验证码加载失败，请重试"; isError = true }
    }
    fun refreshCaptcha() {
        if (loading) return
        loading = true
        scope.launch {
            try { fetchCaptcha() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "验证码加载失败，请重试"; isError = true }
            finally { loading = false }
        }
    }
    fun submit() {
        if (loading || !ready) return
        loading = true
        message = null
        isError = false
        focus.clearFocus()
        scope.launch {
            try {
                val result = if (needCode) auth.verifyCode(code.trim())
                    else auth.login(email.trim(), password, captcha.ifBlank { null })
                when {
                    result.success -> { password = ""; captcha = ""; code = ""; onAuthenticated() }
                    result.step == RsiLoginStep.NEED_CODE -> {
                        needCode = true; needCaptcha = false; showCaptcha = false
                        message = "验证码已发送到你的 RSI 邮箱"
                    }
                    result.step == RsiLoginStep.NEED_CAPTCHA || result.retryCaptcha || result.message.contains("4233") -> {
                        needCaptcha = true
                        fetchCaptcha()
                    }
                    else -> { message = result.message; isError = true }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = "连接失败，请检查网络后重试"; isError = true
            } finally { loading = false }
        }
    }

    LaunchedEffect(needCode, loading) { if (needCode && !loading) codeFocus.requestFocus() }
    LaunchedEffect(showCaptcha, loading) { if (showCaptcha && !loading) captchaFocus.requestFocus() }
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        // Glass belongs to interactive controls. A second, opaque glass card
        // behind the entire form conceals the controls' live optical sampling.
        Column(
            Modifier.align(Alignment.Center).widthIn(max = 440.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp)
                .testTag("rsi-login-form"),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Refuge", style = RefugeTypography.secondary(palette).copy(color = palette.accent))
                Text("登录 RSI", style = RefugeTypography.largeTitle(palette))
                Text("连接你的机库与账户", style = RefugeTypography.body(palette).copy(color = palette.textSecondary))
            }
            RefugeLiquidGlassField(email, { email = it; onLoginEmailChanged(it) }, backdrop, palette,
                "RSI 邮箱", Modifier.fillMaxWidth().testTag("login-email"), "name@example.com",
                enabled = !loading && !needCode,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }))
            RefugeLiquidGlassField(password, { password = it }, backdrop, palette,
                "密码", Modifier.fillMaxWidth().focusRequester(passwordFocus).testTag("login-password"), "输入 RSI 密码",
                enabled = !loading && !needCode,
                leadingIcon = if (showPassword) RefugeIcons.visibilityOff else RefugeIcons.visibility,
                leadingIconContentDescription = if (showPassword) "隐藏密码" else "显示密码",
                onLeadingIconClick = { showPassword = !showPassword },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }))
            if (needCode) RefugeLiquidGlassField(code, { code = it }, backdrop, palette,
                "RSI 邮件验证码", Modifier.fillMaxWidth().focusRequester(codeFocus).testTag("login-code"), "输入邮件中的验证码",
                enabled = !loading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }))
            if (needCaptcha) LoginGlassAction(backdrop, palette, "打开图形验证码", !loading, false, ::refreshCaptcha)
            message?.let {
                Text(it, style = RefugeTypography.secondary(palette).copy(color = if (isError) palette.error else palette.textSecondary),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            LoginGlassAction(backdrop, palette, if (needCode) "验证并登录" else "登录", ready && !loading,
                true, ::submit, loading = loading)
            if (allowClose) LoginGlassAction(backdrop, palette, "返回", !loading, false, onClose)
        }
        if (showCaptcha && captchaImage != null) {
            BackHandler { showCaptcha = false }
            RefugeLiquidSheet(backdrop, palette, "输入图形验证码", { showCaptcha = false }, sheetHeight = 470.dp,
                action = { local ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LoginGlassAction(local, palette, "刷新", !loading, false, ::refreshCaptcha, Modifier.weight(1f))
                        LoginGlassAction(local, palette, "完成", captcha.isNotBlank() && !loading, true,
                            { showCaptcha = false; submit() }, Modifier.weight(1f))
                    }
                }) { local ->
                Image(captchaImage!!.asImageBitmap(), "RSI 图形验证码", Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.Fit)
                RefugeLiquidGlassField(captcha, { captcha = it.filterNot(Char::isWhitespace) }, local, palette,
                    "图形验证码", Modifier.fillMaxWidth().focusRequester(captchaFocus), "输入图片中的字符",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (captcha.isNotBlank()) { showCaptcha = false; submit() } }))
            }
        }
    }
}

@Composable
private fun LoginGlassAction(
    backdrop: Backdrop, palette: RefugePalette, label: String, enabled: Boolean,
    primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false,
) {
    OfficialLiquidButtonPort(onClick, backdrop,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label },
        enabled = enabled, visualHeight = 52.dp,
        tint = if (primary && enabled) palette.accent else Color.Unspecified,
        surfaceColor = if (primary && enabled) Color.Transparent else palette.text.copy(alpha = if (enabled) .08f else .04f),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = palette.text, strokeWidth = 2.dp)
        Text(if (loading) "正在连接…" else label, style = RefugeTypography.body(palette),
            color = if (primary && enabled) Color.White else if (enabled) palette.text else palette.textMuted)
    }
}
