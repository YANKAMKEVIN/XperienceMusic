package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.shared.domain.model.SingTarget
import com.xperiencemusic.shared.domain.model.SingingProgress
import com.xperiencemusic.shared.domain.usecase.PlayLearningExampleUseCase
import com.xperiencemusic.shared.domain.usecase.PlayReferenceNoteUseCase
import com.xperiencemusic.shared.domain.usecase.TrackSingingUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Listens to a sung activity: the microphone, and the other voice played meanwhile for a harmony. */
class SingingSessionController(
    private val trackSinging: TrackSingingUseCase,
    private val playReferenceNote: PlayReferenceNoteUseCase,
    private val examples: PlayLearningExampleUseCase,
) {
    private var listening: Job? = null

    val isListening: Boolean get() = listening?.isActive == true

    /**
     * Listens for [targets], each held [sing]'s seconds. [onProgress] gets every frame; [onFailure] is
     * called once if the microphone fails.
     */
    fun start(
        scope: CoroutineScope,
        sing: SingTarget,
        targets: List<Int>,
        comfortMidi: Int,
        onProgress: (SingingProgress) -> Unit,
        onFailure: () -> Unit,
    ) {
        // The microphone would hear the example otherwise; only the other voice plays, earphones advised.
        examples.stop()
        sing.accompanimentSemitones?.let { semitones ->
            playReferenceNote(comfortMidi + semitones, (sing.secondsEach * sing.semitones.size + ACCOMPANIMENT_MARGIN).toInt())
        }
        listening = trackSinging(targets, sing.secondsEach)
            .onEach(onProgress)
            .catch { onFailure() }
            .launchIn(scope)
    }

    fun stop() {
        if (listening?.isActive == true) examples.stop()
        listening?.cancel()
        listening = null
    }

    private companion object {
        /** Seconds of accompaniment after the last note, so it never stops under the singer. */
        const val ACCOMPANIMENT_MARGIN = 2.0
    }
}
