package com.refuge.next.data

/** RSI's default-image URL returns HTTP 200, so image-loader error fallback cannot detect it. */
internal fun usableHangarImage(url: String?): String? = url?.trim()?.takeIf {
    it.isNotEmpty() && !it.contains("default-image", ignoreCase = true) &&
        !it.contains("placeholder", ignoreCase = true)
}

val HangarItem.displayImageUrl: String?
    get() {
        if (!isUpgrade) return imageUrl
        // Upgrade child entries identify the destination; do not borrow artwork
        // from unrelated bonus items included in the same pledge.
        val target = upgradeTo?.trim().orEmpty()
        return includedEntries.firstNotNullOfOrNull { entry ->
            if (target.isNotEmpty() && entry.title.contains(target, ignoreCase = true))
                usableHangarImage(entry.imageUrl) else null
        } ?: usableHangarImage(imageUrl)
    }
