package com.refuge.next.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.refuge.next.data.*
import com.refuge.next.design.*
import com.refuge.next.material.*
import org.json.JSONObject
import kotlin.math.*

@Composable
internal fun ErkulPowerCard(palette: RefugePalette, ship: JSONObject, slots: List<ErkulSlot>, draft: JSONObject,
    metrics: ErkulMetrics, update: (JSONObject) -> Unit, showTelemetry: Boolean = false) {
    val power = remember(ship, slots, draft) { allocateErkulPower(ship, slots, draft) }
    val active = remember(slots, draft) { activeErkulSlots(slots, draft) }
    val signatures = remember(active, power) {
        listOf("ir", "em").map { axis -> active.sumOf { slot ->
            val item = slot.item!!
            val units = resourceUnits(item, false, "Power")
            val ratio = if (item.optString("type") == "WeaponGun") {
                if (power.supplied("weapons") > 0) 1.0 else 0.0
            } else if (units > 0) powerOutputRatio(item, power.supplied(slot.path)) else 1.0
            (onlineResource(item).obj("signature").obj(axis).number("nominal") ?: 0.0) * ratio
        } }
    }
    val radar = active.firstOrNull { it.item!!.optString("type") == "Radar" }
    val aim = radar?.let { (it.item!!.obj("radar").obj("aimAssist").number("distanceMaxAssignment") ?: 0.0) * powerOutputRatio(it.item, power.supplied(it.path)) }
    val groups = listOf("武器", "引擎", "护盾", "量子", "雷达", "维生", "散热") + if (power.consumers.any { it.group == "其他" }) listOf("其他") else emptyList()
    val chartMaximum = groups.maxOf { group -> power.consumers.filter { it.group == group }.sumOf { it.maximum } }.coerceAtLeast(1)
    val glyphs = listOf(ShipSymbols.weapons, ShipSymbols.engine, ShipSymbols.shield, ShipSymbols.quantum, ShipSymbols.radar, ShipSymbols.life, ShipSymbols.cooler, ShipSymbols.power)
    RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("电力分配", style = RefugeTypography.title(palette), modifier = Modifier.weight(1f))
                Text("${power.capacity - power.used} / ${power.capacity} 格可用", style = RefugeTypography.caption(palette).copy(color = palette.accent))
            }
            if (showTelemetry) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("红外" to signatures[0], "电磁" to signatures[1], "截面 Z" to (ship.obj("vehicle").obj("crossSection").number("z") ?: 0.0)).forEach { (label, value) ->
                        Column { Text(label, style = RefugeTypography.caption(palette)); Text(String.format(java.util.Locale.US, "%,.0f", value), style = RefugeTypography.headline(palette)) }
                    }
                }
                val coolingRatio = if (metrics.cooling > 0) (metrics.coolingUsed / metrics.cooling).coerceIn(0.0, 1.0).toFloat() else 0f
                Text("散热负载  ${(coolingRatio * 100).roundToInt()}%", style = RefugeTypography.caption(palette))
                LinearProgressIndicator(coolingRatio, Modifier.fillMaxWidth().height(4.dp), palette.accent, palette.glassStrong)
                aim?.let { Text("瞄准辅助  ${it.roundToInt()} m", style = RefugeTypography.caption(palette)) }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.SpaceEvenly) {
                groups.forEachIndexed { index, group ->
                    val members = power.consumers.filter { it.group == group }
                    val maximum = members.sumOf { it.maximum }
                    val value = members.sumOf { power.supplied(it.path) }
                    val onChange by rememberUpdatedState<(Int) -> Unit>({ update(setPowerGroup(power, draft, group, it)) })
                    Column(Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(value.toString(), style = RefugeTypography.headline(palette))
                        Canvas(Modifier.width(48.dp).height(144.dp).semantics {
                            contentDescription = "$group 电力"; stateDescription = "$value / $maximum 格"
                            progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..maximum.coerceAtLeast(1).toFloat(), maximum.coerceAtLeast(1) - 1)
                            setProgress { onChange(it.roundToInt()); true }
                        }.pointerInput(group, maximum, chartMaximum) {
                            detectTapGestures { pos -> if (maximum > 0) onChange(((1f - pos.y / size.height) * chartMaximum).roundToInt().coerceIn(0, maximum)) }
                        }.pointerInput(group, maximum, chartMaximum) {
                            detectVerticalDragGestures { change, _ -> change.consume(); if (maximum > 0) onChange(((1f - change.position.y / size.height) * chartMaximum).roundToInt().coerceIn(0, maximum)) }
                        }) {
                            val count = maximum.coerceAtLeast(1)
                            val gap = 3.dp.toPx()
                            val height = (size.height / chartMaximum - gap).coerceAtLeast(1f)
                            repeat(count) { pip ->
                                drawRoundRect(if (pip < value) palette.accent else palette.text.copy(alpha = .08f),
                                    Offset(4.dp.toPx(), size.height - (pip + 1) * (height + gap)), Size(size.width - 8.dp.toPx(), height), CornerRadius(3.dp.toPx()))
                            }
                        }
                        Icon(glyphs[index], null, tint = if (value > 0) palette.accent else palette.textMuted, modifier = Modifier.size(22.dp))
                        Text(group, style = RefugeTypography.caption(palette))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val nav = draft.optString("flightMode") == "NAV"
                TextButton({ update(JSONObject(draft.toString()).put("flightMode", "SCM").apply { remove("powerSegments") }) }) { Text("SCM", color = if (!nav) palette.accent else palette.textMuted) }
                TextButton({ update(JSONObject(draft.toString()).put("flightMode", "NAV").apply { remove("powerSegments") }) }) { Text("NAV", color = if (nav) palette.accent else palette.textMuted) }
                Spacer(Modifier.weight(1f))
                TextButton({ update(JSONObject(draft.toString()).apply { remove("powerSegments") }) }) { Text("自动分配", color = palette.accent) }
            }
        }
    }
}
