package com.refuge.next.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import java.util.concurrent.TimeUnit

internal fun JSONObject.objects(key: String): List<JSONObject> = optJSONArray(key).objects()
internal fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
internal fun JSONObject.strings(key: String): List<String> = optJSONArray(key)?.let { rows -> (0 until rows.length()).map { rows.optString(it) } }.orEmpty()
internal fun JSONObject.number(key: String): Double? = (opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() }
internal fun JSONObject.obj(key: String): JSONObject = optJSONObject(key) ?: JSONObject()
internal fun JSONObject.itemName(): String = obj("i18n").optString("displayName").ifBlank {
    obj("i18n").optString("name").ifBlank { optString("name").ifBlank { optString("className") } }
}

internal data class ErkulCatalog(
    val branch: String, val version: String, val manifest: JSONObject, val ships: List<JSONObject>,
    val components: Map<String, JSONObject>, val manufacturers: Map<String, String>, val offline: Boolean = false,
)

/** Read-only public catalogue. Filenames and versions always come from the live manifest. */
internal class ErkulLoadoutRepository(private val context: Context) {
    companion object {
        @Volatile private var instance: ErkulLoadoutRepository? = null
        fun shared(context: Context): ErkulLoadoutRepository = instance ?: synchronized(this) {
            instance ?: ErkulLoadoutRepository(context.applicationContext).also { instance = it }
        }
    }
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS).build()
    private val dir = File(context.filesDir, "erkul-v8").apply { mkdirs() }
    private val lock = Mutex()
    private val cache = java.util.concurrent.ConcurrentHashMap<String, ErkulCatalog>()
    private val ships = android.util.LruCache<String, JSONObject>(3)
    private val shipLock = Mutex()
    private val dailyRefreshMillis = 24L * 60L * 60L * 1_000L
    fun cachedCatalog(branch: String): ErkulCatalog? = cache[branch]
    fun cachedShip(catalog: ErkulCatalog, id: String): JSONObject? = ships["${catalog.branch}:${catalog.version}:$id"]

    suspend fun preload(branch: String) {
        // Hydrate the local snapshot first.  A stale snapshot is still useful
        // for immediate rendering while the once-per-day cloud refresh runs.
        val current = catalog(branch)
        warmAssembledData(branch, current)
        if (shouldRefresh(branch)) {
            runCatching {
                val refreshed = catalog(branch, refresh = true)
                warmAssembledData(branch, refreshed)
            }
        }
    }

    private suspend fun warmAssembledData(branch: String, current: ErkulCatalog) {
        current.ships.firstOrNull()?.let { runCatching { ship(current, it) } }
        // Warm the disk cache without keeping every parsed hardpoint tree in RAM.
        withContext(Dispatchers.IO) {
            current.manifest.objects("groups").forEach { group ->
                runCatching {
                    val index = JSONObject(read(branch, group.getString("indexPath")))
                    index.objects("blobs").forEach { blob ->
                        ensureActive()
                        runCatching { read(branch, blob.getString("path")) }
                    }
                }
            }
        }
    }

    private fun shouldRefresh(branch: String): Boolean {
        val file = File(dir, "$branch/catalog.bin")
        return !file.isFile ||
            (System.currentTimeMillis() - file.lastModified()).coerceAtLeast(0L) >= dailyRefreshMillis
    }

    private fun decode(bytes: ByteArray): String = InflaterInputStream(bytes.inputStream(), Inflater(true)).bufferedReader().use { it.readText() }
    private fun read(branch: String, path: String, refresh: Boolean = false): String {
        require(branch in setOf("LIVE", "PTU") && !path.contains("..") && Regex("[A-Za-z0-9_/.-]+\\.bin").matches(path))
        val file = File(dir, "$branch/${path.replace('/', '_')}").apply { parentFile?.mkdirs() }
        if (!refresh && file.isFile) return file.readText()
        val raw = client.newCall(Request.Builder().url("https://cdn.erkul.games/$branch/$path").build()).execute().use {
            check(it.isSuccessful) { "Erkul 目录暂时不可用（${it.code}）" }
            decode(it.body?.bytes() ?: error("目录响应为空"))
        }
        // Decode/parse before replacing a good cache entry.
        if (raw.trimStart().startsWith("[")) JSONArray(raw) else JSONObject(raw)
        file.writeText(raw)
        return raw
    }

    suspend fun catalog(branch: String, refresh: Boolean = false): ErkulCatalog = withContext(Dispatchers.IO) {
        lock.withLock {
            if (!refresh) cache[branch]?.let { return@withLock it }
            // A normal read is cache-first.  Only an explicit/manual refresh
            // reaches the network before returning, so a cold screen can use
            // the downloaded dataset without waiting for the CDN.
            var offline = !refresh
            val local = if (!refresh) runCatching {
                val file = File(dir, "$branch/catalog.bin")
                if (file.isFile) JSONObject(file.readText()) else null
            }.getOrNull() else null
            val manifest = local ?: try { JSONObject(read(branch, "catalog.bin", refresh = true)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                offline = true
                val file = File(dir, "$branch/catalog.bin")
                if (file.isFile) JSONObject(file.readText()) else return@withLock fallbackCatalog(branch)
            }
            check(manifest.optInt("schemaVersion") == 8) { "Erkul 数据格式已更新，请更新应用" }
            val version = manifest.getString("dataVersion")
            cache[branch]?.takeIf { it.version == version }?.let { return@withLock it.copy(offline = offline) }
            val indexEntry = manifest.objects("singles").firstOrNull { it.getString("kind") == "index" }
            val index = if (indexEntry != null) JSONObject(read(branch, indexEntry.getString("path"))) else JSONObject().put("ships", JSONArray())
            val families = manifest.objects("families")
            val components = coroutineScope {
                families.chunked(4).flatMap { batch -> batch.map { family -> async {
                    JSONArray(read(branch, family.getString("path"))).objects()
                } }.awaitAll().flatten() }
            }
            val manufacturers = components.filter { it.optString("category") == "Manufacturer" || it.has("code") }
                .associate { it.optString("className") to it.itemName() }
                ErkulCatalog(branch, version, manifest, index.objects("ships"),
                components.filter { it.has("className") }.associateBy { it.getString("className").lowercase() }, manufacturers, offline)
                .also { cache[branch] = it }
        }
    }

    suspend fun ship(catalog: ErkulCatalog, summary: JSONObject): JSONObject {
        val key = "${catalog.branch}:${catalog.version}:${summary.getString("className")}"
        ships[key]?.let { return it }
        return withContext(Dispatchers.IO) { shipLock.withLock {
        ships[key]?.let { return@withLock it }
        val kind = if (summary.optString("category") == "AssembledGroundVehicle") "groundvehicles" else "ships"
        val group = catalog.manifest.objects("groups").firstOrNull { it.getString("kind") == kind }
        if (group == null) return@withLock fallbackShip(summary).also { ships.put(key, it) }
        val className = summary.getString("className")
        val indexPath = group.getString("indexPath")
        val localIndex = runCatching { JSONObject(read(catalog.branch, indexPath)) }.getOrNull()
        if (localIndex != null) {
            val path = localIndex.objects("blobs").first { it.getString("id") == className }.getString("path")
            return@withLock JSONObject(read(catalog.branch, path)).also { ships.put(key, it) }
        }

        // The CDN can temporarily deny a branch's assembled group index even
        // though its catalog, summaries, and component families are already
        // cached. Keep the selector usable in that case by resolving a ship
        // from the other locally complete branch. LIVE/PTU share the same
        // assembled payload shape, and the selected branch's summary still
        // supplies the visible name, manufacturer, and size.
        val fallbackBranch = if (catalog.branch == "PTU") "LIVE" else "PTU"
        val fallbackIndex = cachedGroupIndex(fallbackBranch, kind)
        val fallbackPath = fallbackIndex?.objects("blobs")
            ?.firstOrNull { it.optString("id") == className }
            ?.optString("path")
            ?.takeIf(String::isNotBlank)
        if (fallbackPath != null) {
            return@withLock JSONObject(read(fallbackBranch, fallbackPath)).also { ships.put(key, it) }
        }
        // A catalog can be available while the assembled payload is denied.
        // Keep the editor usable with a deterministic local snapshot rather
        // than replacing the whole screen with an error card.
        return@withLock fallbackShip(summary)
        } }
    }

    /** Small offline editor snapshot used when the CDN is unavailable on a cold install. */
    private fun fallbackCatalog(branch: String): ErkulCatalog {
        val names = listOf(
            "M80" to "Origin Jumpworks", "100i" to "Origin Jumpworks",
            "325a" to "Origin Jumpworks", "Perseus" to "RSI",
        )
        val ships = names.map { (name, maker) ->
            JSONObject().put("className", name.lowercase().replace(" ", "_"))
                .put("name", name).put("manufacturerName", maker).put("size", 3)
                .put("i18n", JSONObject().put("displayName", name).put("class", "Ship"))
        }
        val manifest = JSONObject().put("schemaVersion", 8).put("dataVersion", "offline-${branch.lowercase()}")
            .put("singles", JSONArray()).put("families", JSONArray()).put("groups", JSONArray())
        val offlineComponents = listOf(
            offlinePowerPlant(), offlineCooler(), offlineShield(), offlineQuantumDrive(),
        ).associateBy { it.getString("className").lowercase() }
        return ErkulCatalog(branch, "offline-${branch.lowercase()}", manifest, ships, offlineComponents, emptyMap(), offline = true)
            .also { cache[branch] = it }
    }

    private fun offlineFlow(resource: String, units: Double, output: Boolean, unitKind: String = "powerSegment"): JSONObject =
        JSONObject().put(if (output) "produces" else "consumes", JSONArray().put(
            JSONObject().put("resource", resource).put("unitKind", unitKind).put("units", units)
        ))

    private fun offlineComponent(
        className: String,
        name: String,
        type: String,
        category: String,
        size: Int,
        onlineFlows: List<JSONObject>,
        extra: JSONObject = JSONObject(),
    ): JSONObject = JSONObject().put("className", className).put("name", name)
        .put("type", type).put("category", category).put("size", size)
        .put("mass", size * 90.0)
        .put("i18n", JSONObject().put("displayName", name).put("displayType", category).put("class", category))
        .put("resource", JSONObject().put("states", JSONArray().put(JSONObject().put("name", "Online").put("flows", JSONArray(onlineFlows)))))
        .let { JSONObject(it.toString()).apply { extra.keys().forEach { key -> put(key, extra.get(key)) } } }

    private fun offlinePowerPlant() = offlineComponent(
        "offline_power_plant", "原厂发电机", "PowerPlant", "PowerPlant", 3,
        listOf(offlineFlow("Power", 30.0, output = true)),
    )

    private fun offlineCooler() = offlineComponent(
        "offline_cooler", "原厂冷却器", "Cooler", "Cooler", 3,
        listOf(offlineFlow("Power", 4.0, output = false), offlineFlow("Coolant", 40.0, output = true, unitKind = "coolantPerSecond")),
        JSONObject().put("connection", JSONObject().put("heatRateOnline", 4.0)),
    )

    private fun offlineShield() = offlineComponent(
        "offline_shield", "原厂护盾", "Shield", "Shield", 3,
        listOf(offlineFlow("Power", 6.0, output = false)),
        JSONObject().put("shield", JSONObject().put("maxShieldHealth", 2400.0).put("maxShieldRegen", 80.0)),
    )

    private fun offlineQuantumDrive() = offlineComponent(
        "offline_quantum_drive", "原厂量子引擎", "QuantumDrive", "QuantumDrive", 3,
        listOf(offlineFlow("Power", 5.0, output = false)),
        JSONObject().put("qdrive", JSONObject().put("params", JSONObject().put("driveSpeed", 120000000.0)).put("quantumFuelRequirement", .01)),
    )

    private fun fallbackShip(summary: JSONObject): JSONObject {
        val className = summary.optString("className", "m80")
        val displayName = summary.itemName().ifBlank { summary.optString("name", className) }
        val powerPlant = offlinePowerPlant()
        val cooler = offlineCooler()
        val shield = offlineShield()
        val quantum = offlineQuantumDrive()
        fun slot(name: String, item: JSONObject) = JSONObject().put("portName", name)
            .put("hardpoint", JSONObject().put("name", name).put("minSize", 3).put("maxSize", 3)
                .put("accepts", JSONArray().put(JSONObject().put("type", item.optString("type"))))
                .put("flags", JSONObject().put("editable", true))).put("item", item)
        val slots = JSONArray().put(slot("powerPlant", powerPlant)).put(slot("cooler", cooler))
            .put(slot("shield", shield)).put(slot("quantumDrive", quantum))
        return JSONObject(summary.toString()).put("className", className).put("name", displayName)
            .put("i18n", JSONObject().put("displayName", displayName).put("class", "Ship"))
            .put("vehicle", JSONObject().put("crewSize", 1).put("hardpoints", JSONArray()))
            .put("slots", slots)
            .put("precomputed", JSONObject().put("massFixedKg", 10000.0).put("cargo", 0).put("hp", JSONObject().put("total", 10000.0)).put("fuel", JSONObject().put("quantumCapacity", 100.0)))
            .put("powerPools", JSONObject())
    }

    private fun cachedGroupIndex(branch: String, kind: String): JSONObject? {
        val prefix = "$kind.group."
        return File(dir, branch).listFiles()
            ?.firstOrNull { it.isFile && it.name.startsWith(prefix) && it.name.endsWith(".bin") }
            ?.let { runCatching { JSONObject(it.readText()) }.getOrNull() }
    }

    fun savedPlans(branch: String, version: String, ship: String): List<Pair<String, JSONObject>> =
        context.getSharedPreferences("named-loadouts", Context.MODE_PRIVATE).all.entries
            .filter { it.key.startsWith("$branch:$version:$ship:") }
            .mapNotNull { entry -> runCatching { entry.key.substringAfter("$branch:$version:$ship:") to JSONObject(entry.value.toString()) }.getOrNull() }
            .sortedBy { it.first }

    fun savePlan(branch: String, version: String, ship: String, name: String, draft: JSONObject) {
        context.getSharedPreferences("named-loadouts", Context.MODE_PRIVATE).edit()
            .putString("$branch:$version:$ship:${name.trim()}", draft.toString()).apply()
    }

    fun loadDraft(branch: String, version: String, ship: String, slot: Int): JSONObject = runCatching {
        JSONObject(context.getSharedPreferences("native-loadouts", Context.MODE_PRIVATE).getString("$branch:$version:$ship:$slot", "{}")!!)
    }.getOrElse { JSONObject() }
    fun saveDraft(branch: String, version: String, ship: String, slot: Int, draft: JSONObject) {
        context.getSharedPreferences("native-loadouts", Context.MODE_PRIVATE).edit().putString("$branch:$version:$ship:$slot", draft.toString()).apply()
    }
}

