package com.xperiencemusic.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.xperiencemusic.shared.di.initKoin
import com.xperiencemusic.shared.navigation.XperienceMusicApp

fun MainViewController() = ComposeUIViewController(configure = { initKoin() }) { XperienceMusicApp() }
