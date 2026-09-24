package com.refuge.next

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil3.ImageLoader
import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The emulator's GL bridge serializes hardware bitmap uploads. Keeping
        // decode work bounded prevents a burst of catalog thumbnails from
        // starving the first Compose traversal and triggering an input ANR.
        // Instrumentation and activity recreation can enter this callback
        // after Coil has already resolved its process singleton. Reusing the
        // existing loader is safe; a second setSafe call is not.
        runCatching {
            SingletonImageLoader.setSafe { context ->
                ImageLoader.Builder(context)
                    .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(2))
                    .decoderCoroutineContext(Dispatchers.IO.limitedParallelism(1))
                    .build()
            }
        }
        setContent { RefugeApp() }
    }
}
