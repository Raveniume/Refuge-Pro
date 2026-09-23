package com.refuge.next.data

data class TerminalPortType(val type: String, val subTypes: Set<String> = emptySet())

/** IDs include the parent path: identical child names on two turrets remain two slots. */
data class TerminalPort(
    val id: String,
    val name: String,
    val position: String? = null,
    val type: String? = null,
    val minSize: Int? = null,
    val maxSize: Int? = null,
    val editable: Boolean = false,
    val equippedItemId: String? = null,
    val equippedItem: TerminalItem? = null,
    val compatibleTypes: List<TerminalPortType> = emptyList(),
    val requiredTags: Set<String> = emptySet(),
    val hasChildren: Boolean = false,
)

/** Values copied from the source response. Null means that the source omitted it. */
data class TerminalPerformance(
    val massKg: Double? = null,
    val massTotalKg: Double? = null,
    val cargoScu: Double? = null,
    val shieldHp: Double? = null,
    val shieldRegen: Double? = null,
    val pilotDps: Double? = null,
    val sustainedDps: Double? = null,
    val burstDps: Double? = null,
    val scmSpeed: Double? = null,
    val maxSpeed: Double? = null,
    val powerGeneration: Double? = null,
    val powerUsage: Double? = null,
    val coolingGeneration: Double? = null,
    val coolingUsage: Double? = null,
    val quantumSpeed: Double? = null,
)

internal fun terminalTypeCategory(type: String?): TerminalCategory? = when (type?.lowercase()) {
    "weapongun" -> TerminalCategory.SHIP_COMPONENTS
    "shield" -> TerminalCategory.SHIELDS
    "cooler" -> TerminalCategory.COOLERS
    "powerplant" -> TerminalCategory.POWER_PLANTS
    "quantumdrive" -> TerminalCategory.QUANTUM_DRIVES
    else -> null
}

internal fun terminalPortCategory(port: TerminalPort): TerminalCategory? =
    terminalTypeCategory(port.type) ?: port.compatibleTypes.firstNotNullOfOrNull { terminalTypeCategory(it.type) }

/** Replacing parent assemblies requires rebuilding their child tree; retain them for now. */
internal fun terminalPortCanEdit(port: TerminalPort): Boolean =
    port.editable && !port.hasChildren && terminalPortCategory(port) != null

internal fun terminalPortAccepts(port: TerminalPort, item: TerminalItem, version: String? = null): Boolean {
    if (!terminalPortCanEdit(port) || terminalPortCategory(port) != item.category) return false
    if (version != null && item.sourceVersion != version) return false
    val size = item.size ?: return false
    val minimum = port.minSize ?: return false
    val maximum = port.maxSize ?: return false
    if (size !in minimum..maximum) return false
    if (!item.portTags.containsAll(port.requiredTags)) return false
    return port.compatibleTypes.any { accepted ->
        accepted.type.equals(item.sourceType, true) &&
            (accepted.subTypes.isEmpty() || accepted.subTypes.any { it.equals(item.sourceSubType, true) })
    }
}

/** A missing replacement or metric invalidates the total instead of producing a partial sum. */
internal fun calculateTerminalPortPerformance(
    ship: TerminalItem,
    overrides: Map<String, String?>,
    catalog: Map<String, TerminalItem>,
): TerminalPerformance {
    val stock = ship.performance ?: TerminalPerformance()
    val changed = ship.ports.filter { overrides.containsKey(it.id) && overrides[it.id] != it.equippedItemId }
    fun component(port: TerminalPort, replacement: Boolean): TerminalItem? {
        val id = if (replacement) overrides[port.id] else port.equippedItemId
        return port.equippedItem?.takeIf { it.id == id && it.sourceVersion == ship.sourceVersion }
            ?: catalog[id]?.takeIf { it.sourceVersion == ship.sourceVersion }
    }
    fun adjust(base: Double?, category: TerminalCategory? = null, metric: (TerminalPerformance) -> Double?): Double? {
        var total = base ?: return null
        for (port in changed.filter { category == null || terminalPortCategory(it) == category }) {
            if (!terminalPortCanEdit(port)) return null
            val before = if (port.equippedItemId == null) 0.0 else component(port, false)?.performance?.let(metric) ?: return null
            val after = if (overrides[port.id] == null) 0.0 else {
                val item = component(port, true) ?: return null
                if (!terminalPortAccepts(port, item, ship.sourceVersion)) return null
                item.performance?.let(metric) ?: return null
            }
            total += after - before
        }
        return total.takeIf { it.isFinite() && it >= 0 }
    }
    // Capacitors and power allocation are not a linear delta. Rated per-gun
    // metrics are shown separately; ship DPS is invalidated after changes.
    return stock.copy(
        massTotalKg = adjust(stock.massTotalKg) { it.massKg },
        shieldHp = adjust(stock.shieldHp, TerminalCategory.SHIELDS) { it.shieldHp },
        shieldRegen = if (changed.isEmpty()) stock.shieldRegen else null,
        powerGeneration = adjust(stock.powerGeneration, TerminalCategory.POWER_PLANTS) { it.powerGeneration },
        coolingGeneration = adjust(stock.coolingGeneration, TerminalCategory.COOLERS) { it.coolingGeneration },
        powerUsage = if (changed.isEmpty()) stock.powerUsage else null,
        coolingUsage = if (changed.isEmpty()) stock.coolingUsage else null,
        pilotDps = if (changed.isEmpty()) stock.pilotDps else null,
        sustainedDps = if (changed.isEmpty()) stock.sustainedDps else null,
        scmSpeed = if (changed.isEmpty()) stock.scmSpeed else null,
        maxSpeed = if (changed.isEmpty()) stock.maxSpeed else null,
        quantumSpeed = if (changed.isEmpty()) stock.quantumSpeed else null,
    )
}
