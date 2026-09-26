package com.refuge.next

import android.app.Activity
import android.animation.ValueAnimator
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.refuge.next.data.ProductionHangarRepository
import com.refuge.next.data.ProductionHangarLogRepository
import com.refuge.next.data.ProductionTerminalRepository
import com.refuge.next.data.ProductionCatalogStoreRepository
import com.refuge.next.data.ProductionBuybackRepository
import com.refuge.next.data.RsiLiveHangarLogRepository
import com.refuge.next.data.InMemoryCartRepository
import com.refuge.next.data.ProductionProfileRepository
import com.refuge.next.data.ProductionUtilityRepository
import com.refuge.next.data.ProductionCcuRepository
import com.refuge.next.data.PreferencesSettingsRepository
import com.refuge.next.data.SettingsRepository
import com.refuge.next.data.UserStatusSource
import com.refuge.next.data.UserPresence
import com.refuge.next.data.ProfileData
import com.refuge.next.data.ProductionCacheDataSource
import com.refuge.next.data.RsiAuthDataSource
import com.refuge.next.data.RsiLiveHangarRepository
import com.refuge.next.data.RsiLiveProfileRepository
import com.refuge.next.data.RsiLiveBuybackRepository
import com.refuge.next.data.WikiTerminalRepository
import com.refuge.next.data.RsiLiveStoreRepository
import com.refuge.next.data.RsiLiveUtilityRepository
import com.refuge.next.data.RsiLiveCcuRepository
import com.refuge.next.data.RsiLiveCcuPurchaseRepository
import com.refuge.next.data.CcuPurchaseRepository
import com.refuge.next.data.ProductionTranslationRepository
import com.refuge.next.design.RefugeColors
import com.refuge.next.design.LocalRefugeTranslationEnabled
import com.refuge.next.material.RefugeScene
import com.refuge.next.material.LocalRootNavigationOverlay
import com.refuge.next.material.RootNavigationOverlay
import com.refuge.next.material.LocalOpticalGlassEnabled
import com.refuge.next.material.LocalRemoteArtworkEnabled
import com.refuge.next.motion.RefugeRouteTransition
import com.refuge.next.navigation.LocalRootScrollRegistry
import com.refuge.next.navigation.RootScrollRegistry
import com.refuge.next.screens.DesignLabScreen
import com.refuge.next.screens.HangarScreen
import com.refuge.next.screens.StoreScreen
import com.refuge.next.screens.TerminalScreen
import com.refuge.next.screens.ProfileScreen
import com.refuge.next.screens.ToolsScreen
import com.refuge.next.screens.SettingsScreen
import com.refuge.next.screens.CcuScreen
import com.refuge.next.screens.RsiLoginScreen
import com.refuge.next.screens.PresencePickerSheet
import com.refuge.next.screens.RootBottomNav
import com.refuge.next.screens.StoreUpgradePurchaseScreen
import com.refuge.next.screens.HangarOwnedCcuApplyScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The root navigation mirrors the legacy five-item bar. Tools remain a full
// root destination so the selected lens and back behavior match the old app.
// The legacy root navigation has four destinations. Tools remains an
// internal route, but it is not a root tab and secondary detail routes do not
// inherit the root bar.
private val productionRootRoutes = setOf(0, 1, 2, 4)

private fun productionRouteOrder(route: Int): Int = when (route) {
    0 -> 0
    1 -> 1
    2 -> 2
    3 -> 3
    4 -> 4
    else -> 4 + route
}

@Composable
fun RefugeApp() {
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Commit a minimal frame before constructing the repository graph. The
        // Android splash screen can then exit independently of route startup.
        withFrameNanos { }
        mounted = true
    }
    if (!mounted) {
        Box(Modifier.fillMaxSize().background(Color.Black))
    } else {
        RefugeAppContent()
    }
}

