package com.refuge.next

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.refuge.next.data.ProductionHangarRepository
import com.refuge.next.data.ProductionCatalogStoreRepository
import com.refuge.next.data.ProductionTerminalRepository
import com.refuge.next.data.ProductionBuybackRepository
import com.refuge.next.data.ProductionHangarLogRepository
import com.refuge.next.data.InMemoryCartRepository
import com.refuge.next.data.ProductionProfileRepository
import com.refuge.next.data.ProductionUtilityRepository
import com.refuge.next.data.ProductionCcuRepository
import com.refuge.next.data.AppSettings
import com.refuge.next.data.PreferencesSettingsRepository
import com.refuge.next.data.SettingsRepository
import com.refuge.next.data.UserStatusSource
import com.refuge.next.data.ProductionCacheDataSource
import com.refuge.next.data.RsiAuthDataSource
import com.refuge.next.data.RsiLiveHangarRepository
import com.refuge.next.data.RsiLiveProfileRepository
import com.refuge.next.data.RsiLiveBuybackRepository
import com.refuge.next.design.RefugeColors
import com.refuge.next.material.RefugeScene
import com.refuge.next.motion.RefugeRouteTransition
import com.refuge.next.screens.DesignLabScreen
import com.refuge.next.screens.HangarScreen
import com.refuge.next.screens.StoreScreen
import com.refuge.next.screens.TerminalScreen
import com.refuge.next.screens.ProfileScreen
import com.refuge.next.screens.ToolsScreen
import com.refuge.next.screens.SettingsScreen
import com.refuge.next.screens.CcuScreen
import com.refuge.next.screens.RsiLoginScreen

private val productionRootRoutes = setOf(0, 1, 2, 4)

