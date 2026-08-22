package com.refuge.next.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The production build is deliberately offline-first. This manifest identifies
 * the bundled, versioned legacy-cache import used by the repositories, so the
 * UI never needs to know whether a future network refresh was available.
 */
data class ProductionCacheManifest(
    val version: String,
    val source: String,
    val refreshedAt: String,
) {
    val label: String
        get() = "$version · $refreshedAt"
}

val productionCacheManifest = ProductionCacheManifest(
    version = "legacy-cache-v1",
    source = "RefugeNext legacy API/cache contract",
    refreshedAt = "2026-08-20",
)

data class AppSettings(
    val darkTheme: Boolean = true,
    val syncLogs: Boolean = true,
    val localOnly: Boolean = true,
)

interface SettingsRepository {
    fun load(): AppSettings
    fun save(settings: AppSettings)
    fun clearLocalCache()
}

/** Small SharedPreferences adapter; account and hangar records are never removed. */
class PreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val preferences = context.getSharedPreferences("refuge_settings", Context.MODE_PRIVATE)

    override fun load(): AppSettings = AppSettings(
        darkTheme = preferences.getBoolean(KEY_DARK_THEME, true),
        syncLogs = preferences.getBoolean(KEY_SYNC_LOGS, true),
        localOnly = preferences.getBoolean(KEY_LOCAL_ONLY, true),
    )

    override fun save(settings: AppSettings) {
        preferences.edit()
            .putBoolean(KEY_DARK_THEME, settings.darkTheme)
            .putBoolean(KEY_SYNC_LOGS, settings.syncLogs)
            .putBoolean(KEY_LOCAL_ONLY, settings.localOnly)
            .apply()
    }

    override fun clearLocalCache() {
        // The bundled production cache is immutable. Clearing this marker makes
        // diagnostics truthful without deleting account or hangar data.
        preferences.edit().remove(KEY_LAST_CLEAR).putLong(KEY_LAST_CLEAR, System.currentTimeMillis()).apply()
    }

    companion object {
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_SYNC_LOGS = "sync_logs"
        private const val KEY_LOCAL_ONLY = "local_only"
        private const val KEY_LAST_CLEAR = "last_cache_clear"
    }
}

/** Shared presence state used by every production header and the profile page. */
class UserStatusSource(initialOnline: Boolean) {
    var isOnline by mutableStateOf(initialOnline)
        private set

    fun toggle() {
        isOnline = !isOnline
    }
}
