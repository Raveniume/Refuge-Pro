package com.refuge.next.data

import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RepositoryRefreshTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun callerCancellationDoesNotCancelRepositoryRefresh() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val refresh = RepositoryRefresh<Int>(scope = scope)
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Int>()

        val caller = async {
            refresh.await("account-a") {
                started.complete(Unit)
                release.await()
                7.also { finished.complete(it) }
            }
        }
        started.await()
        caller.cancelAndJoin()
        release.complete(Unit)

        assertEquals(7, withTimeout(2_000) { finished.await() })
        scope.cancel()
    }

    @Test
    fun activeRefreshIsSharedButNextPageOpenCanRefreshAgain() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val calls = AtomicInteger()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val refresh = RepositoryRefresh<Int>(scope = scope)
        val first = async {
            refresh.await("global") {
                calls.incrementAndGet()
                started.complete(Unit)
                release.await()
                11
            }
        }
        started.await()
        val second = async(start = CoroutineStart.UNDISPATCHED) { refresh.await("global") { calls.incrementAndGet() } }
        release.complete(Unit)

        assertEquals(11, first.await())
        assertEquals(11, second.await())
        assertEquals(12, refresh.await("global") { calls.incrementAndGet() + 10 })
        assertEquals(2, calls.get())
        scope.cancel()
    }

    @Test
    fun minimumIntervalDebouncesDuplicateLoadsUnlessKeyChanges() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val now = AtomicLong(1_000)
        val calls = AtomicInteger()
        val refresh = RepositoryRefresh<Int>(
            minimumIntervalMillis = 2_000,
            scope = scope,
            nowMillis = now::get,
        )

        assertEquals(1, refresh.await("2026-09-04") { calls.incrementAndGet() })
        now.set(2_500)
        assertEquals(1, refresh.await("2026-09-04") { calls.incrementAndGet() })
        assertEquals(2, refresh.await("2026-09-05") { calls.incrementAndGet() })
        assertEquals(2, calls.get())
        scope.cancel()
    }

    @Test
    fun accountSnapshotNamesAreStablePrivateAndIsolated() {
        val first = accountSnapshotKey(" Pilot@Example.com ")
        val same = accountSnapshotKey("pilot@example.com")
        val second = accountSnapshotKey("other@example.com")

        assertEquals(first, same)
        assertNotEquals(first, second)
        assertFalse(first.contains("pilot", ignoreCase = true))
        assertEquals(
            File("cache", "profile-$first.json"),
            accountSnapshotFile(File("cache"), "profile", first),
        )
    }

    @Test
    fun legacySnapshotIsMigratedOnlyToItsInferredAccount() {
        val filesDir = temporaryFolder.newFolder()
        val ownerKey = accountSnapshotKey("owner@example.com")
        val otherKey = accountSnapshotKey("other@example.com")
        File(filesDir, "profile.json").writeText("""{"email":"owner@example.com","value":"legacy"}""")
        File(filesDir, "profile-$ownerKey.json").writeText("""{"email":"owner@example.com","value":""}""")
        val cache = AccountSnapshotCache(
            filesDir = filesDir,
            stem = "profile",
            read = { file -> Regex("\"value\"\\s*:\\s*\"([^\"]*)\"").find(file.readText())?.groupValues?.get(1) },
            isMeaningful = String::isNotBlank,
        )

        assertEquals("legacy", cache.get(ownerKey))
        assertEquals("legacy", Regex("\"value\"\\s*:\\s*\"([^\"]*)\"").find(cache.file(ownerKey).readText())?.groupValues?.get(1))
        assertEquals(ownerKey, File(filesDir, "account_snapshots.legacy-owner").readText())
        assertEquals(null, cache.get(otherKey))
    }

    @Test
    fun unscopedSnapshotWithoutIdentityIsNotClaimed() {
        val filesDir = temporaryFolder.newFolder()
        val firstKey = accountSnapshotKey("first@example.com")
        val secondKey = accountSnapshotKey("second@example.com")
        File(filesDir, "hangar_items.json").writeText("legacy inventory")
        val cache = AccountSnapshotCache(filesDir, "hangar_items", File::readText, String::isNotBlank)

        assertEquals(null, cache.get(firstKey))
        assertEquals(null, cache.get(secondKey))
        assertFalse(File(filesDir, "account_snapshots.legacy-owner").exists())
    }

    @Test
    fun snapshotPeekNeverReadsDiskButAwaitPublishesItToMemory() = runBlocking {
        val filesDir = temporaryFolder.newFolder()
        val accountKey = accountSnapshotKey("owner@example.com")
        val reads = AtomicInteger()
        File(filesDir, "profile-$accountKey.json").writeText("cached profile")
        val cache = AccountSnapshotCache(
            filesDir = filesDir,
            stem = "profile",
            read = { file -> reads.incrementAndGet(); file.readText() },
            isMeaningful = String::isNotBlank,
        )

        assertEquals(null, cache.peek(accountKey))
        assertEquals(0, reads.get())
        assertEquals("cached profile", cache.await(accountKey))
        assertEquals("cached profile", cache.peek(accountKey))
        assertEquals(1, reads.get())
    }

    @Test
    fun neutralProfileRefreshRetainsMeaningfulSnapshot() {
        val previous = ProfileData(
            handle = "pilot",
            organizationName = "Pathfinder Fleet",
            totalSpent = "\$250",
            registerDate = "2020-01-02",
            email = "old@example.com",
            isAuthenticated = true,
        )
        val neutral = ProfileData(
            handle = "pilot",
            email = "new@example.com",
            isAuthenticated = true,
        )

        val retained = retainMeaningfulProfileSnapshot(previous, neutral)

        assertEquals("Pathfinder Fleet", retained.organizationName)
        assertEquals("\$250", retained.totalSpent)
        assertEquals("2020-01-02", retained.registerDate)
        assertEquals("new@example.com", retained.email)
    }

    @Test
    fun meaningfulProfileRefreshCanReplaceOlderSnapshot() {
        val previous = ProfileData(organizationName = "Old Fleet", totalSpent = "\$250")
        val refreshed = ProfileData(handle = "pilot", totalSpent = "\$300", isAuthenticated = true)

        val retained = retainMeaningfulProfileSnapshot(previous, refreshed)
        assertEquals("\$300", retained.totalSpent)
        assertEquals("Old Fleet", retained.organizationName)
    }

    @Test
    fun beijingMidnightChangesStoreDateAndForcesRefresh() {
        val beforeMidnight = Instant.parse("2026-09-04T15:59:59Z")
        val afterMidnight = Instant.parse("2026-09-04T16:00:00Z")

        assertEquals(LocalDate.parse("2026-09-04"), storeSnapshotDate(beforeMidnight))
        assertEquals(LocalDate.parse("2026-09-05"), storeSnapshotDate(afterMidnight))
        assertFalse(shouldForceStoreRefresh(LocalDate.parse("2026-09-05"), afterMidnight))
        assertEquals(true, shouldForceStoreRefresh(LocalDate.parse("2026-09-04"), afterMidnight))
        assertEquals(true, shouldForceStoreRefresh(null, afterMidnight))
    }
}
