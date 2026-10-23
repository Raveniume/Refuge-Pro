package com.refuge.next.data

import org.json.JSONObject
import kotlin.math.*

internal data class ErkulMetrics(
    val burst: Double, val sustained: Double, val pilotBurst: Double, val shield: Double,
    val shieldRegen: Double, val power: Double, val powerUsed: Double, val cooling: Double,
    val coolingUsed: Double, val mass: Double, val quantumSpeed: Double?, val quantumRange: Double?,
    val weaponPool: Double, val weaponDemand: Double,
)

/** Public ship data contains an array of root parts, each with child parts. */
internal fun erkulHullPartCounts(ship: JSONObject): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()
    fun visit(part: JSONObject) {
        val category = part.optString("criticality")
        if (category.isNotBlank()) counts[category] = (counts[category] ?: 0) + 1
        part.objects("children").forEach(::visit)
    }
    ship.obj("vehicle").objects("parts").forEach(::visit)
    return counts
}

internal fun resourceUnits(item: JSONObject, output: Boolean, resource: String, unit: String? = null): Double {
    val online = item.obj("resource").objects("states").firstOrNull { it.optString("name") == "Online" } ?: return 0.0
    return online.objects("flows").flatMap { it.objects(if (output) "produces" else "consumes") }
        .filter { it.optString("resource") == resource && (unit == null || it.optString("unitKind") == unit) }
        .sumOf { it.number("units") ?: 0.0 }
}

/** Erkul uses the Travelling resource flow, not the legacy qdrive tuning field. */
internal fun quantumFuelPerGm(item: JSONObject): Double? = item.obj("resource").objects("states")
    .flatMap { it.objects("flows") }.flatMap { it.objects("consumes") }
    .firstOrNull { it.optString("resource") == "QuantumFuel" }?.number("units")
    ?: item.obj("qdrive").number("quantumFuelRequirement")

/** Erkul's alpha model: fire-action projectile count and damage multiplier are
 * part of the shot. Bare ammo containers and beams do not have alpha damage. */
internal fun weaponAlphaMax(item: JSONObject): Double? {
    val weapon = item.obj("weapon")
    val ammo = weapon.obj("ammo")
    if (ammo.optString("kind").isBlank() || ammo.optString("kind") == "countermeasure") return null
    val fire = weapon.objects("fireActions").firstOrNull() ?: return null
    if (fire.optString("kind") == "beam") return null
    val damage = ammo.obj("damage")
    if (damage.length() == 0) return null
    val charge = if (fire.optString("kind") == "charged") fire.number("maxChargeDamageMultiplier") ?: 1.0 else 1.0
    val totalDamage = damage.keys().asSequence().sumOf { damage.number(it) ?: 0.0 }
    return totalDamage * (fire.number("pelletCount") ?: 1.0) * (fire.number("damageMultiplier") ?: 1.0) * charge
}

private fun fireRateHz(fire: JSONObject): Double = when (fire.optString("kind")) {
    "single", "rapid" -> (fire.number("fireRate") ?: 0.0) / 60.0
    "burst" -> {
        val count = fire.number("shotCount") ?: 0.0
        val duration = 60.0 + (fire.number("cooldownTime") ?: 0.0) * count
        if (duration <= 0.0) 0.0 else count * (fire.number("fireRate") ?: 0.0) / duration
    }
    "charged" -> {
        val interval = max(fire.number("cooldownTime") ?: 0.0, fire.number("fireRate")?.takeIf { it > 0 }?.let { 60.0 / it } ?: 0.0)
        val cycle = (fire.number("chargeTime") ?: 0.0) + interval
        if (cycle <= 0.0) 0.0 else 1.0 / cycle
    }
    else -> 0.0
}

internal fun weaponBurst(item: JSONObject): Double {
    val weapon = item.obj("weapon")
    val fire = weapon.objects("fireActions").firstOrNull() ?: return 0.0
    if (fire.optString("kind") == "beam") return fire.number("damagePerSecond") ?: 0.0
    val alpha = weaponAlphaMax(item) ?: return 0.0
    return alpha * fireRateHz(fire)
}

internal fun weaponForMode(item: JSONObject, draft: JSONObject): JSONObject {
    if (draft.optString("gimbalMode") != "GIMBAL") return item
    val multiplier = item.objects("gimbalModes").firstOrNull()?.number("fireRateMultiplier") ?: 1.0
    return JSONObject(item.toString()).apply {
        obj("weapon").objects("fireActions").forEach { fire ->
            fire.number("fireRate")?.let { fire.put("fireRate", it * multiplier) }
        }
    }
}

/**
 * Mirrors the public Erkul calculator's two sustained-DPS paths.
 * Energy weapons use a rounded pool magazine and recharge cycle. Ballistic
 * weapons use net heat per shot and count complete shots before overheat;
 * treating both as one continuous cycle was the source of the old DPS drift.
 */
