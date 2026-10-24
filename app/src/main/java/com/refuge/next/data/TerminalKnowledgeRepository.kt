package com.refuge.next.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** Public game references use a separate client: RSI cookies and tokens never leave RSI. */
internal class TerminalKnowledgeRepository(context: Context) {
    private val directory = File(context.filesDir, "terminal-knowledge").apply { mkdirs() }
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS).build()

    private fun read(url: String, key: String, refresh: Boolean): String {
        val file = File(directory, key)
        if (!refresh && file.isFile) return file.readText()
        val value = client.newCall(Request.Builder().url(url).build()).execute().use {
            check(it.isSuccessful) { "资料服务暂时不可用（${it.code}）" }
            it.body?.string() ?: error("资料响应为空")
        }
        if (value.trimStart().startsWith("[")) JSONArray(value) else JSONObject(value)
        val pending = File(directory, "$key.pending")
        pending.writeText(value)
        check(pending.renameTo(file)) { "资料缓存保存失败" }
        return value
    }

    private fun readFresh(url: String): String = client.newCall(Request.Builder().url(url).build()).execute().use {
        check(it.isSuccessful) { "资料版本服务暂时不可用（${it.code}）" }
        it.body?.string() ?: error("资料版本响应为空")
    }

    suspend fun missions(refresh: Boolean = false): MissionCatalog = withContext(Dispatchers.IO) {
        // Version metadata is tiny and is checked on every page entry. The
        // large mission payload stays cached when the website version is the
        // same; a network failure falls back to the last known manifest.
        val versions = JSONArray(runCatching {
            readFresh("https://scmdb.net/data/versions.json")
        }.getOrElse {
            read("https://scmdb.net/data/versions.json", "mission-versions.json", refresh = false)
        })
        val selected = versions.objects().firstOrNull { it.optString("version").contains("-live.", true) }
            ?: error("暂未找到 LIVE 任务资料")
        val version = selected.getString("version")
        val filename = selected.getString("file")
        require(Regex("[A-Za-z0-9_.-]+\\.json").matches(filename))
        val sourceUrl = "https://scmdb.net/data/$filename"
        val source = JSONObject(runCatching {
            if (!refresh) {
                val local = File(directory, filename)
                if (local.isFile && JSONObject(local.readText()).optString("version").equals(version, true)) local.readText()
                else read(sourceUrl, filename, refresh = true)
            } else read(sourceUrl, filename, refresh = true)
        }.getOrElse { read(sourceUrl, filename, refresh = false) })
        check(source.getString("version").equals(version, true)) { "任务资料版本不一致" }
        val language = runCatching {
            val languageUrl = "https://raw.githubusercontent.com/Walkersifolia/SCMDB_zh_cn/main/lang-zh_CN-live.json"
            JSONObject(if (!refresh) {
                val local = File(directory, "mission-zh.json")
                if (local.isFile && JSONObject(local.readText()).optString("version").equals(version, true)) local.readText()
                else read(languageUrl, "mission-zh.json", refresh = true)
            } else read(languageUrl, "mission-zh.json", refresh = true))
        }.getOrNull()?.takeIf { it.optString("version").equals(version, true) }
        MissionCatalog(version, source, language)
    }

    private fun scm(path: String, params: Map<String, String>): JSONObject {
        val url = "https://scms.flowcld.com/app-api/product/blueprint/$path".toHttpUrl().newBuilder()
        params.forEach { (key, value) -> url.addQueryParameter(key, value) }
        val response = client.newCall(Request.Builder().url(url.build()).build()).execute().use {
            check(it.isSuccessful) { "蓝图服务暂时不可用（${it.code}）" }
            JSONObject(it.body?.string() ?: error("蓝图响应为空"))
        }
        check(response.optInt("code", -1) == 0) { "蓝图服务未能返回资料，请重试" }
        return response
    }

    suspend fun blueprints(query: String, category: String, page: Int, rewardOnly: Boolean): BlueprintPage = withContext(Dispatchers.IO) {
        val params = mutableMapOf("pageNo" to "$page", "pageSize" to "40", "usageMode" to "fabricate")
        if (query.isNotBlank()) params["keyword"] = query.trim()
        if (category.isNotBlank()) params["categoryName"] = category
        if (rewardOnly) params["isReward"] = "true"
        val data = scm("page", params).getJSONObject("data")
        BlueprintPage(data.objects("list"), data.optInt("total"))
    }

    suspend fun blueprint(id: Long): JSONObject = withContext(Dispatchers.IO) {
        scm("get-detail", mapOf("id" to "$id")).getJSONObject("data")
    }
}

internal data class BlueprintPage(val rows: List<JSONObject>, val total: Int)

internal class MissionCatalog(val version: String, val source: JSONObject, val language: JSONObject?) {
    val missions = source.objects("contracts")
    private val keys = language?.optJSONObject("keys") ?: JSONObject()
    fun translated(key: String, fallback: String, chinese: Boolean = true): String {
        val text = if (chinese) keys.optJSONObject(key.removePrefix("@"))?.optString("tr").orEmpty().ifBlank { fallback } else fallback
        return text.replace("\\n", "\n").replace(Regex("</?[^>]+>"), "")
    }
    fun text(row: JSONObject, field: String, chinese: Boolean = true) = translated(
        row.optString("${field}LocKey").ifBlank { row.optString("${field}Key") }, row.optString(field), chinese)
    fun locations(row: JSONObject, field: String): List<String> = row.strings(field).mapNotNull {
        source.obj("locationPools").optJSONObject(it)?.optString("name")?.takeIf(String::isNotBlank)
    }.distinct()
    fun find(identity: String): JSONObject? = missions.firstOrNull {
        it.optString("id") == identity || identity in it.strings("contractDefinitionIds") || it.optString("debugName") == identity
    }
}

internal fun JSONObject.blueprintTitle(): String = optString("blueprintNameCn").ifBlank { optString("blueprintName") }
internal fun blueprintModifiers(option: JSONObject, quality: Int): List<Pair<String, Double>> {
    val rows = when (val raw = option.opt("modifiers")) {
        is JSONArray -> raw.objects()
        is String -> runCatching { JSONArray(raw).objects() }.getOrDefault(emptyList())
        else -> emptyList()
    }
    return rows.groupBy { it.optString("property") }.mapNotNull { (_, ranges) ->
        val range = ranges.firstOrNull { quality in it.optInt("startQuality")..it.optInt("endQuality") } ?: return@mapNotNull null
        val start = range.optInt("startQuality"); val end = range.optInt("endQuality")
        val fraction = if (end == start) 0.0 else (quality - start).toDouble() / (end - start)
        val value = range.optDouble("modifierAtStart") + fraction * (range.optDouble("modifierAtEnd") - range.optDouble("modifierAtStart"))
        val delta = if (range.optString("modifierType") == "multiplicative") (value - 1.0) * 100 else value * 100
        range.optString("property_cn").ifBlank { range.optString("property") } to delta
    }
}
