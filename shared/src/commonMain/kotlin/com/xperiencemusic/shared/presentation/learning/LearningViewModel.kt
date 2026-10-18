package com.xperiencemusic.shared.presentation.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.domain.learning.*
import com.xperiencemusic.shared.domain.model.*
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.GrowthRepository
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.TickObjectiveUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class LearningPreferences(val touchSound:Boolean=true,val rewardSound:Boolean=false,val haptics:Boolean=true,val reduceMotion:Boolean=false,val largeText:Boolean=false,val weeklyTarget:Int=2,val startingPoint:String?=null)
@Serializable
private data class Resume(val index:Int=0,val encounter:Int=0,val notes:Set<Int> = emptySet(),val answer:String?=null,val hints:Int=0)
enum class LearningMode { HOME, ACTIVITY, SUMMARY }

data class LearningUiState(
    val today:kotlinx.datetime.LocalDate=kotlinx.datetime.LocalDate(2026,1,1),val loading:Boolean=true,val allowed:Boolean=false,val memberId:String?=null,
    val module:LearningModule=LearningCatalog.module(LearningCatalog.DEFAULT),val index:Int=0,
    val encounter:Int=0,val progress:LearningProgress=LearningProgress(),val notes:Set<Int> = emptySet(),val answer:String?=null,
    val listened:Boolean=false,val result:Boolean?=null,val saving:Boolean=false,val error:String?=null,
    val moduleReviews:Map<String,ExerciseStatus> = emptyMap(),val reviewLoaded:Boolean=false,val reviewStatus:ExerciseStatus?=null,val feedback:String?=null,val reviewDue:Boolean=false,
    val mode:LearningMode=LearningMode.HOME,val hints:Int=0,val showSteps:Boolean=false,val showSettings:Boolean=false,
    val preferences:LearningPreferences=LearningPreferences(),val feedbackText:String?=null,val taps:List<Double> = emptyList(),
    val sessionDone:List<String> = emptyList(),val sessionDifficult:Set<String> = emptySet(),val successSerial:Int=0,
) {
    val activity get()=variedActivity(module.activities[index],encounter)
    val completed get()=progress.completedIn(module)
    val complete get()=progress.complete(module)
    val canCheck get()=!saving && allowed && when(activity.kind) {
        LearningKind.KEYBOARD -> notes.isNotEmpty()
        LearningKind.EAR -> listened && answer!=null
        LearningKind.CHOICE -> answer!=null
        LearningKind.RHYTHM -> taps.size>=8
        else -> true
    }
}
private data class LearningForm(
    val index:Int?=null,val encounter:Int=0,val notes:Set<Int> = emptySet(),val answer:String?=null,val listened:Boolean=false,
    val result:Boolean?=null,val saving:Boolean=false,val error:String?=null,val hints:Int=0,
    val mode:LearningMode=LearningMode.HOME,val steps:Boolean=false,val settings:Boolean=false,
    val preferences:LearningPreferences=LearningPreferences(),val feedback:String?=null,val taps:List<Double> = emptyList(),
    val plan:List<Int> = emptyList(),val done:List<String> = emptyList(),val difficult:Set<String> = emptySet(),val successSerial:Int=0,
)
sealed interface LearningEvent {
    data class Open(val index:Int):LearningEvent
    data class Key(val midi:Int):LearningEvent
    data class Answer(val value:String):LearningEvent
    data class Preferences(val value:LearningPreferences):LearningEvent
    data object ChallengeExample:LearningEvent
    data object Begin:LearningEvent
    data object Resume:LearningEvent
    data object Home:LearningEvent
    data object Steps:LearningEvent
    data object Settings:LearningEvent
    data object Hint:LearningEvent
    data object Listen:LearningEvent
    data object ListenBroken:LearningEvent
    data object ListenSelection:LearningEvent
    data object Tap:LearningEvent
    data object ResetTaps:LearningEvent
    data object Metronome:LearningEvent
    data object Check:LearningEvent
    data object Next:LearningEvent
    data object RequestReview:LearningEvent
    data object Sync:LearningEvent
}

