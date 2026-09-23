package com.refuge.next.data

/** Prepared locally; this type has no transport and never sends a request. */
class PledgeActionRequest private constructor(
    val action: DestructiveAction,
    val endpoint: String,
    val pledgeId: Long,
    val recipientEmail: String? = null,
    val recipientName: String? = null,
    private val currentPassword: String? = null,
) {
    fun body(): Map<String, String> = buildMap {
        put("pledge_id", pledgeId.toString())
        if (action == DestructiveAction.GIFT) {
            put("current_password", requireNotNull(currentPassword))
            put("email", requireNotNull(recipientEmail))
            put("name", requireNotNull(recipientName))
        }
    }

    override fun toString() = "PledgeActionRequest(action=$action, pledgeId=$pledgeId, credentials=redacted)"

    companion object {
        fun gift(
            pledgeId: Long,
            status: String,
            canGift: Boolean,
            email: String,
            name: String,
            password: String,
        ): PledgeActionRequest {
            require(pledgeId > 0) { "Missing pledge ID" }
            require(canGift && !status.equals("Gifted", true)) { "Pledge is not giftable" }
            val recipient = email.trim()
            require(recipient.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) { "Invalid recipient email" }
            require(name.trim().isNotEmpty()) { "Missing recipient name" }
            require(password.isNotBlank()) { "Missing current password" }
            return PledgeActionRequest(DestructiveAction.GIFT, "api/account/giftPledge", pledgeId,
                recipient, name.trim(), password)
        }

        fun recall(pledgeId: Long, status: String): PledgeActionRequest {
            require(pledgeId > 0) { "Missing pledge ID" }
            require(status.equals("Gifted", true)) { "Pledge has not been gifted" }
            return PledgeActionRequest(DestructiveAction.RECALL, "api/account/cancelGift", pledgeId)
        }
    }
}
