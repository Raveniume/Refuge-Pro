package com.refuge.next.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateRepositoryTest {
    @Test
    fun newerReleaseComparisonHandlesTagsAndPatchVersions() {
        assertTrue(isNewerRelease("v0.3.4", "0.3.3"))
        assertTrue(isNewerRelease("0.4.0", "0.3.9"))
        assertFalse(isNewerRelease("0.3.3", "0.3.3"))
        assertFalse(isNewerRelease("0.3.2", "0.3.3"))
    }
}
