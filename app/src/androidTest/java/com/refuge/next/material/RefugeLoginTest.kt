package com.refuge.next.material

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.refuge.next.data.*
import com.refuge.next.design.RefugeColors
import com.refuge.next.screens.RsiLoginScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RefugeLoginTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun physicalKeysImeAndRetryCompleteEmailVerification() {
        var attempts = 0
        var completed = false
        val auth = object : StubAuth() {
            override suspend fun login(email: String, password: String, captcha: String?): RsiLoginResult {
                assertEquals("qa@example.com", email)
                assertEquals("fixture-secret", password)
                if (attempts++ == 0) throw java.io.IOException("offline")
                return RsiLoginResult(RsiLoginStep.NEED_CODE, "")
            }
            override suspend fun verifyCode(code: String): RsiLoginResult {
                assertEquals("123456", code)
                return RsiLoginResult(RsiLoginStep.AUTHENTICATED, "")
            }
        }
        val dark = mutableStateOf(true)
        compose.setContent {
            val palette = if (dark.value) RefugeColors.dark else RefugeColors.light
            RefugeScene(palette) { RsiLoginScreen(it, palette, auth, allowClose = false,
                onAuthenticated = { completed = true }, onClose = {}) }
        }
        compose.onNodeWithContentDescription("登录").assertIsNotEnabled()
        save("login-dark")
        compose.runOnIdle { dark.value = false }
        save("login-light")
        compose.onNodeWithTag("login-email").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("login-email").assertIsFocused()
        // Android hardware key events exercise the native editing path, not
        // just the SetText semantics action used by most form tests.
        InstrumentationRegistry.getInstrumentation().sendStringSync("qa@example.com")
        compose.onNodeWithTag("login-email").assertTextContains("qa@example.com").performImeAction()
        compose.onNodeWithTag("login-password").assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendStringSync("fixture-secret")
        compose.onNodeWithTag("login-password").performImeAction()
        compose.waitUntil(5000) { compose.onAllNodesWithText("连接失败，请检查网络后重试").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("登录").assertIsEnabled().performClick()
        compose.onNodeWithTag("login-code").performTextInput("123456")
        compose.onNodeWithTag("login-code").performImeAction()
        compose.runOnIdle { assertTrue(completed); assertEquals(2, attempts) }
        save("login-code-completed")
    }

    @Test fun captchaRefreshClearsOldAnswerAndSubmitsTheNewChallenge() {
        var requests = 0
        var completed = false
        val bitmap = android.graphics.Bitmap.createBitmap(120, 50, android.graphics.Bitmap.Config.ARGB_8888)
        val png = java.io.ByteArrayOutputStream().also { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        val auth = object : StubAuth() {
            override suspend fun captcha(): ByteArray { requests++; return png }
            override suspend fun login(email: String, password: String, captcha: String?): RsiLoginResult =
                if (captcha == "NEW") RsiLoginResult(RsiLoginStep.AUTHENTICATED, "")
                else RsiLoginResult(RsiLoginStep.NEED_CAPTCHA, "")
        }
        compose.setContent { RefugeScene(RefugeColors.dark) {
            RsiLoginScreen(it, RefugeColors.dark, auth, allowClose = false,
                onAuthenticated = { completed = true }, onClose = {})
        } }
        compose.onNodeWithTag("login-email").performTextInput("qa@example.com")
        compose.onNodeWithTag("login-password").performTextInput("fixture-secret")
        compose.onNodeWithTag("login-password").performImeAction()
        compose.onNodeWithContentDescription("图形验证码").performTextInput("OLD")
        compose.onNodeWithContentDescription("刷新").performClick()
        compose.onNodeWithContentDescription("图形验证码").assert(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText,
                androidx.compose.ui.text.AnnotatedString("")))
        compose.onNodeWithContentDescription("完成").assertIsNotEnabled()
        compose.onNodeWithContentDescription("图形验证码").performTextInput("NEW")
        compose.onNodeWithContentDescription("图形验证码").performImeAction()
        compose.runOnIdle { assertTrue(completed); assertEquals(2, requests) }
    }

    @Test fun compactLandscapeWithLargeTextKeepsTheSubmitActionReachable() {
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides
                    androidx.compose.ui.unit.Density(density.density, 1.5f)) {
                androidx.compose.foundation.layout.Box(
                    // This fixture models the *remaining* viewport above the IME.
                    // Do not subtract the portrait device's keyboard from it again.
                    androidx.compose.ui.Modifier.requiredSize(360.dp, 320.dp)
                        .consumeWindowInsets(WindowInsets.ime)) {
                    RefugeScene(RefugeColors.light) {
                        RsiLoginScreen(it, RefugeColors.light, StubAuth(), allowClose = false,
                            onAuthenticated = {}, onClose = {})
                    }
                }
            }
        }
        compose.onNodeWithTag("login-email").performScrollTo().performTextInput("qa@example.com")
        compose.onNodeWithTag("login-password").performScrollTo().performTextInput("fixture-secret")
        compose.onNodeWithContentDescription("登录").performScrollTo()
        save("login-compact-landscape-before-assert")
        evidenceFile("login-compact-tree.txt").writeText(compose.onRoot(useUnmergedTree = true).printToString())
        compose.onNodeWithContentDescription("登录").assertIsDisplayed().assertIsEnabled()
        save("login-compact-landscape-large-text")
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        evidenceFile("$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    private open class StubAuth : RsiAuthRepository {
        override fun session(): RsiSession? = null
        override suspend fun login(email: String, password: String, captcha: String?) = RsiLoginResult(RsiLoginStep.FAILED, "")
        override suspend fun verifyCode(code: String) = RsiLoginResult(RsiLoginStep.FAILED, "")
        override suspend fun captcha(): ByteArray? = null
        override fun logout() = Unit
    }
}
