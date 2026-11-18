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

    /**
     * The id of the video [url] points at, read from the usual shapes (`youtu.be/ID`,
     * `watch?v=ID`, `/shorts/ID`, `/embed/ID`, `/live/ID`); null for a channel, a playlist or a search.
     */
    fun videoId(url: String): String? {
        val address = normalize(url) ?: return null
        val host = address.substringAfter("://").substringBefore('/').lowercase()
        val path = address.substringAfter("://").substringAfter('/', "").substringBefore('?').substringBefore('#')
        val query = address.substringAfter('?', "").substringBefore('#')
        val segments = path.split('/').filter { it.isNotEmpty() }
        val id = when {
            host == "youtu.be" -> segments.firstOrNull()
            segments.firstOrNull() == "watch" ->
                query.split('&').firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
            segments.firstOrNull() in pathsWithId -> segments.getOrNull(1)
            else -> null
        }
        return id?.takeIf { idPattern.matches(it) }
    }

    /** The picture YouTube shows for the video of [url]; it exists for every video. */
    fun thumbnailUrl(url: String): String? = videoId(url)?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }

    private val pathsWithId = setOf("shorts", "embed", "live", "v")
    private val idPattern = Regex("[A-Za-z0-9_-]{6,20}")
}

val Song.youtubeUrl: String? get() = links.firstOrNull { it.label == YouTubeLink.LABEL }?.url
