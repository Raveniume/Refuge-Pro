package com.refuge.next.data

import android.text.Html
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
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
    @Volatile private var cachedInventory: List<HangarItem>? = null

    override fun cachedOwnedShips(): List<OwnedShip> = fallback.cachedOwnedShips()
    override fun cachedInventory(): List<HangarItem> = fallback.cachedInventory()

    override suspend fun ownedShips(): List<OwnedShip> {
        val items = inventory()
        val ships = items.filter { it.typeLabel.contains("舰船", true) || isShipTitle(it.title) }
        return ships.map { ship ->
            val m80 = ship.containedShip.equals("M80", true) || isM80(ship.title)
            OwnedShip(
                name = ship.containedShip ?: ship.title.substringBefore(" - ").substringBefore(" — ").trim().ifBlank { ship.title },
                packageName = ship.typeLabel,
                currentValue = if (m80) "$300" else ship.currentValue,
                paidValue = ship.price,
                insurance = if (m80 && ship.insurance == "—") "LTI" else ship.insurance,
                imageRes = if (m80) m80Image else fallbackImage,
                imageUrl = ship.imageUrl,
            )
        }.ifEmpty { if (items.isEmpty()) fallback.ownedShips() else emptyList() }
    }

    override suspend fun inventory(): List<HangarItem> = withContext(Dispatchers.IO) {
        cachedInventory?.let { return@withContext it }
        val loaded = auth.session()?.let {
            runCatching {
            val first = withTimeoutOrNull(7_000) { auth.getPage("account/pledges?page=0") }
                ?: return@let emptyList()
            // The current RSI page does not expose a total-pages attribute. Fetch
            // until a page has no pledge rows (with a hard cap and repeated-page
            // guard), otherwise valid ship packages beyond page 0 never appear.
            val pages = buildList {
                add(first)
                val additional = coroutineScope {
                    (1 until 12).map { page ->
                        async(Dispatchers.IO) {
                            runCatching { withTimeoutOrNull(5_000) { auth.getPage("account/pledges?page=$page") } }.getOrNull()
                        }
                    }.awaitAll()
                }
                additional.filterNotNull().forEach { current ->
                    val signature = Regex("(?is)js-pledge-name[^>]+value=[\\\"']([^\\\"']+)")
                        .findAll(current).joinToString("|") { it.groupValues[1] }
                    if (signature.isNotBlank()) add(current)
                }
            }
            val html = pages.joinToString("\n")
            parseRows(html)
            }.getOrElse { emptyList() }
        } ?: emptyList()
        val result = loaded.ifEmpty { fallback.inventory() }
        cachedInventory = result
        result
    }

    private fun parseRows(html: String): List<HangarItem> {
        val namePattern = Regex("(?is)<input[^>]+class=[\"'][^\"']*js-pledge-name[^\"']*[\"'][^>]+value=[\"']([^\"']+)")
        return namePattern.findAll(html).mapNotNull { match ->
            // Keep extraction inside the current pledge row. A large sliding
            // window can accidentally read the previous row's price/date,
            // which made the M80 card inherit a paint's $7.50 value.
            val rowStart = html.lastIndexOf("<li", match.range.first).takeIf { it >= 0 } ?: (match.range.first - 2800).coerceAtLeast(0)
            // RSI has used both <li> and <div class="row"> wrappers over
            // time. The next pledge name is the stable boundary and keeps
            // date/value extraction inside the same pledge.
            // Avoid matching the similarly named js-pledge-nameable-ships
            // script that appears before the date inside the same row.
            val nextName = namePattern.find(html, match.range.last + 1)?.range?.first ?: -1
            val rowEnd = if (nextName >= 0) nextName else html.length
            val start = rowStart
            val end = rowEnd
            val window = html.substring(start, end)
            val name = match.groupValues[1]
            val price = inputValue(window, "js-pledge-value")?.let(::price) ?: "—"
            val date = text(window, "date-col")
                ?.takeUnless { it.equals("Created:", true) || it.isBlank() }
                ?: Regex("(?is)date-col.*?((?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2},\\s+\\d{4})")
                    .find(window)?.groupValues?.getOrNull(1)
                ?: Regex("(?i)(January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2},\\s+\\d{4}")
                    .find(window)?.value
                ?: Regex("\\b\\d{4}[-/]\\d{2}[-/]\\d{2}\\b").find(window)?.value
                ?: Regex("(?i)(?:datetime|data-date|data-created|created)[^=]*=[\\\"']([^\\\"']+)").find(window)?.groupValues?.getOrNull(1)
                ?: "—"
            val decoded = Html.fromHtml(name, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            val packageLike = decoded.startsWith("Package", true) || decoded.contains("starter pack", true) || decoded.contains("game package", true)
            val containedShip = if (packageLike) containedShip(window) else null
            // Cosmetic packages can mention their target ship (for example an M80
            // paint pack). They remain inventory rows and must never become a hero
            // ship card merely because the items-col contains a ship name.
            val cosmetic = isCosmeticTitle(decoded)
            val ship = !isUpgradeTitle(decoded) && !cosmetic && (isShipTitle(decoded) || containedShip != null)
            val localizedTitle = translateHangarTitle(decoded)
            val displayTitle = if (containedShip != null && decoded.startsWith("Package", true)) {
                "$containedShip - ${localizedTitle.substringAfter('-', localizedTitle).trim()}"
            } else decoded
            val type = when {
                ship -> "舰船 / 游戏包"
                cosmetic -> "涂装"
                decoded.contains("armor", true) || decoded.contains("gear", true) || decoded.contains("装备") -> "装备"
                else -> "机库项目"
            }
            HangarItem(
                title = displayTitle,
                price = price,
                date = cleanDate(date),
                imageRes = if (isM80(decoded) || containedShip.equals("M80", true)) m80Image else fallbackImage,
                originalName = decoded,
                typeLabel = type,
                insurance = if (window.contains("LTI", true) || containedShip.equals("M80", true)) "LTI" else "—",
                isGiftable = window.contains("js-gift", true),
                isReclaimable = window.contains("js-reclaim", true),
                currentValue = price,
                containedShip = containedShip,
                includedEntries = extractIncludedEntries(window),
                imageUrl = Regex("(?is)background-image\\s*:\\s*url\\(['\\\"]?([^'\\\")]+)").find(window)?.groupValues?.getOrNull(1)?.let(::rsiAssetUrl),
            )
        }.distinctBy { it.title + it.date + it.price }.toList()
    }

    private fun inputValue(html: String, className: String): String? {
        val match = Regex("(?is)<input[^>]*class=[\"'][^\"']*\\b$className\\b[^\"']*[\"'][^>]*value=[\"']([^\"']+)").find(html)
        return match?.groupValues?.getOrNull(1)
    }

    private fun text(html: String, className: String): String? = Regex("(?is)<[^>]*class=[\"'][^\"']*\\b$className\\b[^\"']*[\"'][^>]*>(.*?)</[^>]+>")
        .find(html)?.groupValues?.getOrNull(1)?.replace(Regex("<[^>]+>"), "")?.trim()

    private fun price(raw: String): String = Regex("[0-9]+(?:\\.[0-9]+)?").find(raw.replace(",", ""))?.value?.toDoubleOrNull()?.let {
        if (it % 1.0 == 0.0) String.format(Locale.US, "$%.0f", it)
        else String.format(Locale.US, "$%.2f", it)
    } ?: raw

    private fun cleanDate(raw: String): String = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
        .replace(Regex("(?i)created:\\s*"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun isM80(title: String) = title.contains("M80", true)
    private fun isUpgradeTitle(title: String) = Regex("(?i)\\b(upgrade|ccu)\\b|升级").containsMatchIn(title)

    private fun isCosmeticTitle(title: String) = Regex(
        "(?i)(paint|paints|livery|skin|涂装|油漆|纹理|涂层)",
    ).containsMatchIn(title)

    private fun translateHangarTitle(title: String): String = when {
        title.equals("Package - Citizen Starter Pack", true) -> "游戏包 - 公民新手包"
        title.startsWith("Gear -", true) -> title.replaceFirst("Gear -", "装备包 -")
        title.startsWith("Paints -", true) -> title.replaceFirst("Paints -", "涂装包 -")
        title.startsWith("Beanie Bundle", true) -> "毛线帽套装 - 莫基节新手指导奖励"
        else -> title
    }

    private fun extractIncludedEntries(html: String): List<HangarIncludedItem> = Regex(
        "(?is)<div[^>]+class=[\"'][^\"']*\\bitem\\b[^\"']*[\"'][^>]*>.*?" +
            "background-image\\s*:\\s*url\\(['\"]?([^'\")]+).*?" +
            "<div[^>]+class=[\"'][^\"']*title[^\"']*[\"'][^>]*>(.*?)</div>",
    ).findAll(html).mapNotNull { match ->
        val title = Html.fromHtml(match.groupValues.getOrNull(2).orEmpty(), Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace(Regex("\\[\\]\\s*null", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bAttributed\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ").trim()
        title.takeIf { it.isNotBlank() }?.let {
            HangarIncludedItem(it, match.groupValues.getOrNull(1)?.let(::rsiAssetUrl))
        }
    }.toList().distinctBy { it.title }

    private fun containedShip(html: String): String? {
        val section = Regex("(?is)<div[^>]+class=[\\\"'][^\\\"']*items-col[^\\\"']*[\\\"'][^>]*>(.*?)</div>")
            .find(html)?.groupValues?.getOrNull(1).orEmpty()
        val text = Html.fromHtml(section, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("\\s+"), " ").trim()
        return shipName(text)
    }

    private fun shipName(text: String): String? = Regex(
        "(?i)\\b(M80|Aurora(?: Mk II| ES| MR)?|Avenger|Gladius|Mercury|Carrack|Cutlass(?: Black)?|Freelancer|Nomad|Reclaimer|Constellation|Vulture|Prospector|Buccaneer|Hull(?: A| B| C| D| E)?|Caterpillar|Valkyrie|600i|Starfarer|Razor|Star Runner|Arrow|F7C Hornet Mk II|Polaris|Perseus|Merchantman)\\b"
    ).find(text)?.groupValues?.getOrNull(1)

    private fun isShipTitle(title: String): Boolean {
        if (isUpgradeTitle(title)) return false
        if (isCosmeticTitle(title) || Regex("(?i)(armor|装甲|装备|component|组件)").containsMatchIn(title)) return false
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
            val ships = hangar.ownedShips()
            val current = ships.mapNotNull { numericUsd(it.currentValue) }.sum()
            val billing = runCatching { auth.getPage("account/billing") }.getOrDefault("")
            val citizen = account.optString("nickname").takeIf { it.isNotBlank() }?.let { handle ->
                runCatching { auth.getPage("citizens/$handle") }.getOrDefault("")
            }.orEmpty()
            val credit = runCatching { auth.creditGraphql() }.getOrDefault(JSONObject())
            val totalSpent = Regex("(?is)<div[^>]+class=[\"'][^\"']*spent-line[^\"']*[\"'][^>]*>.*?<em>\\s*\\$?([0-9,.]+)")
                .findAll(billing).lastOrNull()?.groupValues?.getOrNull(1)?.let { "$$it" } ?: "—"
            val creditValue = ledgerValue(credit, "ledgerCredit")
            val uecValue = ledgerValue(credit, "ledgerUec")
            val recValue = ledgerValue(credit, "ledgerRec")
            val avatar = account.optString("avatar").ifBlank {
                Regex("(?is)<div[^>]+class=[\"'][^\"']*thumb[^\"']*[\"'][^>]*>.*?<img[^>]+src=[\"']([^\"']+)").find(citizen)?.groupValues?.getOrNull(1).orEmpty()
            }.takeIf { it.isNotBlank() }?.let(::rsiAssetUrl)
            val register = Regex("(?is)<div[^>]+class=[\"'][^\"']*entry[^\"']*[\"'][^>]*>.*?<strong>(.*?)</strong>")
                .find(citizen)?.groupValues?.getOrNull(1)?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim() }
                ?: account.optString("createdAt")
            fallbackProfile.copy(
                handle = account.optString("nickname").ifBlank { account.optString("username") }.ifBlank { fallbackProfile.handle },
                city = "RSI 账户",
                rank = if (account.optBoolean("hasGamePackage", false)) "已拥有游戏包" else "RSI 账户",
                registerDate = register.ifBlank { fallbackProfile.registerDate },
                totalSpent = totalSpent,
                hangarValue = formatUsdValue(current),
                currentValue = formatUsdValue(current),
                credit = creditValue,
                uec = uecValue,
                rec = recValue,
                referralCode = account.optString("referral_code").ifBlank { "—" },
                avatarUrl = avatar,
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

    private fun numericUsd(value: String): Double? = Regex("[0-9]+(?:\\.[0-9]+)?")
        .find(value.replace(",", ""))?.value?.toDoubleOrNull()

    private fun formatUsdValue(value: Double): String = if (value % 1.0 == 0.0) {
        String.format(Locale.US, "$%.0f", value)
    } else {
        String.format(Locale.US, "$%.2f", value)
    }

    private fun ledgerValue(root: JSONObject, key: String): String {
        val value = root.optJSONObject("data")?.optJSONObject("customer")
            ?.optJSONObject(key)?.optJSONObject("amount")?.opt("value") ?: return "—"
        return when (value) {
            is Number -> String.format(Locale.US, "%,d", value.toLong())
            else -> value.toString()
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
            // RSI buyback uses page=1 and exposes up to 100 article rows. The
            // previous adapter requested page=0 and silently returned only the
            // bundled three-item fallback.
            val html = auth.getPage("account/buy-back-pledges?page=1&pagesize=100")
            Regex("(?is)<article[^>]+class=[\"'][^\"']*\\bpledge\\b[^\"']*[\"'][^>]*>(.*?)</article>")
                .findAll(html)
                .mapNotNull { match ->
                    val row = match.groupValues[1]
                    val title = Regex("(?is)<h1[^>]*>(.*?)</h1>").find(row)?.groupValues?.getOrNull(1)
                        ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim() }
                        ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val dds = Regex("(?is)<dd[^>]*>(.*?)</dd>").findAll(row)
                        .map { Html.fromHtml(it.groupValues[1], Html.FROM_HTML_MODE_LEGACY).toString().trim() }
                        .toList()
                    val image = Regex("(?is)<img[^>]+src=[\"']([^\"']+)").find(row)?.groupValues?.getOrNull(1)?.let(::rsiAssetUrl)
                    val href = Regex("(?is)class=[\"'][^\"']*holosmallbtn[^\"']*[\"'][^>]+href=[\"']([^\"']+)").find(row)?.groupValues?.getOrNull(1).orEmpty()
                    val isUpgrade = title.contains("upgrade", true) || title.contains("升级")
                    BuybackItem(
                        title = translateBuybackTitle(title),
                        price = "—",
                        date = dds.firstOrNull()?.let(::cleanBuybackDate) ?: "—",
                        imageRes = fallbackImage,
                        originalName = title,
                        isUpgrade = isUpgrade || row.contains("data-fromshipid", true),
                        imageUrl = image,
                        contains = dds.drop(1),
                    )
                }.toList()
                .distinctBy { it.originalName + it.date }
                .ifEmpty { fallback.items() }
        }.getOrElse { fallback.items() }
    }

    private fun translateBuybackTitle(title: String): String = when {
        title.startsWith("Subscribers Store - ", true) -> title.removePrefix("Subscribers Store - ")
        else -> title
    }

    private fun cleanBuybackDate(value: String): String = runCatching {
        val parsed = java.time.LocalDate.parse(value, java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US))
        parsed.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA))
    }.getOrDefault(value)
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
                        val imageUrl = terminalImage(entry)
                            ?: terminalImageFallback(name)
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
        }.onFailure { Log.w("RefugeTerminal", "Wiki terminal refresh failed", it) }
            .getOrElse { fallback.items() }
    }

    private fun localized(node: JSONObject?): String {
        if (node == null) return ""
        return listOf("zh_CN", "en_EN", "en_US", "en").firstNotNullOfOrNull { key ->
            node.optString(key).takeIf { it.isNotBlank() }
        } ?: node.optString("text")
    }

    // The public wiki occasionally omits thumbnails for the first page of
    // vehicles. Keep the terminal visual list image-backed instead of
    // rendering a blank card while the upstream catalogue catches up.
    private fun terminalImageFallback(name: String): String? = when {
        name.equals("A1 Spirit", true) -> "https://media.starcitizen.tools/thumb/6/66/A1_x2_bombing_ground_vehicle_attackers_-_Cut.jpg/600px-A1_x2_bombing_ground_vehicle_attackers_-_Cut.jpg.webp"
        name.equals("A2 Hercules Starlifter", true) -> "https://media.starcitizen.tools/thumb/8/8f/A2_Hercules_Starlifter_-_Cargo.jpg/600px-A2_Hercules_Starlifter_-_Cargo.jpg.webp"
        else -> null
    }

    private fun terminalImage(entry: JSONObject): String? {
        val images = entry.opt("images")
        val raw = when (images) {
            null -> ""
            is JSONObject -> images.optString("thumbnail_url").ifBlank { images.optString("original_url") }
            is org.json.JSONArray -> images.optJSONObject(0)?.let {
                it.optString("thumbnail_url").ifBlank { it.optString("original_url") }
            }.orEmpty()
            else -> ""
        }.ifBlank { entry.optString("image") }
        return raw.takeIf { it.isNotBlank() }?.let(::rsiAssetUrl)
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
                    .takeIf { it.isNotBlank() }
                    ?.let(::rsiAssetUrl)
                    .orEmpty()
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

private fun rsiAssetUrl(value: String): String = when {
    value.startsWith("http://", true) || value.startsWith("https://", true) -> value
    value.startsWith("//") -> "https:$value"
    value.startsWith("/") -> "https://robertsspaceindustries.com$value"
    else -> "https://media.robertsspaceindustries.com/$value"
}
