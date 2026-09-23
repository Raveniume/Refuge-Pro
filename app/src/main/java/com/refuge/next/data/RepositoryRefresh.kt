package com.refuge.next.data

import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Owns refresh work independently from a screen coroutine. A cancelled caller
 * stops waiting, but the in-flight load can still finish and persist its cache.
 */
internal class RepositoryRefresh<T>(
    minimumIntervalMillis: Long = 0L,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private data class Completed<T>(val value: T, val atMillis: Long)

    private val minimumIntervalMillis = minimumIntervalMillis.coerceAtLeast(0L)
    private val mutex = Mutex()
    private val active = mutableMapOf<String, Deferred<T>>()
    private val completed = mutableMapOf<String, Completed<T>>()

    suspend fun await(
        key: String,
        force: Boolean = false,
        load: suspend () -> T,
    ): T {
        val task = mutex.withLock {
            active[key]?.takeIf { it.isActive }
                ?: completed[key]
                    ?.takeIf { !force && nowMillis() - it.atMillis < minimumIntervalMillis }
                    ?.let { CompletableDeferred(it.value) }
                ?: scope.async {
                    val value = load()
                    mutex.withLock {
                        completed[key] = Completed(value, nowMillis())
                    }
                    value
                }.also { created ->
                    active[key] = created
                    created.invokeOnCompletion {
                        scope.launch {
                            mutex.withLock {
                                if (active[key] === created) active.remove(key)
                            }
                        }
                    }
                }
        }
        return task.await()
    }

    fun start(
        key: String,
        force: Boolean = false,
        load: suspend () -> T,
    ) {
        scope.launch {
            runCatching { await(key, force, load) }
        }
    }
}

internal class AccountSnapshotCache<T : Any>(
    private val filesDir: File,
    private val stem: String,
    private val read: (File) -> T?,
    private val isMeaningful: (T) -> Boolean = { true },
    private val mergeLegacyWithScoped: ((legacy: T, scoped: T) -> T)? = null,
) {
    private val memory = java.util.concurrent.ConcurrentHashMap<String, T>()

    /** Non-blocking view for state initialization on the Compose thread. */
    fun peek(accountKey: String): T? = memory[accountKey]

    /**
     * Return the in-memory snapshot when available, otherwise hydrate the
     * already-written account file once.  Account snapshots are deliberately
     * small (the largest account projection is the hangar list), and this
     * fallback is only used by the first synchronous composition; subsequent
     * calls are a plain map lookup.  Refresh/network paths continue to use
     * [await] so they never parse JSON on the main dispatcher.
     */
    fun peekOrLoad(accountKey: String): T? = memory[accountKey] ?: get(accountKey)

    /** Start disk hydration before a route is composed without blocking it. */
    fun preload(accountKey: String, scope: CoroutineScope) {
        if (memory.containsKey(accountKey)) return
        scope.launch(Dispatchers.IO) { get(accountKey) }
    }

    /** Reads and memoizes the account snapshot on the repository IO dispatcher. */
    suspend fun await(accountKey: String): T? = withContext(Dispatchers.IO) {
        get(accountKey)
    }

    fun get(accountKey: String): T? {
        memory[accountKey]?.let { return it }
        val loaded = synchronized(legacyMigrationLock) {
            val scopedFile = file(accountKey)
            val scoped = if (scopedFile.isFile) runCatching { read(scopedFile) }.getOrNull() else null
            val legacy = legacySnapshot(accountKey)
            when {
                scoped != null && isMeaningful(scoped) && legacy != null && isMeaningful(legacy) ->
                    mergeLegacyWithScoped?.invoke(legacy, scoped) ?: scoped
                scoped != null && isMeaningful(scoped) -> scoped
                legacy != null && isMeaningful(legacy) -> legacy.also {
                    runCatching { legacyFile.copyTo(scopedFile, overwrite = true) }
                }
                else -> scoped
            }
        } ?: return null
        return memory.putIfAbsent(accountKey, loaded) ?: loaded
    }

    fun put(accountKey: String, value: T) {
        memory[accountKey] = value
    }

    fun file(accountKey: String): File = accountSnapshotFile(filesDir, stem, accountKey)

    private val legacyFile: File
        get() = File(filesDir, "$stem.json")

    private fun legacySnapshot(accountKey: String): T? {
        if (!legacyFile.isFile) return null
        val ownerFile = File(filesDir, LEGACY_OWNER_FILE)
        val storedOwner = runCatching { ownerFile.readText().trim() }
            .getOrNull()
            ?.takeIf(::isAccountSnapshotKey)
        val inferredOwner = inferredLegacyOwnerKey()
        val owner = storedOwner ?: inferredOwner ?: return null
        if (owner != accountKey) return null
        if (storedOwner == null) {
            runCatching { ownerFile.writeText(owner) }
        }
        return runCatching { read(legacyFile) }.getOrNull()
    }

    private fun inferredLegacyOwnerKey(): String? = runCatching {
        File(filesDir, "profile.json")
            .takeIf(File::isFile)
            ?.readText()
            ?.let { raw ->
                Regex("\"email\"\\s*:\\s*\"([^\"]+)\"")
                    .find(raw)
                    ?.groupValues
                    ?.getOrNull(1)
            }
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let(::accountSnapshotKey)
    }.getOrNull()

    private companion object {
        const val LEGACY_OWNER_FILE = "account_snapshots.legacy-owner"
        val legacyMigrationLock = Any()

        fun isAccountSnapshotKey(value: String): Boolean =
            value.length == 24 && value.all { it in '0'..'9' || it in 'a'..'f' }
    }
}

internal fun RsiAuthRepository.currentAccountSnapshotKey(): String? =
    session()?.takeIf(RsiSession::isAuthenticated)?.email?.let(::accountSnapshotKey)

internal fun accountSnapshotKey(email: String): String {
    val normalized = email.trim().lowercase(Locale.US)
    require(normalized.isNotEmpty()) { "Account email is required for scoped cache data" }
    val digest = MessageDigest.getInstance("SHA-256")
        .digest("refuge-account-cache-v1:$normalized".toByteArray(Charsets.UTF_8))
    return buildString(24) {
        for (byte in digest.take(12)) append("%02x".format(Locale.US, byte.toInt() and 0xff))
    }
}

internal fun accountSnapshotFile(filesDir: File, stem: String, accountKey: String): File =
    File(filesDir, "$stem-$accountKey.json")
