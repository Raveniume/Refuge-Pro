package com.refuge.next.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import com.kyant.backdrop.Backdrop
import com.refuge.next.material.RefugeCompactUtilityPill
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeLightweightGlassSurface
import com.refuge.next.material.RefugeThreeArchedCircle

@Composable
fun ProductionLoadingState(
    backdrop: Backdrop,
    palette: RefugePalette,
    label: String,
) {
    RefugeLightweightGlassSurface(
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
            RefugeThreeArchedCircle(color = palette.accent, modifier = Modifier.size(22.dp))
            Text(label, style = RefugeTypography.body(palette))
        }
    }
}

@Composable
fun ProductionEmptyState(
    palette: RefugePalette,
    label: String,
) {
    RefugeLightweightGlassSurface(
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs),
        ) {
            Icon(RefugeIcons.search, null, tint = palette.textMuted)
            Text(label, style = RefugeTypography.body(palette))
        }
    }
}

@Composable
fun ProductionErrorState(
    backdrop: Backdrop,
    palette: RefugePalette,
    label: String,
    onRetry: () -> Unit,
) {
    RefugeLightweightGlassSurface(
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
            Icon(RefugeIcons.alert, null, tint = palette.error)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xxs)) {
                Text("暂时无法读取资料", style = RefugeTypography.body(palette).copy(color = palette.text))
                Text(label, style = RefugeTypography.caption(palette))
            }
            RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.refresh, "重试", onRetry)
        }
    }
}
