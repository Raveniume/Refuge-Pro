package com.refuge.next.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses the versioned legacy-cache import used by production repositories.
 * The parser is isolated from Compose so a network/cache refresh can replace
 * this source without changing page contracts.
 */
class ProductionCacheDataSource(
    private val context: Context,
    private val assetPath: String = "cache/production_cache.json",
) {
    private val document: JSONObject by lazy {
        context.assets.open(assetPath).bufferedReader().use { JSONObject(it.readText()) }
    }

    fun manifest(): ProductionCacheManifest = ProductionCacheManifest(
        version = document.optString("version", productionCacheManifest.version),
        source = document.optString("source", productionCacheManifest.source),
        refreshedAt = document.optString("refreshedAt", productionCacheManifest.refreshedAt),
    )

    fun ownedShips(m80Image: Int, fallbackImage: Int): List<OwnedShip> = document
        .getJSONObject("hangar")
        .getJSONArray("ownedShips")
        .mapObjects { entry ->
            OwnedShip(
                name = entry.getString("name"),
                packageName = entry.getString("packageName"),
                currentValue = entry.getString("currentValue"),
                paidValue = entry.getString("paidValue"),
                insurance = entry.getString("insurance"),
                imageRes = entry.imageResource(m80Image, fallbackImage),
            )
        }

    fun hangarItems(m80Image: Int, fallbackImage: Int): List<HangarItem> = document
        .getJSONObject("hangar")
        .getJSONArray("inventory")
        .mapObjects { entry ->
            HangarItem(
                title = entry.getString("title"),
                price = entry.getString("price"),
                date = entry.getString("date"),
                imageRes = entry.imageResource(m80Image, fallbackImage),
                isGiftable = entry.optBoolean("giftable", true),
                isReclaimable = entry.optBoolean("reclaimable", true),
                originalName = entry.optString("originalName", "—"),
                typeLabel = entry.optString("typeLabel", "本地机库项目"),
                insurance = entry.optString("insurance", "—"),
                currentValue = entry.optString("currentValue", entry.getString("price")),
                savings = entry.optString("savings", "$0"),
                includedItems = entry.optJSONArray("includedItems")?.toStringList().orEmpty(),
                upgradeFrom = entry.optNullableString("upgradeFrom"),
                upgradeTo = entry.optNullableString("upgradeTo"),
                upgradeFromPrice = entry.optNullableString("upgradeFromPrice"),
                upgradeToPrice = entry.optNullableString("upgradeToPrice"),
            )
        }

    fun buyback(m80Image: Int, fallbackImage: Int): List<BuybackItem> = document
        .getJSONArray("buyback")
        .mapObjects { entry ->
            BuybackItem(
                title = entry.getString("title"),
                price = entry.getString("price"),
                date = entry.getString("date"),
                imageRes = entry.imageResource(m80Image, fallbackImage),
                originalName = entry.optString("originalName", "—"),
                isUpgrade = entry.optBoolean("isUpgrade", false),
            )
        }

    fun logs(): List<String> = document.getJSONArray("logs").toStringList()

    fun storeProducts(): List<StoreProduct> = document
        .getJSONArray("store")
        .mapObjects { entry ->
            StoreProduct(
                id = entry.getString("id"),
                title = entry.getString("title"),
                category = StoreCategory.valueOf(entry.getString("category")),
                metadata = entry.getString("metadata"),
                priceCents = entry.getInt("priceCents"),
                imageUrl = entry.getString("imageUrl"),
                description = entry.getString("description"),
                isWarbond = entry.optBoolean("isWarbond", false),
                isPackage = entry.optBoolean("isPackage", false),
            )
        }

    fun terminalItems(): List<TerminalItem> = document
        .getJSONArray("terminal")
        .mapObjects { entry ->
            TerminalItem(
                id = entry.getString("id"),
                name = entry.getString("name"),
                manufacturer = entry.getString("manufacturer"),
                category = TerminalCategory.valueOf(entry.getString("category")),
                tags = entry.getJSONArray("tags").toStringList(),
                value = entry.getString("value"),
                usd = entry.getString("usd"),
                description = entry.getString("description"),
            )
        }

    fun profile(): ProfileData = document.getJSONObject("profile").let { entry ->
        ProfileData(
            handle = entry.optString("handle", "Raveniume"),
            city = entry.optString("city", "星环城"),
            rank = entry.optString("rank", "Experienced"),
            totalSpent = entry.optString("totalSpent", "$140"),
            hangarValue = entry.optString("hangarValue", "$300"),
            credit = entry.optString("credit", "$60"),
            registerDate = entry.optString("registerDate", "2023-06-18"),
            uec = entry.optString("uec", "128,400"),
            rec = entry.optString("rec", "2,960"),
            currentValue = entry.optString("currentValue", "$300"),
            referralCode = entry.optString("referralCode", "RAVEN-7K2Q"),
        )
    }

    fun toolGroups(): List<Pair<String, List<ToolItem>>> = document
        .getJSONArray("toolGroups")
        .mapObjects { group ->
            group.getString("title") to group.getJSONArray("items").mapObjects { item ->
                ToolItem(item.getString("id"), item.getString("title"), item.getString("subtitle"))
            }
        }

    fun toolDetail(toolId: String): ToolDetail? = document
        .getJSONObject("toolDetails")
        .optJSONObject(toolId)
        ?.let { entry ->
            ToolDetail(
                toolId = toolId,
                rows = entry.getJSONArray("rows").mapObjects { row ->
                    row.getString("label") to row.getString("value")
                },
                externalUrl = entry.optNullableString("externalUrl"),
            )
        }

    fun ccuShips(m80Image: Int, fallbackImage: Int): List<CcuShip> = document
        .getJSONObject("ccu")
        .getJSONArray("ships")
        .mapObjects { entry ->
            CcuShip(
                id = entry.getString("id"),
                name = entry.getString("name"),
                purchasePrice = entry.getInt("purchasePrice"),
                imageRes = entry.imageResource(m80Image, fallbackImage),
            )
        }

    fun ownedCcu(): List<OwnedCcu> = document
        .getJSONObject("ccu")
        .getJSONArray("owned")
        .mapObjects { entry ->
            OwnedCcu(
                id = entry.getString("id"),
                title = entry.getString("title"),
                purchasePrice = entry.getInt("purchasePrice"),
                appliedTo = entry.getString("appliedTo"),
            )
        }
}

private fun JSONObject.imageResource(m80Image: Int, fallbackImage: Int): Int =
    if (optString("image", "fallback") == "m80") m80Image else fallbackImage

private fun JSONObject.optNullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

private fun JSONArray.toStringList(): List<String> = buildList {
    for (index in 0 until length()) add(getString(index))
}

private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> = buildList {
    for (index in 0 until length()) add(transform(getJSONObject(index)))
}