internal data class ErkulSlot(val path: String, val port: JSONObject, val item: JSONObject?, val original: JSONObject?, val pilot: Boolean, val depth: Int)

/** Resolve the whole tree after parent replacement; old turret/missile children never survive a swap. */
internal fun resolveErkulSlots(ship: JSONObject, catalog: ErkulCatalog, overrides: JSONObject): List<ErkulSlot> {
    val all = mutableListOf<ErkulSlot>()
    fun visit(nodes: List<JSONObject>, ports: List<JSONObject>, prefix: String, pilot: Boolean, depth: Int) {
        if (depth > 20) return
        for (node in nodes) {
            val name = node.optString("portName")
            val path = if (prefix.isBlank()) name else "$prefix/$name"
            val port = node.optJSONObject("hardpoint") ?: ports.firstOrNull { it.optString("name") == name } ?: JSONObject()
            val original = node.optJSONObject("item")
            val item = if (overrides.has(path)) catalog.components[overrides.optString(path).lowercase()] else original
            val isPilot = port.obj("control").optBoolean("pilot", pilot)
            all += ErkulSlot(path, port, item, original, isPilot, depth)
            if (item != null) {
                val childNodes = if (overrides.has(path)) item.objects("slots") else node.objects("children").ifEmpty { item.objects("slots") }
                // An empty replaceable mount still has real child hardpoints.
                val children = childNodes.ifEmpty { item.objects("ports").map { JSONObject().put("portName", it.optString("name")).put("hardpoint", it) } }
                visit(children, item.objects("ports"), path, isPilot, depth + 1)
            }
        }
    }
    visit(ship.objects("slots"), ship.obj("vehicle").objects("hardpoints"), "", true, 0)
    return all
}

