package com.gkim.im.android.core.assets

import org.junit.Assert.assertEquals
import org.junit.Test

class AppImageLoaderTest {

    @Test
    fun `memory cache size percent matches design contract`() {
        // design.md pins the memory cache at 20 % of available; build()
        // reads this constant so the loader and the contract stay in sync.
        assertEquals(0.20, AppImageLoaderConfig.MemorySizePercent, 0.0001)
    }

    @Test
    fun `disk cache size matches design contract`() {
        // design.md pins the disk cache at 256 MB.
        assertEquals(256L * 1024L * 1024L, AppImageLoaderConfig.DiskCacheSizeBytes)
    }

    @Test
    fun `disk cache directory is the skins subdirectory`() {
        // design.md pins the directory name to `skins` under cacheDir;
        // versioned key naming means we never need to invalidate within
        // this directory.
        assertEquals("skins", AppImageLoaderConfig.DiskCacheDirName)
    }
}
