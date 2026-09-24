package com.refuge.next.material

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object RefugeIcons {
    val loadout: ImageVector = ShipSymbols.loadout
    val home: ImageVector = Icons.Outlined.Home
    /** Navigation glyphs mirror RefugeNext's Flutter MainNavigationBar. */
    val homeSelected: ImageVector = Icons.Outlined.Home
    /** Hollow rounded grid matching the selected terminal glyph's four cells. */
    val terminal: ImageVector by lazy { roundedTerminalGrid() }
    val terminalSelected: ImageVector = terminal
    val storeOutline: ImageVector = Icons.Outlined.ShoppingBag
    val storeSelected: ImageVector = Icons.Outlined.ShoppingBag
    val profileSelected: ImageVector = Icons.Outlined.Person
    val tools: ImageVector = Icons.Outlined.Build
    val toolsSelected: ImageVector = Icons.Rounded.Build
    val design: ImageVector = Icons.Outlined.Tune
    val filter: ImageVector = Icons.Outlined.FilterList
    val sort: ImageVector = Icons.Outlined.SwapVert
    val sortAscending: ImageVector = Icons.Outlined.ArrowUpward
    val sortDescending: ImageVector = Icons.Outlined.ArrowDownward
    val search: ImageVector = Icons.Outlined.Search
    val store: ImageVector = Icons.Outlined.Storefront
    val cart: ImageVector = Icons.Outlined.ShoppingBag
    val upgrade: ImageVector = Icons.Outlined.Upgrade
    val gift: ImageVector = Icons.Outlined.LocalOffer
    /** Original RefugeNext Hangar action glyphs. */
    val hangarGift: ImageVector = Icons.Outlined.CardGiftcard
    val recallGift: ImageVector = Icons.Outlined.Undo
    val hangarOpenExternal: ImageVector = Icons.Outlined.ArrowOutward
    val hangarUpgrade: ImageVector = Icons.Outlined.KeyboardDoubleArrowUp
    val giveaway: ImageVector = Icons.Outlined.ArrowUpward
    val reclaim: ImageVector = Icons.Outlined.Recycling
    val refresh: ImageVector = Icons.Outlined.Refresh
    val more: ImageVector = Icons.Outlined.MoreHoriz
    val settings: ImageVector = Icons.Outlined.Settings
    val chevron: ImageVector = Icons.Outlined.ChevronRight
    val back: ImageVector = Icons.Outlined.ArrowBack
    val profile: ImageVector = Icons.Outlined.Person
    val light: ImageVector = Icons.Outlined.LightMode
    val dark: ImageVector = Icons.Outlined.DarkMode
    val success: ImageVector = Icons.Outlined.CheckCircle
    val check: ImageVector = Icons.Outlined.Check
    val alert: ImageVector = Icons.Outlined.WarningAmber
    val visibility: ImageVector = Icons.Outlined.Visibility
    val visibilityOff: ImageVector = Icons.Outlined.VisibilityOff
    val notification: ImageVector = Icons.Outlined.NotificationsNone
    val log: ImageVector = Icons.Outlined.Description
    val analytics: ImageVector = Icons.Outlined.Assessment
    val personSearch: ImageVector = Icons.Outlined.PersonSearch
    val people: ImageVector = Icons.Outlined.People
    val personAdd: ImageVector = Icons.Outlined.PersonAdd
    val ship: ImageVector = Icons.Outlined.DirectionsBoat
    val rocket: ImageVector = Icons.Outlined.RocketLaunch
    val flightTakeoff: ImageVector = Icons.Outlined.FlightTakeoff
    val wallet: ImageVector = Icons.Outlined.AccountBalanceWallet
    val inventory: ImageVector = Icons.Outlined.Inventory2
    val science: ImageVector = Icons.Outlined.Science
    val description: ImageVector = Icons.Outlined.Description
    /** Route-shaped planner glyph used by the legacy CCU planning card. */
    val plannerRoute: ImageVector by lazy { plannerRouteIcon() }
}

private fun plannerRouteIcon(): ImageVector = ImageVector.Builder(
    name = "PlannerRoute",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = PathData {
        moveTo(7f, 4f)
        lineTo(7f, 16f)
        curveTo(7f, 18.2f, 8.8f, 20f, 11f, 20f)
        curveTo(13.2f, 20f, 15f, 18.2f, 15f, 16f)
        lineTo(15f, 8f)
        curveTo(15f, 5.8f, 16.8f, 4f, 19f, 4f)
        lineTo(19f, 9f)
    },
    fill = null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 2.2f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
).addPath(
    pathData = PathData {
        moveTo(8.5f, 4f)
        curveTo(8.5f, 4.83f, 7.83f, 5.5f, 7f, 5.5f)
        curveTo(6.17f, 5.5f, 5.5f, 4.83f, 5.5f, 4f)
        curveTo(5.5f, 3.17f, 6.17f, 2.5f, 7f, 2.5f)
        curveTo(7.83f, 2.5f, 8.5f, 3.17f, 8.5f, 4f)
        close()
        moveTo(20.5f, 9f)
        curveTo(20.5f, 9.83f, 19.83f, 10.5f, 19f, 10.5f)
        curveTo(18.17f, 10.5f, 17.5f, 9.83f, 17.5f, 9f)
        curveTo(17.5f, 8.17f, 18.17f, 7.5f, 19f, 7.5f)
        curveTo(19.83f, 7.5f, 20.5f, 8.17f, 20.5f, 9f)
        close()
    },
    fill = SolidColor(Color.Black),
).build()

private fun roundedTerminalGrid(): ImageVector = ImageVector.Builder(
    name = "TerminalGridOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = PathData {
        fun roundedRect(left: Float, top: Float) {
            val right = left + 8f
            val bottom = top + 8f
            val radius = 1.6f
            moveTo(left + radius, top)
            lineTo(right - radius, top)
            curveTo(right - .7f, top, right, top + .7f, right, top + radius)
            lineTo(right, bottom - radius)
            curveTo(right, bottom - .7f, right - .7f, bottom, right - radius, bottom)
            lineTo(left + radius, bottom)
            curveTo(left + .7f, bottom, left, bottom - .7f, left, bottom - radius)
            lineTo(left, top + radius)
            curveTo(left, top + .7f, left + .7f, top, left + radius, top)
            close()
        }
        roundedRect(2f, 2f)
        roundedRect(14f, 2f)
        roundedRect(2f, 14f)
        roundedRect(14f, 14f)
    },
    fill = null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 1.8f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
).build()
