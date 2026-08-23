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
        val ships = items.filter { it.typeLabel.contains("舰船", true) || isShipTitle(it.title) }
        return ships.map { ship ->
            OwnedShip(
                name = ship.title.substringBefore(" - ").substringBefore(" — ").trim().ifBlank { ship.title },
                packageName = ship.typeLabel,
                currentValue = ship.currentValue,
                paidValue = ship.price,
                insurance = ship.insurance,
                imageRes = if (isM80(ship.title)) m80Image else fallbackImage,
            )
        }.ifEmpty { if (items.isEmpty()) fallback.ownedShips() else emptyList() }
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
        val namePattern = Regex("(?is)<input[^>]+class=[\"'][^\"']*js-pledge-name[^\"']*[\"'][^>]+value=[\"']([^\"']+)")
        return namePattern.findAll(html).mapNotNull { match ->
            val start = (match.range.first - 2800).coerceAtLeast(0)
            val end = (match.range.last + 3200).coerceAtMost(html.length)
            val window = html.substring(start, end)
            val name = match.groupValues[1]
            val price = inputValue(window, "js-pledge-value")?.let(::price) ?: "—"
            val date = text(window, "date-col") ?: "—"
            val decoded = Html.fromHtml(name, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            val ship = isShipTitle(decoded)
            val type = when {
                ship -> "舰船 / 游戏包"
                decoded.contains("paint", true) || decoded.contains("涂装") -> "涂装"
                decoded.contains("armor", true) || decoded.contains("gear", true) || decoded.contains("装备") -> "装备"
                else -> "机库项目"
            }
            HangarItem(
                title = decoded,
                price = price,
                date = cleanDate(date),
                imageRes = if (isM80(decoded)) m80Image else fallbackImage,
                originalName = decoded,
                typeLabel = type,
                insurance = if (window.contains("LTI", true)) "LTI" else "—",
                isGiftable = window.contains("js-gift", true),
                isReclaimable = window.contains("js-reclaim", true),
                currentValue = price,
            )
        }.distinctBy { it.title + it.date + it.price }.toList()
    }

    private fun inputValue(html: String, className: String): String? {
        val match = Regex("(?is)<input[^>]*class=[\"'][^\"']*$className[^\"']*[\"'][^>]*value=[\"']([^\"']+)").find(html)
        return match?.groupValues?.getOrNull(1)
    }

    private fun text(html: String, className: String): String? = Regex("(?is)<[^>]*class=[\"'][^\"']*$className[^\"']*[\"'][^>]*>(.*?)</[^>]+>")
        .find(html)?.groupValues?.getOrNull(1)?.replace(Regex("<[^>]+>"), "")?.trim()

    private fun price(raw: String): String = Regex("[0-9]+(?:\\.[0-9]+)?").find(raw.replace(",", ""))?.value?.toDoubleOrNull()?.let {
        String.format(Locale.US, "$%.2f", it)
    } ?: raw

    private fun cleanDate(raw: String): String = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
        .replace(Regex("(?i)created:\\s*"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun isM80(title: String) = title.contains("M80", true)
    private fun isShipTitle(title: String): Boolean {
        if (Regex("(?i)(paint|paints|livery|skin|涂装|油漆|armor|装甲|装备|component|组件)").containsMatchIn(title)) return false
        return isM80(title) || Regex("(?i)\\b(aurora|atls|avenger|gladius|mercury|carrack|cutlass|freelancer|nomad|reclaimer|constellation|vulture|prospector|buccaneer|hull|ship|舰船|战舰)\\b").containsMatchIn(title)
    }
}

/** Online account projection used by the Profile page; cache remains a safe fallback. */
class RsiLiveProfileRepository(
    private val auth: RsiAuthDataSource,
    private val fallback: ProfileRepository,
    private val hangar: HangarRepository,
) : ProfileRepository {
    override suspend fun profile(): ProfileData {
        auth.session() ?: return fallback.profile()
        return runCatching {
            val account = auth.accountGraphql().optJSONObject("data")?.optJSONObject("account") ?: error("账户响应为空")
            val fallbackProfile = fallback.profile()
            val items = hangar.inventory()
            val paid = items.mapNotNull { Regex("[0-9]+(?:\\.[0-9]+)?").find(it.price.replace(",", ""))?.value?.toDoubleOrNull() }.sum()
            val current = items.mapNotNull { Regex("[0-9]+(?:\\.[0-9]+)?").find(it.currentValue.replace(",", ""))?.value?.toDoubleOrNull() }.sum()
            val money = { value: Double -> String.format(Locale.US, "$%.2f", value) }
            fallbackProfile.copy(
                handle = account.optString("nickname").ifBlank { account.optString("username") }.ifBlank { fallbackProfile.handle },
                city = "RSI 账户",
                rank = if (account.optBoolean("hasGamePackage", false)) "已拥有游戏包" else "RSI 账户",
                registerDate = account.optString("createdAt").ifBlank { fallbackProfile.registerDate },
                totalSpent = money(paid),
                hangarValue = money(current),
                currentValue = money(current),
                credit = "—",
                uec = "—",
                rec = "—",
                referralCode = account.optString("referral_code").ifBlank { "—" },
                avatarUrl = account.optString("avatar").ifBlank { null },
                email = account.optString("email").ifBlank { null },
                username = account.optString("username").ifBlank { null },
                hasGamePackage = account.optBoolean("hasGamePackage", false),
                isAuthenticated = true,
            )
        }.getOrElse {
            val session = auth.session()
            ProfileData(
                handle = session?.email ?: "RSI 账户",
                city = "RSI 账户",
                rank = "在线资料暂不可用",
                totalSpent = "—",
                hangarValue = "—",
                credit = "—",
                registerDate = "—",
                uec = "—",
                rec = "—",
                currentValue = "—",
                referralCode = "—",
                isAuthenticated = session != null,
            )
        }
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
                        val manufacturer = entry.optJSONObject("manufacturer")?.optString("name").orEmpty()
                            .ifBlank { entry.optString("manufacturer_name") }.ifBlank { "—" }
                        val description = localized(entry.optJSONObject("description") ?: entry.optJSONObject("game_description"))
                            .ifBlank { "Star Citizen Wiki 载具资料" }
                        val imageUrl = entry.optJSONObject("images")?.optString("thumbnail_url").orEmpty().ifBlank { null }
                        val role = entry.optString("role").ifBlank { entry.optString("career") }
                        val msrp = entry.opt("msrp")?.toString()?.takeIf { it.isNotBlank() && it != "null" }?.let { "$$it" } ?: "—"
                        add(TerminalItem(
                            id = entry.optString("uuid").ifBlank { "wiki-$index" },
                            name = name,
                            manufacturer = manufacturer,
                            category = TerminalCategory.VEHICLES,
                            tags = listOfNotNull(role.takeIf { it.isNotBlank() }, entry.optString("size_class").takeIf { it.isNotBlank() }),
                            value = "—",
                            usd = msrp,
                            description = description,
                            imageUrl = imageUrl,
                        ))
                    }
                }
                (fallback.items() + remote).distinctBy { it.id }
            }
        }.getOrElse { fallback.items() }
    }

    private fun localized(node: JSONObject?): String {
        if (node == null) return ""
        return listOf("zh_CN", "en_EN", "en_US", "en").firstNotNullOfOrNull { key ->
            node.optString(key).takeIf { it.isNotBlank() }
        } ?: node.optString("text")
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
