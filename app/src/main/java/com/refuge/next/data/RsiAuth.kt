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
import java.util.UUID

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
    /**
     * RSI's image-CAPTCHA flow is stateful: the CAPTCHA response establishes a
     * short-lived challenge cookie that must be sent with the following sign-in
     * request. Keep those cookies in the same in-memory client session rather
     * than relying on a new request having no cookie context.
     */
    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            private val jar = linkedMapOf<String, Cookie>()

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookies.forEach { cookie ->
                    val key = "${cookie.domain};${cookie.path};${cookie.name}"
                    if (cookie.expiresAt < System.currentTimeMillis()) jar.remove(key)
                    else jar[key] = cookie
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> =
                jar.values.filter { it.matches(url) }
        })
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private var pendingEmail: String = prefs.getString(KEY_PENDING_EMAIL, "") ?: ""
    private var pendingDevice: String = prefs.getString(KEY_PENDING_DEVICE, "") ?: ""
    private var pendingToken: String = prefs.getString(KEY_PENDING_TOKEN, "") ?: ""
    private var pendingAuth: String = prefs.getString(KEY_PENDING_AUTH, "") ?: ""

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
            val device = pendingDevice.ifBlank { UUID.randomUUID().toString().replace("-", "").take(24) }
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
        val session = session() ?: error("需要先登录 RSI")
        val query = """
            query account { account { isAnonymous ... on RsiAuthenticatedAccount {
              avatar displayname email hasGamePackage nickname profileUrl referral_code
              createdAt username status
            } } }
        """.trimIndent()
        graphql(session, query)
    }

    suspend fun storeCatalogPage(page: Int): JSONObject = withContext(Dispatchers.IO) {
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
                .put("skus", JSONObject().put("products", org.json.JSONArray())))
        publicGraphql(query, variables)
    }

    private fun graphql(session: RsiSession, query: String): JSONObject {
        return executeGraphql(query, JSONObject(), session)
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
            if (pendingDevice.isNotBlank() || pendingToken.isNotBlank() || pendingAuth.isNotBlank()) {
                builder.header("Cookie", buildString {
                    if (pendingDevice.isNotBlank()) append("_rsi_device=").append(pendingDevice).append(';')
                    if (pendingToken.isNotBlank()) append("Rsi-Token=").append(pendingToken).append(';')
                    if (pendingAuth.isNotBlank()) append("Rsi-Account-Auth=").append(pendingAuth).append(';')
                })
            }
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
        val responseDevice = data?.optString("device_id").orEmpty().ifBlank { device }
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
        val success = json.optInt("success", 0) == 1 || code == "ErrNoGamePackage"
        if (!success || data == null && code != "ErrNoGamePackage") return RsiLoginResult(RsiLoginStep.FAILED, message)
        val token = data?.optString("session_id").orEmpty().ifBlank { cookieToken }
        val actualDevice = data?.optString("device_id").orEmpty().ifBlank { device }
        if (token.isBlank()) return RsiLoginResult(RsiLoginStep.FAILED, "RSI 未返回会话令牌")
        val stored = RsiSession(email, actualDevice, token, cookieAuth)
        prefs.edit()
            .putString(KEY_EMAIL, stored.email)
            .putString(KEY_DEVICE, stored.device)
            .putString(KEY_TOKEN, stored.token)
            .putString(KEY_AUTH, stored.accountAuth)
            .apply()
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
    private fun captureCookie(value: String) {
        val pair = value.substringBefore(';')
        val key = pair.substringBefore('=').trim()
        val token = pair.substringAfter('=', "").trim()
        when (key) {
            "Rsi-Token" -> cookieToken = token
            "Rsi-Account-Auth" -> cookieAuth = token
            "_rsi_device" -> if (pendingDevice.isBlank()) pendingDevice = token
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
