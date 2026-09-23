package com.refuge.next.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.refuge.next.data.*

@Composable
internal fun rememberShipReference(name: String): ShipReference? {
    val context = LocalContext.current
    val catalog = remember { ShipReferenceCatalog.shared(context) }
    val entries by catalog.entries.collectAsState()
    return remember(entries, name) { findShipReference(entries, name) }
}
