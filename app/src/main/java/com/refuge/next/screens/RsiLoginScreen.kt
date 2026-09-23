package com.refuge.next.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.data.RsiAuthDataSource
import com.refuge.next.data.RsiLoginStep
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import com.refuge.next.material.RefugeLiquidGlass
import com.refuge.next.material.RefugeLiquidGlassField
import com.refuge.next.material.RefugeLiquidGlassButton
import com.refuge.next.material.ModalGlassScope
import com.refuge.next.material.RefugeGlassControl
import com.refuge.next.material.RefugeModalSurface
import com.refuge.next.material.RefugeIcons
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun RsiLoginScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    auth: com.refuge.next.data.RsiAuthRepository,
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
    var showCaptchaDialog by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val captchaFocusRequester = remember { FocusRequester() }
    val codeFocusRequester = remember { FocusRequester() }
    val formReady = email.isNotBlank() && password.isNotBlank() &&
        (!needCaptcha || captcha.isNotBlank()) && (!needCode || code.isNotBlank())
    LaunchedEffect(showCaptchaDialog) {
        if (showCaptchaDialog) {
            delay(120)
            captchaFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(needCode) {
        if (needCode) {
            delay(120)
            codeFocusRequester.requestFocus()
        }
    }
    // The login card must be laid out above the IME. Without imePadding the
    // centered card can remain behind the password keyboard, leaving its
    // action visually present in stale coordinates but outside the touchable
    // viewport.
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        RefugeLiquidGlass(
            backdrop = backdrop,
            palette = palette,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(max = 560.dp)
                .padding(horizontal = RefugeSpacing.page),
            radius = 28.dp,
            padding = PaddingValues(22.dp),
            surface = if (palette.background.luminance() < .5f) {
                palette.contentSurfaceStrong
            } else {
                Color.White
            },
            surfaceAlpha = if (palette.background.luminance() < .5f) .58f else .82f,
            blurRadius = 12.dp,
            edgeAlpha = .04f,
            edgeColor = palette.outline,
            highlightAlpha = .08f,
        ) {
            Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("登录 RSI", style = RefugeTypography.largeTitle(palette).copy(color = palette.text))
                }
            }
            Spacer(Modifier.height(2.dp))
            RefugeLiquidGlassField(
                value = email,
                onValueChange = { email = it; onLoginEmailChanged(it) },
                backdrop = backdrop,
                palette = palette,
                label = "RSI 邮箱",
                placeholder = "name@example.com",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            RefugeLiquidGlassField(
                value = password,
                onValueChange = { password = it },
                backdrop = backdrop,
                palette = palette,
                label = "密码",
                placeholder = "输入 RSI 密码",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = if (showPassword) RefugeIcons.visibilityOff else RefugeIcons.visibility,
                leadingIconContentDescription = if (showPassword) "隐藏密码" else "显示密码",
                onLeadingIconClick = { showPassword = !showPassword },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            if (needCaptcha) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                    Text(
                        "需要图形验证码",
                        style = RefugeTypography.secondary(palette).copy(color = palette.textSecondary),
                        modifier = Modifier.weight(1f),
                    )
                    RefugeLiquidGlassButton(
                        backdrop = backdrop,
                        palette = palette,
                        onClick = {
                            scope.launch {
                                captchaImage = auth.captcha()?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                                if (captchaImage != null) showCaptchaDialog = true
                            }
                        },
                        modifier = Modifier.width(128.dp).height(48.dp),
                        radius = 24.dp,
                        padding = PaddingValues(0.dp),
                        edgeAlpha = .10f,
                        highlightAlpha = .10f,
                    ) {
                        Text("重新打开", color = palette.text, style = RefugeTypography.body(palette))
                    }
                }
            }
            if (needCode) RefugeLiquidGlassField(
                value = code,
                onValueChange = { code = it },
                backdrop = backdrop,
                palette = palette,
                label = "RSI 邮件验证码",
                placeholder = "输入邮件中的验证码",
                modifier = Modifier.fillMaxWidth().focusRequester(codeFocusRequester),
            )
            message?.let { Text(it, style = RefugeTypography.secondary(palette).copy(color = palette.error)) }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (allowClose) {
                    RefugeLiquidGlassButton(
                        backdrop = backdrop,
                        palette = palette,
                        onClick = { if (!loading) onClose() },
                        modifier = Modifier
                            .width(128.dp)
                            .height(48.dp)
                            .alpha(if (loading) .5f else 1f),
                        padding = PaddingValues(0.dp),
                        edgeAlpha = .10f,
                        highlightAlpha = .10f,
                    ) {
                        Text("返回", color = palette.text, style = RefugeTypography.body(palette))
                    }
                    Spacer(Modifier.width(RefugeSpacing.sm))
                }
                RefugeLiquidGlassButton(
                    backdrop = backdrop,
                    palette = palette,
                    radius = 24.dp,
                    surface = if (formReady && !loading) palette.accent else palette.contentSurfaceStrong,
                    surfaceAlpha = if (formReady && !loading) .92f else .84f,
                    edgeAlpha = if (formReady && !loading) .16f else .24f,
                    highlightAlpha = .10f,
                    onClick = {
                        if (!loading && formReady) {
                        loading = true
                        message = null
                        scope.launch {
                            val result = if (needCode) auth.verifyCode(code) else auth.login(email, password, captcha.ifBlank { null })
                            needCode = result.step == RsiLoginStep.NEED_CODE
                            needCaptcha = result.step == RsiLoginStep.NEED_CAPTCHA
                            if (result.step == RsiLoginStep.NEED_CAPTCHA) {
                                captchaImage = auth.captcha()?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                                showCaptchaDialog = captchaImage != null
                                message = if (captchaImage == null) "验证码加载失败，请重试" else null
                            } else if (result.step == RsiLoginStep.NEED_CODE) {
                                message = "验证码已发送到你的 RSI 邮箱，请输入后登录"
                            } else if (result.retryCaptcha || result.message.contains("4233")) {
                                captcha = ""
                                needCaptcha = true
                                captchaImage = auth.captcha()?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                                showCaptchaDialog = captchaImage != null
                                message = "图形验证码无效，请重新输入"
                            } else {
                                message = result.message
                            }
                            loading = false
                            if (result.success) onAuthenticated()
                        }
                        }
                    },
                        modifier = Modifier
                            .width(128.dp)
                            .height(48.dp)
                        .semantics {
                            role = androidx.compose.ui.semantics.Role.Button
                            contentDescription = "登录"
                        },
                    enabled = formReady && !loading,
                ) {
                    if (loading) androidx.compose.foundation.Canvas(Modifier.size(18.dp)) {
                        drawArc(
                            color = Color.White,
                            startAngle = -90f,
                            sweepAngle = 250f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx()),
                        )
                    }
                    else Text(
                        if (needCode) "验证并登录" else if (needCaptcha) "提交验证码" else "登录",
                        color = if (formReady && !loading) Color.White else palette.text,
                        style = RefugeTypography.body(palette),
                    )
                }
            }
            }
        }
        if (showCaptchaDialog && captchaImage != null) {
            BackHandler { showCaptchaDialog = false }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(palette.scrim)
                    .zIndex(2f),
                contentAlignment = Alignment.Center,
            ) {
                ModalGlassScope(
                    modifier = Modifier
                        .fillMaxWidth(.94f)
                        .widthIn(max = 520.dp)
                        .padding(14.dp),
                    underlay = backdrop,
                    base = {
                        RefugeModalSurface(
                            palette = palette,
                            modifier = Modifier.matchParentSize(),
                            radius = 28.dp,
                            fill = if (palette.background.luminance() < .5f) {
                                palette.contentSurfaceStrong
                            } else {
                                Color.White
                            },
                        ) {}
                    },
                    content = { modalBackdrop ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm),
                            ) {
                                Text("输入图形验证码", style = RefugeTypography.title(palette).copy(color = palette.text))
                                Text("请按图片内容输入，验证码不会保存。", style = RefugeTypography.body(palette).copy(color = palette.textSecondary))
                                Image(
                                    bitmap = captchaImage!!.asImageBitmap(),
                                    contentDescription = "RSI 图形验证码",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 140.dp, max = 190.dp),
                                )
                                RefugeLiquidGlassField(
                                    value = captcha,
                                    onValueChange = { captcha = it.filterNot(Char::isWhitespace) },
                                    backdrop = modalBackdrop,
                                    palette = palette,
                                    label = "图形验证码",
                                    placeholder = "输入图片中的字符",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(captchaFocusRequester),
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RefugeLiquidGlassButton(
                                        backdrop = modalBackdrop,
                                        palette = palette,
                                        modifier = Modifier.width(92.dp).height(48.dp),
                                        padding = PaddingValues(0.dp),
                                        edgeAlpha = .10f,
                                        highlightAlpha = .10f,
                                        refractionHeight = 0.dp,
                                        refractionAmount = 0.dp,
                                        blurRadius = 2.dp,
                                        onClick = {
                                            scope.launch {
                                                captchaImage = auth.captcha()?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                                            }
                                        },
                                    ) { Text("刷新", color = palette.text) }
                                    RefugeLiquidGlassButton(
                                        backdrop = modalBackdrop,
                                        palette = palette,
                                        modifier = Modifier.width(92.dp).height(48.dp),
                                        padding = PaddingValues(0.dp),
                                        edgeAlpha = .10f,
                                        highlightAlpha = .10f,
                                        refractionHeight = 0.dp,
                                        refractionAmount = 0.dp,
                                        blurRadius = 2.dp,
                                        onClick = { showCaptchaDialog = false },
                                    ) { Text("完成", color = palette.text) }
                                }
                            }
                    },
                )
            }
        }
    }
}
