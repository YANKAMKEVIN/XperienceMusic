package com.xperiencemusic.shared.domain.model

/**
 * The YouTube video of a song: the original the band listens to, as in the WhatsApp setlist
 * messages it replaces. Stored as the song's link labelled [LABEL].
 */
object YouTubeLink {
    const val LABEL = "YouTube"

    private val hosts = setOf("youtu.be", "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com")

    /**
     * The address to store: trimmed, with `https://` added when it was pasted without it. Null for
     * a blank input (no link) and for anything that is not a YouTube address.
     */
    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val url = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        val scheme = url.substringBefore("://").lowercase()
        val host = url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#').lowercase()
        return url.takeIf { scheme in setOf("http", "https") && host in hosts && url.length > scheme.length + 3 + host.length + 1 }
    }
}

val Song.youtubeUrl: String? get() = links.firstOrNull { it.label == YouTubeLink.LABEL }?.url
