package com.refuge.next.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.refuge.next.R
import com.refuge.next.material.RefugeIcons
import com.refuge.next.reference.ReferenceLiquidButton
import com.refuge.next.reference.ReferenceOpticalTest
import com.refuge.next.reference.ReferenceLiquidSelectionBar
import com.refuge.next.reference.ReferenceSearchField
import com.refuge.next.reference.ReferenceSegmentedControl
import com.refuge.next.reference.ReferenceSelectionItem

@Composable
fun ReferenceLabScreen(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var showSheet by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }

    ReferenceLabBackground(isDark) { backdrop ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Reference Replication Lab", color = if (isDark) Color.White else Color.Black)
                        }
                        ReferenceLiquidButton(
                            backdrop = backdrop,
                            onClick = onToggleTheme,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(if (isDark) RefugeIcons.light else RefugeIcons.dark, contentDescription = "Toggle appearance")
                        }
                    }
                }
                item {
                    Text("Segmented Control", color = if (isDark) Color.White else Color.Black)
                    Spacer(Modifier.size(8.dp))
                    ReferenceSegmentedControl(backdrop, isDark, listOf("Tab 1", "Tab 2", "Tab 3"))
                }
                item {
                    Text("Floating Icon Button", color = if (isDark) Color.White else Color.Black)
                    Spacer(Modifier.size(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ReferenceLiquidButton(
                            backdrop = backdrop,
                            onClick = { showAlert = true },
                            modifier = Modifier.size(56.dp),
                        ) {
                            Icon(RefugeIcons.notification, contentDescription = "Open alert", tint = if (isDark) Color.White else Color.Black)
                        }
                    }
                }
                item {
                    Text("Search / Field", color = if (isDark) Color.White else Color.Black)
                    Spacer(Modifier.size(8.dp))
                    ReferenceSearchField(backdrop, isDark, search, { search = it }, RefugeIcons.search)
                }
                item {
                    Text("Sheet / Alert", color = if (isDark) Color.White else Color.Black)
                    Spacer(Modifier.size(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReferenceLiquidButton(backdrop, { showSheet = true }) { Text("Sheet", color = if (isDark) Color.White else Color.Black) }
                        ReferenceLiquidButton(backdrop, { showAlert = true }) { Text("Alert", color = if (isDark) Color.White else Color.Black) }
                    }
                }
                item {
                    Text("Optical Test", color = if (isDark) Color.White else Color.Black)
                    Spacer(Modifier.size(8.dp))
                    ReferenceOpticalTest(backdrop, isDark)
                }
            }

            ReferenceLiquidSelectionBar(
                backdrop = backdrop,
                isDark = isDark,
                tabsCount = 3,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(.88f).navigationBarsPadding().padding(bottom = 10.dp),
            ) { selected, select ->
                repeat(3) { index ->
                    ReferenceSelectionItem(
                        icon = listOf(RefugeIcons.home, RefugeIcons.design, RefugeIcons.tools)[index],
                        label = "Tab ${index + 1}",
                        selected = index == selected,
                        isDark = isDark,
                        onClick = { select(index) },
                    )
                }
            }
        }
    }

    if (showSheet) ReferenceSheet(isDark, onDismiss = { showSheet = false })
    if (showAlert) ReferenceAlert(isDark, onDismiss = { showAlert = false })
}

@Composable
private fun ReferenceLabBackground(
    isDark: Boolean,
    content: @Composable (LayerBackdrop) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    val wallpaper: ImageBitmap = ImageBitmap.imageResource(R.drawable.reference_wallpaper_light)
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            drawImage(wallpaper, dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
            drawRect(if (isDark) Color.Black.copy(alpha = .44f) else Color.White.copy(alpha = .04f), style = Fill)
        }
        content(backdrop)
    }
}

@Composable
private fun ReferenceSheet(isDark: Boolean, onDismiss: () -> Unit) {
    val modalBackdrop = rememberLayerBackdrop()
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val base = if (isDark) Color(0xFF242426) else Color(0xFFF7F7F8)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isDark) .52f else .28f)), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxWidth().clip(shape)) {
                Box(Modifier.matchParentSize().background(base).layerBackdrop(modalBackdrop))
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(Modifier.align(Alignment.CenterHorizontally).size(width = 32.dp, height = 4.dp).background(Color.Gray.copy(.55f), RoundedCornerShape(2.dp)))
                    Text("Sheet", color = if (isDark) Color.White else Color.Black)
                    Text("Filter options", color = if (isDark) Color.White.copy(.72f) else Color.Black.copy(.64f))
                    ReferenceLiquidButton(modalBackdrop, onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Done", color = if (isDark) Color.White else Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceAlert(isDark: Boolean, onDismiss: () -> Unit) {
    val modalBackdrop = rememberLayerBackdrop()
    val shape = RoundedCornerShape(28.dp)
    val base = if (isDark) Color(0xFF2C2C2E) else Color.White
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isDark) .52f else .28f)), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth(.82f).clip(shape)) {
                Box(Modifier.matchParentSize().background(base).layerBackdrop(modalBackdrop))
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Alert", color = if (isDark) Color.White else Color.Black)
                    Text("Changes are ready to apply.", color = if (isDark) Color.White.copy(.72f) else Color.Black.copy(.64f))
                    ReferenceLiquidButton(modalBackdrop, onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Okay", color = if (isDark) Color.White else Color.Black)
                    }
                }
            }
        }
    }
}
