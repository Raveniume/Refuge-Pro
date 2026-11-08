package com.refuge.next.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.refuge.next.R
import com.refuge.next.data.*
import com.refuge.next.design.*
import com.refuge.next.material.*

internal fun equipmentGroup(slot: ErkulSlot): String = when (slot.item?.optString("type")
    ?: slot.port.objects("accepts").firstOrNull()?.optString("type")) {
    "WeaponGun", "Turret", "TurretBase", "WeaponMount" -> "武器 · Armament"
    "Missile", "MissileLauncher", "Bomb", "BombLauncher" -> "弹药 · Ordnance"
    "Shield", "ShieldController" -> "护盾 · Shields"
    "PowerPlant" -> "发电机 · Power plant"
    "Cooler" -> "散热器 · Coolers"
    "QuantumDrive", "JumpDrive", "FlightController", "Engine" -> "航行 · Flight systems"
    "Radar" -> "雷达 · Radar"
    "LifeSupport", "LifeSupportGenerator" -> "维生 · Life support"
    else -> "其他设备 · Equipment"
}

internal fun equipmentIcon(slot: ErkulSlot): Int = when (slot.item?.optString("type")
    ?: slot.port.objects("accepts").firstOrNull()?.optString("type")) {
    "WeaponGun", "Turret", "TurretBase" -> R.drawable.erkul_power_weapons
    "WeaponMount" -> R.drawable.erkul_power_mounts
    "Missile", "MissileLauncher" -> R.drawable.erkul_power_missiles
    "Bomb", "BombLauncher" -> R.drawable.erkul_power_bombs
    "Shield", "ShieldController" -> R.drawable.erkul_power_shields
    "PowerPlant" -> R.drawable.erkul_power_power_plant_output
    "Cooler" -> R.drawable.erkul_power_coolers
    "Engine", "FlightController" -> R.drawable.erkul_power_engines
    "QuantumDrive", "JumpDrive" -> R.drawable.erkul_power_quantum_drives
    "Radar" -> R.drawable.erkul_power_radars
    else -> R.drawable.erkul_power_life_support
}

/** One card per hardpoint, including empty slots and repeated identical components. */
@Composable
internal fun ErkulEquipmentCard(
    palette: RefugePalette, slot: ErkulSlot, enabled: Boolean,
    onChoose: () -> Unit, onDetail: () -> Unit, onToggle: () -> Unit,
    onRemove: () -> Unit, onRestore: () -> Unit,
) {
    val item = slot.item
    val editable = slot.port.obj("flags").optBoolean("editable") &&
        !slot.port.obj("flags").optBoolean("uneditable")
    RefugeLightweightGlassSurface(palette,
        Modifier.fillMaxWidth().padding(start = (slot.depth.coerceAtMost(2) * 10).dp)
            .testTag("equipment:${slot.path}"), padding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(equipmentIcon(slot)), null, tint = palette.accent, modifier = Modifier.size(24.dp))
                Text(item?.let { "S${it.optInt("size")}" } ?: "S${slot.port.optInt("maxSize")}",
                    style = RefugeTypography.headline(palette).copy(color = palette.accent),
                    modifier = Modifier.background(palette.accentSoft, refugeContinuousShape(7.dp)).padding(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(item?.let { translatedGameItemName(it.itemName(), it.optString("className")) } ?: "空槽",
                        style = RefugeTypography.headline(palette), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(erkulPortLabel(slot), style = RefugeTypography.caption(palette))
                }
                TextButton(onChoose, enabled = editable, contentPadding = PaddingValues(8.dp)) {
                    Text(if (editable) "更换" else "固定", color = if (editable) palette.accent else palette.textMuted)
                }
            }
            if (item != null) {
                val rows = erkulComponentRows(item).filter { it.first !in setOf("规格", "质量", "组件生命值") }
                val columns = if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f) 2 else 3
                rows.take(6).chunked(columns).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pair.forEach { (label, value) ->
                            Column(Modifier.weight(1f).background(palette.contentSurfaceStrong, refugeContinuousShape(7.dp)).padding(8.dp)) {
                                Text(label.removeSuffix("（额定）"), style = RefugeTypography.caption(palette))
                                Text(value, style = RefugeTypography.headline(palette))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onDetail) { Text("详情", color = palette.accent) }
                    TextButton(onToggle) { Text(if (enabled) "停用" else "启用", color = palette.accent) }
                    TextButton(onRestore, enabled = editable) { Text("原厂", color = if (editable) palette.accent else palette.textMuted) }
                    TextButton(onRemove, enabled = editable) { Text("卸下", color = if (editable) palette.error else palette.textMuted) }
                }
            } else TextButton(onRestore, enabled = editable) { Text("恢复原厂组件", color = palette.accent) }
        }
    }
}
