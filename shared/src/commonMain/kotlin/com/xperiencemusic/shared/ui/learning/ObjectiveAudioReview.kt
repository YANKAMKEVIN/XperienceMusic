package com.xperiencemusic.shared.ui.learning

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.learning.ObjectiveRecordings
import com.xperiencemusic.shared.domain.model.PlaybackTrack
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import org.koin.compose.koinInject

@Composable fun ObjectiveAudioReview(memberId:String,objectiveId:String) {
    val recordings=koinInject<ObjectiveRecordings>()
    val player=koinInject<PlaybackController>()
    val flow=remember(memberId,recordings){recordings.observe(memberId)}
    val rows by flow.collectAsStateWithLifecycle(emptyList())
    val playback by player.state.collectAsStateWithLifecycle()
    rows.filter{it.objectiveId==objectiveId}.sortedByDescending { it.submittedAt }.forEachIndexed { index,r->
        val id="objective-${r.audioPath}"
        OutlinedButton(onClick={player.playOrToggle(PlaybackTrack(id,"Prise d’évaluation","${r.seconds} secondes",r.audioPath))}) {
            Text(if(playback.track?.id==id && playback.isPlaying)"Mettre en pause" else "Prise ${rows.count { it.objectiveId==objectiveId }-index} · ${r.submittedAt.take(10).ifEmpty { "sur ce téléphone" }} · ${r.seconds} s")
        }
    }
}
