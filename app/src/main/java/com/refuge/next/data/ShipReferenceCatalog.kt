package com.refuge.next.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.util.concurrent.TimeUnit

internal fun storefrontText(raw: String): String {
    // Decode escaped markup before stripping tags. Anchor labels remain readable.
    val decoded = org.jsoup.parser.Parser.unescapeEntities(raw, false)
    return Jsoup.parse(decoded).text().trim()
}

internal data class ShipReference(val name: String, val manufacturer: String, val description: String,
    val images: List<String>, val details: List<Pair<String, String>>, val sourceUrl: String? = null)

internal class ShipReferenceCatalog(context: Context) {
    companion object {
        @Volatile private var instance: ShipReferenceCatalog? = null
        fun shared(context: Context) = instance ?: synchronized(this) {
            instance ?: ShipReferenceCatalog(context.applicationContext).also { instance = it }
        }
    }
    val entries = MutableStateFlow<List<ShipReference>>(emptyList())
    private val file = File(context.filesDir, "rsi-ship-reference.json")
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private val refresh = RepositoryRefresh<List<ShipReference>>(300_000)
    private val hydration = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
        runCatching { if (file.isFile) parse(file.readText()) else emptyList() }.getOrDefault(emptyList()).also { entries.value = it }
    }
    suspend fun prepare(): List<ShipReference> = refresh.await("matrix") {
        hydration.await()
        withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url("https://robertsspaceindustries.com/ship-matrix/index").build()).execute().use {
                    check(it.isSuccessful)
                    val raw = it.body!!.string()
                    parse(raw).also { rows -> check(rows.isNotEmpty()); file.writeText(raw); entries.value = rows }
                }
            }.getOrElse { entries.value }
        }
    }
    private fun parse(raw: String): List<ShipReference> = JSONObject(raw).objects("data").map { ship ->
        ShipReference(ship.optString("name"), ship.obj("manufacturer").optString("name"), storefrontText(ship.optString("description")),
            ship.objects("media").flatMap { media -> listOf(media.obj("images").optString("slideshow"), media.optString("source_url")) }.mapNotNull(::normalizeImageUrl),
            buildList {
                add("定位" to ship.optString("type"))
                add("尺寸" to ship.optString("size"))
                if (!ship.isNull("min_crew") && !ship.isNull("max_crew"))
                    add("乘员" to "${ship.optInt("min_crew")}–${ship.optInt("max_crew")}")
                if (!ship.isNull("cargocapacity")) add("货舱" to "${ship.optInt("cargocapacity")} SCU")
                val dimensions = listOf("length", "beam", "height").map { ship.optDouble(it) }
                if (dimensions.all { it.isFinite() && it > 0 }) add("长 × 宽 × 高" to dimensions.joinToString(" × ", postfix = " m"))
                add("状态" to ship.optString("production_status"))
            }.filter { it.second.isNotBlank() && it.second != "null" },
            "https://robertsspaceindustries.com/pledge/ships/${ship.optString("slug")}".takeIf { ship.optString("slug").isNotBlank() })
    }.sortedByDescending { it.name.length }
}

internal fun findShipReference(entries: List<ShipReference>, name: String): ShipReference? {
    val normalized = name.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
    return entries.firstOrNull { entry ->
        val key = entry.name.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
        key.isNotBlank() && (" $normalized ").contains(" $key ")
    }
}