internal fun erkulSlotAccepts(slot: ErkulSlot, item: JSONObject): Boolean {
    val port = slot.port
    if (!port.obj("flags").optBoolean("editable") || port.obj("flags").optBoolean("uneditable")) return false
    if (port.obj("flags").optBoolean("classLocked") && item.optString("className") != slot.original?.optString("className")) return false
    val size = item.number("size") ?: return false
    val min = port.number("minSize") ?: return false
    val max = port.number("maxSize") ?: return false
    if (size < min || size > max) return false
    if (port.objects("accepts").none { it.optString("type").equals(item.optString("type"), true) &&
        (it.strings("subTypes").isEmpty() || it.strings("subTypes").any { sub -> sub.equals(item.optString("subType"), true) }) }) return false
    fun normalize(tags: List<String>) = tags.map { it.removePrefix("$").lowercase() }.toSet()
    val itemTags = normalize(item.strings("tags"))
    if (!itemTags.containsAll(normalize(port.strings("requiredTags")))) return false
    val requirements = normalize(item.strings("requiredTags"))
    return requirements.isEmpty() || normalize(port.strings("portTags")).containsAll(requirements)
}

internal fun erkulSlotGroup(slot: ErkulSlot): String = when(slot.item?.optString("type") ?: slot.port.objects("accepts").firstOrNull()?.optString("type")) {
    "WeaponGun", "Turret" -> "武器与炮塔"
    "Shield", "ShieldController" -> "护盾"
    "PowerPlant", "Cooler" -> "电力与散热"
    "QuantumDrive", "JumpDrive", "FlightController" -> "引擎与航行"
    "Missile", "MissileLauncher", "BombLauncher", "Bomb" -> "导弹与挂架"
    else -> "其他设备"
}

internal fun erkulPortLabel(slot: ErkulSlot): String {
    val location = slot.path.lowercase().let { p -> listOf("left" to "左", "right" to "右", "nose" to "机首", "wing" to "翼", "top" to "顶部", "bottom" to "底部").filter { it.first in p }.joinToString("") { it.second } }
    val rawType = slot.item?.optString("type").orEmpty()
    val type = mapOf(
        "PowerPlant" to "发电机", "Cooler" to "冷却器", "Shield" to "护盾",
        "QuantumDrive" to "量子引擎", "FlightController" to "飞行控制器",
        "Radar" to "雷达", "WeaponGun" to "武器",
    )[rawType] ?: slot.item?.obj("i18n")?.optString("displayType").orEmpty()
    return listOf(location, type.ifBlank { erkulSlotGroup(slot) }).filter { it.isNotBlank() }.joinToString(" · ")
}
