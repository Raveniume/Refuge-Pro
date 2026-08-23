package com.refuge.next.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.data.RsiAuthDataSource
import com.refuge.next.data.RsiLoginStep
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import com.refuge.next.material.RefugeLiquidGlass
import kotlinx.coroutines.launch

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
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    RefugeLiquidGlass(
        backdrop = backdrop,
        palette = palette,
        modifier = Modifier.fillMaxSize().padding(RefugeSpacing.page),
        radius = 24.dp,
        padding = PaddingValues(24.dp),
        surface = palette.contentSurfaceStrong,
        surfaceAlpha = .72f,
        blurRadius = 5.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
            Text("连接 RSI 账户", style = RefugeTypography.largeTitle(palette))
            Text("使用原 RefugeNext 的 RSI launcher 登录接口。密码仅用于本次请求，不会写入本地。", style = RefugeTypography.secondary(palette))
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("邮箱") }, singleLine = true)
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("密码") }, singleLine = true)
            if (needCaptcha) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                    captchaImage?.let { Image(it.asImageBitmap(), "RSI 验证码", Modifier.height(48.dp).weight(1f)) }
                    Button(onClick = {
                        scope.launch { captchaImage = auth.captcha()?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) } }
                    }) { Text("获取验证码") }
                }
                OutlinedTextField(captcha, { captcha = it }, Modifier.fillMaxWidth(), label = { Text("验证码") }, singleLine = true)
            }
            if (needCode) OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("RSI 验证码") }, singleLine = true)
            message?.let { Text(it, style = RefugeTypography.secondary(palette).copy(color = palette.error)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (allowClose) {
                    Button(onClick = onClose, enabled = !loading) { Text("返回") }
                    Spacer(Modifier.width(RefugeSpacing.sm))
                }
                Button(
                    onClick = {
                        loading = true
                        message = null
                        scope.launch {
                            val result = if (needCode) auth.verifyCode(code) else auth.login(email, password, captcha.ifBlank { null })
                            loading = false
                            needCode = result.step == RsiLoginStep.NEED_CODE
                            needCaptcha = result.step == RsiLoginStep.NEED_CAPTCHA
                            message = result.message
                            if (result.success) onAuthenticated()
                        }
                    },
                    enabled = !loading,
                ) {
                    if (loading) CircularProgressIndicator(Modifier.height(16.dp), strokeWidth = 2.dp) else Text(if (needCode) "验证并登录" else "登录")
                }
            }
        }
    }
}
