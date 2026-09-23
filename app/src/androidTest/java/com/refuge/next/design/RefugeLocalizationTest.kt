package com.refuge.next.design

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.refuge.next.data.TranslationRepository
import org.junit.Rule
import org.junit.Test

/** In-memory presentation tests: no account, repositories, files or network mutations. */
class RefugeLocalizationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun toggleUpdatesMountedNamesAndRetainsEnglishWhenSourceChanges() {
        val enabled = mutableStateOf(true)
        val source = mutableStateOf("Aurora MR")
        val repository = object : TranslationRepository {
            override fun translate(value: String) = when (value) {
                "Aurora MR" -> "极光 MR"
                "Avenger Titan" -> "复仇者泰坦"
                else -> value
            }
            override fun translateGameItem(value: String) =
                if (value == "BeamGun_SCItem") "光束枪" else value
        }
        compose.setContent {
            CompositionLocalProvider(
                LocalRefugeTranslation provides repository,
                LocalRefugeTranslationEnabled provides enabled.value,
            ) {
                Column {
                    Text(translatedShipName("旧汉化标题", source.value))
                    Text(translatedGameItemName("Beam Gun", "BeamGun_SCItem"))
                }
            }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("极光 MR").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithText("光束枪").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle { enabled.value = false }
        compose.onNodeWithText("Aurora MR").assertIsDisplayed()
        compose.onNodeWithText("Beam Gun").assertIsDisplayed()

        compose.runOnIdle { source.value = "Avenger Titan" }
        compose.onNodeWithText("Avenger Titan").assertIsDisplayed()
        compose.runOnIdle { enabled.value = true }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("复仇者泰坦").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle { enabled.value = false }
        compose.onNodeWithText("Avenger Titan").assertIsDisplayed()
        compose.onNodeWithText("Beam Gun").assertIsDisplayed()
    }
}
