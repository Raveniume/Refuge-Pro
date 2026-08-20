package com.refuge.next

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.refuge.next.data.PreviewHangarRepository
import com.refuge.next.data.CachedCatalogStoreRepository
import com.refuge.next.data.CachedTerminalRepository
import com.refuge.next.design.RefugeColors
import com.refuge.next.material.RefugeScene
import com.refuge.next.screens.DesignLabScreen
import com.refuge.next.screens.HangarScreen
import com.refuge.next.screens.StoreScreen
import com.refuge.next.screens.TerminalScreen
import com.refuge.next.screens.ProfileScreen
import com.refuge.next.screens.ToolsScreen
import com.refuge.next.screens.SettingsScreen
import com.refuge.next.screens.CcuScreen

@Composable
fun RefugeApp() {
    var isDark by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val palette = if (isDark) RefugeColors.dark else RefugeColors.light
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }
    val repository = remember {
        PreviewHangarRepository(
            fallbackImage = R.drawable.ship_placeholder,
            m80Image = R.drawable.m80_hero,
        )
    }
    val storeRepository = remember { CachedCatalogStoreRepository() }
    val terminalRepository = remember { CachedTerminalRepository() }

    RefugeScene(palette) { backdrop ->
        RefugeContent(
            selectedTab = selectedTab,
            onNavigate = { selectedTab = it },
            onOpenDesignLab = { selectedTab = 7 },
            backdrop = backdrop,
            palette = palette,
            repository = repository,
            storeRepository = storeRepository,
            terminalRepository = terminalRepository,
            isDark = isDark,
            onToggleTheme = { isDark = !isDark },
        )
    }
}

@Composable
private fun BoxScope.RefugeContent(
    selectedTab: Int,
    onNavigate: (Int) -> Unit,
    onOpenDesignLab: () -> Unit,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    palette: com.refuge.next.design.RefugePalette,
    repository: PreviewHangarRepository,
    storeRepository: CachedCatalogStoreRepository,
    terminalRepository: CachedTerminalRepository,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
) {
    when (selectedTab) {
        0 -> HangarScreen(
            backdrop = backdrop,
            palette = palette,
            repository = repository,
            isDark = isDark,
            selectedBottomTab = selectedTab,
            onNavigate = onNavigate,
            onToggleTheme = onToggleTheme,
            onOpenDesignLab = onOpenDesignLab,
            onOpenCcu = { onNavigate(6) },
        )

        1 -> StoreScreen(
            backdrop = backdrop,
            palette = palette,
            repository = storeRepository,
            isDark = isDark,
            selectedBottomTab = selectedTab,
            onNavigate = onNavigate,
        )

        2 -> TerminalScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            selectedBottomTab = selectedTab,
            onNavigate = onNavigate,
            repository = terminalRepository,
        )

        3 -> ToolsScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            selectedBottomTab = selectedTab,
            onNavigate = onNavigate,
        )

        4 -> ProfileScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            selectedBottomTab = selectedTab,
            onNavigate = onNavigate,
            onToggleTheme = onToggleTheme,
        )

        5 -> SettingsScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            selectedBottomTab = 4,
            onNavigate = onNavigate,
            onToggleTheme = onToggleTheme,
        )

        6 -> CcuScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            selectedBottomTab = 0,
            onNavigate = onNavigate,
        )

        7 -> DesignLabScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            onToggleTheme = onToggleTheme,
        )

        else -> DesignLabScreen(
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            onToggleTheme = onToggleTheme,
        )
    }
}
