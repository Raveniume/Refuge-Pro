package com.refuge.next.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import com.refuge.next.R
import org.json.JSONObject
import kotlin.math.*

@Composable
internal fun ErkulPowerCard(backdrop: com.kyant.backdrop.backdrops.LayerBackdrop, palette: RefugePalette, ship: JSONObject, slots: List<ErkulSlot>, draft: JSONObject,
    metrics: ErkulMetrics, update: (JSONObject) -> Unit, showTelemetry: Boolean = false) {
    val power = remember(ship, slots, draft) { allocateErkulPower(ship, slots, draft) }
    val active = remember(slots, draft) { activeErkulSlots(slots, draft) }
    val available = remember(ship, slots, draft.optString("flightMode")) {
        allocateErkulPower(ship, slots, JSONObject(draft.toString()).apply { remove("disabled"); remove("powerSegments") })
    }
    val signatures = remember(ship, active, power, metrics) {
        erkulSignatures(ship, active, power, metrics.cooling, metrics.coolingUsed)
    }
    val radar = active.firstOrNull { it.item!!.optString("type") == "Radar" }
    val aim = radar?.let {
        val settings = it.item!!.obj("radar").obj("aimAssist")
        val minimum = settings.number("distanceMinAssignment") ?: 0.0
        val maximum = settings.number("distanceMaxAssignment") ?: minimum
        if (power.supplied(it.path) <= 0) 0.0 else minimum + (maximum - minimum) * powerOutputRatio(it.item, power.supplied(it.path))
    }
    val groups = listOf("武器", "引擎", "护盾", "量子", "雷达", "维生", "散热") + if (power.consumers.any { it.group == "其他" }) listOf("其他") else emptyList()
    val chartMaximum = groups.maxOf { group -> available.consumers.filter { it.group == group }.sumOf { it.maximum } }.coerceAtLeast(1)
    val glyphs = listOf(
        R.drawable.erkul_power_weapons,
        R.drawable.erkul_power_engines,
        R.drawable.erkul_power_shields,
        R.drawable.erkul_power_quantum_drives,
        R.drawable.erkul_power_radars,
        R.drawable.erkul_power_life_support,
        R.drawable.erkul_power_coolers,
        R.drawable.erkul_power_power_plant_output,
    )
    RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("电力分配", style = RefugeTypography.title(palette), modifier = Modifier.weight(1f))
                Text("${power.capacity - power.used} / ${power.capacity} 格可用", style = RefugeTypography.caption(palette).copy(color = palette.accent))
            }
            if (showTelemetry) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(
                        Triple("红外", signatures.infrared, R.drawable.erkul_power_infra_red),
                        Triple("电磁", signatures.electromagnetic, R.drawable.erkul_power_electromagnetic),
                        Triple("截面 Z", signatures.crossSectionZ, R.drawable.erkul_power_cross_section),
                    ).forEach { (label, value, icon) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Image(
                                painter = painterResource(icon),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(palette.accent),
                                modifier = Modifier.size(19.dp),
                            )
                            Column {
                                Text(label, style = RefugeTypography.caption(palette))
                                Text(String.format(java.util.Locale.US, "%,.0f", value), style = RefugeTypography.headline(palette))
                            }
                        }
                    }
                }
                // Keep the label truthful when a build exceeds its cooling
                // capacity. Only the visual bar is capped at its track end.
                val coolingRatio = if (metrics.cooling > 0) {
                    (metrics.coolingUsed / metrics.cooling).coerceAtLeast(0.0).toFloat()
                } else 0f
                Text("散热负载  ${(coolingRatio * 100).roundToInt()}%", style = RefugeTypography.caption(palette))
                LinearProgressIndicator(coolingRatio.coerceAtMost(1f), Modifier.fillMaxWidth().height(4.dp),
                    if (coolingRatio > 1f) palette.error else palette.accent, palette.glassStrong)
                aim?.let { Text("瞄准辅助  ${it.roundToInt()} m", style = RefugeTypography.caption(palette)) }
            }
            // Keep every group visible on a compact phone. The previous fixed
            // 48dp columns overflowed when a ship exposed eight groups, so the
            // last pips and icon were clipped instead of being usable.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                groups.forEachIndexed { index, group ->
                    val members = available.consumers.filter { it.group == group }
                    val maximum = members.sumOf { it.maximum }
                    val value = members.sumOf { power.supplied(it.path) }
                    val onChange by rememberUpdatedState<(Int) -> Unit>({ update(setPowerGroup(power, draft, group, it)) })
                    Column(
                        // Let the row divide the available width evenly. A
                        // fixed 46dp cap made eight groups exceed a compact
                        // phone and clipped the last power icon.
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(value.toString(), style = RefugeTypography.headline(palette))
                        Canvas(Modifier.fillMaxWidth().height(144.dp).semantics {
                            contentDescription = "$group 电力"; stateDescription = "$value / $maximum 格"
                            progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..maximum.coerceAtLeast(1).toFloat(), maximum.coerceAtLeast(1) - 1)
                            setProgress { onChange(it.roundToInt()); true }
                        }.pointerInput(group, maximum, chartMaximum) {
                            detectTapGestures { pos -> if (maximum > 0) onChange(((1f - pos.y / size.height) * chartMaximum).roundToInt().coerceIn(0, maximum)) }
                        }) {
                            val count = maximum.coerceAtLeast(1)
                            val gap = 3.dp.toPx()
                            val height = (size.height / chartMaximum - gap).coerceAtLeast(1f)
                            repeat(count) { pip ->
                                drawRoundRect(
                                    if (pip < value) palette.accent else palette.text.copy(alpha = .08f),
                                    Offset(2.dp.toPx(), size.height - (pip + 1) * (height + gap)),
                                    Size((size.width - 4.dp.toPx()).coerceAtLeast(1f), height),
                                    CornerRadius(3.dp.toPx()),
                                )
                            }
                        }
                        val allDisabled = members.isNotEmpty() && members.all { member -> member.path in draft.strings("disabled") }
                        // Keep the glyph visually compact while giving the
                        // power toggle the same 44 dp target as every other
                        // native control. This makes tapping a group reliable
                        // on a phone without changing the Erkul geometry.
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .semantics {
                                    role = Role.Button
                                    contentDescription = if (allDisabled) "启用$group" else "停用$group"
                                    if (members.isEmpty()) disabled()
                                }
                                .clickable(
                                    enabled = members.isNotEmpty(),
                                    interactionSource = null,
                                    indication = null,
                                    onClick = { update(togglePowerGroup(draft, members, !allDisabled)) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(glyphs[index]),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(if (allDisabled) palette.textMuted else palette.accent),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(group, style = RefugeTypography.caption(palette))
                    }
                }
            }
            val componentConsumers = available.consumers.filter { it.item != null && it.item.optString("type") in setOf("Cooler", "PowerPlant") }
            if (componentConsumers.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("核心组件", style = RefugeTypography.caption(palette))
                    componentConsumers.forEach { consumer ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(consumer.item?.itemName().orEmpty(), style = RefugeTypography.body(palette), modifier = Modifier.weight(1f), maxLines = 1)
                            Text("${power.supplied(consumer.path)} / ${consumer.maximum} pips", style = RefugeTypography.caption(palette).copy(color = palette.accent))
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val nav = draft.optString("flightMode") == "NAV"
                RefugeLiquidModeSelector(backdrop, palette, listOf("SCM", "NAV"), if (nav) 1 else 0,
                    { update(JSONObject(draft.toString()).put("flightMode", if (it == 0) "SCM" else "NAV").apply { remove("powerSegments") }) },
                    modifier = Modifier.weight(1f), fixedWidth = true)
                Spacer(Modifier.width(12.dp))
                RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.refresh, "自动分配",
                    { update(JSONObject(draft.toString()).apply { remove("powerSegments") }) })
            }
        }
    }
}
