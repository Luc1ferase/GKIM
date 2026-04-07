package com.gkim.im.android

import android.app.Application
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.DefaultAppContainer

class GkimApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
