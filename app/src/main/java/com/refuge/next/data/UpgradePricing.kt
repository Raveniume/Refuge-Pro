package com.refuge.next.data

/** All amounts are USD cents. A missing/non-positive price is not a free CCU. */
fun upgradeDiscount(msrp: Int, editionPrice: Int): Int =
    if (msrp > 0 && editionPrice > 0 && editionPrice < msrp) msrp - editionPrice else 0

data class UpgradePriceQuote(val sourceShipId: Int, val targetShipId: Int, val skuId: Int, val upgradePrice: Int)

fun parseUpgradeUsdCents(value: String): Int? {
    val normalized = value.trim().removePrefix("US$").removePrefix("$").removePrefix("USD ").replace(",", "")
    if (!normalized.matches(Regex("[0-9]+(?:\\.[0-9]{1,2})?"))) return null
    return runCatching { normalized.toBigDecimal().movePointRight(2).intValueExact() }.getOrNull()
}
