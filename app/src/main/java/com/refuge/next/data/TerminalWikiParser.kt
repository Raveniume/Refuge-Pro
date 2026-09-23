package com.refuge.next.data

import java.net.URI

/** Pure schema boundary, also used for the on-disk projection. No Android JSON stubs in tests. */
internal object TerminalWikiParser {
    fun enrich(item: TerminalItem, source: Map<*, *>, fullPorts: Boolean = false): TerminalItem {
        val images = imageUrls(source)
        val version = source.text("version")
        return item.copy(
            size = source.number("size")?.takeIf { it % 1.0 == 0.0 && it <= Int.MAX_VALUE }?.toInt(),
            sourceType = source.text("type"),
            sourceSubType = source.text("sub_type") ?: source.text("subtype"),
            sourceVersion = version,
            portTags = source.strings("port_tags").toSet(),
            performance = performance(source),
            ports = if (fullPorts) ports(source["ports"], version) else emptyList(),
            imageUrl = images.firstOrNull() ?: normalizeImageUrl(item.imageUrl),
            imageUrls = images,
            filterAttributes = source.obj("filter_attributes").mapNotNull { (key, value) ->
                (value as? String)?.let { key.toString() to it }
            }.toMap() + buildMap {
                source.text("career")?.let { put("职能", it) }
                source.obj("shield").text("face_type")?.let { put("护盾类型", it) }
                source.text("grade")?.let { put("等级", it) }
                source.obj("personal_weapon").text("fire_mode")?.let { put("射击模式", it) }
                source.obj("vehicle_weapon").text("type")?.let { put("类型", it) }
                if (item.category == TerminalCategory.VEHICLES) put("载具类型", when {
                    source["is_gravlev"] == true -> "反重力载具"
                    source["is_spaceship"] == false || source.text("type")?.contains("ground", true) == true -> "地面载具"
                    else -> "飞船"
                })
            },
        )
    }

    fun imageUrls(source: Map<*, *>, preferOriginal: Boolean = false): List<String> = buildList {
        val keys = if (preferOriginal) listOf("original_url", "original", "thumbnail_url", "thumbnail", "url", "image_url")
            else listOf("thumbnail_url", "thumbnail", "original_url", "original", "url", "image_url")
        fun collect(node: Any?) {
            when (node) {
                is String -> normalizeImageUrl(node)?.let(::add)
                is Map<*, *> -> keys.forEach { key ->
                    normalizeImageUrl(node[key] as? String, node.text("source"))?.let(::add)
                }
                is List<*> -> node.forEach { child ->
                    when (child) {
                        is Map<*, *> -> keys.forEach { key -> normalizeImageUrl(child[key] as? String, child.text("source"))?.let(::add) }
                        is String -> normalizeImageUrl(child)?.let(::add)
                    }
                }
            }
        }
        collect(source["images"])
        collect(source["image"])
        listOf("image_url", "thumbnail_url", "original_url", "imageUrl").forEach { collect(source[it]) }
    }.distinct()

