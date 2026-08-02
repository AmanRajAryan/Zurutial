package aman.zurutial.media

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object VideoCache {
    private var cache: SimpleCache? = null

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Synchronized
    fun getInstance(context: Context): SimpleCache {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "media_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024) // 500 MB max
            val databaseProvider = StandaloneDatabaseProvider(context)
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return cache!!
    }
}
