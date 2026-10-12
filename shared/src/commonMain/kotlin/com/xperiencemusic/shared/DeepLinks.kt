package com.xperiencemusic.shared

import com.xperiencemusic.shared.domain.usecase.SignInLinkInbox

/** One inbox for the whole app, reachable before Koin starts. */
internal val signInLinkInbox = SignInLinkInbox()

/** Called by Android's MainActivity and iOS's ContentView with the URL the app was opened with. */
fun openDeepLink(url: String) {
    signInLinkInbox.receive(url)
}
