package com.xperiencemusic.app

import android.app.Application
import com.xperiencemusic.shared.di.initKoin
import org.koin.android.ext.koin.androidContext

/**
 * Starts Koin with the process, before any activity, service or receiver: Android can restart the
 * playback service on its own (media resumption, a killed process), without the activity.
 */
class XperienceMusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@XperienceMusicApplication) }
    }
}
