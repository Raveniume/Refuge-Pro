package com.refuge.next.data

import org.junit.Assert.*
import org.junit.Test

class PledgeActionRequestTest {
    @Test fun giftMatchesFlutterRequestAndRedactsCredentials() {
        val request = PledgeActionRequest.gift(101, "Attributed", true,
            " recipient@example.com ", " Recipient ", "fixture-password")
        assertEquals("api/account/giftPledge", request.endpoint)
        assertEquals(mapOf("pledge_id" to "101", "current_password" to "fixture-password",
            "email" to "recipient@example.com", "name" to "Recipient"), request.body())
        assertFalse(request.toString().contains("fixture-password"))
        assertFalse(request.toString().contains("recipient@example.com"))
        assertFalse(SafeMutationGuard().execute(request).executed)
    }

    @Test fun recallMatchesFlutterAndCannotBeBuiltForUngiftedPledge() {
        val request = PledgeActionRequest.recall(103, "Gifted")
        assertEquals("api/account/cancelGift", request.endpoint)
        assertEquals(mapOf("pledge_id" to "103"), request.body())
        assertEquals(DestructiveAction.RECALL, SafeMutationGuard().execute(request).action)
        assertFalse(SafeMutationGuard().execute(request).executed)
        assertThrows(IllegalArgumentException::class.java) { PledgeActionRequest.recall(103, "Attributed") }
        assertThrows(IllegalArgumentException::class.java) { PledgeActionRequest.recall(0, "Gifted") }
    }

    @Test fun giftRejectsUnavailablePledgesAndMissingRecipientOrPassword() {
        fun draft(id: Long = 101, status: String = "Attributed", allowed: Boolean = true,
            email: String = "qa@example.com", name: String = "Recipient", password: String = "fixture") =
            PledgeActionRequest.gift(id, status, allowed, email, name, password)
        listOf<() -> Unit>(
            { draft(id = 0) }, { draft(status = "Gifted") }, { draft(allowed = false) },
            { draft(email = "invalid") }, { draft(name = " ") }, { draft(password = "") },
        ).forEach { invalid -> assertThrows(IllegalArgumentException::class.java) { invalid() } }
    }
}
