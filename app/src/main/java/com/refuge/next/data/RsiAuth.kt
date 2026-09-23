package com.refuge.next.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Persisted RSI session. Passwords are intentionally never stored. */
data class RsiSession(
    val email: String,
    val device: String,
    val token: String,
    val accountAuth: String = "",
    val csrf: String = "",
) {
    val isAuthenticated: Boolean get() = token.isNotBlank() && device.isNotBlank()
}

enum class RsiLoginStep { AUTHENTICATED, NEED_CODE, NEED_CAPTCHA, FAILED }

data class RsiLoginResult(
    val step: RsiLoginStep,
    val message: String,
    val session: RsiSession? = null,
    val retryCaptcha: Boolean = false,
) {
    val success: Boolean get() = step == RsiLoginStep.AUTHENTICATED
}

interface RsiAuthRepository {
    fun session(): RsiSession?
    suspend fun login(email: String, password: String, captcha: String? = null): RsiLoginResult
    suspend fun verifyCode(code: String): RsiLoginResult
    suspend fun captcha(): ByteArray?
    fun logout()
}

/**
 * Small, dependency-light port of RefugeNext's RsiApiClient login contract.
 * It keeps the original endpoints and cookie/token semantics while leaving
 * all purchase/mutation requests behind explicit production UI confirmation.
 */
class RsiAuthDataSource(context: Context) : RsiAuthRepository {
    private val prefs = context.getSharedPreferences("refuge_rsi_session", Context.MODE_PRIVATE)
    /** Login challenges are process-local because RSI binds them to its cookie jar. */
    private var allowPersistedSessionCookies = true

    fun loginEmailDraft(): String = prefs.getString(KEY_LOGIN_EMAIL_DRAFT, "").orEmpty()

    fun saveLoginEmailDraft(email: String) {
        prefs.edit().putString(KEY_LOGIN_EMAIL_DRAFT, email.trim()).apply()
    }
    /**
     * RSI's image-CAPTCHA flow is stateful: the CAPTCHA response establishes a
     * short-lived challenge cookie that must be sent with the following sign-in
     * request. Keep those cookies in the same in-memory client session rather
     * than relying on a new request having no cookie context.
     */
    private val cookieJar = RsiCookieJar {
        if (allowPersistedSessionCookies) session() else null
    }
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private var pendingEmail: String = prefs.getString(KEY_PENDING_EMAIL, "") ?: ""
    private var pendingDevice: String = ""
    private var pendingToken: String = ""
    private var pendingAuth: String = ""

    init {
        // A challenge token without its in-memory cookies can never be
        // completed after a process restart. Keep only the draft email.
        prefs.edit()
            .remove(KEY_PENDING_DEVICE)
            .remove(KEY_PENDING_TOKEN)
            .remove(KEY_PENDING_AUTH)
            .apply()
    }
    /**
     * The hangar, local CCU planner and purchase selector all consume the
     * same read-only ship catalogue. Keep one in-flight/result window per
     * account so opening those surfaces together does not issue duplicate
     * initShipUpgrade requests (and re-run the context-token handshake).
     */
    private val shipUpgradeCatalogRefresh = RepositoryRefresh<JSONObject>(minimumIntervalMillis = 1_000L)

    override fun session(): RsiSession? {
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val device = prefs.getString(KEY_DEVICE, "") ?: ""
        val token = prefs.getString(KEY_TOKEN, "") ?: ""
        if (email.isBlank() || device.isBlank() || token.isBlank()) return null
        return RsiSession(
            email = email,
            device = device,
            token = token,
            accountAuth = prefs.getString(KEY_AUTH, "") ?: "",
            csrf = prefs.getString(KEY_CSRF, "") ?: "",
        )
    }

