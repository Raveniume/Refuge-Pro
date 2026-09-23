package com.refuge.next.data

import org.json.JSONArray
import org.json.JSONObject

/** initShipUpgrade is the ship dictionary; only filterShips.to authorizes a current sale. */
internal fun currentUpgradeCatalog(dictionary: JSONObject, filtered: JSONObject): JSONObject {
    val ships = dictionary.getJSONObject("data").getJSONArray("ships")
    val targets = filtered.getJSONObject("data").getJSONObject("to").getJSONArray("ships")
    val current = (0 until targets.length()).map { targets.getJSONObject(it) }.associateBy { it.getInt("id") }
    val merged = JSONArray()
    for (index in 0 until ships.length()) {
        val ship = JSONObject(ships.getJSONObject(index).toString())
        val live = current[ship.getInt("id")]
        // Retain non-sale ships as possible FROM choices, but strip every historical SKU.
        val details = ship.optJSONArray("skus") ?: JSONArray()
        val byId = (0 until details.length()).map { details.getJSONObject(it) }.associateBy { it.getInt("id") }
        val skus = JSONArray()
        val liveSkus = live?.optJSONArray("skus") ?: JSONArray()
        for (skuIndex in 0 until liveSkus.length()) {
            val fresh = liveSkus.getJSONObject(skuIndex)
            val sku = JSONObject(byId[fresh.getInt("id")]?.toString() ?: "{}")
            fresh.keys().forEach { key -> sku.put(key, fresh.get(key)) }
            skus.put(sku)
        }
        ship.put("skus", skus)
        merged.put(ship)
    }
    return JSONObject().put("data", JSONObject().put("ships", merged))
        .put("currentSaleSchema", 1).put("fetchedAt", System.currentTimeMillis())
}
