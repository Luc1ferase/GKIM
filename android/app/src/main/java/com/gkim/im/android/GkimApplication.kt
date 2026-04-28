package com.gkim.im.android

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.gkim.im.android.core.assets.buildAppImageLoader
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.DefaultAppContainer

class GkimApplication : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }

    // R1.4 — Coil resolves this singleton lazily on first AsyncImage call.
    override fun newImageLoader(): ImageLoader = buildAppImageLoader(this)
}