    override suspend fun login(email: String, password: String, captcha: String?): RsiLoginResult =
        withContext(Dispatchers.IO) {
            if (email.isBlank() || password.isBlank()) {
                return@withContext RsiLoginResult(RsiLoginStep.FAILED, "请输入邮箱和密码")
            }
            // A blank captcha means a fresh first step. Discard any persisted
            // half-finished challenge so a restarted app cannot reuse an old
            // token/device pair with a new image.
            if (captcha.isNullOrBlank()) {
                allowPersistedSessionCookies = false
                cookieJar.clear()
                pendingToken = ""
                // Do not invent a device id here. RSI creates the challenge
                // and binds it to the current HTTP session; a client-created
                // id can make a correct captcha fail on the retry.
                pendingDevice = ""
                pendingAuth = ""
                cookieToken = ""
                cookieDevice = ""
                cookieAuth = ""
                savePending()
            }
            // Do not invent a device header for the captcha challenge. RSI
            // assigns the device only when it returns one in the response;
            // sending a client-generated value binds the image to a different
            // challenge and makes an otherwise valid answer fail.
            val device = pendingDevice
            pendingEmail = email
            pendingDevice = device
            savePending()
            val body = JSONObject()
                .put("username", email)
                .put("password", password)
                .put("captcha", captcha)
                .put("remember", true)
            val response = request("api/launcher/v3/signin", body, includeSession = false)
            parseLogin(response, email, device)
        }

    override suspend fun verifyCode(code: String): RsiLoginResult = withContext(Dispatchers.IO) {
        if (code.isBlank()) return@withContext RsiLoginResult(RsiLoginStep.FAILED, "请输入验证码")
        val body = JSONObject()
            .put("code", code)
            .put("device_name", "Refuge")
            .put("device_type", "computer")
            .put("duration", "year")
        val response = request("api/launcher/v3/signin/multiStep", body, includeSession = false)
        parseLogin(response, pendingEmail, pendingDevice)
    }

