package com.refuge.next.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.data.*
import com.refuge.next.design.*
import com.refuge.next.material.*
import org.json.JSONObject
import java.util.Locale

private fun Double.metric() = String.format(Locale.US, "%,.0f", this).replace(',', ' ')
private fun Double.precise() = String.format(Locale.US, "%,.1f", this).removeSuffix(".0")
private fun JSONObject.damage() = keys().asSequence().sumOf { number(it) ?: 0.0 }

@Composable
private fun DashboardPanel(
    palette: RefugePalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    source: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = palette.accent, modifier = Modifier.size(22.dp))
                Text(title, style = RefugeTypography.headline(palette))
                Spacer(Modifier.weight(1f))
                source?.let { Text(it, style = RefugeTypography.caption(palette), color = palette.textMuted) }
            }
            content()
        }
    }
}

@Composable
private fun DashboardSection(palette: RefugePalette, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = RefugeTypography.caption(palette).copy(fontWeight = FontWeight.SemiBold, color = palette.textSecondary))
        content()
    }
}

@Composable
private fun MetricTiles(
    palette: RefugePalette,
    values: List<Pair<String, String>>,
    emphasis: Int = -1,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    icons: List<androidx.compose.ui.graphics.vector.ImageVector?>? = null,
) {
    val columns = if (LocalDensity.current.fontScale > 1.3f) 1 else values.size.coerceAtMost(3)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        values.chunked(columns).forEach { chunk ->
            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chunk.forEach { entry ->
                    val highlighted = values.indexOf(entry) == emphasis
                    Column(
                        Modifier.weight(1f).fillMaxHeight().background(
                            if (highlighted) palette.accentSoft else palette.contentSurfaceStrong,
                            refugeContinuousShape(10.dp),
                        ).padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (icons?.getOrNull(values.indexOf(entry)) ?: icon)?.let {
                                Icon(it, null, tint = if (highlighted) palette.accent else palette.textMuted, modifier = Modifier.size(17.dp))
                            }
                            Text(entry.second, style = RefugeTypography.value(palette).copy(
                                fontSize = 21.sp,
                                lineHeight = 25.sp,
                                color = if (highlighted) palette.accent else palette.text,
                            ))
                        }
                        Text(entry.first, style = RefugeTypography.caption(palette))
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributeChips(palette: RefugePalette, label: String, entries: List<Pair<String, String>>) {
    DashboardSection(palette, label) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.forEach { (name, value) ->
                Row(
                    Modifier.background(palette.contentSurfaceStrong, refugeContinuousShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(name, style = RefugeTypography.caption(palette))
                    Text(value, style = RefugeTypography.caption(palette).copy(color = palette.text, fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

private fun damageGroup(slot: ErkulSlot): String {
    return when (erkulWeaponLocation(slot)) {
        "mannedTurret" -> "有人炮塔"
        "remoteOperator" -> "遥控炮塔"
        "pdc" -> "PDS"
        else -> "驾驶员"
    }
}

@Composable
internal fun ErkulDashboard(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    ship: JSONObject,
    slots: List<ErkulSlot>,
    catalog: ErkulCatalog,
    draft: JSONObject,
    metrics: ErkulMetrics,
    update: (JSONObject) -> Unit,
) {
    val active = remember(slots, draft) { activeErkulSlots(slots, draft) }
    val weapons = active.filter { it.item?.optString("type") == "WeaponGun" && draft.optString("flightMode") != "NAV" }
    val allocation = remember(ship, slots, draft) { allocateErkulPower(ship, slots, draft) }
    val ratio = if (metrics.weaponDemand <= 0) 1.0 else (allocation.supplied("weapons") / metrics.weaponDemand).coerceIn(0.0, 1.0)
    val alpha = weapons.mapNotNull { it.item?.let(::weaponAlphaMax) }
        .takeIf { it.size == weapons.size }?.sum()?.metric() ?: "—"
    val armorItem = active.firstOrNull { it.item?.has("armor") == true }?.item
    val armor = armorItem?.obj("armor") ?: JSONObject()
    val shields = active.filter { it.item?.has("shield") == true && allocation.supplied(it.path) > 0 }
    val sourceLabel = "${catalog.branch} · ${catalog.version}"
    val stockFlight = ship.obj("precomputed").obj("flight")

    val damagePanel: @Composable () -> Unit = {
        DashboardPanel(palette, ShipSymbols.damage, "火力 · Damage", sourceLabel) {
            RefugeLiquidModeSelector(
                backdrop, palette, listOf("LOCK", "GIMBAL / AUTO / PRE"),
                if (draft.optString("gimbalMode") == "GIMBAL") 1 else 0,
                { update(JSONObject(draft.toString()).put("gimbalMode", if (it == 0) "LOCK" else "GIMBAL")) },
                modifier = Modifier.fillMaxWidth(), fixedWidth = true,
            )
            MetricTiles(palette, listOf("Σ ALPHA" to alpha, "持续 DPS" to metrics.sustained.metric(), "爆发 DPS" to metrics.burst.metric()), emphasis = 1, icon = ShipSymbols.weapons)
            DashboardSection(palette, "武器来源 · Weapon source") {
                DamageTableRow(palette, listOf("来源", "Σ ALPHA", "持续", "爆发"), header = true)
                listOf("驾驶员" to "Pilot", "有人炮塔" to "Manned turret", "遥控炮塔" to "Remote turret", "PDS" to "PDS").forEach { (key, name) ->
                    val group = weapons.filter { damageGroup(it) == key }
                    val groupAlpha = group.takeIf { it.isNotEmpty() }?.mapNotNull { it.item?.let(::weaponAlphaMax) }
                        ?.takeIf { it.size == group.size }?.sum()?.metric() ?: "—"
                    Divider(color = palette.divider, thickness = .5.dp)
                    DamageTableRow(
                        palette, listOf(name, groupAlpha,
                            if (group.isEmpty()) "—" else group.sumOf { weaponSustained(weaponForMode(it.item!!, draft), ratio) }.metric(),
                            if (group.isEmpty()) "—" else group.sumOf { weaponBurst(weaponForMode(it.item!!, draft)) }.metric()),
                    )
                }
            }
            MetricTiles(palette, listOf(
                "导弹伤害" to active.filter { it.item?.optString("type") == "Missile" }.sumOf { it.item!!.obj("missile").obj("damage").damage() }.metric(),
                "炸弹伤害" to active.filter { it.item?.optString("type") == "Bomb" }.takeIf { it.isNotEmpty() }
                    ?.sumOf { it.item!!.obj("bomb").obj("damage").damage() }?.metric().orEmpty().ifBlank { "—" },
            ), icons = listOf(ShipSymbols.missile, ShipSymbols.bomb))
        }
    }

    val defensePanel: @Composable () -> Unit = {
        DashboardPanel(palette, ShipSymbols.defense, "防御 · Defense", sourceLabel) {
            MetricTiles(palette, listOf("护盾 HP" to metrics.shield.metric(), "回复 HP/s" to metrics.shieldRegen.metric()), emphasis = 1, icon = ShipSymbols.shield)
            RefugeInfoRows(palette, listOf("护盾形状" to ship.optString("shield").ifBlank { "—" }))
            fun shieldValues(field: String) = listOf("physical" to "Phys", "energy" to "Energy", "distortion" to "Distort").map { (key, title) ->
                val values = shields.mapNotNull { it.item!!.obj("shield").obj(field).obj(key).number("max") }.distinct()
                title to when {
                    values.isEmpty() -> "—"
                    values.size == 1 -> "${(values.first() * 100).precise()}%"
                    else -> "${(values.min() * 100).precise()}–${(values.max() * 100).precise()}%"
                }
            }
            AttributeChips(palette, "抗性 · Resistance", shieldValues("resistance"))
            AttributeChips(palette, "吸收 · Absorption", shieldValues("absorption"))
            DashboardSection(palette, "装甲 · Armor") {
                MetricTiles(palette, listOf("ARMOR HP" to (armorItem?.obj("health")?.number("hp")?.metric() ?: "—")), icon = ShipSymbols.hull)
                listOf("deflectionValue" to "偏转 · Deflection", "damageMultiplier" to "减伤 · Damage reduction", "selfResistance" to "自身抗性 · Self resistance").forEach { (field, label) ->
                    AttributeChips(palette, label, listOf("physical" to "Phys", "energy" to "Energy").map { (key, name) ->
                        name to (armor.obj(field).number(key)?.let { if (field == "deflectionValue") it.precise() else "${((1 - it) * 100).precise()}%" } ?: "—")
                    })
                }
                AttributeChips(palette, "信号 · Signature", listOf("signalElectromagnetic" to "EM", "signalInfrared" to "IR", "signalCrossSection" to "CS").map { (key, label) ->
                    label to (armor.number(key)?.let { String.format(Locale.US, "%+.0f%%", (it - 1) * 100) } ?: "—")
                })
                AttributeChips(palette, "对抗措施 · Countermeasures", listOf("Decoy", "Noise").map { type ->
                    val count = active.filter { slot ->
                        val item = slot.item ?: return@filter false
                        item.optString("type") == "WeaponDefensive" && item.obj("i18n").optString("shortName").equals(type, true)
                    }.sumOf { it.item!!.obj("ammo").optInt("maxCount") }
                    type to count.takeIf { it > 0 }?.toString().orEmpty().ifBlank { "—" }
                })
            }
        }
    }

    val hullPanel: @Composable () -> Unit = {
        DashboardPanel(palette, ShipSymbols.hull, "船体 · Hull", sourceLabel) {
            val hp = ship.obj("precomputed").obj("hp")
            val dimensions = ship.obj("vehicle").obj("dimensions")
            RefugeInfoRows(palette, listOf(
                "尺寸" to listOf("y", "x", "z").mapNotNull { dimensions.number(it)?.precise() }.joinToString(" × ").let { if (it.isBlank()) "—" else "$it m" },
                "质量" to "${metrics.mass.metric()} kg",
            ))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${hp.number("total")?.metric() ?: "—"}", style = RefugeTypography.value(palette))
                Text("hp", style = RefugeTypography.caption(palette).copy(color = palette.textMuted), modifier = Modifier.padding(bottom = 4.dp))
            }
            val groups = listOf("vital" to "Vital", "secondary" to "Secondary", "breakable" to "Breakable", "subpart" to "Subpart")
            val total = hp.number("total") ?: 0.0
            Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                groups.forEachIndexed { index, (key, _) ->
                    val value = hp.number(key) ?: 0.0
                    if (value > 0 && total > 0) Box(Modifier.weight(value.toFloat()).fillMaxHeight().background(palette.accent.copy(alpha = 1f - index * .18f)))
                }
            }
            val partCounts = remember(ship) { erkulHullPartCounts(ship) }
            groups.chunked(2).forEach { pair ->
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pair.forEach { (key, name) ->
                        Column(Modifier.weight(1f).fillMaxHeight().background(palette.contentSurfaceStrong, refugeContinuousShape(10.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(7.dp).background(palette.accent.copy(alpha = 1f - groups.indexOf(key to name) * .18f), refugeContinuousShape(2.dp)))
                                Text(name, style = RefugeTypography.caption(palette))
                            }
                            Text((partCounts[key] ?: 0).toString(), style = RefugeTypography.value(palette))
                            Text("${hp.number(key)?.metric() ?: "—"} hp", style = RefugeTypography.caption(palette))
                        }
                    }
                }
            }
        }
    }

    val flightPanel: @Composable () -> Unit = {
        DashboardPanel(palette, ShipSymbols.flight, "飞行 · Flight", sourceLabel) {
            val controller = slots.firstOrNull { it.item?.optString("type") == "FlightController" }
            val blades = remember(catalog, controller?.path) {
                if (controller == null) emptyList() else catalog.components.values.filter {
                    it.optString("type") == "FlightController" && erkulSlotAccepts(controller, it)
                }.sortedBy { it.itemName() }
            }
            if (blades.size > 1 && controller != null) {
                val labels = blades.map { blade -> when {
                    blade.itemName().contains("Handling", true) || blade.itemName().contains(" PHB ", true) -> "HND"
                    blade.itemName().contains("Speed", true) || blade.itemName().contains(" TSB ", true) -> "SPD"
                    blade.itemName().contains("Standard", true) -> "STD"
                    else -> blade.obj("i18n").optString("shortName").ifBlank { blade.itemName() }
                } }
                RefugeLiquidModeSelector(backdrop, palette, labels, blades.indexOfFirst { it.optString("className") == controller.item?.optString("className") }.coerceAtLeast(0), { index ->
                    update(JSONObject(draft.toString()).put("overrides", JSONObject(draft.obj("overrides").toString()).put(controller.path, blades[index].optString("className"))))
                }, modifier = Modifier.fillMaxWidth(), fixedWidth = true)
            }
            val params = controller?.item?.obj("params") ?: JSONObject()
            val maxAngular = params.obj("maxAngularVelocity")
            val afterburner = params.obj("afterburner").obj("angVelocityMultiplier")
            fun angular(axis: String, baseKey: String, boostKey: String): String {
                val base = maxAngular.number(axis) ?: stockFlight.number(baseKey)
                val boost = if (maxAngular.number(axis) != null) base?.times(afterburner.number(axis) ?: 1.0) else stockFlight.number(boostKey)
                return if (base == null) "—" else "${base.precise()} › ${boost?.precise() ?: base.precise()}"
            }
            Text("基础 › 额定加力", style = RefugeTypography.caption(palette))
            MetricTiles(palette, listOf("PITCH · °/s" to angular("x", "pitch", "boostedPitch"), "YAW · °/s" to angular("z", "yaw", "boostedYaw"), "ROLL · °/s" to angular("y", "roll", "boostedRoll")), emphasis = 0, icon = ShipSymbols.flight)
            RefugeInfoRows(palette, listOf(
                "SCM 速度" to "${(params.number("scmSpeed") ?: stockFlight.number("scmSpeed"))?.precise() ?: "—"} m/s",
                "前向加力" to "${(params.number("boostSpeedForward") ?: stockFlight.number("boostSpeedForward"))?.precise() ?: "—"} m/s",
                "NAV 速度" to "${(params.number("maxSpeed") ?: stockFlight.number("maxSpeed"))?.precise() ?: "—"} m/s",
                "氢燃料" to "${ship.obj("precomputed").obj("fuel").number("hydrogenCapacity")?.precise() ?: "—"} SCU",
                "量子燃料" to "${ship.obj("precomputed").obj("fuel").number("quantumCapacity")?.precise() ?: "—"} SCU",
                "量子航程" to "${metrics.quantumRange?.precise() ?: ship.obj("precomputed").obj("quantum").number("rangeGm")?.precise() ?: "—"} Gm",
            ))
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth().testTag("erkul-dashboard")) {
        if (maxWidth >= 720.dp) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { damagePanel(); hullPanel() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { defensePanel(); flightPanel() }
        } else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { damagePanel(); defensePanel(); hullPanel(); flightPanel() }
    }
}

@Composable
private fun DamageTableRow(palette: RefugePalette, columns: List<String>, header: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        columns.forEachIndexed { index, value ->
            Text(value, Modifier.weight(if (index == 0) 1.5f else 1f), style = if (header) RefugeTypography.caption(palette) else RefugeTypography.body(palette), textAlign = if (index == 0) TextAlign.Start else TextAlign.End)
        }
    }
}
