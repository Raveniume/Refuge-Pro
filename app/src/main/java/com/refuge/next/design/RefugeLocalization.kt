package com.refuge.next.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import com.refuge.next.data.TranslationRepository
import com.refuge.next.data.displayName
import com.refuge.next.data.sourceName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalRefugeTranslation = staticCompositionLocalOf<TranslationRepository?> { null }
val LocalRefugeTranslationEnabled = staticCompositionLocalOf { true }

/** Preserve raw catalogue names for identity/route calculations; translate display text on IO. */
@Composable
fun translatedShipName(value: String, originalName: String? = null): String =
    translatedName(sourceName(value, originalName), gameItem = false)

@Composable
fun translatedGameItemName(value: String, className: String? = null): String =
    translatedName(value, gameItem = true, className = className)

@Composable
private fun translatedName(value: String, gameItem: Boolean, className: String? = null): String {
    if (!LocalRefugeTranslationEnabled.current) return value
    val repository = LocalRefugeTranslation.current ?: return value
    // A recycled row must start with its own source name, never the previous row's translation.
    return key(value, repository, gameItem, className) {
        val translated by produceState(value) {
            this.value = withContext(Dispatchers.IO) {
                displayName(value, true, repository, gameItem, className)
            }
        }
        translated
    }
}