    override suspend fun captcha(): ByteArray? = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url(BASE_URL + "api/launcher/v3/signin/captcha")
            .post("{}".toRequestBody(jsonType))
            .header("User-Agent", USER_AGENT)
            .header("Referer", BASE_URL)
        // The launcher client sends the challenge token with captcha requests.
        // Without it RSI can return a visually valid image that is not bound to
        // the pending sign-in challenge, so every answer is rejected.
        if (pendingToken.isNotBlank()) builder.header("x-rsi-token", pendingToken)
        if (pendingDevice.isNotBlank()) builder.header("x-rsi-device", pendingDevice)
        runCatching {
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val type = response.header("Content-Type").orEmpty()
                if (!type.startsWith("image/")) return@use null
                response.body?.bytes()
            }
        }.getOrNull()
    }

    override fun logout() {
        allowPersistedSessionCookies = true
        cookieJar.clear()
        pendingEmail = ""
        pendingDevice = ""
        pendingToken = ""
        pendingAuth = ""
        prefs.edit()
            .remove(KEY_EMAIL)
            .remove(KEY_DEVICE)
            .remove(KEY_TOKEN)
            .remove(KEY_AUTH)
            .remove(KEY_CSRF)
            .apply()
    }

    suspend fun getPage(endpoint: String): String = withContext(Dispatchers.IO) {
        val session = session() ?: error("需要先登录 RSI")
        val cookie = cookie(session)
        val request = Request.Builder()
            .url(BASE_URL + endpoint.trimStart('/'))
            .get()
            .headers(okhttp3.Headers.headersOf(
                "Cookie", cookie,
                "User-Agent", USER_AGENT,
                "Referer", BASE_URL,
                "x-rsi-token", session.token,
                "x-rsi-device", session.device,
                "x-csrf-token", session.csrf,
            ))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("RSI 请求失败：${response.code}")
            response.body?.string().orEmpty()
        }
    }

    suspend fun accountGraphql(): JSONObject = withContext(Dispatchers.IO) {
        val session = refreshCsrfToken()
        val query = """
            query account { account { isAnonymous ... on RsiAuthenticatedAccount {
              avatar displayname email hasGamePackage nickname profileUrl referral_code
              createdAt username status hasReferred referrerReferralCode
              badgeIcons {
                organization { icon name url }
              }
            } } }
        """.trimIndent()
        graphql(session, query)
    }

    suspend fun creditGraphql(): JSONObject = withContext(Dispatchers.IO) {
        val session = refreshCsrfToken()
        val query = """
            query credit { customer {
              ledgerCredit: ledger(ledgerCode: "credit") { amount { value currency { code symbol } } }
              ledgerUec: ledger(ledgerCode: "uec") { amount { value currency { code symbol } } }
              ledgerRec: ledger(ledgerCode: "rec") { amount { value currency { code symbol } } }
            } }
        """.trimIndent()
        graphql(session, query)
    }

    /** Read-only referral list query ported from the original Flutter app. */
    suspend fun referralRecruits(
        converted: Boolean,
        campaignId: String,
        page: Int = 1,
        limit: Int = 25,
    ): JSONObject = withContext(Dispatchers.IO) {
        val session = refreshCsrfToken()
        val query = """
            query GetReferralRecruitsList(${ '$' }converted: Boolean!, ${ '$' }limit: Int!, ${ '$' }page: Int!, ${ '$' }campaignId: ID!) {
              referralRecruitsList(query: {converted: ${ '$' }converted, limit: ${ '$' }limit, page: ${ '$' }page, campaignId: ${ '$' }campaignId}) {
                recruitsCount prospectsCount data { id displayName nickname avatar enlistedOn convertedOn }
              }
              referralCountByCampaign(campaignId: ${ '$' }campaignId)
            }
        """.trimIndent()
        executeGraphql(
            query,
            JSONObject()
                .put("converted", converted)
                .put("limit", limit)
                .put("page", page)
                .put("campaignId", campaignId),
            session,
        )
    }

    /** Initial Spectrum friend state. This endpoint is read-only despite using POST. */
    suspend fun spectrumIdentify(): JSONObject = withContext(Dispatchers.IO) {
        val session = refreshCsrfToken()
        authenticatedPost("api/spectrum/auth/identify", JSONObject(), session)
    }

    /**
     * Writes the same presence value used by the official Spectrum client.
     * The app updates its local indicator first, then retries this boundary
     * from the shared status source when RSI is temporarily unavailable.
     */
    suspend fun setSpectrumPresenceStatus(status: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = status.trim().lowercase()
        require(normalized in setOf("online", "away", "do_not_disturb", "playing", "invisible")) {
            "无效的 RSI Spectrum 状态"
        }
        val session = refreshCsrfToken()
        val response = authenticatedPost(
            "api/spectrum/member/presence/setStatus",
            JSONObject().put("status", normalized),
            session,
        )
        val success = response.optInt("success", 0) == 1 ||
            response.optBoolean("success", false) ||
            response.optString("success").equals("1", ignoreCase = true) ||
            response.optString("success").equals("true", ignoreCase = true)
        android.util.Log.i(
            "RefugePresence",
            "Spectrum setStatus response success=$success value=${response.opt("success")}",
        )
        success
    }

    suspend fun storeCatalogPage(page: Int, productIds: List<String>): JSONObject = withContext(Dispatchers.IO) {
        val query = """
            mutation UpdateCatalogQueryMutation(${ '$' }storeFront: String, ${ '$' }query: SearchQuery!) {
              store(name: ${ '$' }storeFront, browse: true) { listing: search(query: ${ '$' }query) {
                resources { id name title subtitle body type
                  media { thumbnail { storeSmall } }
                  nativePrice { amount discounted }
                  price { amount discounted }
                  tags { name }
                  ... on TySku { isWarbond isPackage }
                }
              } }
            }
        """.trimIndent()
        val variables = JSONObject()
            .put("storeFront", "pledge")
            .put("query", JSONObject()
                .put("page", page)
                .put("sort", JSONObject().put("field", "weight").put("direction", "desc"))
                .put("skus", JSONObject().put("products", org.json.JSONArray(productIds))))
        publicGraphql(query, variables)
    }

    /** Read-only ship-upgrade catalogue used for MSRP and selector parity. */
    suspend fun shipUpgradeCatalog(): JSONObject {
        // currentAccountSnapshotKey is account-scoped and therefore prevents
        // a prior user's catalogue from being reused after logout/login.
        val key = currentAccountSnapshotKey()
            ?: session()?.email?.let(::accountSnapshotKey)
            ?: "anonymous"
        return shipUpgradeCatalogRefresh.await(key) {
            withContext(Dispatchers.IO) {
                prepareUpgradeContext()
                val query = """
                    query initShipUpgrade { ships {
                      id name focus type flyableStatus owned msrp link
                      medias { productThumbMediumAndSmall slideShow }
                      manufacturer { id name }
                      skus { id title available price body unlimitedStock availableStock }
                    } }
                """.trimIndent()
                upgradeGraphql(query, JSONObject())
            }
        }
    }

    suspend fun filterShipUpgrades(fromId: Int? = null, toId: Int? = null): JSONObject = withContext(Dispatchers.IO) {
        prepareUpgradeContext()
        val query = """
            query filterShips(${ '$' }fromId: Int, ${ '$' }toId: Int) {
              from(to: ${ '$' }toId) { ships { id } }
              to(from: ${ '$' }fromId) { ships { id skus {
                id price title upgradePrice unlimitedStock showStock available availableStock
              } } }
            }
        """.trimIndent()
        val variables = JSONObject().put("fromId", fromId).put("toId", toId)
        upgradeGraphql(query, variables)
    }

    /** Authenticated, read-only account API boundary used by cache refreshers. */
    internal suspend fun reclaimPledge(body: Map<String, String>, expectedAccount: String): JSONObject = withContext(Dispatchers.IO) {
        check(currentAccountSnapshotKey() == expectedAccount) { "账户已变更，请重新确认" }
        val active = refreshCsrfToken()
        check(currentAccountSnapshotKey() == expectedAccount) { "账户已变更，请重新确认" }
        val request = Request.Builder()
            .url(BASE_URL + "api/account/reclaimPledge")
            .post(JSONObject(body).toString().toRequestBody(jsonType))
            .header("Cookie", cookie(active))
            .header("User-Agent", USER_AGENT)
            .header("Referer", BASE_URL + "account/pledges")
            .header("x-rsi-token", active.token)
            .header("x-rsi-device", active.device)
            .header("x-csrf-token", active.csrf)
            .build()
        // A non-idempotent pledge action must never be replayed after a lost response.
        client.newBuilder().retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
            .build().newCall(request).execute().use { response ->
                check(response.isSuccessful) { "RSI 回收响应未确认（HTTP ${response.code}）" }
                JSONObject(response.body?.string().orEmpty()).also {
                    check(it.opt("success") is Number) { "RSI 未返回回收结果" }
                }
            }
    }

    /** Authenticated, read-only account API boundary used by cache refreshers. */
    suspend fun accountPost(endpoint: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val active = refreshCsrfToken()
        authenticatedPost(endpoint, body, active)
    }

    private fun prepareUpgradeContext() {
        val session = refreshCsrfToken()
        authenticatedPost("api/account/v2/setAuthToken", JSONObject(), session)
        authenticatedPost("api/ship-upgrades/setContextToken", JSONObject(), session)
    }

    private fun authenticatedPost(endpoint: String, body: JSONObject, session: RsiSession): JSONObject {
        val request = Request.Builder()
            .url(BASE_URL + endpoint)
            .post(body.toString().toRequestBody(jsonType))
            .header("Content-Type", jsonType.toString())
            .header("Cookie", cookie(session))
            .header("User-Agent", USER_AGENT)
            .header("Referer", BASE_URL)
            .header("x-rsi-token", session.token)
            .header("x-rsi-device", session.device)
            .header("x-csrf-token", session.csrf)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("RSI 上下文请求失败：${response.code}")
            return JSONObject(response.body?.string().orEmpty().ifBlank { "{}" })
        }
    }

    private fun upgradeGraphql(query: String, variables: JSONObject): JSONObject {
        val session = session() ?: error("需要先登录 RSI")
        val request = Request.Builder()
            .url("https://robertsspaceindustries.com/pledge-store/api/upgrade/graphql")
            .post(JSONObject().put("query", query).put("variables", variables).toString().toRequestBody(jsonType))
            .header("Content-Type", jsonType.toString())
            .header("Cookie", cookie(session))
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://robertsspaceindustries.com/pledge-store/ship-upgrades")
            .header("x-rsi-token", session.token)
            .header("x-rsi-device", session.device)
            .header("x-csrf-token", session.csrf)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("RSI 升级目录请求失败：${response.code}")
            return JSONObject(response.body?.string().orEmpty())
        }
    }

    private fun graphql(session: RsiSession, query: String): JSONObject {
        return executeGraphql(query, JSONObject(), session)
    }

    /** RSI's authenticated GraphQL endpoint requires a token embedded in the home page. */
    private fun refreshCsrfToken(): RsiSession {
        val active = session() ?: error("需要先登录 RSI")
        val request = Request.Builder()
            .url(BASE_URL)
            .get()
            .header("Cookie", cookie(active))
            .header("User-Agent", USER_AGENT)
            .header("Referer", BASE_URL)
            .header("x-rsi-token", active.token)
            .header("x-rsi-device", active.device)
            .header("x-csrf-token", active.csrf)
            .build()
        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("RSI CSRF 刷新失败：${response.code}")
            response.headers.values("Set-Cookie").forEach(::captureCookie)
            response.body?.string().orEmpty()
        }
        val csrf = parseRsiCsrfToken(html) ?: error("RSI 未返回 CSRF 令牌")
        prefs.edit().putString(KEY_CSRF, csrf).apply()
        return active.copy(csrf = csrf)
    }

    private fun publicGraphql(query: String, variables: JSONObject): JSONObject = executeGraphql(query, variables, null)

    private fun executeGraphql(query: String, variables: JSONObject, session: RsiSession?): JSONObject {
        val request = Request.Builder()
            .url(BASE_URL + "graphql")
            .post(JSONObject().put("query", query).put("variables", variables).toString().toRequestBody(jsonType))
            .header("Content-Type", jsonType.toString())
            .header("User-Agent", USER_AGENT)
            .header("Referer", BASE_URL)
        if (session != null) {
            request.header("Cookie", cookie(session))
                .header("x-rsi-token", session.token)
                .header("x-rsi-device", session.device)
                .header("x-csrf-token", session.csrf)
        }
        val built = request.build()
        client.newCall(built).execute().use { response ->
            if (!response.isSuccessful) error("RSI GraphQL 请求失败：${response.code}")
            return JSONObject(response.body?.string().orEmpty())
        }
    }

    private fun request(endpoint: String, body: JSONObject, includeSession: Boolean): JSONObject {
        val builder = Request.Builder()
            .url(BASE_URL + endpoint)
            .post(body.toString().toRequestBody(jsonType))
            .header("Content-Type", jsonType.toString())
            .header("User-Agent", USER_AGENT)
            .header("Referer", BASE_URL)
        if (includeSession) session()?.let { builder.header("Cookie", cookie(it)) }
        if (endpoint.contains("signin")) {
            // RsiApiClient adds these launcher headers on every request after
            // the first response, including the CAPTCHA retry.
            if (pendingToken.isNotBlank()) builder.header("x-rsi-token", pendingToken)
            if (pendingDevice.isNotBlank()) builder.header("x-rsi-device", pendingDevice)
        }
        if (endpoint.endsWith("signin/multiStep")) {
            if (pendingDevice.isNotBlank()) builder.header("x-rsi-device", pendingDevice)
            if (pendingToken.isNotBlank()) builder.header("x-rsi-token", pendingToken)
            // Leave the Cookie header to OkHttp's CookieJar. The challenge
            // cookie returned by the captcha endpoint must be sent together
            // with any launcher cookies; replacing it with a hand-built list
            // was the reason valid answers were rejected.
        }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful && text.isBlank()) error("RSI 登录请求失败：${response.code}")
            response.headers.values("Set-Cookie").forEach(::captureCookie)
            return JSONObject(text.ifBlank { "{}" })
        }
    }

    private fun parseLogin(json: JSONObject, email: String, device: String): RsiLoginResult {
        val code = json.optString("code")
        val message = json.optString("msg").ifBlank { json.optString("message") }.ifBlank { "RSI 登录未完成" }
        val data = json.optJSONObject("data")
        val responseToken = data?.optString("session_id").orEmpty().ifBlank { cookieToken }
        val responseDevice = data?.optString("device_id").orEmpty()
            .ifBlank { cookieDevice }
            .ifBlank { device }
            .ifBlank { pendingDevice }
        if (code == "ErrMultiStepRequired" || code == "ErrCaptchaRequiredLauncher") {
            pendingEmail = email
            pendingDevice = responseDevice
            pendingToken = responseToken
            pendingAuth = cookieAuth
            savePending()
            return if (code == "ErrMultiStepRequired") {
                RsiLoginResult(RsiLoginStep.NEED_CODE, "需要输入 RSI 验证码")
            } else {
                RsiLoginResult(RsiLoginStep.NEED_CAPTCHA, "需要输入验证码")
            }
        }
        if (code == "ErrWrongPassword_email") return RsiLoginResult(RsiLoginStep.FAILED, "邮箱或密码错误")
        if (code == "ErrMaxThrottleLogin") return RsiLoginResult(RsiLoginStep.FAILED, "登录过于频繁，请稍后再试")
        val retryCaptcha = code.equals("ErrCaptchaInvalid", ignoreCase = true) ||
            code.equals("ErrCaptchaIncorrect", ignoreCase = true) ||
            code.equals("ErrCaptchaFailed", ignoreCase = true) ||
            message.contains("captcha", ignoreCase = true) &&
                (message.contains("invalid", true) || message.contains("incorrect", true) || message.contains("wrong", true))
        val success = when (val value = json.opt("success")) {
            is Number -> value.toInt() == 1
            is Boolean -> value
            is String -> value == "1" || value.equals("true", ignoreCase = true)
            else -> false
        } || code == "ErrNoGamePackage"
        if (retryCaptcha) return RsiLoginResult(
            RsiLoginStep.FAILED,
            "图形验证码无效，请重新输入",
            retryCaptcha = true,
        )
        if (!success || data == null && code != "ErrNoGamePackage") return RsiLoginResult(RsiLoginStep.FAILED, message)
        val token = data?.optString("session_id").orEmpty().ifBlank { cookieToken }
        val actualDevice = data?.optString("device_id").orEmpty()
            .ifBlank { cookieDevice }
            .ifBlank { device }
        if (token.isBlank()) return RsiLoginResult(RsiLoginStep.FAILED, "RSI 未返回会话令牌")
        val stored = RsiSession(email, actualDevice, token, cookieAuth)
        prefs.edit()
            .putString(KEY_EMAIL, stored.email)
            .putString(KEY_DEVICE, stored.device)
            .putString(KEY_TOKEN, stored.token)
            .putString(KEY_AUTH, stored.accountAuth)
            .apply()
        allowPersistedSessionCookies = true
        cookieJar.clear()
        pendingEmail = ""
        pendingDevice = ""
        pendingToken = ""
        pendingAuth = ""
        prefs.edit()
            .remove(KEY_PENDING_EMAIL)
            .remove(KEY_PENDING_DEVICE)
            .remove(KEY_PENDING_TOKEN)
            .remove(KEY_PENDING_AUTH)
            .apply()
        return RsiLoginResult(RsiLoginStep.AUTHENTICATED, "登录成功", stored)
    }

    private var cookieToken: String = ""
    private var cookieAuth: String = ""
    private var cookieDevice: String = ""
    private fun captureCookie(value: String) {
        val pair = value.substringBefore(';')
        val key = pair.substringBefore('=').trim()
        val token = pair.substringAfter('=', "").trim()
        when (key.lowercase()) {
            "rsi-token" -> cookieToken = token
            "rsi-account-auth" -> cookieAuth = token
            "_rsi_device" -> cookieDevice = token
        }
    }

    private fun cookie(session: RsiSession): String = buildString {
        append("_rsi_device=").append(session.device).append(';')
        append("Rsi-Token=").append(session.token).append(';')
        if (session.accountAuth.isNotBlank()) append("Rsi-Account-Auth=").append(session.accountAuth).append(';')
    }

    private fun savePending() = prefs.edit()
        .putString(KEY_PENDING_EMAIL, pendingEmail)
        .putString(KEY_PENDING_DEVICE, pendingDevice)
        .putString(KEY_PENDING_TOKEN, pendingToken)
        .putString(KEY_PENDING_AUTH, pendingAuth)
        .apply()

    companion object {
        private const val BASE_URL = "https://robertsspaceindustries.com/"
        private const val USER_AGENT = "Mozilla/5.0 (Android) AppleWebKit/537.36 Chrome/122 Safari/537.36"
        private const val KEY_EMAIL = "email"
        private const val KEY_LOGIN_EMAIL_DRAFT = "login_email_draft"
        private const val KEY_DEVICE = "device"
        private const val KEY_TOKEN = "token"
        private const val KEY_AUTH = "account_auth"
        private const val KEY_CSRF = "csrf"
        private const val KEY_PENDING_EMAIL = "pending_email"
        private const val KEY_PENDING_DEVICE = "pending_device"
        private const val KEY_PENDING_TOKEN = "pending_token"
        private const val KEY_PENDING_AUTH = "pending_auth"
    }
}

