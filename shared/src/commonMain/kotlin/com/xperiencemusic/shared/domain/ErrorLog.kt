package com.xperiencemusic.shared.domain

/**
 * Where unexpected failures are reported, so they are not silently turned into a generic message.
 * `println` reaches logcat (tag System.out) on Android and the Xcode console on iOS.
 */
internal object ErrorLog {
    var sink: (message: String, cause: Throwable) -> Unit = { message, cause ->
        println("XperienceMusic: $message\n${cause.stackTraceToString()}")
    }

    fun unexpected(message: String, cause: Throwable) = sink(message, cause)
}
