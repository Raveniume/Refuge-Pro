package com.refuge.next.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductionDataTest {
    @Test
    fun csrfMetaTokenIsExtractedWithoutExposingMarkup() {
        assertEquals(
            "csrf-example",
            parseRsiCsrfToken(
                "<meta charset='utf-8'><meta name=\"csrf-token\" content=\"csrf-example\"><meta name='other' content='ignored'>",
            ),
        )
        assertEquals(null, parseRsiCsrfToken("<meta name='csrf-token' content=''>"))
    }

    @Test
    fun accountSidebarHandleSupportsGraphqlFailureFallback() {
        val html = """
            <aside>
              <span class="c-account-sidebar__profile-info-displayname">Public Name</span>
              <a class="c-account-sidebar__profile-info-handle">@SidebarHandle</a>
            </aside>
        """.trimIndent()

        assertEquals("SidebarHandle", parseRsiAccountSidebarHandle(html))
        assertEquals(
            "SidebarHandle",
            resolveRsiProfileHandle(null, null, html, "RSI 账户"),
        )
    }

    @Test
    fun meaningfulCachedProfileFieldsSurvivePartialRefresh() {
        val old = ProfileData(
            handle = "CachedHandle",
            organizationName = "Cached Org",
            organizationRank = "Experienced",
            organizationLevel = 2,
            level = "2",
            totalSpent = "$100",
            isAuthenticated = true,
        )
        val refreshed = retainMeaningfulProfileSnapshot(
            old,
            ProfileData(handle = "CachedHandle", isAuthenticated = true),
        )

        assertEquals("Cached Org", refreshed.organizationName)
        assertEquals("Experienced", refreshed.organizationRank)
        assertEquals(2, refreshed.organizationLevel)
        assertEquals("2", refreshed.level)
        assertEquals("$100", refreshed.totalSpent)
    }

    @Test
    fun successfulDossierWithoutPublicOrganizationClearsStaleFleetFields() {
        val old = ProfileData(
            handle = "Citizen",
            organizationId = "OLDORG",
            organizationName = "Old Organization",
            organizationRank = "Officer",
            organizationLevel = 4,
            organizationImage = "https://example.invalid/old-org.png",
            rank = "Officer",
            level = "4",
            totalSpent = "$100",
            isAuthenticated = true,
        )

        val refreshed = retainRsiProfileRefresh(
            previous = old,
            refreshed = sanitizeCachedProfile(
                old.copy(
                    organizationId = null,
                    organizationName = null,
                    organizationRank = null,
                    organizationLevel = 0,
                    organizationImage = null,
                    rank = "—",
                    level = "—",
                ),
            ),
            citizenDossierAvailable = true,
        )

        assertEquals(null, refreshed.organizationId)
        assertEquals(null, refreshed.organizationName)
        assertEquals(null, refreshed.organizationRank)
        assertEquals(0, refreshed.organizationLevel)
        assertEquals(null, refreshed.organizationImage)
        assertEquals("—", refreshed.rank)
        assertEquals("—", refreshed.level)
        assertEquals("$100", refreshed.totalSpent)
    }

    @Test
    fun citizenDossierMapsOnlyPublicMainOrganizationFields() {
        val html = """
            <p class="entry citizen-record"><span class="label">UEE Citizen Record</span><strong>#4725716</strong></p>
            <div class="profile left-col">
              <div class="thumb"><img src="/media/avatar.jpg" /></div>
              <p class="entry"><span class="label">Handle name</span><strong>CurrentCitizen</strong></p>
            </div>
            <p class="entry"><span class="label">Enlisted</span><strong class="value">Nov 24, 2025</strong></p>
            <div class="main-org right-col visibility-V">
              <div class="thumb"><a href="/orgs/CURRENTORG"><img src="/media/current-org-logo.jpg" /></a></div>
              <p class="entry"><a href="/orgs/CURRENTORG">Current Test Organization</a></p>
              <p class="entry"><span class="label">Spectrum Identification (SID)</span><strong>CURRENTORG</strong></p>
              <p class="entry"><span class="label">Organization rank</span><strong>Experienced</strong></p>
              <div class="ranking"><span class="active"></span><span class="active"></span><span></span></div>
            </div>
        """.trimIndent()

        val result = parseRsiCitizenProfile(html)

        assertEquals(true, isRsiCitizenDossier(html))
        assertEquals("/media/avatar.jpg", result.avatarPath)
        assertEquals("Nov 24, 2025", result.enlisted)
        assertEquals("CURRENTORG", result.organizationId)
        assertEquals("Current Test Organization", result.organizationName)
        assertEquals("/media/current-org-logo.jpg", result.organizationImagePath)
        assertEquals("Experienced", result.organizationRank)
        assertEquals(2, result.organizationLevel)
    }

    @Test
    fun legacyRightColumnCitizenDossierMapsRealOrganizationStructure() {
        val html = """
            <section class="profile">
              <div class="left-col profile">
                <div class="thumb">
                  <img src="/media/legacy-avatar.jpg" />
                </div>
                <p class="entry">
                  <strong class="value data10">Legacy Citizen</strong>
                </p>
                <p class="entry">
                  <span class="label data6">Handle name</span>
                  <strong class="value data10">LegacyCitizen</strong>
                </p>
              </div>
              <div class="right-col visibility-V">
                <span class="title">Main organization</span>
                <div class="inner clearfix">
                  <div class="thumb">
                    <a href="/orgs/LEGACYORG"><img src="/media/legacy-org-logo.png" /></a>
                  </div>
                  <div class="info">
                    <p class="entry">
                      <a href="/orgs/LEGACYORG" class="value data10">Legacy Test Organization</a>
                    </p>
                    <p class="entry">
                      <span class="label data6">Spectrum Identification (SID)</span>
                      <strong class="value data2">LEGACYORG</strong>
                    </p>
                    <p class="entry">
                      <span class="label data6">Organization rank</span>
                      <strong class="value data10">Senior Officer</strong>
                    </p>
                    <div class="ranking data14">
                      <span class="rank-slot active"><span></span></span>
                      <span class="active"><span></span></span>
                      <span class="active"><span></span></span>
                      <span class="active"><span></span></span>
                      <span><span></span></span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="left-col">
                <p class="entry"><span class="label">Enlisted</span><strong class="value">Feb 03, 2021</strong></p>
              </div>
              <div class="right-col">
                <p class="entry website"><span class="label">Website</span><a href="/orgs/NOT_THE_MAIN_ORG">Bio link</a></p>
              </div>
            </section>
            <p class="entry citizen-record"><span class="label">UEE Citizen Record</span><strong>#1000</strong></p>
        """.trimIndent()

        val result = parseRsiCitizenProfile(html)

        assertEquals(true, isRsiCitizenDossier(html))
        assertEquals("/media/legacy-avatar.jpg", result.avatarPath)
        assertEquals("Feb 03, 2021", result.enlisted)
        assertEquals("LEGACYORG", result.organizationId)
        assertEquals("Legacy Test Organization", result.organizationName)
        assertEquals("/media/legacy-org-logo.png", result.organizationImagePath)
        assertEquals("Senior Officer", result.organizationRank)
        assertEquals(4, result.organizationLevel)
    }

    @Test
    fun citizenWithoutPublicMainOrganizationDoesNotInventFleetData() {
        val result = parseRsiCitizenProfile(
            """
                <div class="profile left-col"><div class="thumb"><img src="/media/avatar.jpg" /></div></div>
                <p><span class="label">UEE Citizen Record</span><strong>#1</strong></p>
                <p><span class="label">Handle name</span><strong>PrivateCitizen</strong></p>
                <p class="entry"><span class="label">Enlisted</span><strong>Jan 01, 2024</strong></p>
            """.trimIndent(),
        )

        assertEquals(null, result.organizationId)
        assertEquals(true, isRsiCitizenDossier(
            "UEE Citizen Record Handle name",
        ))
        assertEquals(null, result.organizationName)
        assertEquals(null, result.organizationRank)
        assertEquals(0, result.organizationLevel)
    }

    @Test
    fun legacyBiographyRightColumnIsNotMistakenForMainOrganization() {
        val result = parseRsiCitizenProfile(
            """
                <div class="left-col profile">
                  <div class="thumb"><img src="/media/private-avatar.jpg" /></div>
                  <p class="entry"><span class="label">Handle name</span><strong>PrivateCitizen</strong></p>
                </div>
                <div class="left-col">
                  <p class="entry"><span class="label">Enlisted</span><strong>Jan 01, 2024</strong></p>
                </div>
                <div class="right-col">
                  <p class="entry website"><a href="/orgs/COMMUNITY_DIRECTORY">Community directory</a></p>
                  <div class="entry bio">No public main organization</div>
                </div>
                <p><span class="label">UEE Citizen Record</span><strong>#1</strong></p>
            """.trimIndent(),
        )

        assertEquals("/media/private-avatar.jpg", result.avatarPath)
        assertEquals(null, result.organizationId)
        assertEquals(null, result.organizationName)
        assertEquals(null, result.organizationRank)
        assertEquals(0, result.organizationLevel)
    }

    @Test
    fun oldGamePackageAndShipCountPlaceholdersAreRemovedFromProfileCache() {
        val migrated = sanitizeCachedProfile(
            ProfileData(
                rank = "已拥有游戏包",
                organizationName = "舰队 2 艘",
                organizationRank = "已拥有游戏包",
                organizationImage = "https://example.invalid/not-an-org.png",
                level = "99",
            ),
        )

        assertEquals("—", migrated.rank)
        assertEquals(null, migrated.organizationName)
        assertEquals(null, migrated.organizationRank)
        assertEquals(null, migrated.organizationImage)
        assertEquals("—", migrated.level)
    }

    @Test
    fun organizationRankAndLevelRemainTheOnlyLegacyProfileAliases() {
        val migrated = sanitizeCachedProfile(
            ProfileData(
                rank = "RSI 账户",
                organizationName = "星环城",
                organizationRank = "Experienced",
                organizationLevel = 2,
                level = "—",
            ),
        )

        assertEquals("Experienced", migrated.rank)
        assertEquals("2", migrated.level)
    }

    @Test
    fun unappliedUpgradeNeverBecomesAnOwnedHeroShip() {
        val upgrade = HangarItem(
            title = "Aurora to M80 CCU",
            price = "$5",
            date = "2026年08月24日",
            imageRes = 0,
            isUpgrade = true,
            upgradeFrom = "Aurora",
            upgradeTo = "M80",
            resolvedFinalShip = "M80",
        )

        assertEquals(false, isHeroShipCandidate(upgrade))
        assertEquals(null, heroShipName(upgrade))
    }

    @Test
    fun packageWithTypedShipIsEligibleForHero() {
        val pack = HangarItem(
            title = "Citizen Starter Pack",
            price = "$140",
            date = "2026年08月24日",
            imageRes = 0,
            containedShip = "M80",
            resolvedFinalShip = "M80",
        )

        assertEquals(true, isHeroShipCandidate(pack))
        assertEquals("M80", heroShipName(pack))
    }

    @Test
    fun unresolvedUpgradeDoesNotBecomeHero() {
        val upgrade = HangarItem(
            title = "Unknown CCU",
            price = "$5",
            date = "2026年08月24日",
            imageRes = 0,
            isUpgrade = true,
        )

        assertEquals(false, isHeroShipCandidate(upgrade))
    }

    @Test
    fun resolvedShipNameWithoutTypedShipChildDoesNotBecomeHero() {
        val inventoryItem = HangarItem(
            title = "M80 Paint Pack",
            price = "$7.50",
            date = "2026年08月24日",
            imageRes = 0,
            resolvedFinalShip = "M80",
        )

        assertEquals(false, isHeroShipCandidate(inventoryItem))
        assertEquals(null, heroShipName(inventoryItem))
    }

    @Test
    fun shipValueUsesActualPurchasePrices() {
        assertEquals(
            22_000,
            calculateShipValue(
                seedPurchasePrice = 2_000,
                ownedCcuPurchasePrices = listOf(2_000, 5_000),
                remainingPayment = 13_000,
            ),
        )
    }

    @Test
    fun ccuRoutePlanUsesOnlyItsSelectedOwnedEdges() {
        val seed = CcuShip("seed", "Seed", 2_000, 0)
        val mid = CcuShip("mid", "Mid", 7_000, 0)
        val target = CcuShip("target", "Target", 20_000, 0)
        val selected = OwnedCcu("selected", "Seed to Mid", 2_000, "Mid", "Seed", "Mid")
        val unrelated = OwnedCcu("other", "Other to Target", 1_000, "Target", "Other", "Target")
        val plan = planCcuRoute(seed, target, listOf(seed, mid, target), listOf(selected, unrelated))!!

        assertEquals(listOf("selected"), plan.selectedOwned.map { it.id })
        assertEquals(13_000, plan.remainingPayment)
        assertEquals(17_000, plan.shipValue)
    }

    @Test
    fun unrelatedOwnedCcuDoesNotReduceRemainingPayment() {
        val seed = CcuShip("seed", "Seed", 2_000, 0)
        val target = CcuShip("target", "Target", 20_000, 0)
        val owned = listOf(OwnedCcu("ccu", "Other to Target", 5_000, "Target", "Other", "Target"))

        assertEquals(18_000, calculateRemainingPayment(seed, target, listOf(seed, target), owned))
    }

    @Test
    fun connectedOwnedCcuChainMinimizesRemainingSpend() {
        val seed = CcuShip("seed", "Seed", 2_000, 0)
        val midA = CcuShip("mid-a", "Mid A", 7_000, 0)
        val midB = CcuShip("mid-b", "Mid B", 15_000, 0)
        val target = CcuShip("target", "Target", 20_000, 0)
        val owned = listOf(
            OwnedCcu("a", "Mid A to Mid B", 3_000, "Mid B", "Mid A", "Mid B"),
            OwnedCcu("b", "Mid B to Target", 2_000, "Target", "Mid B", "Target"),
            OwnedCcu("unused", "Seed to Mid A", 9_000, "Mid A", "Wrong Seed", "Mid A"),
        )

        val plan = planCcuRoute(seed, target, listOf(seed, midA, midB, target), owned)!!

        assertEquals(5_000, plan.remainingPayment)
        assertEquals(listOf("a", "b"), plan.selectedOwned.map { it.id })
        assertEquals(listOf("seed>mid-a", "mid-a>mid-b", "mid-b>target"), plan.steps.map { "${it.from.id}>${it.to.id}" })
    }

    @Test
    fun plannerRejectsOwnedEdgesThatDoNotIncreaseMsrp() {
        val seed = CcuShip("seed", "Seed", 2_000, 0)
        val target = CcuShip("target", "Target", 20_000, 0)
        val reverse = OwnedCcu("reverse", "Target to Seed", 1_000, "Seed", "Target", "Seed")

        val plan = planCcuRoute(seed, target, listOf(seed, target), listOf(reverse))!!

        assertEquals(18_000, plan.remainingPayment)
        assertEquals(emptyList<OwnedCcu>(), plan.selectedOwned)
    }

    @Test
    fun targetSelectorOnlyOffersShipsWithHigherPurchasePrice() {
        val seed = CcuShip("seed", "Seed", 20_000, 0)
        val ships = listOf(
            seed,
            CcuShip("lower", "Lower", 10_000, 0),
            CcuShip("higher", "Higher", 30_000, 0),
        )

        assertEquals(listOf("higher"), eligibleTargetShips(seed, ships).map { it.id })
    }

    @Test
    fun localCartAggregatesAndRemovesLinesWithoutCheckout() {
        val cart = InMemoryCartRepository()
        val product = StoreProduct(
            id = "sku",
            title = "Test ship",
            category = StoreCategory.SHIPS,
            metadata = "Test",
            priceCents = 1_500,
            imageUrl = "",
            description = "",
        )

        cart.add(product)
        cart.add(product)
        assertEquals(2, cart.lines().single().quantity)
        cart.remove(product.id)
        assertEquals(1, cart.lines().single().quantity)
        cart.clear()
        assertEquals(emptyList<CartLine>(), cart.lines())
    }

    @Test
    fun productionTerminalWithoutASourceDoesNotFabricateRows() = kotlinx.coroutines.runBlocking {
        assertEquals(emptyList<TerminalItem>(), ProductionTerminalRepository().items())
    }

    @Test
    fun terminalSummaryUsesLegacyHighestPurchasePrice() {
        assertEquals(300.0, selectTerminalPurchasePrice(listOf(100.0, 300.0, 200.0)))
        assertEquals(null, selectTerminalPurchasePrice(emptyList()))
    }

    @Test
    fun terminalLoadoutCalculatesOnlyMetricsPresentInTheSnapshot() {
        val ship = TerminalItem("ship", "Test ship", "RSI", TerminalCategory.VEHICLES, emptyList(), "—", "—", "", details = listOf("Mass" to "12,500 kg", "Cargo" to "64 SCU"))
        val power = TerminalItem("power", "Power plant", "A", TerminalCategory.POWER_PLANTS, emptyList(), "—", "—", "", details = listOf("Power" to "2,000"))
        val stats = calculateTerminalLoadoutStats(ship, listOf(power))
        assertEquals(12_500.0, stats.massKg!!, 0.01)
        assertEquals(2_000.0, stats.power!!, 0.01)
        assertEquals(64.0, stats.cargo!!, 0.01)
        assertEquals(null, stats.shield)
    }

    @Test
    fun ccuPlanReportsMaximumSavingsAgainstTheDirectUpgrade() {
        val seed = CcuShip("seed", "Seed", 2_000, 0)
        val middle = CcuShip("middle", "Middle", 7_000, 0)
        val target = CcuShip("target", "Target", 20_000, 0)
        val owned = OwnedCcu("owned", "Middle → Target", 3_000, "Target", "Middle", "Target")
        val plan = planCcuRoute(seed, target, listOf(seed, middle, target), listOf(owned))!!
        assertEquals(18_000, plan.standardPayment)
        assertEquals(5_000, plan.remainingPayment)
        assertEquals(13_000, plan.maximumSavings)
    }

    @Test
    fun utilityDetailsComeFromTheProductionRepositoryBoundary() = kotlinx.coroutines.runBlocking {
        val detail = ProductionUtilityRepository().detail("crowdfunding")
        assertEquals("等待在线数据", detail.rows.first { it.first == "状态" }.second)
        assertEquals(null, detail.externalUrl)
    }

    @Test
    fun externalUtilityEntriesRetainTheirSafeNavigationContract() = kotlinx.coroutines.runBlocking {
        val detail = ProductionUtilityRepository().detail("web-hangar")
        assertEquals("https://robertsspaceindustries.com/account/pledges", detail.externalUrl)
        assertEquals("不会执行", detail.rows.first { it.first == "账户变更" }.second)
    }

    @Test
    fun ownedCcuChainIsAvailableWithoutRemotePlanning() = kotlinx.coroutines.runBlocking {
        val owned = OwnedCcu("owned", "Aurora → M80", 5_000, "M80")
        val chain = ProductionCcuRepository().chain(owned)
        assertEquals("Aurora", chain.single().from)
        assertEquals("M80", chain.single().to)
    }

    @Test
    fun userStatusIsOneSharedPresenceSource() {
        assertEquals(null, ProfileData().isOnline)
        val source = UserStatusSource(initialPresence = UserPresence.ONLINE)
        source.set(UserPresence.INVISIBLE)
        assertEquals(UserPresence.INVISIBLE, source.presence)
        source.set(UserPresence.PLAYING)
        assertEquals(UserPresence.PLAYING, source.presence)
    }

    @Test
    fun userPresenceUsesSpectrumWireValues() {
        assertEquals("do_not_disturb", UserPresence.DO_NOT_DISTURB.remoteValue)
        assertEquals(UserPresence.PLAYING, UserPresence.fromRemote("in_game"))
        assertEquals(UserPresence.INVISIBLE, UserPresence.fromRemote("offline"))
    }

    @Test
    fun destructiveActionsAreAlwaysIntercepted() {
        val result = SafeNoOpDestructiveActionExecutor().execute(DestructiveAction.RSI_PURCHASE)
        assertEquals(false, result.executed)
        assertEquals(DestructiveAction.RSI_PURCHASE, result.action)
    }
}
