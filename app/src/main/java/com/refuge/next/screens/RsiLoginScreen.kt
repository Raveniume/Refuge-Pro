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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.data.RsiAuthDataSource
import com.refuge.next.data.RsiLoginStep
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import com.refuge.next.material.RefugeLiquidGlass
import com.refuge.next.material.RefugeLiquidGlassButton
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun RsiLoginScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    auth: RsiAuthDataSource,
    allowClose: Boolean = true,
    onAuthenticated: () -> Unit,
    onClose: () -> Unit,
) {
    var email by remember { mutableStateOf(auth.session()?.email.orEmpty()) }
    var password by remember { mutableStateOf("") }
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
    val fieldColors = TextFieldDefaults.outlinedTextFieldColors(
        textColor = palette.text,
        cursorColor = palette.accent,
        focusedBorderColor = palette.accent,
        unfocusedBorderColor = palette.outline,
        focusedLabelColor = palette.accent,
        unfocusedLabelColor = palette.textSecondary,
    )
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
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        RefugeLiquidGlass(
            backdrop = backdrop,
            palette = palette,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(max = 560.dp)
                .padding(horizontal = RefugeSpacing.page),
            radius = 24.dp,
            padding = PaddingValues(24.dp),
            surface = palette.contentSurfaceStrong,
            surfaceAlpha = .72f,
            blurRadius = 5.dp,
        ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm),
        ) {
            Text("登录 RSI", style = RefugeTypography.largeTitle(palette).copy(color = palette.text))
            Text("连接后才能读取你的真实机库、商店和终端资料。", style = RefugeTypography.body(palette).copy(color = palette.textSecondary))
            Text("密码只用于本次登录请求，不会写入本地。", style = RefugeTypography.caption(palette).copy(color = palette.textMuted))
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("RSI 邮箱") },
                placeholder = { Text("name@example.com") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = fieldColors,
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = fieldColors,
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
                        radius = 16.dp,
                        padding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                    ) { Text("重新打开", color = palette.text) }
                }
            }
            if (needCode) OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                modifier = Modifier.fillMaxWidth().focusRequester(codeFocusRequester),
                label = { Text("RSI 邮件验证码") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
            )
            message?.let { Text(it, style = RefugeTypography.secondary(palette).copy(color = palette.error)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (allowClose) {
                    Button(onClick = onClose, enabled = !loading, colors = ButtonDefaults.buttonColors(backgroundColor = palette.glassStrong, contentColor = palette.text)) { Text("返回") }
                    Spacer(Modifier.width(RefugeSpacing.sm))
                }
                Button(
                    onClick = {
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
                            } else {
                                message = result.message
                            }
                            loading = false
                            if (result.success) onAuthenticated()
                        }
                    },
                    enabled = !loading && email.isNotBlank() && password.isNotBlank() &&
                        (!needCaptcha || captcha.isNotBlank()) && (!needCode || code.isNotBlank()),
                    colors = ButtonDefaults.buttonColors(backgroundColor = palette.accent, contentColor = palette.background),
                ) {
                    if (loading) CircularProgressIndicator(Modifier.height(16.dp), strokeWidth = 2.dp)
                    else Text(if (needCode) "验证并登录" else if (needCaptcha) "提交验证码" else "登录")
                }
            }
            }
        }
        if (showCaptchaDialog && captchaImage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = .18f)),
                contentAlignment = Alignment.Center,
            ) {
                RefugeLiquidGlass(
                    backdrop = backdrop,
                    palette = palette,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    radius = 26.dp,
                    padding = PaddingValues(20.dp),
                    surface = palette.contentSurfaceStrong,
                    surfaceAlpha = .86f,
                    blurRadius = 7.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                        Text("输入图形验证码", style = RefugeTypography.title(palette).copy(color = palette.text))
                        Text("请按图片内容输入，验证码不会保存。", style = RefugeTypography.caption(palette).copy(color = palette.textSecondary))
                        Image(
                            bitmap = captchaImage!!.asImageBitmap(),
                            contentDescription = "RSI 图形验证码",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp, max = 190.dp),
                        )
                        RefugeLiquidGlass(
                            backdrop = backdrop,
                            palette = palette,
                            modifier = Modifier.fillMaxWidth(),
                            radius = 18.dp,
                            padding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            surface = palette.glassStrong,
                            surfaceAlpha = .20f,
                            blurRadius = 4.dp,
                        ) {
                            OutlinedTextField(
                                value = captcha,
                                onValueChange = { captcha = it },
                                modifier = Modifier.fillMaxWidth().focusRequester(captchaFocusRequester),
                                label = { Text("图形验证码") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            RefugeLiquidGlassButton(
                                backdrop = backdrop,
                                palette = palette,
                                onClick = {
                                    scope.launch {
                                        captchaImage = auth.captcha()?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                                    }
                                },
                                radius = 16.dp,
                            ) { Text("刷新", color = palette.text) }
                            Spacer(Modifier.width(RefugeSpacing.sm))
                            RefugeLiquidGlassButton(
                                backdrop = backdrop,
                                palette = palette,
                                onClick = { showCaptchaDialog = false },
                                radius = 16.dp,
                            ) { Text("完成", color = palette.text) }
                        }
                    }
                }
            }
        }
    }
}
