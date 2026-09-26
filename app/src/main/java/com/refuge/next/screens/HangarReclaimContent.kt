package com.refuge.next.screens

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.data.HangarItem
import com.refuge.next.data.HangarReclaimRequest
import com.refuge.next.data.HangarReclaimResult
import com.refuge.next.data.HangarRepository
import com.refuge.next.design.*
import com.refuge.next.reference.OfficialLiquidButtonPort
import com.refuge.next.material.RefugeLiquidGlassField
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Inline content only: the parent owns all sheet geometry and navigation. */
@Composable
internal fun HangarReclaimContent(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    item: HangarItem?,
    repository: HangarRepository,
    onInventoryChanged: () -> Unit,
) {
    if (item == null) {
        Text("未找到真实机库物品，请刷新后重试", style = RefugeTypography.body(palette))
        return
    }
    var quantity by remember(item.id) { mutableStateOf("1") }
    var password by remember(item.id) { mutableStateOf("") }
    var prepared by remember(item.id) { mutableStateOf<HangarReclaimRequest?>(null) }
    var busy by remember(item.id) { mutableStateOf(false) }
    var error by remember(item.id) { mutableStateOf<String?>(null) }
    var result by remember(item.id) { mutableStateOf<HangarReclaimResult?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val changed by rememberUpdatedState(onInventoryChanged)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { response ->
        val request = prepared
        prepared = null
        password = ""
        if (response.resultCode != Activity.RESULT_OK || request == null) {
            busy = false
            error = "设备验证取消或失败，未提交回收"
        } else {
            scope.launch {
                try {
                    result = repository.reclaim(request, deviceVerified = true)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    error = failure.message ?: "回收前核验失败"
                } finally {
                    busy = false
                    changed()
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(translatedShipName(item.title, item.originalName), style = RefugeTypography.headline(palette))
        Text("每件可回收价值 ${item.price} · 可回收数量 ${item.quantity}", style = RefugeTypography.body(palette))
        Text("确认后物品将转为商店信用点并从机库移除。此操作不可撤销。", style = RefugeTypography.secondary(palette))
        if (result == null) {
            RefugeLiquidGlassField(
                value = quantity,
                onValueChange = { quantity = it; prepared = null; error = null },
                backdrop = backdrop,
                palette = palette,
                label = "回收数量（1–${item.quantity}）",
                placeholder = "输入数量",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !busy && prepared == null,
                modifier = Modifier.fillMaxWidth(),
            )
            RefugeLiquidGlassField(
                value = password,
                onValueChange = { password = it; prepared = null; error = null },
                backdrop = backdrop,
                palette = palette,
                label = "当前 RSI 账户密码",
                placeholder = "输入密码",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !busy && prepared == null,
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, style = RefugeTypography.body(palette).copy(color = palette.error)) }
            prepared?.let { request ->
                Text("即将回收 ${request.pledgeIds.size} 件：编号 ${request.pledgeIds.joinToString()}。请核对后确认。",
                    style = RefugeTypography.body(palette))
            }
            OfficialLiquidButtonPort(
                backdrop = backdrop, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                visualHeight = 52.dp, onClick = {
                    error = null
                    if (prepared == null) {
                        try {
                            prepared = HangarReclaimRequest.prepare(quantity, item.quantity, item.idList, item.isReclaimable, password)
                            focus.clearFocus()
                        } catch (failure: IllegalArgumentException) { error = failure.message }
                    } else {
                        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                        @Suppress("DEPRECATION")
                        val intent = keyguard?.takeIf { it.isDeviceSecure }
                            ?.createConfirmDeviceCredentialIntent("请验证以回收物品", "回收 ${prepared!!.pledgeIds.size} 件 ${item.title}")
                        if (intent == null) {
                            error = "此设备未设置安全锁屏，无法回收；请先设置设备密码"
                        } else {
                            busy = true
                            try { launcher.launch(intent) } catch (_: Exception) {
                                busy = false
                                error = "无法启动设备验证，未提交回收"
                            }
                        }
                    }
                },
            ) {
                Text(if (busy) "正在验证或回收…" else if (prepared == null) "核对回收信息" else "确认回收并验证设备",
                    style = RefugeTypography.body(palette))
            }
            if (prepared != null && !busy) TextButton(onClick = { prepared = null }) {
                Text("修改数量或密码", color = palette.accent)
            }
        } else {
            Text(result!!.message, style = RefugeTypography.body(palette))
            Text("请核对最新机库；再次回收需要重新打开物品。", style = RefugeTypography.caption(palette))
        }
        if (!busy) TextButton(onClick = {
            busy = true
            prepared = null
            password = ""
            scope.launch {
                try {
                    repository.refreshInventory()
                    error = null
                    result = result?.copy(refreshFailure = null)
                    changed()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { error = failure.message ?: "机库刷新失败" }
                finally { busy = false }
            }
        }, modifier = Modifier.heightIn(min = 48.dp)) { Text("刷新机库", color = palette.accent) }
        if (result != null) error?.let { Text(it, style = RefugeTypography.body(palette).copy(color = palette.error)) }
    }
}
