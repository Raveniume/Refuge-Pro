package com.refuge.next.data

/** A request contains pledge identities, never a repeated copy of a single ID. */
class HangarReclaimRequest private constructor(
    val pledgeIds: List<Long>,
    private val password: String,
) {
    fun bodyFor(id: Long): Map<String, String> {
        require(id in pledgeIds)
        return mapOf("pledge_id" to id.toString(), "current_password" to password)
    }

    override fun toString() = "HangarReclaimRequest(quantity=${pledgeIds.size}, credentials=redacted)"

    companion object {
        fun prepare(quantity: String, available: Int, ids: List<Long>, reclaimable: Boolean, password: String): HangarReclaimRequest {
            val count = quantity.trim().takeIf { it.matches(Regex("[0-9]+")) }?.toIntOrNull()
            require(count != null && count > 0) { "请输入正确的回收数量" }
            require(count <= available) { "回收数量不能大于物品数量" }
            require(reclaimable) { "该物品当前不可回收" }
            require(ids.size == available && ids.all { it > 0 } && ids.distinct().size == ids.size) {
                "物品编号或数量不完整，请刷新机库后重试"
            }
            require(password.isNotBlank()) { "请输入当前 RSI 账户密码" }
            return HangarReclaimRequest(ids.take(count).toList(), password)
        }
    }
}

data class HangarReclaimResult(
    val requested: Int,
    val confirmedIds: List<Long>,
    val failure: String? = null,
    val uncertainId: Long? = null,
    val refreshFailure: String? = null,
) {
    val complete: Boolean get() = confirmedIds.size == requested && failure == null
    val message: String get() = buildString {
        append(if (complete) "已回收 ${confirmedIds.size} 件" else "已确认回收 ${confirmedIds.size}/$requested 件")
        failure?.let { append("。$it") }
        if (uncertainId != null) append("。编号 $uncertainId 的结果未确认，请核对机库和日志，勿重复提交")
        refreshFailure?.let { append("。机库刷新失败：$it") }
    }
}

/** Pure sequential runner: no transport, automatic retry or assumed success. */
internal suspend fun runHangarReclaim(
    request: HangarReclaimRequest,
    deviceVerified: Boolean,
    validate: suspend (List<Long>) -> Unit,
    send: suspend (Long, Map<String, String>) -> String?,
    onConfirmed: suspend (Long) -> Unit,
    refresh: suspend () -> Unit,
): HangarReclaimResult {
    require(deviceVerified) { "请先完成设备安全验证" }
    validate(request.pledgeIds)
    val confirmed = mutableListOf<Long>()
    var failure: String? = null
    var uncertain: Long? = null
    for (id in request.pledgeIds) {
        try {
            val rejected = send(id, request.bodyFor(id))
            if (rejected != null) {
                failure = rejected
                break
            }
            confirmed += id
            onConfirmed(id)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // An exception after dispatch cannot prove that RSI rejected the request.
            uncertain = id
            failure = "请求中断，已停止后续回收"
            break
        }
    }
    val refreshFailure = try {
        refresh()
        null
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        error.message ?: "无法获取最新机库"
    }
    return HangarReclaimResult(request.pledgeIds.size, confirmed.toList(), failure, uncertain, refreshFailure)
}
