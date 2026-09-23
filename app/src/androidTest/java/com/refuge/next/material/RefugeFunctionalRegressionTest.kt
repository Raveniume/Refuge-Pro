package com.refuge.next.material

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.R
import com.refuge.next.data.*
import com.refuge.next.design.RefugeColors
import com.refuge.next.design.RefugePalette
import com.refuge.next.screens.HangarScreen
import com.refuge.next.screens.PresencePickerSheet
import com.refuge.next.screens.SettingsScreen
import com.refuge.next.screens.StoreScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Production composables with isolated repositories; no account or remote mutations. */
class RefugeFunctionalRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val products = listOf(
        product("Atlas", 20000, metadata = "Anvil", warbond = true),
        product("Arrow", 7500, metadata = "Anvil"),
        product("Aurora", 3000, metadata = "RSI", warbond = true),
        product("Blue Paint", 1000, category = StoreCategory.PAINTS),
        product("Flight Suit", 1500, category = StoreCategory.GEAR),
        product("Starter Pack", 4500, category = StoreCategory.PACKAGES),
        product("Rover", 4000, category = StoreCategory.VEHICLES),
        product("Fleet Bundle", 30000, category = StoreCategory.BUNDLES),
        product("Monthly Plan", 1000, category = StoreCategory.SUBSCRIPTIONS),
    )

    @Test fun storeSearchCategoriesAndEmptyState() {
        store()
        compose.onNodeWithText("3 \u9879").assertIsDisplayed()
        compose.onNodeWithContentDescription("\u641c\u7d22\u5546\u54c1").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("aNvIl")
        compose.onNodeWithText("2 \u9879").assertIsDisplayed()
        compose.onNodeWithText("Arrow").assertIsDisplayed()
        compose.onNodeWithText("Atlas").assertIsDisplayed()
        compose.onNodeWithText("Aurora").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextReplacement("not-a-product")
        compose.onNodeWithText("0 \u9879").assertIsDisplayed()
        compose.onNodeWithText("没有匹配“not-a-product”的商品", substring = false).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNodeWithContentDescription("\u641c\u7d22\u5546\u54c1").performClick()
        for (category in StoreCategory.entries.drop(1)) {
            compose.onNodeWithText(category.label).performScrollTo().performClick()
            compose.onNodeWithText("1 \u9879").assertIsDisplayed()
            compose.onNodeWithText(products.single { it.category == category }.title).assertIsDisplayed()
        }
    }

    @Test fun storePriceWarbondFiltersAndSortChangeResults() {
        store()
        compose.onNodeWithText("\u6392\u5e8f\uff1a\u9ed8\u8ba4").performClick()
        compose.onNodeWithText("\u4ef7\u683c\u4ece\u9ad8\u5230\u4f4e").performClick()
        assertAbove("Atlas", "Arrow")
        compose.onNodeWithText("\u7b5b\u9009").performClick()
        compose.onNodeWithText("0-100").performClick()
        back()
        compose.onNodeWithText("2 \u9879").assertIsDisplayed()
        compose.onNodeWithText("Aurora").assertIsDisplayed()
        compose.onNodeWithText("Arrow").assertIsDisplayed()
        compose.onNodeWithText("Atlas").assertDoesNotExist()
        compose.onNodeWithText("\u7b5b\u9009 \u00b7 \u5df2\u9009").performClick()
        compose.onNodeWithText("\u5168\u90e8").performClick()
        compose.onNodeWithText("\u4ec5\u663e\u793a\u6218\u4e89\u503a\u5238").performClick()
        back()
        compose.onNodeWithText("2 \u9879").assertIsDisplayed()
        assertAbove("Atlas", "Aurora")
        compose.onNodeWithText("Arrow").assertDoesNotExist()
        compose.onNodeWithText("\u7b5b\u9009 \u00b7 \u5df2\u9009").performClick()
        compose.onNodeWithText("100-500").performClick()
        back()
        compose.onNodeWithText("1 \u9879").assertIsDisplayed()
        compose.onNodeWithText("Atlas").assertIsDisplayed()
    }

    @Test fun localCartQuantityTotalsRemovalAndClear() {
        val cart = InMemoryCartRepository()
        store(cart = cart)
        compose.onNodeWithText("Aurora").performClick()
        compose.onNodeWithText("\u52a0\u5165\u8d2d\u7269\u8f66").performClick()
        compose.onNodeWithText("\u5df2\u52a0\u5165 1 \u4ef6").performClick()
        compose.onNodeWithText("\u5df2\u52a0\u5165 2 \u4ef6").assertIsDisplayed()
        back()
        compose.onNodeWithContentDescription("\u8d2d\u7269\u8f66").performClick()
        compose.onNodeWithText("2 \u00d7 $30").assertIsDisplayed()
        compose.onNodeWithText("$60.00").assertIsDisplayed()
        compose.onNodeWithText("\u79fb\u9664").performClick()
        compose.onNodeWithText("1 \u00d7 $30").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, cart.lines().single().quantity) }
        compose.onNodeWithText("\u79fb\u9664").performClick()
        compose.onNodeWithText("\u8d2d\u7269\u8f66\u4e3a\u7a7a").assertIsDisplayed()
        compose.onNodeWithText("\u5b8c\u6210").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        for (title in listOf("Aurora", "Arrow")) {
            compose.onNodeWithText(title).performClick()
            compose.onNodeWithText("\u52a0\u5165\u8d2d\u7269\u8f66").performClick()
            back()
        }
        compose.onNodeWithContentDescription("\u8d2d\u7269\u8f66").performClick()
        compose.onNodeWithText("$105.00").assertIsDisplayed()
        compose.onNodeWithText("\u6e05\u7a7a").performClick()
        compose.onNodeWithText("\u8d2d\u7269\u8f66\u4e3a\u7a7a").assertIsDisplayed()
        compose.runOnIdle { assertTrue(cart.lines().isEmpty()) }
    }

    @Test fun storeFailedLoadCanRetryWithoutRestart() {
        var attempts = 0
        val repository = object : StoreRepository {
            override suspend fun products(): List<StoreProduct> {
                attempts++
                if (attempts == 1) error("Fixture connection failed")
                return products
            }
        }
        store(repository)
        compose.onNodeWithText("Fixture connection failed").assertIsDisplayed()
        compose.onNodeWithText("\u91cd\u8bd5").performClick()
        compose.onNodeWithText("3 \u9879").assertIsDisplayed()
        compose.onNodeWithText("Aurora").assertIsDisplayed()
        compose.onNodeWithText("Fixture connection failed").assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, attempts) }
    }

    @Test fun hangarSearchAndCombinedFiltersPreserveInventory() {
        val inventory = listOf(
            hangarItem(1, "Alpha", "Aurora MR", "2026-09-01", ship = true, giftable = true),
            hangarItem(2, "Beta", "Arrow", "2026-09-02", ship = true, giftable = false),
            hangarItem(3, "Gamma", "Paint", "2026-09-03", ship = false, giftable = true),
        )
        val repository = object : HangarRepository {
            override fun cachedInventory() = inventory
            override suspend fun inventory() = inventory
            override suspend fun ownedShips() = emptyList<OwnedShip>()
        }
        val buyback = object : BuybackRepository {
            override suspend fun items() = emptyList<BuybackItem>()
        }
        scene { canvas, palette ->
            HangarScreen(canvas, palette, repository, buyback, ProductionHangarLogRepository(),
                ProductionCcuRepository(), false, 0, {}, {}, {}, true, UserPresence.ONLINE, null, {})
        }
        compose.onNodeWithContentDescription("\u641c\u7d22\u673a\u5e93").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("aUrOrA")
        compose.onNodeWithText("Aurora MR").assertIsDisplayed()
        compose.onNodeWithText("Arrow").assertDoesNotExist()
        compose.onNodeWithText("Paint").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNodeWithContentDescription("\u641c\u7d22\u673a\u5e93").performClick()
        compose.onNodeWithText("\u7b5b\u9009").performClick()
        compose.onNodeWithText("\u4ec5\u663e\u793a\u8230\u8239").performClick()
        compose.onNodeWithText("\u53ef\u8d60\u9001").performClick()
        compose.onNodeWithContentDescription("\u5b8c\u6210\u7b5b\u9009").performClick()
        compose.onNodeWithText("Aurora MR").assertIsDisplayed()
        compose.onNodeWithText("Arrow").assertDoesNotExist()
        compose.onNodeWithText("Paint").assertDoesNotExist()
        compose.onNodeWithText("\u7b5b\u9009").performClick()
        compose.onNodeWithText("\u4ec5\u663e\u793a\u8230\u8239").performClick()
        compose.onNodeWithText("\u53ef\u8d60\u9001").performClick()
        compose.onNodeWithContentDescription("\u5b8c\u6210\u7b5b\u9009").performClick()
        compose.onNodeWithText("\u6392\u5e8f\uff1a\u6700\u65b0").performClick()
        compose.onNodeWithText("\u6700\u65e9\u540c\u6b65").performClick()
        assertAbove("Aurora MR", "Arrow")
        compose.runOnIdle { assertEquals(3, repository.cachedInventory().size) }
    }

    @Test fun settingsThemePersistenceAboutAndPresenceCallbacks() {
        // Use the test APK's context, never the installed app's settings or account store.
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val repository = PreferencesSettingsRepository(testContext)
        repository.save(AppSettings(darkTheme = false))
        val settings = mutableStateOf(repository.load())
        val showPresence = mutableStateOf(false)
        val presence = UserStatusSource(initialPresence = UserPresence.ONLINE)
        var navigatedTo = -1
        var clears = 0
        compose.setContent {
            val palette = if (settings.value.darkTheme) RefugeColors.dark else RefugeColors.light
            RefugeScene(palette) { canvas ->
                PageGlassScope(canvas, content = {
                    SettingsScreen(canvas, palette, settings.value.darkTheme, settings.value.translationEnabled, { settings.value = settings.value.copy(translationEnabled = !settings.value.translationEnabled); repository.save(settings.value) }, 4,
                        { navigatedTo = it }, {
                            settings.value = settings.value.copy(darkTheme = !settings.value.darkTheme)
                            repository.save(settings.value)
                        }, { clears++ }, productionCacheManifest, true, presence.presence, null,
                        { showPresence.value = true })
                    if (showPresence.value) PresencePickerSheet(canvas, palette, presence.presence,
                        { presence.set(it); showPresence.value = false }, { showPresence.value = false })
                }, overlay = {})
            }
        }
        compose.onNodeWithText("\u5df2\u5173\u95ed").assertIsDisplayed()
        compose.onNodeWithContentDescription("\u6df1\u8272\u4e3b\u9898").performClick()
        compose.onNodeWithText("\u5df2\u5f00\u542f").assertIsDisplayed()
        compose.runOnIdle { assertTrue(PreferencesSettingsRepository(testContext).load().darkTheme) }
        compose.onNodeWithText("\u6df1\u8272\u4e3b\u9898").performClick()
        compose.onNodeWithText("\u5df2\u5173\u95ed").assertIsDisplayed()
        compose.onNodeWithText("\u6e05\u7406\u7f13\u5b58").performClick()
        compose.runOnIdle { assertEquals(1, clears) }
        compose.onNodeWithText("\u7248\u672c").performClick()
        compose.onNodeWithText("RefugeNext").assertIsDisplayed()
        back()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.runOnIdle { showPresence.value = true }
        compose.onNodeWithText(UserPresence.AWAY.label).performClick()
        compose.runOnIdle { assertEquals(UserPresence.AWAY, presence.presence) }
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithContentDescription("\u8fd4\u56de").performClick()
        compose.runOnIdle { assertEquals(4, navigatedTo) }
    }

    private fun store(
        repository: StoreRepository = object : StoreRepository {
            override fun cachedProducts() = products
            override suspend fun products() = products
        },
        cart: InMemoryCartRepository = InMemoryCartRepository(),
    ) = scene { canvas, palette ->
        StoreScreen(canvas, palette, repository, cart, false, 1, {}, {},
            true, UserPresence.ONLINE, null, {})
    }

    private fun scene(content: @Composable (LayerBackdrop, RefugePalette) -> Unit) {
        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                RefugeScene(RefugeColors.light) { canvas ->
                    PageGlassScope(canvas, content = { content(canvas, RefugeColors.light) }, overlay = {})
                }
            }
        }
    }

    private fun assertAbove(first: String, second: String) {
        val firstBounds = compose.onNodeWithText(first).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val secondBounds = compose.onNodeWithText(second).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("$first should precede $second", firstBounds.top < secondBounds.top)
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5000) { compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty() }
        compose.onNode(isDialog()).assertDoesNotExist()
    }

    private fun product(title: String, price: Int, metadata: String = "Fixture",
        warbond: Boolean = false, category: StoreCategory = StoreCategory.SHIPS) =
        StoreProduct(title, title, category, metadata, price,
            "android.resource://com.refuge.next.compose/${R.drawable.m80_hero}", "Fixture description", warbond)

    private fun hangarItem(id: Long, title: String, original: String, date: String,
        ship: Boolean, giftable: Boolean) = HangarItem(title, "$30", date, R.drawable.m80_hero,
        id = id, originalName = original, isGiftable = giftable,
        typeLabel = if (ship) "\u8230\u8239" else "\u6d82\u88c5")
}
