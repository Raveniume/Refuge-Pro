package com.refuge.next.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RsiUpgradeTargetsTest {
    @Test fun targetsUseRsiPledgeIdsIncludingNestedAndUnquotedMarkup() {
        val html = """<input value="999"><div class="row active"><label><input value=42></label><span>Localized name</span></div>
            <div class=row><input value='53'/></div><div class=row><input value='42'/></div>
            <div class=row><input value='csrf'><input value='-1'><input value='0'></div>"""
        assertEquals(setOf(42L, 53L), parseUpgradeTargetIds(html))
        assertEquals(emptySet<Long>(), parseUpgradeTargetIds("<p>No eligible ships</p>"))
    }
}
