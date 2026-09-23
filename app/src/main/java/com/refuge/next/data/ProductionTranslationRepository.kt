package com.refuge.next.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

interface TranslationRepository {
    fun translate(value: String): String
    fun translateGameItem(value: String): String
}

/** Only explicit source fields may recover old localized snapshots; never reverse-translate. */
internal fun sourceName(value: String, originalName: String? = null): String =
    originalName?.takeIf { it.isNotBlank() && it != "—" } ?: value

internal fun displayName(
    value: String,
    enabled: Boolean,
    repository: TranslationRepository?,
    gameItem: Boolean = false,
    className: String? = null,
): String {
    if (!enabled || repository == null) return value
    if (!gameItem) return repository.translate(value)
    val key = className?.takeIf(String::isNotBlank)
    if (key != null) {
        val translated = repository.translateGameItem(key)
        if (translated != key) return translated
    }
    return repository.translateGameItem(value)
}

/**
 * Direct port of the original Flutter TranslationRepo lookup rules.
 * The source asset is the same versioned translation table advertised by the
 * RefugeNext version endpoint; missing keys always fall back to the source text.
 */
class ProductionTranslationRepository(context: Context) : TranslationRepository {
    private val appContext = context.applicationContext
    // Keep table parsing off the UI thread; display-name lookups also run on IO.
    private val translations by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        loadMap(appContext, "data/translation.1.2.2.json")
    }
    private val itemNames by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        loadMap(appContext, "data/item_names.json")
    }
    private val translator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LegacyNameTranslator(translations, itemNames)
    }
    private val warmScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Warm the immutable tables without changing any repository/cache records.
        warmScope.launch {
            translator
        }
    }

    override fun translate(value: String): String = translator.translate(value)
    override fun translateGameItem(value: String): String = translator.translateGameItem(value)

    private fun loadMap(context: Context, path: String): Map<String, String> = runCatching {
        val json = JSONObject(context.assets.open(path).bufferedReader().use { it.readText() })
        buildMap {
            json.keys().forEach { key -> json.optString(key).takeIf { it.isNotBlank() }?.let { put(key, it) } }
        }
    }.onFailure { Log.w("RefugeTranslation", "Failed to load $path", it) }.getOrDefault(emptyMap())
}

/** Pure lookup over the legacy tables. Never write its output back into source models. */
internal class LegacyNameTranslator(
    translations: Map<String, String>,
    itemNames: Map<String, String>,
) : TranslationRepository {
    private val translations = translations.entries.associate { normalize(it.key) to it.value }
    private val itemNames = itemNames.entries.associate { it.key.lowercase(Locale.US) to it.value }

    override fun translate(value: String): String {
        val normalized = normalize(value)
        if (normalized.isBlank()) return value
        corrections[normalized]?.let { return it }
        translations[normalized]?.let { return it }
        val translated = normalized.split(" - ").joinToString(" - ") { part ->
            if (part.contains(" to ", true) && part.contains("Edition", true)) translateUpgradePart(part)
            else part.split(" · ").joinToString(" · ") { segment ->
                corrections[segment] ?: translations[segment] ?: segment.split(" / ").joinToString(" / ") { term ->
                    corrections[term] ?: translations[term] ?: term
                }
            }
        }
        return if (translated == normalized) value else translated
    }

    override fun translateGameItem(value: String): String {
        var key = value.lowercase(Locale.US)
        if (key.endsWith("_scitem")) key = key.dropLast(7)
        return itemNames["item_name$key"] ?: itemNames["item_name_$key"] ?: translate(value)
    }

    private fun translateUpgradePart(value: String): String {
        val edition = when {
            value.contains("Warbond Edition", true) -> "战争债券版"
            value.contains("Standard Edition", true) -> "标准版"
            else -> ""
        }
        val core = value.replace(Regex("(?i)\\s*(Warbond|Standard) Edition\\s*"), "").trim()
        val endpoints = core.split(Regex("(?i)\\s+to\\s+"), limit = 2)
        if (endpoints.size != 2) return translations[value] ?: value
        return "${translateShipName(endpoints[0])} 到 ${translateShipName(endpoints[1])}${edition.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()}"
    }

    private fun translateShipName(value: String): String {
        val cleaned = value
            .replace(Regex("(?i)\\b(Banu|Drake|Crusader|Argo|Esperia|CNOU|Aegis|Anvil|RSI)\\b"), "")
            .replace(Regex("\\s+"), " ").trim()
        return corrections[value] ?: translations[value] ?: corrections[cleaned] ?: translations[cleaned] ?: value
    }

    private fun normalize(value: String): String = value
        .replace("\n", "")
        .replace("*", "")
        .replace('‘', '\'').replace('’', '\'')
        .replace('“', '"').replace('”', '"')
        .replace(Regex("\\s+"), " ").trim()

    private companion object {
        val corrections = mapOf(
            "Cargo Career Kit" to "货运职业套装",
            "Touring" to "巡游", "Luxury" to "豪华", "Interdiction" to "拦截",
            "Perseus" to "RSI 英仙座",
            "RSI Perseus" to "RSI 英仙座",
            "Polaris" to "RSI 北极星",
            "RSI Polaris" to "RSI 北极星",
            "Beanie Bundle" to "毛线帽套装",
            "Chiron Foundation Armor" to "凯龙奠基节装甲",
            "L-22 Alpha Wolf" to "L-22 阿尔法狼",
            "Dynasty Paint" to "王朝涂装",
            "Origin Jumpworks" to "起源悦动",
            "Aegis Dynamics" to "圣盾动力", "Anvil Aerospace" to "铁砧航空航天",
            "Drake Interplanetary" to "德雷克星际", "Crusader Industries" to "十字军工业",
            "Roberts Space Industries" to "罗伯特太空工业", "ARGO Astronautics" to "阿尔戈航天",
            "Consolidated Outland" to "联合外域", "MISC" to "武藏星航", "Mirai" to "未来",
            "Esperia" to "埃斯佩里亚", "Banu" to "巴努", "Gatac Manufacture" to "盖塔克制造",
            "Interceptor" to "截击机",
            "Roberts Space Industries (RSI)" to "罗伯特太空工业 (RSI)",
            "Package" to "游戏包",
            "Citizen Starter Pack" to "公民新手包",
            "Skin" to "涂装",
            "Exploration" to "探索",
            "Starter / Pathfinder" to "新手 / 探路者",
            "Combat" to "战斗",
            "Light Fighter" to "轻型战斗机",
            "Transporter" to "运输舰",
            "Luxury Touring" to "豪华巡游",
            "Light Freight" to "轻型货运",
            "Medium Freight" to "中型货运",
            "Heavy Freight" to "重型货运",
            "Bubble" to "球形护盾", "Quadrant" to "象限护盾",
            "Competition" to "竞速", "Destroyer" to "驱逐舰", "Ground" to "地面载具",
            "Gunship" to "炮艇", "Industrial" to "工业", "Multi-Role" to "多用途",
            "Snub Fighter" to "短程战斗机", "Starter" to "新手", "Support" to "支援",
            "Small" to "小型", "Medium" to "中型", "Large" to "大型", "Capital" to "主力级",
            "flight-ready" to "可飞行", "in-concept" to "概念设计", "in-production" to "生产中",
            "This is not a GAME PACKAGE. Please note a GAME PACKAGE is required to play the game and fly or access your ships." to "此商品不含游戏资格，进入游戏需要另购游戏包。",
        )
    }
}
