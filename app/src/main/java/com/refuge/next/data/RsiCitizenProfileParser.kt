package com.refuge.next.data

/** Public fields exposed by RSI's citizen dossier. */
internal data class RsiCitizenProfileProjection(
    val avatarPath: String? = null,
    val enlisted: String? = null,
    val organizationId: String? = null,
    val organizationName: String? = null,
    val organizationImagePath: String? = null,
    val organizationRank: String? = null,
    val organizationLevel: Int = 0,
)

internal fun isRsiCitizenDossier(html: String): Boolean =
    html.contains("UEE Citizen Record", ignoreCase = true) &&
        html.contains("Handle name", ignoreCase = true)

/** Account pages expose the public handle even when authenticated GraphQL is unavailable. */
internal fun parseRsiAccountSidebarHandle(html: String): String? {
    val element = Regex(
        """(?is)<([a-z][a-z0-9:-]*)\b[^>]*class\s*=\s*["'][^"']*\bc-account-sidebar__profile-info-handle\b[^"']*["'][^>]*>(.*?)</\1\s*>""",
    ).find(html)?.groupValues?.getOrNull(2) ?: return null
    return cleanRsiProfileText(element)
        .removePrefix("@").trim()
        .takeIf(::isUsableRsiHandle)
}

internal fun resolveRsiProfileHandle(
    graphqlNickname: String?,
    graphqlUsername: String?,
    accountHtml: String,
    cachedHandle: String?,
): String? = sequenceOf(
    graphqlNickname,
    graphqlUsername,
    parseRsiAccountSidebarHandle(accountHtml),
    cachedHandle,
).mapNotNull { candidate ->
    candidate?.trim()?.removePrefix("@")?.trim()?.takeIf(::isUsableRsiHandle)
}.firstOrNull()

private fun isUsableRsiHandle(value: String): Boolean =
    value.isNotBlank() && !isKnownProfilePlaceholder(value)

/**
 * Mirrors the original Flutter parser's `.profile.left-col` and
 * `.main-org.right-col` projection without treating account badges as fleet
 * data. A valid dossier with no public main organization intentionally yields
 * null organization fields.
 */
internal fun parseRsiCitizenProfile(html: String): RsiCitizenProfileProjection {
    if (html.isBlank()) return RsiCitizenProfileProjection()

    val classedDivs = classedDivStarts(html)
    val profileStart = classedDivs.firstOrNull { start ->
        "profile" in start.classes && "left-col" in start.classes
    }
    val organizationStart = classedDivs.firstOrNull { "main-org" in it.classes }
        ?: classedDivs.firstOrNull { start ->
            "right-col" in start.classes && isLegacyOrganizationSection(
                profileColumnSection(html, classedDivs, start),
            )
        }
    val profileSection = profileStart?.let { start ->
        profileColumnSection(html, classedDivs, start)
    }.orEmpty()
    val avatarPath = Regex(
        """(?is)<div\b[^>]*class\s*=\s*["'][^"']*\bthumb\b[^"']*["'][^>]*>.*?<img\b[^>]*src\s*=\s*["']([^"']+)["']""",
    ).find(profileSection)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
    val enlisted = labeledStrongValue(html, "Enlisted")

    if (organizationStart == null) {
        return RsiCitizenProfileProjection(avatarPath = avatarPath, enlisted = enlisted)
    }

    val organizationSection = profileColumnSection(html, classedDivs, organizationStart)
    val organizationLinks = Regex(
        """(?is)<a\b[^>]*href\s*=\s*["'](?:https?://[^/"']+)?/orgs/([^"'/?#]+)["'][^>]*>(.*?)</a>""",
    ).findAll(organizationSection).toList()
    val organizationId = organizationLinks.firstOrNull()
        ?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
    val organizationName = organizationLinks.firstNotNullOfOrNull { match ->
        cleanRsiProfileText(match.groupValues.getOrNull(2).orEmpty()).takeIf(String::isNotBlank)
    }
    val organizationImagePath = Regex(
        """(?is)<div\b[^>]*class\s*=\s*["'][^"']*\bthumb\b[^"']*["'][^>]*>.*?<img\b[^>]*src\s*=\s*["']([^"']+)["']""",
    ).find(organizationSection)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
    val organizationRank = labeledStrongValue(organizationSection, "Organization rank")
        ?: legacyOrganizationRank(organizationSection)
    val rankingBlock = Regex(
        """(?is)<div\b[^>]*class\s*=\s*["'][^"']*\branking\b[^"']*["'][^>]*>(.*?)</div>""",
    ).find(organizationSection)?.groupValues?.getOrNull(1).orEmpty()
    val organizationLevel = Regex(
        """(?is)<span\b[^>]*class\s*=\s*["'][^"']*\bactive\b[^"']*["'][^>]*>""",
    ).findAll(rankingBlock).count()

    return RsiCitizenProfileProjection(
        avatarPath = avatarPath,
        enlisted = enlisted,
        organizationId = organizationId,
        organizationName = organizationName,
        organizationImagePath = organizationImagePath,
        organizationRank = organizationRank,
        organizationLevel = organizationLevel,
    )
}

