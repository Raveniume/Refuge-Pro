package com.refuge.next.data

typealias FacetSelection = Map<String, Set<String>>

internal fun FacetSelection.accepts(key: String, values: Iterable<String>): Boolean =
    this[key].isNullOrEmpty() || values.any { it in this[key].orEmpty() }

internal fun hangarPriceCents(value: String): Int? = Regex("-?[0-9]+(?:\\.[0-9]+)?")
    .find(value.replace(",", ""))?.value?.toBigDecimalOrNull()?.movePointRight(2)?.toInt()

internal fun HangarItem.matchesLegacyFilters(filters: FacetSelection): Boolean {
    val types = buildList {
        if (isUpgrade) add("升级")
        if (!isUpgrade && (containedShip != null || includedEntries.any { it.kind.equals("Ship", true) } || typeLabel.contains("舰船"))) add("舰船")
        if (includedEntries.any { it.kind.equals("Paint", true) || Regex("(?i)paint|skin|涂装").containsMatchIn(it.title) } || typeLabel.contains("涂装")) add("涂装")
        if (Regex("(?i)subscriber|subscription|订阅").containsMatchIn(title + originalName)) add("订阅")
    }
    val paid = hangarPriceCents(price)
    val bands = buildList {
        if (paid != null) {
            if (paid > 0) add("非0")
            if (paid in 0..10000) add("0-100")
            if (paid in 10000..50000) add("100-500")
            if (paid >= 50000) add("500+")
        }
    }
    val lifetime = insurance.contains("LTI", true) || insurance.contains("永久")
    val months = Regex("(\\d+)\\s*(?:month|个月|月)", RegexOption.IGNORE_CASE).find(insurance)?.groupValues?.get(1)?.toIntOrNull()
    val years = Regex("(\\d+)\\s*(?:year|y|年)", RegexOption.IGNORE_CASE).find(insurance)?.groupValues?.get(1)?.toIntOrNull()
    val insuranceValues = buildList {
        if (lifetime) add("永久保险") else add("其他")
        if ((months ?: 0) >= 120 || (years ?: 0) >= 10) add("10年及以上")
    }
    return filters.accepts("类型", types) && filters.accepts("价格", bands) &&
        filters.accepts("状态", listOf(when (status.lowercase()) { "gifted" -> "已礼物"; "attributed" -> "在库"; else -> status })) &&
        filters.accepts("保险", insuranceValues) &&
        filters.accepts("礼物", listOf(if (isGiftable) "可礼物" else "不可礼物")) &&
        (filters["融船"].isNullOrEmpty() || isReclaimable) &&
        filters.accepts("起始舰船", listOfNotNull(upgradeFrom)) && filters.accepts("目标舰船", listOfNotNull(upgradeTo))
}

internal fun TerminalItem.facetValue(key: String): String? {
    filterAttributes[key]?.takeIf { it.isNotBlank() }?.let { return it }
    return when (key) {
        "厂商" -> manufacturer.takeUnless { it == "—" }
        "尺寸" -> size?.let { "S$it" }
        "类型" -> details.firstOrNull { it.first in listOf("武器类型", "子类型") }?.second ?: sourceSubType
        else -> details.firstOrNull { it.first == key }?.second
    }?.takeUnless { it == "—" || it == "null" || it.isBlank() }
}

internal fun terminalFacetKeys(category: TerminalCategory) = when(category) {
    TerminalCategory.VEHICLES -> listOf("载具类型", "护盾类型", "职能", "厂商")
    TerminalCategory.SHIP_COMPONENTS -> listOf("类型", "尺寸", "厂商")
    TerminalCategory.PERSONAL -> listOf("类型", "尺寸", "射击模式")
    TerminalCategory.ATTACHMENTS -> listOf("类型", "尺寸")
    else -> listOf("类别", "尺寸", "等级")
}