@Composable
private fun RefugeAppContent() {
    val context = LocalContext.current
    val settingsRepository = remember(context) { PreferencesSettingsRepository(context) }
    var settings by remember(settingsRepository) { mutableStateOf(settingsRepository.load()) }
    val userStatus = remember(context) { UserStatusSource(context) }
    val auth = remember(context) { RsiAuthDataSource(context) }
    var authenticated by remember(auth) { mutableStateOf(auth.session()?.isAuthenticated == true) }
    var startupReady by remember(auth) { mutableStateOf(auth.session()?.isAuthenticated != true) }
    val isDark = settings.darkTheme
    val presence = userStatus.presence
    val isOnline = presence != UserPresence.INVISIBLE
    var showPresencePicker by remember { mutableStateOf(false) }
    // Page sheets are rendered inside the route content. The root navigation
    // keeps its layout position, but yields its paint layer while a sheet is
    // open so it cannot appear above the sheet body.
    var rootNavigationVisible by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var rootTab by remember { mutableIntStateOf(0) }
    var showStoreUpgrade by remember { mutableStateOf(false) }
    var selectedOwnedCcuId by remember { mutableStateOf<Long?>(null) }
    var lastNavTapRoute by remember { mutableIntStateOf(-1) }
    var lastNavTapAt by remember { mutableLongStateOf(0L) }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }
    val cacheSource = remember(context) { ProductionCacheDataSource(context) }
    val translationRepository = remember(context) { ProductionTranslationRepository(context) }
    val cacheManifest = remember(cacheSource) { cacheSource.manifest() }
    val fallbackRepository = remember(cacheSource) {
        ProductionHangarRepository(
            fallbackImage = R.drawable.ship_placeholder,
            m80Image = R.drawable.m80_hero,
            source = cacheSource,
        )
    }
    val liveRepository = remember(auth, fallbackRepository) {
        RsiLiveHangarRepository(context, auth, fallbackRepository, R.drawable.ship_placeholder, R.drawable.m80_hero, translationRepository)
    }
    val repository: com.refuge.next.data.HangarRepository = liveRepository
    val fallbackStoreRepository = remember(cacheSource) { ProductionCatalogStoreRepository(cacheSource) }
    val liveStoreRepository = remember(auth, fallbackStoreRepository, translationRepository) { RsiLiveStoreRepository(context, auth, fallbackStoreRepository, translationRepository) }
    val storeRepository: com.refuge.next.data.StoreRepository = liveStoreRepository
    val fallbackTerminalRepository = remember(cacheSource) {
        ProductionTerminalRepository(source = cacheSource)
    }
    val liveTerminalRepository = remember(translationRepository, fallbackTerminalRepository) {
        WikiTerminalRepository(context, translationRepository, fallbackTerminalRepository)
    }
    val terminalRepository: com.refuge.next.data.TerminalRepository = liveTerminalRepository
    val fallbackBuybackRepository = remember(cacheSource) { ProductionBuybackRepository(R.drawable.m80_hero, R.drawable.ship_placeholder, cacheSource) }
    val liveBuybackRepository = remember(context, auth, fallbackBuybackRepository, translationRepository) {
        RsiLiveBuybackRepository(context, auth, fallbackBuybackRepository, R.drawable.ship_placeholder, translationRepository)
    }
    val buybackRepository: com.refuge.next.data.BuybackRepository = liveBuybackRepository
    val fallbackHangarLogRepository = remember(cacheSource) {
        ProductionHangarLogRepository(cacheSource)
    }
    val hangarLogRepository = remember(context, auth, translationRepository, fallbackHangarLogRepository) {
        RsiLiveHangarLogRepository(
            context,
            auth,
            translation = translationRepository,
            fallback = fallbackHangarLogRepository,
        )
    }
    val cartRepository = remember { InMemoryCartRepository() }
    val fallbackProfileRepository = remember(cacheSource) { ProductionProfileRepository(cacheSource) }
    val liveProfileRepository = remember(auth, fallbackProfileRepository, liveRepository) { RsiLiveProfileRepository(context, auth, fallbackProfileRepository, liveRepository) }
    val profileRepository: com.refuge.next.data.ProfileRepository = liveProfileRepository
    var sharedProfile by remember(profileRepository) { mutableStateOf(profileRepository.cachedProfile()) }
    LaunchedEffect(authenticated, profileRepository) {
        if (authenticated) {
            runCatching { profileRepository.awaitCachedProfile() }.onSuccess { cached ->
                if (cached.isAuthenticated) sharedProfile = cached
            }
            // Let the first cached frame reach the compositor before the
            // authenticated profile refresh starts competing for CPU/IO.
            withFrameNanos { }
            delay(900)
            withContext(Dispatchers.IO) {
                runCatching { profileRepository.profile() }
            }.onSuccess { sharedProfile = it }
        }
    }
    val fallbackUtilityRepository = remember(cacheSource) { ProductionUtilityRepository(cacheSource) }
    val liveUtilityRepository = remember(auth, fallbackUtilityRepository) { RsiLiveUtilityRepository(auth, fallbackUtilityRepository) }
    val utilityRepository: com.refuge.next.data.UtilityRepository = liveUtilityRepository
    val fallbackCcuRepository = remember(cacheSource) { ProductionCcuRepository(cacheSource, R.drawable.m80_hero, R.drawable.ship_placeholder) }
    val liveCcuRepository = remember(auth, liveRepository, fallbackCcuRepository) {
        RsiLiveCcuRepository(context, auth, liveRepository, fallbackCcuRepository, R.drawable.ship_placeholder)
    }
    val ccuRepository: com.refuge.next.data.CcuRepository = liveCcuRepository
    // The purchase selector is an online read-only catalogue. It owns a local
    // first frame (including the planner's ship MSRP snapshot) and refreshes
    // independently so opening the sheet never produces an empty first frame.
    val ccuPurchaseRepository: CcuPurchaseRepository = remember(context, auth, liveCcuRepository) {
        RsiLiveCcuPurchaseRepository(context, auth, liveCcuRepository)
    }

    LaunchedEffect(Unit) {
        // Parse the bundled projections before any route is opened. Screens
        // can therefore render a real local frame immediately when RSI is
        // unavailable, while their repositories refresh in the background.
        cacheSource.warmCachesInBackground(R.drawable.m80_hero, R.drawable.ship_placeholder)
        withFrameNanos { }
        delay(350)
        withContext(Dispatchers.IO) { runCatching { com.refuge.next.data.ShipReferenceCatalog.shared(context).prepare() } }
        val loadouts = com.refuge.next.data.ErkulLoadoutRepository.shared(context)
        delay(500)
        withContext(Dispatchers.IO) { runCatching { loadouts.preload("LIVE") } }
        delay(500)
        // Keep PTU and the full terminal index in the startup queue so they do
        // not compete with the first visible page's image and glass shaders.
        withContext(Dispatchers.IO) { runCatching { loadouts.preload("PTU") } }
        delay(900)
        withContext(Dispatchers.IO) { runCatching { terminalRepository.items() } }
    }
    LaunchedEffect(authenticated) {
        if (authenticated && !startupReady) {
            // Compose a minimal authenticated frame first. The full Hangar
            // route contains the largest text/image tree and can then enter
            // after the window has reported its first draw.
            withFrameNanos { }
            delay(900)
            startupReady = true
        }
    }
    LaunchedEffect(authenticated) {
        if (authenticated) {
            withFrameNanos { }
            // Let the cached route and its first visible artwork settle before
            // authenticated refreshes begin competing for CPU and disk IO.
            delay(3200)
            // Keep the persisted avatar choice aligned with Spectrum after a
            // process restart. A failed request remains queued in
            // UserStatusSource and is retried on the next authenticated start.
            launch { runCatching { userStatus.syncToRsi(auth) } }
            withContext(Dispatchers.IO) { runCatching { repository.inventory() } }
            delay(400)
            withContext(Dispatchers.IO) { runCatching { storeRepository.products() } }
            delay(400)
            withContext(Dispatchers.IO) { runCatching { buybackRepository.items() } }
            delay(400)
            withContext(Dispatchers.IO) { runCatching { hangarLogRepository.entries() } }
            delay(400)
            withContext(Dispatchers.IO) { runCatching { utilityRepository.groups() } }
        }
    }

    // Upgrade data participates in the same background refresh lifecycle as
    // Hangar/Store. Its cached snapshot is immediately available, while this
    // replaces it with the latest catalog and owned CCUs after authentication.
    LaunchedEffect(authenticated, ccuRepository) {
        if (authenticated) {
            // The catalogue is background-only at startup. Waiting for one
            // frame keeps shader/image compilation from sharing the first
            // traversal with the upgrade refresh.
            withFrameNanos { }
            delay(2200)
            withContext(Dispatchers.IO) { runCatching { ccuRepository.ships() } }
            withContext(Dispatchers.IO) { runCatching { ccuRepository.owned() } }
            delay(500)
            // Prime the purchase sheet while the user is browsing the root
            // pages; opening it renders the last snapshot first and then
            // replaces it with the latest read-only catalogue.
            withContext(Dispatchers.IO) { runCatching { ccuPurchaseRepository.catalog() } }
        }
    }

    // Secondary production routes share the root tab bar, but system Back must return
    // to the originating root screen instead of finishing the activity.
    BackHandler(enabled = selectedTab !in productionRootRoutes) {
        selectedTab = rootTab
    }

    val themeReveal = remember { Animatable(0f) }
    val appScope = rememberCoroutineScope()
    var themeTransitionJob by remember { mutableStateOf<Job?>(null) }
    var themeVeilColor by remember { mutableStateOf(if (isDark) Color.Black else Color.White) }
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
    val requestThemeToggle: () -> Unit = {
        val targetDark = !settings.darkTheme
        themeTransitionJob?.cancel()
        themeTransitionJob = appScope.launch {
            themeVeilColor = if (targetDark) Color.Black else Color.White
            if (animationsEnabled) {
                themeReveal.animateTo(
                    1f,
                    tween(durationMillis = 90, easing = FastOutLinearInEasing),
                )
            }
            val updated = settings.copy(darkTheme = targetDark)
            settings = updated
            settingsRepository.save(updated)
            if (animationsEnabled) {
                themeReveal.animateTo(
                    0f,
                    tween(durationMillis = 190, easing = LinearOutSlowInEasing),
                )
            } else {
                themeReveal.snapTo(0f)
            }
        }
    }
    val palette = if (isDark) RefugeColors.dark else RefugeColors.light
    val routeStateHolder = rememberSaveableStateHolder()
    val rootScrollRegistry = remember { RootScrollRegistry() }
    // The optical primitives include a safe fallback, so the first interactive
    // frame should already use the same glass material as later frames.
    var opticalGlassReady by remember { mutableStateOf(true) }
    var remoteArtworkReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Keep remote artwork staged after the cached frame. Do not delay the
        // material pass: controls must not switch from flat to glass in place.
        withFrameNanos { }
        remoteArtworkReady = true
    }
    CompositionLocalProvider(
        LocalOpticalGlassEnabled provides opticalGlassReady,
        LocalRemoteArtworkEnabled provides remoteArtworkReady,
        LocalRootScrollRegistry provides rootScrollRegistry,
        com.refuge.next.design.LocalRefugeTranslation provides translationRepository,
        LocalRefugeTranslationEnabled provides settings.translationEnabled,
    ) {
      RefugeScene(
        palette = palette,
      ) { backdrop ->
            if (!authenticated) {
                RsiLoginScreen(
                    backdrop = backdrop,
                    palette = palette,
                    auth = auth,
                    loginEmailDraft = auth.loginEmailDraft(),
                    onLoginEmailChanged = auth::saveLoginEmailDraft,
                    allowClose = false,
                    onAuthenticated = { authenticated = true },
                    onClose = {},
                )
            } else if (!startupReady) {
                Box(
                    Modifier.fillMaxSize().systemBarsPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("我的机库", color = palette.text)
                }
            } else {
                    val navigate: (Int) -> Unit = { route ->
                    rootNavigationVisible = true
                    if (route == 9) {
                        // Keep the store route mounted so its category, search,
                        // and scroll state remain intact beneath the purchase sheet.
                        showStoreUpgrade = true
                    } else if (route == selectedTab) {
                        // A stationary tap selects the current tab; the
                        // legacy interaction is a deliberate double tap. The
                        // second tap returns a scrolled page to its top, and a
                        // second tap while already at the top performs the
                        // page refresh. This keeps the bottom bar from
                        // refreshing while the user is merely changing tabs.
                        val now = SystemClock.uptimeMillis()
                        val isDoubleTap = route == lastNavTapRoute && now - lastNavTapAt <= 360L
                        lastNavTapRoute = route
                        lastNavTapAt = if (isDoubleTap) 0L else now
                        if (isDoubleTap) {
                            appScope.launch {
                                if (rootScrollRegistry.isAtTop(route)) {
                                    rootScrollRegistry.triggerRefresh(route)
                                } else {
                                    rootScrollRegistry.scrollToTop(route)
                                }
                            }
                        }
                    } else {
                        lastNavTapRoute = -1
                        lastNavTapAt = 0L
                        if (route in productionRootRoutes) rootTab = route
                        selectedTab = route
                    }
                }
                CompositionLocalProvider(
                    LocalRootNavigationOverlay provides RootNavigationOverlay(selectedTab to rootNavigationVisible) { pageBackdrop ->
                        if (selectedTab in productionRootRoutes) {
                            RootBottomNav(
                                pageBackdrop,
                                isDark,
                                if (selectedTab in productionRootRoutes) selectedTab else rootTab,
                                navigate,
                            )
                        }
                    },
                ) {
                    // Each root screen owns its page capture. Its navigation
                    // overlay is drawn above that capture, while sheets that
                    // follow the page scope naturally cover both layers.
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                      val route = selectedTab
                      RefugeRouteTransition(
                          targetState = route,
                          order = ::productionRouteOrder,
                          // Root destinations slide as pages. Secondary
                          // routes and detail overlays are presented in place
                          // so the shared bottom navigation never moves with
                          // the page transition.
                          animate = { from, to ->
                              from in productionRootRoutes && to in productionRootRoutes
                          },
                          modifier = Modifier.fillMaxSize(),
                      ) { animatedRoute ->
                        routeStateHolder.SaveableStateProvider(animatedRoute) {
                          RefugeContent(
                                selectedTab = animatedRoute,
                                onNavigate = navigate,
                                onOpenDesignLab = { /* Hangar overflow is intentionally handled in-page. */ },
                                backdrop = backdrop,
                                palette = palette,
                                repository = repository,
                                buybackRepository = buybackRepository,
                                hangarLogRepository = hangarLogRepository,
                                storeRepository = storeRepository,
                                cartRepository = cartRepository,
                                profileRepository = profileRepository,
                                utilityRepository = utilityRepository,
                                ccuRepository = ccuRepository,
                                ccuPurchaseRepository = ccuPurchaseRepository,
                                terminalRepository = terminalRepository,
                                translationRepository = translationRepository,
                                isDark = isDark,
                                translationEnabled = settings.translationEnabled,
                                onToggleTranslation = {
                                    val updated = settings.copy(translationEnabled = !settings.translationEnabled)
                                    settings = updated
                                    settingsRepository.save(updated)
                                },
                                onToggleTheme = requestThemeToggle,
                                isOnline = isOnline,
                                presence = presence,
                                avatarUrl = sharedProfile.avatarUrl,
                                initialProfile = sharedProfile,
                                selectedOwnedCcuId = selectedOwnedCcuId,
                                onSelectOwnedCcu = { id -> selectedOwnedCcuId = id },
                                onToggleOnline = { showPresencePicker = true },
                                auth = auth,
                                onAuthChanged = { authenticated = true },
                                rootTab = rootTab,
                                settingsRepository = settingsRepository,
                                cacheManifest = cacheManifest,
                                // Keep the root bar mounted in its original layout,
                                // but let an in-flow sheet cover its paint layer.
                                // Leaving this callback empty puts the bar above
                                // the opaque selector sheet because the root
                                // overlay is rendered after page content.
                                // Sheets cover the bar through composition
                                // order. Keep the navigation mounted so it
                                // never disappears and reappears during a
                                // modal transition.
                                onTerminalOverlayVisibilityChanged = { },
                                onOpenStoreUpgrade = { showStoreUpgrade = true },
                          )
                        }
                      }
                      if (showStoreUpgrade && rootTab == 1) {
                          StoreUpgradePurchaseScreen(
                              backdrop = backdrop,
                              palette = palette,
                              isDark = isDark,
                              rootTab = rootTab,
                              onNavigate = { showStoreUpgrade = false },
                              onClose = { showStoreUpgrade = false },
                              auth = auth,
                              purchaseRepository = ccuPurchaseRepository,
                              hangarRepository = repository,
                              translationRepository = translationRepository,
                              presence = presence,
                              avatarUrl = sharedProfile.avatarUrl,
                              onAvatarClick = { showPresencePicker = true },
                          )
                      }
                    }
                    if (showPresencePicker) {
                        PresencePickerSheet(
                            backdrop = backdrop,
                            palette = palette,
                            selected = presence,
                            onSelected = {
                                userStatus.set(it)
                                // Keep the selected row visible for the short
                                // Cupertino dismissal window before removing
                                // the sheet from the composition.
                                appScope.launch {
                                    // Let the sheet finish its 150 ms Cupertino
                                    // exit before removing it from composition.
                                    delay(280)
                                    showPresencePicker = false
                                    runCatching { userStatus.syncToRsi(auth) }
                                }
                            },
                            onDismiss = { showPresencePicker = false },
                        )
                    }
                }
            }
            if (themeReveal.value > 0f) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(
                        themeVeilColor.copy(alpha = themeReveal.value * .34f),
                    )
                }
            }
      }
    }
}
@Composable
private fun RefugeContent(
    selectedTab: Int,
    onNavigate: (Int) -> Unit,
    onOpenDesignLab: () -> Unit,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    palette: com.refuge.next.design.RefugePalette,
    repository: com.refuge.next.data.HangarRepository,
    buybackRepository: com.refuge.next.data.BuybackRepository,
    hangarLogRepository: com.refuge.next.data.HangarLogRepository,
    storeRepository: com.refuge.next.data.StoreRepository,
    cartRepository: com.refuge.next.data.CartRepository,
    profileRepository: com.refuge.next.data.ProfileRepository,
    utilityRepository: com.refuge.next.data.UtilityRepository,
    ccuRepository: com.refuge.next.data.CcuRepository,
    ccuPurchaseRepository: CcuPurchaseRepository,
    terminalRepository: com.refuge.next.data.TerminalRepository,
    translationRepository: com.refuge.next.data.TranslationRepository,
    auth: RsiAuthDataSource,
    onAuthChanged: () -> Unit,
    isDark: Boolean,
    translationEnabled: Boolean,
    onToggleTranslation: () -> Unit,
    onToggleTheme: () -> Unit,
    isOnline: Boolean,
    presence: UserPresence,
    avatarUrl: String?,
    initialProfile: ProfileData,
    selectedOwnedCcuId: Long?,
    onSelectOwnedCcu: (Long) -> Unit,
    onToggleOnline: () -> Unit,
    rootTab: Int,
    settingsRepository: SettingsRepository,
    cacheManifest: com.refuge.next.data.ProductionCacheManifest,
    onTerminalOverlayVisibilityChanged: (Boolean) -> Unit,
    onOpenStoreUpgrade: () -> Unit,
) {
      when (selectedTab) {
        0 -> HangarScreen(
            backdrop = backdrop,
            palette = palette,
            repository = repository,
            buybackRepository = buybackRepository,
            hangarLogRepository = hangarLogRepository,
            ccuRepository = ccuRepository,
            isDark = isDark,
            selectedBottomTab = selectedTab,
            onNavigate = onNavigate,
            onOpenDesignLab = onOpenDesignLab,
            onOpenOwnedCcu = { id -> onSelectOwnedCcu(id); onNavigate(10) },
            isOnline = isOnline,
            presence = presence,
            avatarUrl = avatarUrl,
            onToggleOnline = onToggleOnline,
            onOverlayVisibilityChanged = onTerminalOverlayVisibilityChanged,
        )

        1 -> StoreScreen(
            backdrop = backdrop,
            palette = palette,
            repository = storeRepository,
            cartRepository = cartRepository,
            ccuPurchaseRepository = ccuPurchaseRepository,
            isDark = isDark,
            selectedBottomTab = selectedTab,
            onNavigate = onNavigate,
            onOpenCcu = onOpenStoreUpgrade,
            isOnline = isOnline,
            presence = presence,
            avatarUrl = avatarUrl,
            onToggleOnline = onToggleOnline,
            onOverlayVisibilityChanged = onTerminalOverlayVisibilityChanged,
        )

        2 -> TerminalScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            selectedBottomTab = selectedTab,
            onNavigate = onNavigate,
            repository = terminalRepository,
            isOnline = isOnline,
            presence = presence,
            avatarUrl = avatarUrl,
            onToggleOnline = onToggleOnline,
            onOverlayVisibilityChanged = onTerminalOverlayVisibilityChanged,
        )

        3 -> ToolsScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            selectedBottomTab = rootTab,
            onNavigate = onNavigate,
            utilityRepository = utilityRepository,
            isOnline = isOnline,
            presence = presence,
            avatarUrl = avatarUrl,
            onToggleOnline = onToggleOnline,
        )

        4 -> ProfileScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            selectedBottomTab = selectedTab,
            onNavigate = onNavigate,
            profileRepository = profileRepository,
            utilityRepository = utilityRepository,
            onToggleTheme = onToggleTheme,
            onOpenLogin = { onNavigate(8) },
            isOnline = isOnline,
            presence = presence,
            initialProfile = initialProfile,
            fleetSummary = "舰队资料",
            onToggleOnline = onToggleOnline,
            onOverlayVisibilityChanged = onTerminalOverlayVisibilityChanged,
        )

        5 -> SettingsScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            translationEnabled = translationEnabled,
            onToggleTranslation = onToggleTranslation,
            selectedBottomTab = rootTab,
            onNavigate = onNavigate,
            onToggleTheme = onToggleTheme,
            onClearCache = { settingsRepository.clearLocalCache() },
            cacheInfo = cacheManifest,
            isOnline = isOnline,
            presence = presence,
            avatarUrl = avatarUrl,
            onToggleOnline = onToggleOnline,
        )

        6 -> CcuScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            selectedBottomTab = selectedTab,
            onNavigate = onNavigate,
            rootTab = rootTab,
            ccuRepository = ccuRepository,
            ownedShips = repository.cachedOwnedShips(),
            isOnline = isOnline,
            presence = presence,
            avatarUrl = avatarUrl,
            onToggleOnline = onToggleOnline,
            onOverlayVisibilityChanged = onTerminalOverlayVisibilityChanged,
        )

        7 -> DesignLabScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            onToggleTheme = onToggleTheme,
        )

        8 -> RsiLoginScreen(
            backdrop = backdrop,
            palette = palette,
            auth = auth,
            onAuthenticated = { onAuthChanged(); onNavigate(rootTab) },
            onClose = { onNavigate(rootTab) },
        )

        9 -> androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            // Keep the originating store rendered below the purchase sheet.
            // The sheet owns the interaction layer, while the store remains
            // visible through the HIG scrim during the complete transition.
            StoreScreen(
                backdrop = backdrop,
                palette = palette,
                repository = storeRepository,
                cartRepository = cartRepository,
                ccuPurchaseRepository = ccuPurchaseRepository,
                isDark = isDark,
                selectedBottomTab = rootTab,
                onNavigate = onNavigate,
                onOpenCcu = {},
                isOnline = isOnline,
                presence = presence,
                avatarUrl = avatarUrl,
                onToggleOnline = onToggleOnline,
            )
            StoreUpgradePurchaseScreen(
                backdrop = backdrop,
                palette = palette,
                isDark = isDark,
                rootTab = rootTab,
                onNavigate = onNavigate,
                auth = auth,
                purchaseRepository = ccuPurchaseRepository,
                hangarRepository = repository,
                translationRepository = translationRepository,
                presence = presence,
                avatarUrl = avatarUrl,
                onAvatarClick = onToggleOnline,
            )
        }

        10 -> HangarOwnedCcuApplyScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            rootTab = rootTab,
            onNavigate = onNavigate,
            repository = repository,
            ownedCcuId = selectedOwnedCcuId ?: 0L,
            presence = presence,
            avatarUrl = avatarUrl,
            onAvatarClick = onToggleOnline,
        )

        else -> DesignLabScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            onToggleTheme = onToggleTheme,
        )
      }
}
