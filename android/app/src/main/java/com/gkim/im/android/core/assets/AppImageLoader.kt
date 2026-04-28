package com.gkim.im.android.core.assets

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

// R1.4 — singleton ImageLoader configuration.
//
// Cache sizes are extracted as named constants so the contract can be
// asserted by AppImageLoaderTest without spinning up Robolectric.
//
// The singleton is wired through GkimApplication implementing
// coil.ImageLoaderFactory; any AsyncImage call resolves it automatically
// via coil.Coil.imageLoader(context).

object AppImageLoaderConfig {
    const val MemorySizePercent: Double = 0.20
    const val DiskCacheSizeBytes: Long = 256L * 1024L * 1024L
    const val DiskCacheDirName: String = "skins"
}

fun buildAppImageLoader(context: Context): ImageLoader {
    val appContext = context.applicationContext
    return ImageLoader.Builder(appContext)
        .memoryCache {
            MemoryCache.Builder(appContext)
                .maxSizePercent(AppImageLoaderConfig.MemorySizePercent)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(appContext.cacheDir.resolve(AppImageLoaderConfig.DiskCacheDirName))
                .maxSizeBytes(AppImageLoaderConfig.DiskCacheSizeBytes)
                .build()
        }
        .crossfade(true)
        .build()
}
