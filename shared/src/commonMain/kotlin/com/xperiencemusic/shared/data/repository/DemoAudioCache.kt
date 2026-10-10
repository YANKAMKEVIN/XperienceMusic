package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.repository.AudioCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Demo mode: all audio already lives on the phone, so "downloading" only marks it available. */
class DemoAudioCache : AudioCache {
    private val _cachedPaths = MutableStateFlow<Set<String>>(emptySet())
    override val cachedPaths: StateFlow<Set<String>> = _cachedPaths.asStateFlow()

    override fun localUrl(audioPath: String): String? = null

    override suspend fun download(audioPath: String) {
        _cachedPaths.update { it + audioPath }
    }

    override suspend fun clear() {
        _cachedPaths.value = emptySet()
    }
}
