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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
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
            val fieldColors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = palette.text,
                cursorColor = palette.accent,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.outline,
                focusedLabelColor = palette.accent,
                unfocusedLabelColor = palette.textSecondary,
            )
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
                    captchaImage?.let { Image(it.asImageBitmap(), "RSI 验证码", Modifier.height(48.dp).weight(1f)) }
                    Button(onClick = {
                        scope.launch { captchaImage = auth.captcha()?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) } }
                    }) { Text("获取验证码") }
                }
                OutlinedTextField(captcha, { captcha = it }, Modifier.fillMaxWidth(), label = { Text("图形验证码") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors)
            }
            if (needCode) OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("RSI 邮件验证码") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors)
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
                            loading = false
                            needCode = result.step == RsiLoginStep.NEED_CODE
                            needCaptcha = result.step == RsiLoginStep.NEED_CAPTCHA
                            message = result.message
                            if (result.success) onAuthenticated()
                        }
                    },
                    enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = palette.accent, contentColor = palette.background),
                ) {
                    if (loading) CircularProgressIndicator(Modifier.height(16.dp), strokeWidth = 2.dp) else Text(if (needCode) "验证并登录" else "登录")
                }
            }
            }
        }
    }
}
