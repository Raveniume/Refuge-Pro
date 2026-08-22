package com.refuge.next.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refuge.next.R

@Immutable
data class RefugePalette(
    val background: Color,
    val backgroundEdge: Color,
    val backgroundLight: Color,
    val contentSurface: Color,
    val contentSurfaceStrong: Color,
    val glass: Color,
    val glassStrong: Color,
    val glassSelection: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSoft: Color,
    val positive: Color,
    val warning: Color,
    val error: Color,
    val divider: Color,
    val outline: Color,
    val scrim: Color,
)

object RefugeColors {
    val dark = RefugePalette(
        background = Color(0xFF080F1B),
        backgroundEdge = Color(0xFF101D30),
        backgroundLight = Color(0xFF1A2A41),
        contentSurface = Color(0xFF132236).copy(alpha = .74f),
        contentSurfaceStrong = Color(0xFF182A42).copy(alpha = .94f),
        glass = Color.White.copy(alpha = .055f),
        glassStrong = Color.White.copy(alpha = .10f),
        glassSelection = Color.White.copy(alpha = .12f),
        text = Color(0xFFF5F5F7),
        textSecondary = Color(0xFFB8B8BE),
        textMuted = Color(0xFF8E8E93),
        accent = Color(0xFF64B5FF),
        accentSoft = Color(0xFF0A84FF).copy(alpha = .12f),
        positive = Color(0xFF72D6AA),
        warning = Color(0xFFFFD60A),
        error = Color(0xFFFF6961),
        divider = Color.White.copy(alpha = .12f),
        outline = Color.White.copy(alpha = .16f),
        scrim = Color.Black.copy(alpha = .56f),
    )

    val light = RefugePalette(
        background = Color(0xFFF2F2F7),
        backgroundEdge = Color(0xFFE5E5EA),
        backgroundLight = Color(0xFFFFFFFF),
        contentSurface = Color(0xFFEAEAEE).copy(alpha = .88f),
        contentSurfaceStrong = Color.White.copy(alpha = .96f),
        glass = Color.White.copy(alpha = .42f),
        glassStrong = Color.White.copy(alpha = .58f),
        glassSelection = Color.Black.copy(alpha = .075f),
        text = Color(0xFF1C1C1E),
        textSecondary = Color(0xFF4B4B50),
        textMuted = Color(0xFF5F6066),
        accent = Color(0xFF007AFF),
        accentSoft = Color(0xFF007AFF).copy(alpha = .10f),
        positive = Color(0xFF087B51),
        warning = Color(0xFF9A6700),
        error = Color(0xFFFF3B30),
        divider = Color(0xFF3C3C43).copy(alpha = .18f),
        outline = Color(0xFF3C3C43).copy(alpha = .18f),
        scrim = Color.Black.copy(alpha = .34f),
    )
}

object RefugeSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val section = 32.dp
    val page = 24.dp
}

object RefugeRadius {
    // Concentric geometry: controls sit inside grouped panels with a stable
    // inset instead of each layer choosing an unrelated pill radius.
    val image = 12.dp
    val control = 12.dp
    val panel = 16.dp
    val hero = 20.dp
    val floating = 24.dp
    val sheet = 28.dp
    val alert = 22.dp

    fun inner(outer: androidx.compose.ui.unit.Dp, inset: androidx.compose.ui.unit.Dp) =
        (outer - inset).coerceAtLeast(0.dp)
}

object RefugeIconSize {
    val small = 18.dp
    val medium = 22.dp
    val large = 26.dp
}

object RefugeTypography {
    private val pingFang = FontFamily(Font(R.font.pingfang_bold, FontWeight.Medium))
    private val pingFangHeavy = FontFamily(Font(R.font.pingfang_heavy, FontWeight.Bold))

    fun largeTitle(palette: RefugePalette) = TextStyle(
        color = palette.text,
        fontFamily = pingFangHeavy,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
    )

    fun title(palette: RefugePalette) = TextStyle(
        color = palette.text,
        fontFamily = pingFang,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Medium,
    )

    fun headline(palette: RefugePalette) = TextStyle(
        color = palette.text,
        fontFamily = pingFang,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Medium,
    )

    fun body(palette: RefugePalette) = TextStyle(
        color = palette.textSecondary,
        fontFamily = pingFang,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    )

    fun secondary(palette: RefugePalette) = TextStyle(
        color = palette.textMuted,
        fontFamily = pingFang,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    )

    fun caption(palette: RefugePalette) = TextStyle(
        color = palette.textMuted,
        fontFamily = pingFang,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
    )

    fun value(palette: RefugePalette) = TextStyle(
        color = palette.text,
        fontFamily = pingFangHeavy,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Bold,
    )
}
