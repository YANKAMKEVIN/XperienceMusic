package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.repository.AudioUrlProvider
import com.xperiencemusic.shared.domain.repository.TempFileWriter
import com.xperiencemusic.shared.resources.Res

/**
 * Demo mode: files uploaded during the session play back from a temp copy; the sample songs,
 * which have no real audio, all play the bundled demo clip.
 */
class DemoAudioUrlProvider(
    private val storage: InMemoryAudioStorage,
    private val tempFiles: TempFileWriter,
) : AudioUrlProvider {
    override suspend fun playableUrl(audioPath: String): String {
        val uploaded = storage.bytes(audioPath)
        return if (uploaded != null) {
            tempFiles.write(audioPath.replace('/', '_'), uploaded)
        } else {
            Res.getUri(DEMO_CLIP)
        }
    }

    private companion object {
        const val DEMO_CLIP = "files/demo-recording.m4a"
    }
}
