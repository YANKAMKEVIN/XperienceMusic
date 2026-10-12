package com.xperiencemusic.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xperiencemusic.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import com.xperiencemusic.shared.navigation.XperienceMusicApp
import com.xperiencemusic.shared.openDeepLink

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The UI is dark-only: keep light system-bar icons whatever the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        initKoin { androidContext(applicationContext) }
        // Only a fresh launch: a recreated activity, or one reopened from the recent apps, would
        // replay the link it was first started with (an old address and code).
        val fromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !fromHistory) consumeLink(intent)
        setContent { XperienceMusicApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeLink(intent)
    }

    /** Hands the link to the app once, then forgets it. */
    private fun consumeLink(intent: Intent) {
        intent.dataString?.let(::openDeepLink)
        setIntent(Intent(intent).setData(null))
    }
}
