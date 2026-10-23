package com.refuge.next.material

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** One private, replaceable evidence directory. Never export account stores. */
internal fun evidenceFile(name: String): File =
    File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "qa")
        .apply { mkdirs() }.resolve(name)
