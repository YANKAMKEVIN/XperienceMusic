package com.xperiencemusic.shared.domain.model

/** [isDemo]: no backend configured, data is local and the sign-in code is fixed. */
data class AppEnvironment(val isDemo: Boolean, val demoCode: String?)
