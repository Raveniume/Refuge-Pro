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
    private val directClient = client.newBuilder().proxy(java.net.Proxy.NO_PROXY).build()
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
        // Other ships load on demand. Eagerly downloading every vessel at
        // startup monopolized the network while the user was logging in.
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
        val request = Request.Builder().url("https://cdn.erkul.games/$branch/$path").build()
        fun fetch(http: OkHttpClient): String = http.newCall(request).execute().use {
            if (!it.isSuccessful) throw java.io.IOException("Erkul 目录暂时不可用（${it.code}）")
            decode(it.body?.bytes() ?: error("目录响应为空"))
        }
        // A device-wide development proxy may not reach the public CDN.
        // Retry the same public URL directly; never substitute a different branch.
        val raw = try { fetch(client) } catch (_: java.io.IOException) { fetch(directClient) }
        // Decode/parse before replacing a good cache entry.
        if (raw.trimStart().startsWith("[")) JSONArray(raw) else JSONObject(raw)
        file.writeText(raw)
        return raw
    }

    suspend fun catalog(branch: String, refresh: Boolean = false): ErkulCatalog = withContext(Dispatchers.IO) {
        lock.withLock {
            // The manifest is small, so page entry checks its live version.
            // Full index/family payloads are only downloaded when that version
            // differs from the persisted snapshot.
            var offline = false
            val manifest = try { JSONObject(read(branch, "catalog.bin", refresh = true)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                offline = true
                val file = File(dir, "$branch/catalog.bin")
                if (file.isFile) JSONObject(file.readText()) else throw failure
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
        check(group != null) { "当前版本没有对应舰船数据，请更新目录" }
        val className = summary.getString("className")
        val index = JSONObject(read(catalog.branch, group.getString("indexPath")))
        val path = index.objects("blobs").firstOrNull { it.getString("id") == className }
            ?.getString("path") ?: error("当前版本尚未收录该舰船")
        JSONObject(read(catalog.branch, path)).also { ships.put(key, it) }
        } }
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

/** A resolved slot keeps its immediate assembled parent so the dashboard can
 * use Erkul's real pilot/turret/PDS classification instead of guessing from
 * a translated label or path fragment. */
internal data class ErkulSlot(
    val path: String,
    val port: JSONObject,
    val item: JSONObject?,
    val original: JSONObject?,
    val pilot: Boolean,
    val depth: Int,
    val container: JSONObject? = null,
    val rootItem: JSONObject? = null,
    val rootPort: JSONObject? = null,
)

/** Mirrors Erkul's by-location weapon aggregation using the assembled top
 * component, control seats and hardpoint metadata. */
internal fun erkulWeaponLocation(slot: ErkulSlot): String {
    val root = slot.rootItem ?: slot.container ?: slot.item ?: return "pilot"
    val subtype = root.optString("subType").lowercase().filter(Char::isLetterOrDigit)
    val type = root.optString("type").lowercase().filter(Char::isLetterOrDigit)
    if (subtype == "pdcturret" || root.strings("tags").any { it.equals("pdc", true) }) return "pdc"
    if (subtype == "mannedturret" || type == "turretbase") return "mannedTurret"
    val control = slot.rootPort?.obj("control")
    val seats = (control?.strings("seats").orEmpty() + root.strings("seats")).distinct()
    val hasPilotSeat = seats.any { it.contains("pilot", true) || it.contains("driver", true) }
    val hasOtherSeat = seats.any { !(it.contains("pilot", true) || it.contains("driver", true)) }
    return when {
        control == null || control.optBoolean("pilot") || hasPilotSeat || (!hasOtherSeat && slot.pilot) -> "pilot"
        hasOtherSeat -> "remoteOperator"
        else -> "pilot"
    }
}

/** Resolve the whole tree after parent replacement; old turret/missile children never survive a swap. */
internal fun resolveErkulSlots(ship: JSONObject, catalog: ErkulCatalog, overrides: JSONObject): List<ErkulSlot> {
    val all = mutableListOf<ErkulSlot>()
    fun visit(
        nodes: List<JSONObject>, ports: List<JSONObject>, prefix: String, pilot: Boolean, depth: Int,
        container: JSONObject? = null, rootItem: JSONObject? = null, rootPort: JSONObject? = null,
    ) {
        if (depth > 20) return
        for (node in nodes) {
            val name = node.optString("portName")
            val path = if (prefix.isBlank()) name else "$prefix/$name"
            val port = node.optJSONObject("hardpoint") ?: ports.firstOrNull { it.optString("name") == name } ?: JSONObject()
            val original = node.optJSONObject("item")
            val item = if (overrides.has(path)) catalog.components[overrides.optString(path).lowercase()] else original
            val isPilot = port.obj("control").optBoolean("pilot", pilot)
            val topItem = rootItem ?: item.takeIf { prefix.isBlank() }
            val topPort = rootPort ?: port.takeIf { prefix.isBlank() }
            all += ErkulSlot(path, port, item, original, isPilot, depth, container, topItem, topPort)
            if (item != null) {
                val childNodes = if (overrides.has(path)) item.objects("slots") else node.objects("children").ifEmpty { item.objects("slots") }
                // An empty replaceable mount still has real child hardpoints.
                val children = childNodes.ifEmpty { item.objects("ports").map { JSONObject().put("portName", it.optString("name")).put("hardpoint", it) } }
                visit(children, item.objects("ports"), path, isPilot, depth + 1, item, topItem, topPort)
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
