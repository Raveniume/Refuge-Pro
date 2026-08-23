package com.refuge.next.data

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale

/** Online-first adapter for the same account/pledge endpoint used by RefugeNext. */
class RsiLiveHangarRepository(
    private val auth: RsiAuthDataSource,
    private val fallback: HangarRepository,
    private val fallbackImage: Int,
    private val m80Image: Int,
) : HangarRepository {
    override suspend fun ownedShips(): List<OwnedShip> {
        val items = inventory()
        val ship = items.firstOrNull { it.title.contains("M80", true) }
        return if (ship == null) fallback.ownedShips() else listOf(
            OwnedShip(
                name = ship.title.substringBefore(" - ").ifBlank { ship.title },
                packageName = ship.typeLabel,
                currentValue = ship.currentValue,
                paidValue = ship.price,
                insurance = ship.insurance,
                imageRes = if (ship.title.contains("M80", true)) m80Image else fallbackImage,
            ),
        )
    }

    override suspend fun inventory(): List<HangarItem> = withContext(Dispatchers.IO) {
        auth.session() ?: return@withContext fallback.inventory()
        runCatching {
            val first = auth.getPage("account/pledges?page=0")
            val pages = Regex("(?i)(?:data-total-pages|totalPages|total-pages)[^0-9]{0,12}(\\d+)")
                .find(first)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 40) ?: 1
            val html = buildString {
                append(first)
                for (page in 1 until pages) append(auth.getPage("account/pledges?page=$page"))
            }
            parseRows(html)
        }.getOrElse { fallback.inventory() }
    }

    private fun parseRows(html: String): List<HangarItem> {
        val rows = Regex("(?is)<div[^>]+class=[\"'][^\"']*row[^\"']*[\"'][^>]*>(.*?)</div>\\s*</div>").findAll(html).map { it.groupValues[1] }.toList()
        val candidates = if (rows.isEmpty()) listOf(html) else rows
        return candidates.mapNotNull { row ->
            val name = attr(row, "js-pledge-name") ?: text(row, "title") ?: return@mapNotNull null
            val price = attr(row, "js-pledge-value")?.let(::price) ?: "—"
            val date = text(row, "date-col") ?: "—"
            val decoded = Html.fromHtml(name, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            HangarItem(
                title = decoded,
                price = price,
                date = Html.fromHtml(date, Html.FROM_HTML_MODE_LEGACY).toString().trim(),
                imageRes = if (decoded.contains("M80", true)) m80Image else fallbackImage,
                originalName = decoded,
                typeLabel = if (row.contains("js-gift", true)) "可赠送" else "机库项目",
                insurance = if (row.contains("LTI", true)) "LTI" else "—",
                isGiftable = row.contains("js-gift", true),
                isReclaimable = row.contains("js-reclaim", true),
            )
        }.distinctBy { it.title + it.date + it.price }
    }

    private fun attr(html: String, className: String): String? {
        val match = Regex("(?is)<[^>]+class=[\"'][^\"']*$className[^\"']*[\"'][^>]*(?:value|data-value)=[\"']([^\"']+)").find(html)
        return match?.groupValues?.getOrNull(1)
    }

    private fun text(html: String, className: String): String? = Regex("(?is)<[^>]+class=[\"'][^\"']*$className[^\"']*[\"'][^>]*>(.*?)</").find(html)?.groupValues?.getOrNull(1)?.replace(Regex("<[^>]+>"), "")?.trim()

    private fun price(raw: String): String = raw.replace(",", "").trim().toDoubleOrNull()?.let {
        String.format(Locale.US, "$%.2f", it)
    } ?: raw
}

/** Online account projection used by the Profile page; cache remains a safe fallback. */
class RsiLiveProfileRepository(
    private val auth: RsiAuthDataSource,
    private val fallback: ProfileRepository,
) : ProfileRepository {
    override suspend fun profile(): ProfileData {
        auth.session() ?: return fallback.profile()
        return runCatching {
            val account = auth.accountGraphql().optJSONObject("data")?.optJSONObject("account") ?: error("账户响应为空")
            val fallbackProfile = fallback.profile()
            fallbackProfile.copy(
                handle = account.optString("nickname").ifBlank { account.optString("username") }.ifBlank { fallbackProfile.handle },
                city = account.optString("displayname").ifBlank { fallbackProfile.city },
                registerDate = account.optString("createdAt").ifBlank { fallbackProfile.registerDate },
            )
        }.getOrElse { fallback.profile() }
    }
}