    fun performance(source: Map<*, *>): TerminalPerformance {
        val saved = source.obj("performance_snapshot")
        if (saved.isNotEmpty()) return TerminalPerformance(
            massKg = saved.number("massKg"), massTotalKg = saved.number("massTotalKg"),
            cargoScu = saved.number("cargoScu"), shieldHp = saved.number("shieldHp"),
            shieldRegen = saved.number("shieldRegen"), pilotDps = saved.number("pilotDps"),
            sustainedDps = saved.number("sustainedDps"), burstDps = saved.number("burstDps"),
            scmSpeed = saved.number("scmSpeed"), maxSpeed = saved.number("maxSpeed"),
            powerGeneration = saved.number("powerGeneration"), powerUsage = saved.number("powerUsage"),
            coolingGeneration = saved.number("coolingGeneration"), coolingUsage = saved.number("coolingUsage"),
            quantumSpeed = saved.number("quantumSpeed"),
        )
        val shield = source.obj("shield")
        val weaponry = source.obj("weaponry")
        val damage = source.obj("vehicle_weapon").obj("damage")
        val generation = source.obj("resource_network").obj("generation")
        return TerminalPerformance(
            massKg = source.number("mass"),
            massTotalKg = source.number("mass_total"), // mass_loadout is NOT total ship mass.
            cargoScu = source.number("cargo_capacity"),
            shieldHp = source.number("shield_hp") ?: shield.number("hp") ?: shield.number("max_health") ?: shield.number("max_shield_health"),
            shieldRegen = shield.number("regeneration") ?: shield.number("regen_rate"),
            pilotDps = weaponry.number("pilot_dps"),
            sustainedDps = weaponry.number("pilot_sustained_dps") ?: damage.number("sustained_60s"),
            burstDps = damage.number("burst"),
            scmSpeed = source.obj("speed").number("scm"), maxSpeed = source.obj("speed").number("max"),
            powerGeneration = source.obj("power").number("generation_segments") ?: generation.number("power")
                ?: source.obj("power_plant").number("power_segment_generation"),
            powerUsage = source.obj("power").number("used_segments_shields"),
            coolingGeneration = source.obj("cooling").number("generation_segments") ?: generation.number("coolant")
                ?: source.obj("cooler").number("coolant_segment_generation"),
            coolingUsage = source.obj("cooling").number("used_segments_shields"),
            quantumSpeed = source.obj("quantum").number("quantum_speed")
                ?: source.obj("quantum_drive").obj("standard_jump").number("drive_speed"),
        )
    }

    fun ports(value: Any?, version: String?, parent: String = "", depth: Int = 0): List<TerminalPort> {
        if (depth > 24) return emptyList()
        return (value as? List<*>).orEmpty().flatMapIndexed { index, raw ->
            val node = raw as? Map<*, *> ?: return@flatMapIndexed emptyList()
            val name = node.text("name") ?: return@flatMapIndexed emptyList()
            val id = if (parent.isEmpty()) "$index:$name" else "$parent/$index:$name"
            val children = (node["ports"] as? List<*>).orEmpty()
            val equipped = node.obj("equipped_item")
            val equippedId = node.text("equipped_item_uuid") ?: equipped.text("uuid")
            val equippedItem = if (equippedId == null || equipped.text("name") == null) null else enrich(
                TerminalItem(
                    id = equippedId, name = equipped.text("name").orEmpty(),
                    manufacturer = equipped.obj("manufacturer").text("name") ?: "—",
                    category = terminalTypeCategory(equipped.text("type")) ?: TerminalCategory.SHIP_COMPONENTS,
                    tags = emptyList(), value = "—", usd = "—", description = "",
                    className = equipped.text("class_name"),
                ),
                equipped + mapOf("version" to (equipped.text("version") ?: version)),
            )
            val port = TerminalPort(
                id = node.text("snapshot_id") ?: id, name = name,
                position = node.text("position"), type = node.text("type"),
                minSize = node.obj("sizes").int("min"), maxSize = node.obj("sizes").int("max"),
                // editable_children does not authorize replacing this port's item.
                editable = node["editable"] == true,
                equippedItemId = equippedId, equippedItem = equippedItem,
                compatibleTypes = (node["compatible_types"] as? List<*>).orEmpty().mapNotNull { entry ->
                    val type = entry as? Map<*, *> ?: return@mapNotNull null
                    type.text("type")?.let { TerminalPortType(it, type.strings("sub_types").toSet()) }
                },
                requiredTags = node.strings("required_tags").toSet(),
                // Decorative attachment ports must not prevent changing a gun.
                hasChildren = node["has_children"] == true || children.any { child ->
                    val childNode = child as? Map<*, *>
                    childNode?.text("equipped_item_uuid") != null || childNode?.get("editable") == true
                },
            )
            listOf(port) + ports(children, version, id, depth + 1)
        }
    }

