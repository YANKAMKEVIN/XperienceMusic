package com.xperiencemusic.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        // Test builds only: the screen stays on while the app is in front, so the phone does not lock
        // in the middle of a QA session. Release builds keep the normal screen timeout.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
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
