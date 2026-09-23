package com.refuge.next.data

import org.junit.Assert.*
import org.junit.Test

class HangarImagesTest {
    private val placeholder = "https://cdn.robertsspaceindustries.com/static/images/Temp/default-image.png"
    private val target = "https://media.robertsspaceindustries.com/0qo64m8r93b1h/subscribers_vault_thumbnail.jpg"

    @Test fun upgradeSkipsHttp200PlaceholderAndUsesDestinationChild() {
        val item = HangarItem("Upgrade", "$5", "", 0, isUpgrade = true,
            upgradeTo = "S-65 Stingray", imageUrl = placeholder,
            includedEntries = listOf(
                HangarIncludedItem("Upgrade - Prospector to S-65 Stingray Warbond Edition", placeholder),
                HangarIncludedItem("Upgrade - Prospector To S-65 Stingray", target),
            ))
        assertEquals(target, item.displayImageUrl)
    }

    @Test fun unrelatedBonusArtworkIsNotUsedAsShipThumbnail() {
        val item = HangarItem("Upgrade", "$5", "", 0, isUpgrade = true,
            upgradeTo = "S-65 Stingray", imageUrl = placeholder,
            includedEntries = listOf(HangarIncludedItem("Bonus paint", "https://example.com/paint.png")))
        assertNull(item.displayImageUrl)
        assertEquals(target, item.copy(imageUrl = target).displayImageUrl)
    }

    @Test fun normalPledgesRetainTheirOwnArtwork() {
        val item = HangarItem("Package", "$45", "", 0, imageUrl = "package.jpg",
            includedEntries = listOf(HangarIncludedItem("Ship", target)))
        assertEquals("package.jpg", item.displayImageUrl)
    }
}
