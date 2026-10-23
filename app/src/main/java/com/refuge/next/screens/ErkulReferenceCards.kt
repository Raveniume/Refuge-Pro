package com.refuge.next.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.data.*
import com.refuge.next.design.*
import com.refuge.next.material.*
import org.json.JSONObject
import java.util.Locale

private fun Double.display(unit: String = "") = String.format(Locale.US, "%,.1f", this).removeSuffix(".0") + unit
private fun JSONObject.sumValues() = keys().asSequence().sumOf { number(it) ?: 0.0 }

@Composable
internal fun ErkulDataCard(palette: RefugePalette, title: String, rows: List<Pair<String, String>>, extra: @Composable () -> Unit = {}) {
    var collapsed by remember(title) { mutableStateOf(false) }
    RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton({ collapsed = !collapsed }, contentPadding = PaddingValues(0.dp)) {
                Text(title + if (collapsed) " ＋" else " −", style = RefugeTypography.headline(palette))
            }
            if (!collapsed) {
                RefugeInfoRows(palette, rows)
                Spacer(Modifier.height(4.dp))
                extra()
            }
        }
    }
}

internal fun erkulComponentRows(item: JSONObject): List<Pair<String, String>> = buildList {
    fun value(label: String, obj: JSONObject, key: String, unit: String = "", scale: Double = 1.0) {
        obj.number(key)?.let { add(label to (it * scale).display(unit)) }
    }
    add("规格" to "S${item.optInt("size")} · ${item.optString("grade")} · ${item.obj("i18n").optString("class")}")
    value("质量", item, "mass", " kg")
    value("组件生命值", item.obj("health"), "hp", " HP")
    if (item.has("weapon")) {
        val weapon = item.obj("weapon"); val ammo = weapon.obj("ammo")
        add("单发伤害" to ammo.obj("damage").sumValues().display())
        add("爆发 DPS" to weaponBurst(item).display())
        add("持续 DPS（额定）" to weaponSustained(item, 1.0).display())
        value("射速", weapon.objects("fireActions").firstOrNull() ?: JSONObject(), "fireRate", " RPM")
        value("弹速", ammo, "speed", " m/s")
        val speed = ammo.number("speed"); val life = ammo.number("lifetime")
        if (speed != null && life != null) add("射程" to (speed * life).display(" m"))
        value("穿透", ammo.obj("penetration"), "base")
        value("弹量", ammo, "maxAmmoCount")
        value("电容弹量", weapon.obj("regen"), "maxAmmoLoad")
    }
    value("护盾容量", item.obj("shield"), "maxShieldHealth", " HP")
    value("护盾回复", item.obj("shield"), "maxShieldRegen", " HP/s")
    value("受击回复延迟", item.obj("shield"), "damagedRegenDelay", " s")
    value("击穿回复延迟", item.obj("shield"), "downedRegenDelay", " s")
    value("量子速度", item.obj("qdrive").obj("params"), "driveSpeed", " km/s", .001)
    if (item.has("qdrive")) quantumFuelPerGm(item)?.let { add("量子燃耗" to (it * 1000).display(" mSCU/Gm")) }
    if (item.has("missile")) {
        val missile = item.obj("missile")
        add("单枚伤害" to missile.obj("damage").sumValues().display())
        add("制导方式" to missile.obj("targeting").optString("trackingSignalType"))
        value("锁定时间", missile.obj("targeting"), "lockTime", " s")
        value("最小锁定距离", missile.obj("targeting"), "lockRangeMin", " m")
        value("最大锁定距离", missile.obj("targeting"), "lockRangeMax", " m")
        value("速度", missile.obj("gcs"), "linearSpeed", " m/s")
    }
    val power = resourceUnits(item, true, "Power", "powerSegment")
    if (power > 0) add("发电量" to power.display(" 格"))
    val cooling = resourceUnits(item, true, "Coolant")
    if (cooling > 0) add("冷却能力" to cooling.display(" /s"))
    value("瞄准辅助下限", item.obj("radar").obj("aimAssist"), "distanceMinAssignment", " m")
    value("瞄准辅助上限", item.obj("radar").obj("aimAssist"), "distanceMaxAssignment", " m")
}

@Composable
internal fun ErkulComponentDetail(backdrop: LayerBackdrop, palette: RefugePalette, item: JSONObject,
    onBlueprint: (String) -> Unit, onBack: () -> Unit) {
    KnowledgePage(backdrop, palette, "组件详情", onBack) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text(translatedGameItemName(item.itemName(), item.optString("className")), style = RefugeTypography.largeTitle(palette)) }
            item { ErkulDataCard(palette, item.itemName(), erkulComponentRows(item)) }
            if (item.has("craftingBlueprint")) item {
                TextButton({ onBlueprint(item.itemName()) }, Modifier.fillMaxWidth()) { Text("查看制造蓝图与获取任务", color = palette.accent) }
            }
            item { Text(item.obj("i18n").optString("description"), style = RefugeTypography.body(palette)) }
        }
    }
}
