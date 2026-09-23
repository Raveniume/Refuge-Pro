package com.refuge.next.data

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class RsiRequestCookiesTest {
    private val url = "https://robertsspaceindustries.com/account/pledges".toHttpUrl()
    private val session = RsiSession("test@example.test", "test-device", "test-token", "test-account")
    @Test fun challengeCookiesDoNotReplacePersistedAuthentication() {
        val challenge = Cookie.parse(url, "csrf=test-challenge; Path=/; Secure")!!
        val old = Cookie.parse(url, "Rsi-Token=stale-token; Path=/; Secure")!!
        val cookies = rsiRequestCookies(url, session, listOf(challenge, old)).associate { it.name to it.value }
        assertEquals("test-challenge", cookies["csrf"])
        assertEquals("test-token", cookies["Rsi-Token"])
        assertEquals("test-device", cookies["_rsi_device"])
        assertEquals("test-account", cookies["Rsi-Account-Auth"])
    }
    @Test fun authenticationNeverFollowsRedirectsToUnrelatedHosts() {
        assertTrue(rsiRequestCookies("https://example.test/".toHttpUrl(), session, emptyList()).isEmpty())
        assertTrue(rsiRequestCookies("https://fake-robertsspaceindustries.com/".toHttpUrl(), session, emptyList()).isEmpty())
    }
    @Test fun signedOutChallengeFlowRetainsItsCookies() {
        val challenge = Cookie.parse(url, "challenge=test-challenge; Path=/; Secure")!!
        assertEquals(listOf(challenge), rsiRequestCookies(url, null, listOf(challenge)))
    }
}