class RsiLiveBuybackRepository(
    private val auth: RsiAuthDataSource,
    private val fallback: BuybackRepository,
    private val fallbackImage: Int,
) : BuybackRepository {
    override suspend fun items(): List<BuybackItem> {
        if (auth.session() == null) return fallback.items()
        return runCatching {
            val html = auth.getPage("account/buy-back-pledges?page=0&pagesize=100")
            Regex("(?is)<article[^>]+class=[\"'][^\"']*pledge[^\"']*[\"'][^>]*>(.*?)</article>")
                .findAll(html)
                .mapNotNull { match ->
                    val row = match.groupValues[1]
                    val title = Regex("(?is)<h1[^>]*>(.*?)</h1>").find(row)?.groupValues?.getOrNull(1)?.replace(Regex("<[^>]+>"), "")?.trim()
                        ?: return@mapNotNull null
                    val price = Regex("(?i)\\$\\s*([0-9,.]+)").find(row)?.groupValues?.getOrNull(1)?.let { "$$it" } ?: "—"
                    BuybackItem(title, price, "—", fallbackImage, title)
                }.toList()
                .ifEmpty { fallback.items() }
        }.getOrElse { fallback.items() }
    }
}

/** Public Star Citizen Wiki adapter for the terminal's vehicle category. */
class WikiTerminalRepository(
    private val fallback: TerminalRepository,
) : TerminalRepository {
    private val client = OkHttpClient.Builder().build()

    override suspend fun items(): List<TerminalItem> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.star-citizen.wiki/api/vehicles?page%5Bnumber%5D=1&page%5Bsize%5D=200")
                .header("Accept", "application/json")
                .header("User-Agent", "RefugeNext/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Wiki 请求失败：${response.code}")
                val root = JSONObject(response.body?.string().orEmpty())
                val data = root.optJSONArray("data") ?: root.optJSONObject("data")?.optJSONArray("data") ?: error("Wiki 数据为空")
                val remote = buildList {
                    for (index in 0 until data.length()) {
                        val entry = data.optJSONObject(index) ?: continue
                        val name = entry.optString("name").ifBlank { entry.optString("display_name") }.ifBlank { continue }
                        val manufacturer = entry.optString("manufacturer").ifBlank { entry.optString("manufacturer_name") }.ifBlank { "—" }
                        add(TerminalItem(
                            id = entry.optString("uuid").ifBlank { "wiki-$index" },
                            name = name,
                            manufacturer = manufacturer,
                            category = TerminalCategory.VEHICLES,
                            tags = listOfNotNull(entry.optString("classification").takeIf { it.isNotBlank() }),
                            value = "—",
                            usd = entry.optString("price").ifBlank { "—" },
                            description = entry.optString("description").ifBlank { "Star Citizen Wiki 载具资料" },
                        ))
                    }
                }
                (fallback.items() + remote).distinctBy { it.id }
            }
        }.getOrElse { fallback.items() }
    }
}

/** Public RSI GraphQL catalog adapter; cache is retained when the storefront changes schema. */
class RsiLiveStoreRepository(
    private val auth: RsiAuthDataSource,
    private val fallback: StoreRepository,
) : StoreRepository {
    override suspend fun products(): List<StoreProduct> = runCatching {
        val resources = auth.storeCatalogPage(1)
            .optJSONObject("data")?.optJSONObject("store")
            ?.optJSONObject("listing")?.optJSONArray("resources") ?: error("RSI 商店目录为空")
        val remote = buildList {
            for (index in 0 until resources.length()) {
                val item = resources.optJSONObject(index) ?: continue
                val title = item.optString("title").ifBlank { item.optString("name") }.ifBlank { continue }
                val tags = item.optJSONArray("tags")
                val tagText = tags?.let { array -> (0 until array.length()).mapNotNull { array.optJSONObject(it)?.optString("name") }.joinToString(" ") }.orEmpty()
                val type = (item.optString("type") + " " + tagText).lowercase()
                val category = when {
                    "paint" in type || "涂装" in title -> StoreCategory.PAINTS
                    "gear" in type || "equipment" in type || "装备" in title -> StoreCategory.GEAR
                    "package" in type || "游戏包" in title -> StoreCategory.PACKAGES
                    "vehicle" in type || "载具" in title -> StoreCategory.VEHICLES
                    else -> StoreCategory.SHIPS
                }
                val priceNode = item.optJSONObject("price") ?: item.optJSONObject("nativePrice")
                val amount = priceNode?.optString("amount").orEmpty().replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                val image = item.optJSONObject("media")?.optJSONObject("thumbnail")?.optString("storeSmall").orEmpty()
                add(StoreProduct(
                    id = item.optString("id").ifBlank { "remote-$index" },
                    title = title,
                    category = category,
                    metadata = item.optString("subtitle").ifBlank { tagText.ifBlank { "RSI 商品" } },
                    priceCents = (amount * 100).toInt(),
                    imageUrl = image,
                    description = item.optString("body").ifBlank { item.optString("subtitle") },
                    isWarbond = item.optBoolean("isWarbond", false),
                    isPackage = item.optBoolean("isPackage", false),
                ))
            }
        }
        remote.ifEmpty { fallback.products() }
    }.getOrElse { fallback.products() }
}