@Composable
fun RefugeApp() {
    val context = LocalContext.current
    val settingsRepository = remember(context) { PreferencesSettingsRepository(context) }
    var settings by remember(settingsRepository) { mutableStateOf(settingsRepository.load()) }
    val userStatus = remember { UserStatusSource(true) }
    val auth = remember(context) { RsiAuthDataSource(context) }
    val isDark = settings.darkTheme
    val isOnline = userStatus.isOnline
    var selectedTab by remember { mutableIntStateOf(0) }
    var rootTab by remember { mutableIntStateOf(0) }
    val palette = if (isDark) RefugeColors.dark else RefugeColors.light
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }
    val cacheSource = remember(context) { ProductionCacheDataSource(context) }
    val cacheManifest = remember(cacheSource) { cacheSource.manifest() }
    val fallbackRepository = remember(cacheSource) {
        ProductionHangarRepository(
            fallbackImage = R.drawable.ship_placeholder,
            m80Image = R.drawable.m80_hero,
            source = cacheSource,
        )
    }
    val repository = remember(auth, fallbackRepository) {
        RsiLiveHangarRepository(auth, fallbackRepository, R.drawable.ship_placeholder, R.drawable.m80_hero)
    }
    val storeRepository = remember(cacheSource) { ProductionCatalogStoreRepository(cacheSource) }
    val terminalRepository = remember(cacheSource) { ProductionTerminalRepository(cacheSource) }
    val fallbackBuybackRepository = remember(cacheSource) { ProductionBuybackRepository(R.drawable.m80_hero, R.drawable.ship_placeholder, cacheSource) }
    val buybackRepository = remember(auth, fallbackBuybackRepository) { RsiLiveBuybackRepository(auth, fallbackBuybackRepository, R.drawable.ship_placeholder) }
    val hangarLogRepository = remember(cacheSource) { ProductionHangarLogRepository(cacheSource) }
    val cartRepository = remember { InMemoryCartRepository() }
    val fallbackProfileRepository = remember(cacheSource) { ProductionProfileRepository(cacheSource) }
    val profileRepository = remember(auth, fallbackProfileRepository) { RsiLiveProfileRepository(auth, fallbackProfileRepository) }
    val utilityRepository = remember(cacheSource) { ProductionUtilityRepository(cacheSource) }
    val ccuRepository = remember(cacheSource) { ProductionCcuRepository(cacheSource, R.drawable.m80_hero, R.drawable.ship_placeholder) }

    // Secondary production routes share the root tab bar, but system Back must return
    // to the originating root screen instead of finishing the activity.
    BackHandler(enabled = selectedTab !in productionRootRoutes) {
        selectedTab = rootTab
    }

    RefugeScene(palette) { backdrop ->
        RefugeRouteTransition(targetState = selectedTab, modifier = Modifier.fillMaxSize()) { route ->
            RefugeContent(
                selectedTab = route,
                onNavigate = {
                    if (it in productionRootRoutes) {
                        rootTab = it
                    }
                    selectedTab = it
                },
                onOpenDesignLab = { selectedTab = 7 },
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
                terminalRepository = terminalRepository,
                isDark = isDark,
                onToggleTheme = {
                    settings = settings.copy(darkTheme = !settings.darkTheme)
                    settingsRepository.save(settings)
                },
                isOnline = isOnline,
                onToggleOnline = { userStatus.toggle() },
                auth = auth,
                onAuthChanged = { },
                rootTab = rootTab,
                settings = settings,
                settingsRepository = settingsRepository,
                cacheManifest = cacheManifest,
                onToggleSyncLogs = {
                    settings = settings.copy(syncLogs = !settings.syncLogs)
                    settingsRepository.save(settings)
                },
                onToggleLocalOnly = {
                    settings = settings.copy(localOnly = !settings.localOnly)
                    settingsRepository.save(settings)
                },
            )
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
    terminalRepository: com.refuge.next.data.TerminalRepository,
    auth: RsiAuthDataSource,
    onAuthChanged: () -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
    rootTab: Int,
    settings: AppSettings,
    settingsRepository: SettingsRepository,
    cacheManifest: com.refuge.next.data.ProductionCacheManifest,
    onToggleSyncLogs: () -> Unit,
    onToggleLocalOnly: () -> Unit,
) {
    when (selectedTab) {
        0 -> HangarScreen(
            backdrop = backdrop,
            palette = palette,
            repository = repository,
            buybackRepository = buybackRepository,
            hangarLogRepository = hangarLogRepository,
            isDark = isDark,
            selectedBottomTab = selectedTab,
            onNavigate = onNavigate,
            onToggleTheme = onToggleTheme,
            onOpenDesignLab = onOpenDesignLab,
            onOpenCcu = { onNavigate(6) },
            isOnline = isOnline,
            onToggleOnline = onToggleOnline,
        )

        1 -> StoreScreen(
            backdrop = backdrop,
            palette = palette,
            repository = storeRepository,
            cartRepository = cartRepository,
            isDark = isDark,
            selectedBottomTab = selectedTab,
            onNavigate = onNavigate,
            onOpenCcu = { onNavigate(6) },
            isOnline = isOnline,
            onToggleOnline = onToggleOnline,
        )

        2 -> TerminalScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            selectedBottomTab = selectedTab,
            onNavigate = onNavigate,
            repository = terminalRepository,
            isOnline = isOnline,
            onToggleOnline = onToggleOnline,
        )

        3 -> ToolsScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            selectedBottomTab = rootTab,
            onNavigate = onNavigate,
            utilityRepository = utilityRepository,
            isOnline = isOnline,
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
            onToggleOnline = onToggleOnline,
        )

        5 -> SettingsScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            selectedBottomTab = rootTab,
            onNavigate = onNavigate,
            onToggleTheme = onToggleTheme,
            syncLogs = settings.syncLogs,
            localOnly = settings.localOnly,
            onToggleSyncLogs = onToggleSyncLogs,
            onToggleLocalOnly = onToggleLocalOnly,
            onClearCache = { settingsRepository.clearLocalCache() },
            cacheInfo = cacheManifest,
            isOnline = isOnline,
            onToggleOnline = onToggleOnline,
        )

        6 -> CcuScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            selectedBottomTab = rootTab,
            onNavigate = onNavigate,
            rootTab = rootTab,
            ccuRepository = ccuRepository,
            isOnline = isOnline,
            onToggleOnline = onToggleOnline,
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

        else -> DesignLabScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            onToggleTheme = onToggleTheme,
        )
    }
}