internal fun weaponSustained(item: JSONObject, poolRatio: Double): Double {
    val weapon = item.obj("weapon")
    val fire = weapon.objects("fireActions").firstOrNull() ?: return 0.0
    val rate = fireRateHz(fire)
    val alpha = weaponAlphaMax(item) ?: return if (fire.optString("kind") == "beam") {
        fire.number("damagePerSecond") ?: 0.0
    } else 0.0
    if (rate <= 0.0 || alpha <= 0.0) return 0.0

    val regen = weapon.optJSONObject("regen")
    if (regen != null) {
        val pool = kotlin.math.round((regen.number("maxAmmoLoad") ?: 0.0) * poolRatio)
        val refill = (regen.number("maxRegenPerSec") ?: 0.0) * poolRatio
        if (pool <= 0.0 || refill <= 0.0) return 0.0
        val recharge = (regen.number("regenerationCooldown") ?: 0.0) + pool / refill
        val continuous = pool / rate
        val cycle = recharge + continuous
        return if (cycle > 0.0) pool * alpha / cycle else 0.0
    }

    val heat = weapon.obj("connection").obj("heat")
    val heatPerShot = fire.number("heatPerShot") ?: 0.0
    if (heat.length() == 0 || heatPerShot <= 0.0) return alpha * rate
    val timePerShot = 1.0 / rate
    val coolingBeforeNextShot = max(
        0.0,
        timePerShot - (heat.number("timeTillCoolingStarts") ?: 0.0),
    ) * (heat.number("coolingPerSecond") ?: 0.0)
    val netHeatPerSecond = heatPerShot * rate - coolingBeforeNextShot * rate
    if (netHeatPerSecond <= 0.0) return alpha * rate
    val resetTemperature = heat.number("temperatureAfterOverheatFix") ?: heat.number("minTemperature") ?: 0.0
    val reducedTemperature = weapon.obj("connection").number("heatReduceWhenOverheatIsFixed")?.let {
        max(heat.number("minTemperature") ?: 0.0, (heat.number("overheatTemperature") ?: 0.0) - it)
    } ?: resetTemperature
    val usableHeat = (heat.number("overheatTemperature") ?: 0.0) - reducedTemperature
    if (usableHeat <= 0.0) return alpha * rate
    val overheatTime = usableHeat / netHeatPerSecond
    val shots = ceil(overheatTime * rate)
    val cycle = overheatTime + (heat.number("overheatFixTime") ?: 0.0)
    return if (cycle > 0.0) shots * alpha / cycle else 0.0
}

/**
 * Heat is represented by two different fields in the Erkul data set.  Items
 * that radiate heat while online expose `connection.heatRateOnline`; weapons
 * normally expose heat per shot on their fire action instead.  Keeping this
 * conversion here makes the performance sheet useful even when no component
 * has an explicit Coolant consume flow (which is the common case for weapons).
 */
internal fun heatGenerationPerSecond(item: JSONObject): Double {
    val weapon = item.obj("weapon")
    val connection = weapon.obj("connection")
    val online = connection.number("heatRateOnline") ?: 0.0
    val firing = weapon.objects("fireActions").sumOf { action ->
        val heatPerShot = action.number("heatPerShot") ?: 0.0
        val fireRate = action.number("fireRate") ?: 0.0
        val pellets = (action.number("pelletCount") ?: 1.0).coerceAtLeast(1.0)
        if (heatPerShot > 0.0 && fireRate > 0.0) heatPerShot * fireRate / 60.0 * pellets else 0.0
    }
    return (online + firing).coerceAtLeast(0.0)
}

/** Weapon barrel heat is not ship coolant. Erkul's coolant model uses
 * generator load plus the powered shield/radar/life-support/quantum flows. */
internal fun erkulPowerDraw(active: List<ErkulSlot>, allocation: PowerAllocation): Double =
    allocation.segments.filterKeys { it != "weapons" }.values.sum() +
        min(allocation.supplied("weapons").toDouble(), active.filter { it.item!!.optString("type") == "WeaponGun" }
            .sumOf { resourceUnits(it.item!!, false, "Power") })

internal fun erkulCoolingDemand(active: List<ErkulSlot>, allocation: PowerAllocation): Double =
    erkulPowerDraw(active, allocation) + active.filter {
        it.item!!.optString("type") in setOf("Shield", "Radar", "LifeSupportGenerator", "QuantumDrive")
    }.sumOf { slot ->
        resourceUnits(slot.item!!, false, "Power") * powerOutputRatio(slot.item, allocation.supplied(slot.path))
    }

internal data class ErkulSignatures(val infrared: Double, val electromagnetic: Double, val crossSectionZ: Double)

