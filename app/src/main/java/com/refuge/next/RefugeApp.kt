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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.refuge.next.data.PreviewHangarRepository
import com.refuge.next.design.RefugeColors
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeScene
import com.refuge.next.screens.DesignLabScreen
import com.refuge.next.screens.HangarScreen
import com.refuge.next.screens.ReferenceLabScreen

@Composable
fun RefugeApp() {
    var isDark by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(1) }
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
        )
    }

    if (selectedTab == 1) {
        ReferenceLabScreen(
            isDark = isDark,
            onToggleTheme = { isDark = !isDark },
        )
    } else {
        RefugeScene(palette) { backdrop ->
            RefugeContent(
                selectedTab = selectedTab,
                onOpenDesignLab = { selectedTab = 1 },
                backdrop = backdrop,
                palette = palette,
                repository = repository,
                isDark = isDark,
                onToggleTheme = { isDark = !isDark },
            )
        }
    }
}

@Composable
private fun BoxScope.RefugeContent(
    selectedTab: Int,
    onOpenDesignLab: () -> Unit,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    palette: com.refuge.next.design.RefugePalette,
    repository: PreviewHangarRepository,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
) {
    when (selectedTab) {
        0 -> HangarScreen(
            backdrop = backdrop,
            palette = palette,
            repository = repository,
            onOpenDesignLab = onOpenDesignLab,
        )

        1 -> DesignLabScreen(
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
