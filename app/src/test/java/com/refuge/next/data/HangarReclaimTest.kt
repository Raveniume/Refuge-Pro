package com.refuge.next.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class HangarReclaimTest {
    @Test fun prepareUsesTheFirstDistinctPledgeIdsForQuantity() {
        val request = HangarReclaimRequest.prepare("2", 3, listOf(101, 102, 103), true, "secret")
        assertEquals(listOf(101L, 102L), request.pledgeIds)
        assertEquals(mapOf("pledge_id" to "101", "current_password" to "secret"), request.bodyFor(101))
    }

    @Test fun prepareRejectsIncompleteIdentityList() {
        assertThrows(IllegalArgumentException::class.java) {
            HangarReclaimRequest.prepare("2", 2, listOf(101), true, "secret")
        }
    }

    @Test fun runnerStopsAfterRejectedResponseAndStillRefreshes() = runBlocking {
        val request = HangarReclaimRequest.prepare("3", 3, listOf(1, 2, 3), true, "secret")
        val sent = mutableListOf<Long>()
        var refreshed = 0
        val result = runHangarReclaim(request, true, validate = {}, send = { id, _ ->
            sent += id
            if (id == 2L) "余额不足" else null
        }, onConfirmed = {}, refresh = { refreshed++ })
        assertEquals(listOf(1L, 2L), sent)
        assertEquals(listOf(1L), result.confirmedIds)
        assertEquals("余额不足", result.failure)
        assertEquals(1, refreshed)
        assertFalse(result.complete)
    }

    @Test fun runnerMarksTransportExceptionAsUncertainAndDoesNotRetry() = runBlocking {
        val request = HangarReclaimRequest.prepare("1", 1, listOf(9), true, "secret")
        var calls = 0
        val result = runHangarReclaim(request, true, validate = {}, send = { _, _ ->
            calls++
            error("connection lost")
        }, onConfirmed = {}, refresh = {})
        assertEquals(1, calls)
        assertEquals(9L, result.uncertainId)
        assertTrue(result.message.contains("未确认"))
    }
}