    /** Store the lossless subset used by loadouts, not translated details or derived numbers. */
    fun snapshot(item: TerminalItem): Map<String, Any?> = mapOf(
        "filter_attributes" to item.filterAttributes,
        "uuid" to item.id, "name" to item.name, "class_name" to item.className,
        "manufacturer" to mapOf("name" to item.manufacturer),
        "size" to item.size, "type" to item.sourceType, "sub_type" to item.sourceSubType,
        "version" to item.sourceVersion, "port_tags" to item.portTags.toList(),
        "images" to item.imageUrls.ifEmpty { listOfNotNull(item.imageUrl) },
        "performance_snapshot" to item.performance?.let { p -> mapOf(
            "massKg" to p.massKg, "massTotalKg" to p.massTotalKg, "cargoScu" to p.cargoScu,
            "shieldHp" to p.shieldHp, "shieldRegen" to p.shieldRegen, "pilotDps" to p.pilotDps,
            "sustainedDps" to p.sustainedDps, "burstDps" to p.burstDps, "scmSpeed" to p.scmSpeed,
            "maxSpeed" to p.maxSpeed, "powerGeneration" to p.powerGeneration, "powerUsage" to p.powerUsage,
            "coolingGeneration" to p.coolingGeneration, "coolingUsage" to p.coolingUsage, "quantumSpeed" to p.quantumSpeed,
        ) },
        "ports" to item.ports.map { port -> mapOf(
            "snapshot_id" to port.id, "name" to port.name, "position" to port.position, "type" to port.type,
            "sizes" to mapOf("min" to port.minSize, "max" to port.maxSize), "editable" to port.editable,
            "equipped_item_uuid" to port.equippedItemId, "equipped_item" to port.equippedItem?.let(::snapshot),
            "compatible_types" to port.compatibleTypes.map { mapOf("type" to it.type, "sub_types" to it.subTypes.toList()) },
            "required_tags" to port.requiredTags.toList(), "has_children" to port.hasChildren,
        ) },
    )
}

internal fun normalizeImageUrl(raw: String?, source: String? = null): String? {
    val text = raw?.trim()?.takeUnless { it.isEmpty() || it.equals("null", true) || it == "—" } ?: return null
    val url = when {
        text.startsWith("//") -> "https:$text"
        text.startsWith("https://", true) -> text
        text.startsWith("http://", true) -> "https://${text.substring(7)}"
        text.startsWith("/") -> when (source) {
            "starcitizen.tools" -> "https://media.starcitizen.tools$text"
            "star-citizen.wiki" -> "https://cdn.star-citizen.wiki$text"
            else -> "https://api.star-citizen.wiki$text"
        }
        else -> return null
    }
    return runCatching { URI(url.replace(" ", "%20")) }.getOrNull()
        ?.takeIf { it.scheme == "https" && !it.host.isNullOrBlank() && it.rawUserInfo == null }
        ?.toASCIIString()
}

private fun Map<*, *>.text(key: String): String? = (this[key] as? String)?.trim()
    ?.takeUnless { it.isEmpty() || it.equals("null", true) || it == "—" }
private fun Map<*, *>.obj(key: String): Map<*, *> = this[key] as? Map<*, *> ?: emptyMap<Any, Any>()
private fun Map<*, *>.number(key: String): Double? = when (val value = this[key]) {
    is Number -> value.toDouble()
    is String -> value.toDoubleOrNull()
    else -> null
}?.takeIf { it.isFinite() && it >= 0 }
private fun Map<*, *>.int(key: String): Int? = number(key)?.takeIf { it % 1.0 == 0.0 && it <= Int.MAX_VALUE }?.toInt()
private fun Map<*, *>.strings(key: String): List<String> = when (val value = this[key]) {
    is List<*> -> value.filterIsInstance<String>().filter { it.isNotBlank() && it != "null" }
    is String -> listOf(value).filter { it.isNotBlank() && it != "null" }
    else -> emptyList()
}
