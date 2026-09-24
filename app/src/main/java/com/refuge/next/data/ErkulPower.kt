package com.refuge.next.data

import org.json.JSONObject
import kotlin.math.*

internal data class PowerConsumer(val path: String, val group: String, val maximum: Int, val minimum: Int, val item: JSONObject?)
internal data class PowerAllocation(val capacity: Int, val consumers: List<PowerConsumer>, val segments: Map<String, Int>) {
    val used get() = segments.values.sum()
    fun supplied(path: String) = segments[path] ?: 0
}

internal fun powerGroup(type: String): String = when (type) {
    "WeaponGun" -> "武器"
    "FlightController" -> "引擎"
    "Shield" -> "护盾"
    "QuantumDrive" -> "量子"
    "Radar" -> "雷达"
    "LifeSupportGenerator" -> "维生"
    "Cooler" -> "散热"
    else -> "其他"
}

internal fun activeErkulSlots(slots: List<ErkulSlot>, draft: JSONObject): List<ErkulSlot> {
    val disabled = draft.strings("disabled")
    return slots.filter { slot -> slot.item != null && disabled.none { slot.path == it || slot.path.startsWith("$it/") } }
}

internal fun onlineResource(item: JSONObject) = item.obj("resource").objects("states").firstOrNull { it.optString("name") == "Online" } ?: JSONObject()

/**
 * Erkul's power bar does not simply add generator output. When more than one
 * power plant is online it shares each plant's output across the active
 * plants, then adds the plant-size reserve used by the in-game distribution
 * model. For example, two S3 plants producing 24 segments each render as
 * round(24 / 2) + round(24 / 2) + (2 - 1) * (3 + 3) = 30 segments.
 */
internal fun erkulPowerCapacitySegments(slots: List<ErkulSlot>, draft: JSONObject = JSONObject()): Int {
    val generators = activeErkulSlots(slots, draft).filter { slot ->
        val item = slot.item ?: return@filter false
        // Older cached manifests (and a few test fixtures) only expose the
        // equipment type.  Live Erkul data has both fields, so accept either
        // spelling without treating another producer as a power plant.
        (item.optString("category") == "PowerPlant" || item.optString("type") == "PowerPlant") &&
            resourceUnits(item, output = true, resource = "Power", unit = "powerSegment") > 0.0
    }
    if (generators.isEmpty()) return 0
    val count = generators.size
    val sharedOutput = generators.sumOf { slot ->
        kotlin.math.floor(resourceUnits(slot.item!!, true, "Power", "powerSegment") / count + .5).toInt()
    }
    val sizeReserve = generators.sumOf { it.item!!.optInt("size", 0) }
    return (sharedOutput + (count - 1) * sizeReserve).coerceAtLeast(0)
}

// Erkul rounds units * minimumFraction; a missing/zero fraction means one segment.
internal fun minimumPower(units: Int, fraction: Double): Int =
    if (units <= 0) 0 else floor(units * (fraction.takeIf { it > 0 } ?: (1.0 / units)) + .5).toInt().coerceIn(1, units)

internal fun allocateErkulPower(ship: JSONObject, slots: List<ErkulSlot>, draft: JSONObject): PowerAllocation {
    val active = activeErkulSlots(slots, draft)
    val capacity = erkulPowerCapacitySegments(slots, draft)
    val nav = draft.optString("flightMode") == "NAV"
    val consumers = buildList {
        val pool = ship.obj("vehicle").obj("powerPools").objects("pools").firstOrNull { it.optString("itemType") == "WeaponGun" }?.optInt("poolSize") ?: 0
        if (pool > 0 && !nav && active.any { it.item!!.optString("type") == "WeaponGun" }) add(PowerConsumer("weapons", "武器", pool, 1, null))
        active.forEach { slot ->
            val item = slot.item!!
            val type = item.optString("type")
            if (type == "WeaponGun" || (nav && type == "Shield") || (!nav && type == "QuantumDrive")) return@forEach
            val flow = onlineResource(item).objects("flows").firstOrNull { row -> row.objects("consumes").any { it.optString("resource") == "Power" } } ?: return@forEach
            val demand = flow.objects("consumes").first { it.optString("resource") == "Power" }.optDouble("units", 0.0).roundToInt()
            if (demand > 0) add(PowerConsumer(slot.path, powerGroup(type), demand, minimumPower(demand, flow.optDouble("minimumFraction", 0.0)), item))
        }
    }
    val values = consumers.associate { it.path to 0 }.toMutableMap()
    var remaining = capacity
    // Bring essential systems online first, then spread surplus across enabled devices.
    val priority = listOf("维生", "散热", "引擎", "雷达", "护盾", "量子", "武器", "其他")
    val ordered = consumers.sortedBy { priority.indexOf(it.group) }
    if (draft.has("powerSegments")) {
        val saved = draft.obj("powerSegments")
        ordered.forEach { consumer ->
            val requested = saved.optInt(consumer.path, consumer.minimum).coerceIn(0, min(consumer.maximum, remaining))
            val allocated = requested.takeIf { it >= consumer.minimum } ?: 0
            values[consumer.path] = allocated; remaining -= allocated
        }
    } else {
        ordered.forEach { consumer -> if (remaining >= consumer.minimum) { values[consumer.path] = consumer.minimum; remaining -= consumer.minimum } }
        while (remaining > 0) {
            val candidates = ordered.filter { values.getValue(it.path) in 1 until it.maximum }
            if (candidates.isEmpty()) break
            candidates.forEach { if (remaining > 0) { values[it.path] = values.getValue(it.path) + 1; remaining-- } }
        }
    }
    return PowerAllocation(capacity, consumers, values)
}

internal fun setPowerGroup(allocation: PowerAllocation, draft: JSONObject, group: String, requested: Int): JSONObject {
    val members = allocation.consumers.filter { it.group == group }
    val old = members.sumOf { allocation.supplied(it.path) }
    var remaining = requested.coerceIn(0, min(members.sumOf { it.maximum }, allocation.capacity - allocation.used + old))
    val values = allocation.segments.toMutableMap()
    members.forEach { values[it.path] = 0 }
    members.forEach { if (remaining >= it.minimum) { values[it.path] = it.minimum; remaining -= it.minimum } }
    while (remaining > 0) {
        val candidates = members.filter { values.getValue(it.path) in 1 until it.maximum }
        if (candidates.isEmpty()) break
        candidates.forEach { if (remaining > 0) { values[it.path] = values.getValue(it.path) + 1; remaining-- } }
    }
    return JSONObject(draft.toString()).put("powerSegments", JSONObject(values))
}

/** Toggle every component in a power group while keeping the draft format used by Erkul. */
internal fun togglePowerGroup(draft: JSONObject, members: List<PowerConsumer>, disable: Boolean): JSONObject {
    val next = JSONObject(draft.toString())
    val disabled = next.strings("disabled").toMutableSet()
    members.forEach { member ->
        if (disable) disabled.add(member.path) else disabled.remove(member.path)
    }
    if (disabled.isEmpty()) next.remove("disabled")
    else next.put("disabled", org.json.JSONArray(disabled.toList().sorted()))
    return next
}

internal fun powerOutputRatio(item: JSONObject, supplied: Int): Double {
    val demand = resourceUnits(item, false, "Power")
    if (demand <= 0) return 1.0
    if (supplied <= 0) return 0.0
    val band = onlineResource(item).objects("powerRanges").filter { it.optDouble("start") <= supplied }.maxByOrNull { it.optDouble("start") }
    return min(1.0, supplied / demand) * (band?.number("modifier") ?: 1.0)
}
