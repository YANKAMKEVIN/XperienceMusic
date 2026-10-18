package com.xperiencemusic.shared.presentation.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.learning.*
import com.xperiencemusic.shared.domain.model.*
import com.xperiencemusic.shared.domain.repository.*
import com.xperiencemusic.shared.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class AssessmentViewModel(moduleId:String,private val sessions:SessionRepository,private val learning:LearningRepository,
    private val recordings:ObjectiveRecordings,private val recorder:VoiceRecorder,private val preview:PlayLocalAudioUseCase,
    private val request:TickObjectiveUseCase):ViewModel() {
    val module=LearningCatalog.module(moduleId)
    val level=recorder.level
    val message=MutableStateFlow<String?>(null)
    val recordedSeconds=MutableStateFlow(0)
    val sending=MutableStateFlow(false)
    private var voice:RecordedVoice?=null
    init {
        recorder.level.onEach { if(it.reachedLimit)stop();if(it.hasFailed)message.value="Microphone indisponible. Vérifie son autorisation et réessaie." }.launchIn(viewModelScope)
    }
    fun start() {
        if(sending.value)return
        preview.stop();voice=null;recordedSeconds.value=0;message.value=null
        recorder.start(viewModelScope)
    }
    fun denied(){message.value="Autorise le microphone pour enregistrer ton instrument."}
    fun stop(){voice=recorder.stop();recordedSeconds.value=voice?.durationSeconds?:0}
    fun listen(){voice?.let {preview("voice/objective-${module.objectiveId}.wav",it.wav,NowPlaying("Mon essai",module.title))}}
    fun send() {
        val v=voice?:return
        if(sending.value || v.durationSeconds<3)return
        sending.value=true;preview.stop()
        viewModelScope.launch {
            try {
                val member=sessions.currentMember()?:error("signed out")
                check(member.profile.instrument==Instrument.PIANO)
                check(learning.observe(member.id).first().complete(module))
                recordings.send(member.id,module.objectiveId,v.wav,v.durationSeconds)
                voice=null;recordedSeconds.value=0
                val result=request(module.objectiveId,true)
                message.value=if(result is DomainResult.Success)"Prise et demande envoyées. Le retour du responsable apparaîtra dans ce module."
                    else "Prise conservée. Reviens au module pour relancer la demande d’évaluation."
            } catch(e:CancellationException){throw e}
            catch(e:Exception){message.value="Envoi impossible. La prise est encore disponible pour réessayer."}
            finally {sending.value=false}
        }
    }
    override fun onCleared(){recorder.discard();preview.stop()}
}