internal fun erkulSignatures(ship: JSONObject, active: List<ErkulSlot>, allocation: PowerAllocation,
    cooling: Double, coolingUsed: Double): ErkulSignatures {
    val armor = active.firstOrNull { it.item!!.has("armor") }?.item?.obj("armor") ?: JSONObject()
    fun nominal(item: JSONObject, axis: String) = onlineResource(item).obj("signature").obj(axis).number("nominal") ?: 0.0
    val heatRatio = if (cooling > 0) (coolingUsed / cooling).coerceIn(0.0, 1.0) else 1.0
    val infrared = active.filter { it.item!!.optString("type") == "Cooler" }.sumOf {
        nominal(it.item!!, "ir") * powerOutputRatio(it.item, allocation.supplied(it.path))
    } * heatRatio * (armor.number("signalInfrared") ?: 1.0)
    val plants = active.filter { it.item!!.optString("type") == "PowerPlant" }
    val load = if (allocation.capacity > 0) (erkulPowerDraw(active, allocation) / allocation.capacity).coerceIn(0.0, 1.0) else 0.0
    val plantSegments = if (plants.isEmpty()) 0.0 else floor(allocation.capacity * load + .5) / plants.size
    val weapons = active.filter { it.item!!.optString("type") == "WeaponGun" }
    val weaponBlocks = ceil(weapons.sumOf { resourceUnits(it.item!!, false, "Power") }).coerceAtLeast(1.0)
    val electromagnetic = active.sumOf { slot ->
        val item = slot.item!!
        val ratio = when (item.optString("type")) {
            "PowerPlant" -> load * powerRangeModifier(item, plantSegments)
            "WeaponGun" -> allocation.supplied("weapons") / weaponBlocks * powerRangeModifier(item, allocation.supplied("weapons").toDouble())
            else -> if (resourceUnits(item, false, "Power") > 0) powerOutputRatio(item, allocation.supplied(slot.path)) else 0.0
        }
        nominal(item, "em") * ratio
    } * (armor.number("signalElectromagnetic") ?: 1.0)
    return ErkulSignatures(infrared, electromagnetic,
        (ship.obj("vehicle").obj("crossSection").number("z") ?: 0.0) * (armor.number("signalCrossSection") ?: 1.0))
}

/** Coolant generated by a powered convert flow, in Erkul's rating units. */
internal fun coolingGenerationPerSecond(item: JSONObject, powerRatio: Double): Double {
    if (powerRatio <= 0.0) return 0.0
    val generated = resourceUnits(item, output = true, resource = "Coolant")
    return generated * powerRatio
}

internal fun calculateErkulMetrics(ship: JSONObject, slots: List<ErkulSlot>, draft: JSONObject): ErkulMetrics {
    val active = activeErkulSlots(slots, draft)
    val allocation = allocateErkulPower(ship, slots, draft)
    val weapons = active.filter { it.item!!.optString("type") == "WeaponGun" && draft.optString("flightMode") != "NAV" }
    // The rendered capacity follows Erkul's multi-generator distribution
    // model, rather than summing the raw output of every generator.
    val power = allocation.capacity.toDouble()
    val fixedPool = ship.obj("vehicle").obj("powerPools").objects("pools")
        .firstOrNull { it.optString("itemType") == "WeaponGun" }?.number("poolSize") ?: 0.0
    val demand = ceil(weapons.sumOf { resourceUnits(it.item!!, false, "Power") })
    val pool = allocation.supplied("weapons").toDouble()
    val ratio = if (demand <= 0) 1.0 else min(1.0, pool / demand)
    val cooling = active.sumOf { slot ->
        val item = slot.item!!
        val itemRatio = if (item.optString("type") == "WeaponGun") ratio
        else powerOutputRatio(item, allocation.supplied(slot.path))
        coolingGenerationPerSecond(item, itemRatio)
    }
    val coolingUsed = erkulCoolingDemand(active, allocation)
    val quantumItem = active.firstOrNull { it.item!!.has("qdrive") }?.item
    val quantum = quantumItem?.obj("qdrive")
    val fuel = ship.obj("precomputed").obj("fuel").number("quantumCapacity")
    val burn = quantumItem?.let(::quantumFuelPerGm)
    return ErkulMetrics(
        burst = weapons.sumOf { weaponBurst(weaponForMode(it.item!!, draft)) },
        sustained = weapons.sumOf { weaponSustained(weaponForMode(it.item!!, draft), ratio) },
        pilotBurst = weapons.filter { it.pilot }.sumOf { weaponBurst(weaponForMode(it.item!!, draft)) },
        shield = active.filter { allocation.supplied(it.path) > 0 }.sumOf { it.item!!.obj("shield").number("maxShieldHealth") ?: 0.0 },
        shieldRegen = active.sumOf { (it.item!!.obj("shield").number("maxShieldRegen") ?: 0.0) * powerOutputRatio(it.item, allocation.supplied(it.path)) },
        power = power,
        powerUsed = allocation.used.toDouble(),
        cooling = cooling,
        coolingUsed = coolingUsed,
        mass = (ship.obj("precomputed").number("massFixedKg") ?: 0.0) + slots.sumOf {
            (it.item?.number("mass") ?: 0.0) - (it.original?.number("mass") ?: 0.0)
        },
        quantumSpeed = quantum?.obj("params")?.number("driveSpeed"),
        quantumRange = if (fuel != null && burn != null && burn > 0) fuel / burn else null,
        weaponPool = fixedPool, weaponDemand = demand,
    )
}
