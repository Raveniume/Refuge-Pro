package com.refuge.next.data

import org.json.JSONObject

/** Localized price can be another currency; the store displays RSI's native USD amount. */
internal fun nativeStorePriceCents(product: JSONObject): Int =
    product.optJSONObject("nativePrice")?.optInt("amount", 0)?.coerceAtLeast(0) ?: 0
