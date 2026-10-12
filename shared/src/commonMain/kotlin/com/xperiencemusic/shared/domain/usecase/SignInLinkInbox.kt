package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.SignInLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the sign-in link the phone opened the app with until the sign-in screen takes it. It can
 * arrive before the screen exists (cold start from the e-mail).
 */
class SignInLinkInbox {
    private val _link = MutableStateFlow<SignInLink?>(null)
    val link: StateFlow<SignInLink?> = _link.asStateFlow()

    /** Returns false when [url] is not a sign-in link. */
    fun receive(url: String): Boolean {
        val link = SignInLink.parse(url) ?: return false
        _link.value = link
        return true
    }

    fun consume() {
        _link.value = null
    }
}