private data class ClassedDivStart(
    val index: Int,
    val classes: Set<String>,
)

private fun classedDivStarts(html: String): List<ClassedDivStart> = Regex(
    """(?is)<div\b[^>]*class\s*=\s*["']([^"']*)["'][^>]*>""",
).findAll(html).map { match ->
    ClassedDivStart(
        index = match.range.first,
        classes = match.groupValues[1]
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .toSet(),
    )
}.toList()

private fun profileColumnSection(
    html: String,
    classedDivs: List<ClassedDivStart>,
    start: ClassedDivStart,
): String {
    val end = classedDivs.firstOrNull { candidate ->
        candidate.index > start.index && candidate.classes.any { profileColumnClass ->
            profileColumnClass in setOf("left-col", "right-col", "main-org")
        }
    }?.index ?: html.length
    return html.substring(start.index, end)
}

private fun isLegacyOrganizationSection(section: String): Boolean {
    val hasOrganizationLink = Regex(
        """(?is)<a\b[^>]*href\s*=\s*["'](?:https?://[^/"']+)?/orgs/[^"'/?#]+["']""",
    ).containsMatchIn(section)
    if (!hasOrganizationLink) return false

    return section.contains("Organization rank", ignoreCase = true) ||
        Regex(
            """(?is)<div\b[^>]*class\s*=\s*["'][^"']*\branking\b[^"']*["']""",
        ).containsMatchIn(section) ||
        Regex(
            """(?is)<div\b[^>]*class\s*=\s*["'][^"']*\bthumb\b[^"']*["'][^>]*>.*?<a\b[^>]*href\s*=\s*["'](?:https?://[^/"']+)?/orgs/""",
        ).containsMatchIn(section)
}

/** The original Flutter parser used `.right-col .entry:nth-child(3) strong`. */
private fun legacyOrganizationRank(section: String): String? {
    val entries = Regex(
        """(?is)<p\b[^>]*class\s*=\s*["'][^"']*\bentry\b[^"']*["'][^>]*>(.*?)</p>""",
    ).findAll(section).map { it.groupValues[1] }.toList()
    if (entries.firstOrNull()?.contains("/orgs/", ignoreCase = true) != true) return null
    return entries.getOrNull(2)?.let { entry ->
        Regex("""(?is)<strong\b[^>]*>(.*?)</strong>""")
            .find(entry)?.groupValues?.getOrNull(1)
            ?.let(::cleanRsiProfileText)
            ?.takeIf(String::isNotBlank)
    }
}

/** Removes legacy placeholder values that previously masqueraded as fleet data. */
internal fun sanitizeCachedProfile(profile: ProfileData): ProfileData {
    val organizationName = profile.organizationName
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.takeKnownFleetPlaceholder()
    val organizationRank = profile.organizationRank
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.takeUnless(::isKnownProfilePlaceholder)
    val organizationLevel = profile.organizationLevel.coerceAtLeast(0)
    return profile.copy(
        rank = organizationRank ?: "—",
        organizationName = organizationName,
        organizationRank = organizationRank,
        organizationLevel = organizationLevel,
        organizationImage = profile.organizationImage.takeIf { organizationName != null },
        level = organizationLevel.takeIf { it > 0 }?.toString() ?: "—",
    )
}

private fun isNeutralProfileValue(value: String?): Boolean {
    val normalized = value?.trim()?.lowercase().orEmpty()
    return normalized.isEmpty() || normalized == "—" ||
        Regex("^[\\$￥]?0+(?:\\.0+)?(?:\\s*(?:usd|uec|rec))?$").matches(normalized)
}

internal fun hasMeaningfulProfileSnapshot(profile: ProfileData): Boolean = listOf(
    profile.displayName,
    profile.organizationId,
    profile.organizationName,
    profile.avatarUrl,
    profile.totalSpent.takeUnless { it == "—" },
    profile.hangarValue.takeUnless { it == "—" },
    profile.credit.takeUnless { it == "—" },
    profile.registerDate.takeUnless { it == "—" },
    profile.uec.takeUnless { it == "—" },
    profile.rec.takeUnless { it == "—" },
    profile.currentValue.takeUnless { it == "—" },
    profile.referralCode.takeUnless { it == "—" },
).any { !isNeutralProfileValue(it) }

