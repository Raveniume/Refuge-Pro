package com.refuge.next.data

import org.jsoup.Jsoup

/** RSI's chooseUpgradeTarget response contains the authoritative pledge IDs. */
internal fun parseUpgradeTargetIds(rendered: String): Set<Long> =
    Jsoup.parseBodyFragment(rendered).select("div.row input[value]")
        .mapNotNull { it.attr("value").toLongOrNull()?.takeIf { id -> id > 0 } }
        .toSet()
