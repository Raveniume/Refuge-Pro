package com.refuge.next.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import java.util.concurrent.TimeUnit

internal data class AppRelease(
    val version: String,
    val pageUrl: String,
    val notes: String,
    val apkUrl: String?,
)

internal class GitHubUpdateRepository(context: Context) {
    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun latestRelease(): AppRelease = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/Raveniume/Refuge-Pro/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "RefugeNext-Android")
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "GitHub 更新检查失败（${response.code}）" }
            val payload = JSONObject(response.body?.string() ?: error("GitHub 返回内容为空"))
            val version = payload.optString("tag_name").trim().removePrefix("v").removePrefix("V")
            require(version.matches(Regex("\\d+\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?"))) {
                "Release 版本号无效"
            }
            require(version.isNotBlank()) { "Release 没有版本号" }
            val pageUrl = payload.optString("html_url")
            require(pageUrl.startsWith("https://github.com/Raveniume/Refuge-Pro/releases/")) {
                "Release 地址无效"
            }
            val assets = payload.optJSONArray("assets")
            val apkUrl = (0 until (assets?.length() ?: 0))
                .mapNotNull { assets?.optJSONObject(it) }
                .firstOrNull {
                    val name = it.optString("name")
                    name.endsWith(".apk", ignoreCase = true) &&
                        (it.optString("content_type").isBlank() ||
                            it.optString("content_type") == "application/vnd.android.package-archive")
                }
                ?.optString("browser_download_url")
                ?.takeIf { it.startsWith("https://github.com/Raveniume/Refuge-Pro/releases/download/") }
            AppRelease(version, pageUrl, payload.optString("body"), apkUrl)
        }
    }

    suspend fun download(release: AppRelease): File = withContext(Dispatchers.IO) {
        val url = requireNotNull(release.apkUrl) { "此版本没有 APK 安装包" }
        require(url.startsWith("https://github.com/Raveniume/Refuge-Pro/releases/download/")) {
            "APK 下载地址无效"
        }
        val directory = File(appContext.cacheDir, "updates").apply { mkdirs() }
        val target = File(directory, "refugenext-${release.version}.apk")
        if (target.isFile && target.isValidApk()) {
            return@withContext target
        }
        val temporary = File(directory, "${target.name}.part")
        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "RefugeNext-Android")
                .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "APK 下载失败（${response.code}）" }
                val body = response.body ?: error("APK 响应为空")
                body.byteStream().use { input -> FileOutputStream(temporary).use(input::copyTo) }
            }
            require(temporary.isValidApk()) { "下载文件不是有效的 APK" }
            if (target.exists()) target.delete()
            check(temporary.renameTo(target)) { "无法保存 APK 文件" }
            target
        } finally {
            temporary.delete()
        }
    }
}

private fun File.isValidApk(): Boolean {
    if (!isFile || length() < 4) return false
    return runCatching {
        ZipFile(this).use { it.getEntry("AndroidManifest.xml") != null }
    }.getOrDefault(false)
}

internal fun isNewerRelease(candidate: String, current: String): Boolean {
    fun parts(value: String) = value.trimStart('v', 'V').split('.', '-', '+')
        .map { it.toIntOrNull() ?: 0 }
    val left = parts(candidate)
    val right = parts(current)
    for (index in 0 until maxOf(left.size, right.size)) {
        val comparison = (left.getOrElse(index) { 0 }).compareTo(right.getOrElse(index) { 0 })
        if (comparison != 0) return comparison > 0
    }
    return false
}