private class RsiCookieJar(
    private val sessionProvider: () -> RsiSession?,
) : CookieJar {
    private val jar = linkedMapOf<String, Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie ->
            val key = "${cookie.domain};${cookie.path};${cookie.name.lowercase()}"
            if (cookie.expiresAt < System.currentTimeMillis()) jar.remove(key)
            else jar[key] = cookie
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        rsiRequestCookies(url, sessionProvider(), jar.values.toList())

    @Synchronized
    fun clear() = jar.clear()
}

/** OkHttp replaces an explicit Cookie header when CookieJar returns any cookies.
 * Include persisted authentication here so a CSRF/challenge cookie cannot hide it. */
internal fun rsiRequestCookies(url: HttpUrl, session: RsiSession?, received: List<Cookie>): List<Cookie> {
    val cookies = received.filter { it.matches(url) && it.expiresAt > System.currentTimeMillis() }.associateBy { it.name }.toMutableMap()
    if (url.host == "robertsspaceindustries.com" || url.host.endsWith(".robertsspaceindustries.com")) {
        session?.let { value ->
            mapOf("_rsi_device" to value.device, "Rsi-Token" to value.token, "Rsi-Account-Auth" to value.accountAuth)
                .filterValues { it.isNotBlank() }.forEach { (name, token) ->
                    Cookie.parse(url, "$name=$token; Path=/; Secure")?.let { cookies[name] = it }
                }
        }
    }
    return cookies.values.toList()
}

internal fun parseRsiCsrfToken(html: String): String? {
    val metaTags = Regex("(?is)<meta\\b[^>]*>").findAll(html)
    for (tag in metaTags) {
        val attributes = Regex(
            """(?is)([a-z_:][-a-z0-9_:.]*)\s*=\s*(["'])(.*?)\2""",
        ).findAll(tag.value).associate { match ->
            match.groupValues[1].lowercase() to decodeRsiHtmlAttribute(match.groupValues[3])
        }
        if (attributes["name"]?.equals("csrf-token", ignoreCase = true) == true) {
            return attributes["content"]?.trim()?.takeIf(String::isNotBlank)
        }
    }
    return null
}

private fun decodeRsiHtmlAttribute(value: String): String = value
    .replace("&quot;", "\"", ignoreCase = true)
    .replace("&#39;", "'", ignoreCase = true)
    .replace("&apos;", "'", ignoreCase = true)
    .replace("&amp;", "&", ignoreCase = true)
