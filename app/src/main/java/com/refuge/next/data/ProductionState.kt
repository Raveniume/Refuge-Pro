package com.refuge.next.data

import android.content.Context
import android.util.Log
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
    val translationEnabled: Boolean = true,
)

internal data class AppSettingsMigration(
    val settings: AppSettings,
    val obsoleteKeys: Set<String>,
)

internal fun migrateAppSettings(values: Map<String, *>): AppSettingsMigration = AppSettingsMigration(
    settings = AppSettings(
        darkTheme = values[KEY_DARK_THEME] as? Boolean ?: true,
        translationEnabled = values[KEY_TRANSLATION_ENABLED] as? Boolean ?: true,
    ),
    obsoleteKeys = LEGACY_SETTINGS_KEYS.filterTo(linkedSetOf()) { it in values },
)

interface SettingsRepository {
    fun load(): AppSettings
    fun save(settings: AppSettings)
    suspend fun clearLocalCache()
}

/** Small SharedPreferences adapter; account and hangar records are never removed. */
class PreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val appContext = context.applicationContext
    private val preferences = context.getSharedPreferences("refuge_settings", Context.MODE_PRIVATE)

    override fun load(): AppSettings {
        val migration = migrateAppSettings(preferences.all)
        if (migration.obsoleteKeys.isNotEmpty()) {
            preferences.edit().also { editor ->
                migration.obsoleteKeys.forEach(editor::remove)
            }.apply()
        }
        return migration.settings
    }

    override fun save(settings: AppSettings) {
        preferences.edit()
            .putBoolean(KEY_DARK_THEME, settings.darkTheme)
            .putBoolean(KEY_TRANSLATION_ENABLED, settings.translationEnabled)
            .remove(KEY_SYNC_LOGS)
            .remove(KEY_LOCAL_ONLY)
            .apply()
    }

    override suspend fun clearLocalCache() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val images = coil3.SingletonImageLoader.get(appContext)
        images.memoryCache?.clear()
        images.diskCache?.clear()
        preferences.edit().remove(KEY_LAST_CLEAR).putLong(KEY_LAST_CLEAR, System.currentTimeMillis()).apply()
    }

    companion object {
        private const val KEY_LAST_CLEAR = "last_cache_clear"
    }
}

private const val KEY_DARK_THEME = "dark_theme"
private const val KEY_TRANSLATION_ENABLED = "translation_enabled"
private const val KEY_SYNC_LOGS = "sync_logs"
private const val KEY_LOCAL_ONLY = "local_only"
private val LEGACY_SETTINGS_KEYS = setOf(KEY_SYNC_LOGS, KEY_LOCAL_ONLY)

enum class UserPresence(
    val label: String,
    /** Value accepted by RSI Spectrum's member presence endpoint. */
    val remoteValue: String,
) {
    ONLINE("在线", "online"),
    AWAY("离开", "away"),
    DO_NOT_DISTURB("请勿打扰", "do_not_disturb"),
    PLAYING("游戏中", "playing"),
    INVISIBLE("隐身", "invisible");

    companion object {
        fun fromRemote(value: String?): UserPresence? = when (value?.trim()?.lowercase()) {
            "online", "available" -> ONLINE
            "away", "afk" -> AWAY
            "do_not_disturb", "dnd" -> DO_NOT_DISTURB
            "playing", "in_game" -> PLAYING
            "invisible", "offline" -> INVISIBLE
            else -> null
        }
    }
}

/** One persisted presence state shared by every production header/profile. */
class UserStatusSource(
    context: Context? = null,
    initialPresence: UserPresence = UserPresence.ONLINE,
) {
    private val preferences = context?.getSharedPreferences("refuge_presence", Context.MODE_PRIVATE)
    var presence by mutableStateOf(
        preferences?.let { stored ->
            runCatching { UserPresence.valueOf(stored.getString(KEY_PRESENCE, null).orEmpty()) }
                .getOrDefault(initialPresence)
        } ?: initialPresence,
    )
        private set

    fun set(value: UserPresence) {
        presence = value
        preferences?.edit()
            ?.putString(KEY_PRESENCE, value.name)
            // Keep a retry marker until RSI confirms the write. This makes a
            // temporary network failure converge on the official Spectrum
            // state the next time the app starts or regains a session.
            ?.putString(KEY_PENDING_SYNC, value.name)
            ?.apply()
    }

    /** Pushes the selected status to RSI while keeping the UI cache-first. */
    suspend fun syncToRsi(auth: RsiAuthDataSource): Boolean {
        val value = presence
        val synced = runCatching { auth.setSpectrumPresenceStatus(value.remoteValue) }
            .onFailure { error ->
                Log.w("RefugePresence", "Spectrum 状态同步失败，将在下次登录后重试", error)
            }
            .getOrDefault(false)
        if (synced) {
            preferences?.edit()?.remove(KEY_PENDING_SYNC)?.apply()
            Log.i("RefugePresence", "Spectrum 状态已同步: ${value.remoteValue}")
        } else {
            Log.w("RefugePresence", "Spectrum 未确认状态: ${value.remoteValue}")
        }
        return synced
    }

    companion object {
        private const val KEY_PRESENCE = "presence"
        private const val KEY_PENDING_SYNC = "pending_sync"
    }
}