@OptIn(ExperimentalCoroutinesApi::class,ExperimentalUuidApi::class)
class LearningViewModel(
    observeMember:ObserveCurrentMemberUseCase,private val repository:LearningRepository,growth:GrowthRepository,
    private val tickObjective:TickObjectiveUseCase,private val dates:DateProvider,private val audio:PlayLearningExample,
    private val snapshots:SnapshotStore?=null,moduleId:String=LearningCatalog.DEFAULT,
):ViewModel() {
    private val module=LearningCatalog.module(moduleId)
    private val form=MutableStateFlow(LearningForm())
    private val clock=TimeSource.Monotonic.markNow()
    private val member=observeMember().stateIn(viewModelScope,SharingStarted.Eagerly,null)
    private val learning=member.flatMapLatest { m -> if(m==null)flowOf(LearningProgress()) else repository.observe(m.id) }
    private val review=member.flatMapLatest { m ->
        if(m==null)flowOf<List<ObjectiveCheck>?>(null) else growth.observeChecks().map { it as List<ObjectiveCheck>? }.onStart { emit(null) }
    }
    val state=combine(member,learning,review,form) { m,p,checks,f ->
        val c=checks?.firstOrNull { it.memberId==m?.id && it.objectiveId==module.objectiveId }
        LearningUiState(today=dates.today(),loading=m==null,allowed=m?.profile?.instrument==Instrument.PIANO,memberId=m?.id,module=module,
            index=f.index?:p.nextIn(module),encounter=f.encounter,progress=p,notes=f.notes,answer=f.answer,listened=f.listened,
            result=f.result,saving=f.saving,error=f.error,moduleReviews=checks.orEmpty().filter { it.memberId==m?.id }.associate { it.objectiveId to it.status },reviewLoaded=checks!=null,reviewStatus=c?.status,feedback=c?.feedback,
            reviewDue=p.dueIn(module,dates.today()).isNotEmpty(),mode=f.mode,hints=f.hints,showSteps=f.steps,showSettings=f.settings,
            preferences=f.preferences,feedbackText=f.feedback,taps=f.taps,sessionDone=f.done,sessionDifficult=f.difficult,successSerial=f.successSerial)
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),LearningUiState(module=module))

    init {
        member.filterNotNull().onEach { m ->
            val preferences=snapshots?.load("learning-settings-${m.id}",LearningPreferences.serializer())?:LearningPreferences()
            form.update { it.copy(preferences=preferences) }
            repository.sync(m.id)
        }.launchIn(viewModelScope)
    }
    private fun persist() {
        val id=state.value.memberId?:return
        val f=form.value
        val resume = if (f.result == true) {
            val next = state.value.progress.nextIn(module)
            Resume(next, state.value.progress.attempts.count { it.activityId == module.activities[next].id })
        } else Resume(f.index?:state.value.index,f.encounter,f.notes,f.answer,f.hints)
        snapshots?.save("learning-resume-$id-${module.id}",Resume.serializer(),resume)
    }
    fun onEvent(event:LearningEvent) {
        if(state.value.saving)return
        when(event) {
            is LearningEvent.Open -> open(event.index)
            LearningEvent.ChallengeExample -> audio.broken(module.challenge().example)
            LearningEvent.Begin -> {
                val plan=state.value.progress.dailyPlan(module,dates.today()).let { if(state.value.progress.returning(dates.today()))it.take(1) else it }
                form.update { it.copy(plan=plan,done=emptyList(),difficult=emptySet()) }; open(plan.first())
            }
            LearningEvent.Resume -> {
                val id=state.value.memberId?:return
                val saved=snapshots?.load("learning-resume-$id-${module.id}",Resume.serializer())
                if(saved!=null && saved.index in module.activities.indices && saved.index<=state.value.progress.nextIn(module)) {
                    form.update { it.copy(index=saved.index,encounter=saved.encounter,notes=saved.notes,answer=saved.answer,hints=saved.hints,mode=LearningMode.ACTIVITY,result=null,feedback=null,taps=emptyList()) }
                } else open(state.value.progress.nextIn(module))
            }
            LearningEvent.Home -> { audio.stop(); persist(); form.update { it.copy(mode=LearningMode.HOME,taps=emptyList()) } }
            LearningEvent.Steps -> form.update { it.copy(steps=!it.steps) }
            LearningEvent.Settings -> form.update { it.copy(settings=!it.settings) }
            is LearningEvent.Preferences -> {
                form.update { it.copy(preferences=event.value) }
                state.value.memberId?.let { snapshots?.save("learning-settings-$it",LearningPreferences.serializer(),event.value) }
            }
            is LearningEvent.Key -> {
                val s=state.value
                if(!s.allowed)return
                val selected=event.midi !in s.notes
                if(selected && s.preferences.touchSound)audio.key(event.midi)
                form.update { it.copy(index=s.index,notes=if(selected)it.notes+event.midi else it.notes-event.midi,result=null,feedback=null) };persist()
            }
            is LearningEvent.Answer -> { form.update { it.copy(index=state.value.index,answer=event.value,result=null,feedback=null) };persist() }
            LearningEvent.Hint -> { form.update { it.copy(hints=(it.hints+1).coerceAtMost(3)) };persist() }
            LearningEvent.Listen -> { audio(state.value.activity.example);form.update { it.copy(index=state.value.index,listened=true) } }
            LearningEvent.ListenBroken -> { audio.broken(state.value.activity.example.ifEmpty { state.value.activity.expectedNotes.sorted() });form.update { it.copy(listened=true) } }
            LearningEvent.ListenSelection -> audio(state.value.notes.sorted())
            LearningEvent.Metronome -> audio.metronome(state.value.activity.bpm)
            LearningEvent.Tap -> form.update { it.copy(taps=(it.taps+clock.elapsedNow().inWholeMilliseconds/1000.0).takeLast(8),result=null) }
            LearningEvent.ResetTaps -> form.update { it.copy(taps=emptyList(),result=null,feedback=null) }
            LearningEvent.Check -> check()
            LearningEvent.Next -> next()
            LearningEvent.RequestReview -> requestReview()
            LearningEvent.Sync -> state.value.memberId?.let { viewModelScope.launch { repository.sync(it) } }
        }
    }
    private fun open(index:Int) {
        if(index !in module.activities.indices || index>state.value.progress.nextIn(module) && !state.value.complete)return
        audio.stop()
        val encounter=state.value.progress.attempts.count { it.activityId==module.activities[index].id }
        form.update { it.copy(index=index,encounter=encounter,notes=emptySet(),answer=null,listened=false,result=null,hints=0,mode=LearningMode.ACTIVITY,steps=false,feedback=null,taps=emptyList(),error=null) }
        persist()
    }
    private fun next() {
        if(state.value.activity.id !in state.value.completed)return
        val f=form.value
        val planPosition=f.plan.indexOf(state.value.index)
        if(planPosition>=0) {
            if(planPosition==f.plan.lastIndex) { audio.stop();form.update { it.copy(mode=LearningMode.SUMMARY) };return }
            open(f.plan[planPosition+1])
        } else if(state.value.index<module.activities.lastIndex)open(state.value.index+1)
        else { audio.stop();form.update { it.copy(mode=LearningMode.SUMMARY) } }
    }
    private fun check() {
        val s=state.value;if(!s.canCheck)return
        val id=s.memberId?:return
        val timing=rhythmError(s.taps,s.activity.bpm)
        val passed=if(s.activity.kind==LearningKind.RHYTHM)timing!=null && timing<=0.18 else PianoBasics.check(s.activity,s.notes,s.answer)
        form.update { it.copy(index=s.index,saving=true,error=null) }
        viewModelScope.launch {
            try {
                repository.record(LearningAttempt(Uuid.random().toString(),id,s.activity.id,PianoBasics.VERSION,passed,dates.today(),"${s.activity.id}:${s.encounter}",s.hints))
                val feedback=if(passed)"${s.activity.title} : activité réussie${if(s.hints>0)" avec aide" else ""}." else if(timing!=null)"Écart moyen : ${(timing*100).toInt()} %. Vise des intervalles plus réguliers, puis recommence." else correction(s.activity,s.notes,s.answer)
                form.update { it.copy(saving=false,result=passed,feedback=feedback,done=if(passed)(it.done+s.activity.id).distinct() else it.done,
                    difficult=if(!passed || s.hints>0)it.difficult+s.activity.id else it.difficult,successSerial=it.successSerial+if(passed)1 else 0) }
                if(passed && s.preferences.rewardSound)audio.reward()
                persist()
                repository.sync(id)
            } catch(e:CancellationException){throw e}
            catch(e:Exception){form.update { it.copy(saving=false,error="Sauvegarde impossible. Ta sélection est conservée ; réessaie.") }}
        }
    }
    private fun requestReview() {
        val s=state.value
        if(!s.allowed || !s.reviewLoaded || !s.complete || s.reviewStatus in listOf(ExerciseStatus.SUBMITTED,ExerciseStatus.VALIDATED))return
        form.update { it.copy(saving=true,error=null) }
        viewModelScope.launch {
            val result=tickObjective(module.objectiveId,true)
            form.update { it.copy(saving=false,error=if(result is DomainResult.Failure)"Demande non envoyée. Vérifie ta connexion puis réessaie." else null) }
        }
    }
    override fun onCleared(){audio.stop()}
}