/** A partial successful refresh must not erase useful fields from disk cache. */
internal fun retainMeaningfulProfileSnapshot(
    previous: ProfileData?,
    refreshed: ProfileData,
    preserveOrganization: Boolean = true,
): ProfileData {
    val old = previous?.takeIf(::hasMeaningfulProfileSnapshot) ?: return refreshed
    fun keepText(newValue: String, oldValue: String): String =
        newValue.takeUnless(::isNeutralProfileValue) ?: oldValue
    return refreshed.copy(
        handle = refreshed.handle.takeUnless { it.isBlank() || it == "RSI 账户" } ?: old.handle,
        displayName = refreshed.displayName?.takeUnless(::isNeutralProfileValue) ?: old.displayName,
        city = keepText(refreshed.city, old.city),
        rank = if (preserveOrganization) keepText(refreshed.rank, old.rank) else refreshed.rank,
        organizationId = if (preserveOrganization) {
            refreshed.organizationId?.takeUnless(::isNeutralProfileValue) ?: old.organizationId
        } else {
            refreshed.organizationId
        },
        organizationName = if (preserveOrganization) {
            refreshed.organizationName?.takeUnless(::isNeutralProfileValue) ?: old.organizationName
        } else {
            refreshed.organizationName
        },
        organizationRank = if (preserveOrganization) {
            refreshed.organizationRank?.takeUnless(::isNeutralProfileValue) ?: old.organizationRank
        } else {
            refreshed.organizationRank
        },
        organizationLevel = if (preserveOrganization) {
            refreshed.organizationLevel.takeIf { it > 0 } ?: old.organizationLevel
        } else {
            refreshed.organizationLevel
        },
        organizationImage = if (preserveOrganization) {
            refreshed.organizationImage?.takeUnless(::isNeutralProfileValue) ?: old.organizationImage
        } else {
            refreshed.organizationImage
        },
        level = if (preserveOrganization) keepText(refreshed.level, old.level) else refreshed.level,
        totalSpent = keepText(refreshed.totalSpent, old.totalSpent),
        hangarValue = keepText(refreshed.hangarValue, old.hangarValue),
        credit = keepText(refreshed.credit, old.credit),
        registerDate = keepText(refreshed.registerDate, old.registerDate),
        uec = keepText(refreshed.uec, old.uec),
        rec = keepText(refreshed.rec, old.rec),
        currentValue = keepText(refreshed.currentValue, old.currentValue),
        referralCode = keepText(refreshed.referralCode, old.referralCode),
        avatarUrl = refreshed.avatarUrl?.takeUnless(::isNeutralProfileValue) ?: old.avatarUrl,
        email = refreshed.email?.takeIf(String::isNotBlank) ?: old.email,
        username = refreshed.username?.takeIf(String::isNotBlank) ?: old.username,
        hasGamePackage = refreshed.hasGamePackage ?: old.hasGamePackage,
        isAuthenticated = refreshed.isAuthenticated || old.isAuthenticated,
    )
}

/** Clears stale organization fields when a successful public dossier now exposes no main organization. */
internal fun retainRsiProfileRefresh(
    previous: ProfileData?,
    refreshed: ProfileData,
    citizenDossierAvailable: Boolean,
): ProfileData = retainMeaningfulProfileSnapshot(
    previous = previous,
    refreshed = refreshed,
    preserveOrganization = !citizenDossierAvailable || refreshed.organizationName != null,
)

private fun String?.takeKnownFleetPlaceholder(): String? = this?.takeUnless { value ->
    isKnownProfilePlaceholder(value) || Regex("^舰队\\s*\\d+\\s*艘(?:\\s*·.*)?$").matches(value)
}

private fun isKnownProfilePlaceholder(value: String): Boolean = value.trim() in setOf(
    "RSI 账户",
    "已拥有游戏包",
    "在线资料暂不可用",
    "无公开主组织",
    "无公开头衔",
    "未连接 RSI",
    "舰队资料",
    "—",
)

private fun labeledStrongValue(html: String, label: String): String? {
    val escapedLabel = Regex.escape(label)
    return Regex(
        """(?is)<span\b[^>]*class\s*=\s*["'][^"']*\blabel\b[^"']*["'][^>]*>\s*$escapedLabel\s*</span>\s*<strong\b[^>]*>(.*?)</strong>""",
    ).find(html)?.groupValues?.getOrNull(1)
        ?.let(::cleanRsiProfileText)
        ?.takeIf(String::isNotBlank)
}

private fun cleanRsiProfileText(value: String): String = decodeHtmlEntities(
    value.replace(Regex("(?is)<[^>]+>"), " "),
).replace(Regex("\\s+"), " ").trim()

private fun decodeHtmlEntities(value: String): String = Regex(
    """&(#x[0-9a-f]+|#[0-9]+|amp|lt|gt|quot|apos|nbsp);""",
    RegexOption.IGNORE_CASE,
).replace(value) { match ->
    when (val entity = match.groupValues[1].lowercase()) {
        "amp" -> "&"
        "lt" -> "<"
        "gt" -> ">"
        "quot" -> "\""
        "apos" -> "'"
        "nbsp" -> " "
        else -> {
            val codePoint = if (entity.startsWith("#x")) {
                entity.drop(2).toIntOrNull(16)
            } else {
                entity.drop(1).toIntOrNull()
            }
            codePoint?.takeIf(Character::isValidCodePoint)
                ?.let { String(Character.toChars(it)) }
                ?: match.value
        }
    }
}
