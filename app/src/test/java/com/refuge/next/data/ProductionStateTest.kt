package com.refuge.next.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductionStateTest {
    @Test
    fun legacyRefreshPreferencesAreRemovedAndCannotDisableRefresh() {
        val migration = migrateAppSettings(
            mapOf(
                "dark_theme" to false,
                "sync_logs" to false,
                "local_only" to true,
            ),
        )

        assertEquals(AppSettings(darkTheme = false, translationEnabled = true), migration.settings)
        assertEquals(setOf("sync_logs", "local_only"), migration.obsoleteKeys)
    }

    @Test
    fun translationPreferenceDefaultsOnAndCanBeDisabled() {
        assertEquals(true, migrateAppSettings(emptyMap<String, Any>()).settings.translationEnabled)
        assertEquals(false, migrateAppSettings(mapOf("translation_enabled" to false)).settings.translationEnabled)
    }

    @Test
    fun invalidTranslationPreferenceFallsBackWithoutChangingTheme() {
        assertEquals(
            AppSettings(darkTheme = false, translationEnabled = true),
            migrateAppSettings(mapOf("dark_theme" to false, "translation_enabled" to "false")).settings,
        )
    }

    @Test
    fun themeChangesAndLegacyMigrationPreserveDisabledTranslation() {
        val migrated = migrateAppSettings(mapOf("translation_enabled" to false, "local_only" to true))
        assertEquals(false, migrated.settings.copy(darkTheme = false).translationEnabled)
        assertEquals(setOf("local_only"), migrated.obsoleteKeys)
    }
}
