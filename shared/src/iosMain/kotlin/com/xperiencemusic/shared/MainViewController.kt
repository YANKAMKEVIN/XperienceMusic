package com.xperiencemusic.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.xperiencemusic.shared.di.initKoin
import com.xperiencemusic.shared.navigation.XperienceMusicApp

@Suppress("FunctionNaming") // the name is the Swift entry point
fun MainViewController() = ComposeUIViewController(configure = { initKoin() }) { XperienceMusicApp() }
