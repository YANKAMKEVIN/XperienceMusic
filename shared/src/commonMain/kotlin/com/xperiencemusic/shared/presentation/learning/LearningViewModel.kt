package com.xperiencemusic.shared.presentation.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.domain.learning.*
import com.xperiencemusic.shared.domain.model.*
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.GrowthRepository
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.PlayReferenceNoteUseCase
import com.xperiencemusic.shared.domain.usecase.TickObjectiveUseCase
import com.xperiencemusic.shared.domain.usecase.TrackSingingUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
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
    val sessionDone:List<String> = emptyList(),val sessionDifficult:Set<String> = emptySet(),
    /** The title each worked activity had this session: a variant ("À toi · Si♭ majeur") differs from its base. */
    val sessionTitles:Map<String,String> = emptyMap(),val successSerial:Int=0,
    /** Sung activities: the singer's comfort note, what the microphone hears and how far the take went. */
    val comfortMidi:Int=VoicePart.ALTO.comfortMidi,val singing:Boolean=false,val sung:Boolean=false,val heardMidi:Int?=null,
    val singStep:Int=0,val holdSeconds:Double=0.0,val micError:String?=null,
    /** The microphone was refused: the screen offers the phone settings. */
    val micDenied:Boolean=false,
    /** Sung activities set aside for later: never counted as done, they no longer lock the next steps. */
    val postponed:Set<String> = emptySet(),
    /** Where the member left off last time, if they ever opened an activity of this module. */
    val resumeIndex:Int?=null,
) {
    /** The furthest step open to the member. */
    val reached get()=progress.nextIn(module,postponed)
    /** "Reprendre" only stands for a real spot left behind, still open to the member. */
    val canResume get()=resumeIndex!=null && resumeIndex in module.activities.indices && (resumeIndex<=reached || complete)
    val canPostpone get()=allowed && !saving && !singing && result!=true && activity.canPostpone() && activity.id !in completed
    val singTargets get()=activity.sing?.semitones?.map { comfortMidi+it }.orEmpty()
    /** Ear questions without listed answers ask "Majeur ou mineur ?". */
    val choiceOptions get()=if(activity.kind==LearningKind.EAR && activity.choices.isEmpty())listOf("Majeur","Mineur") else activity.choices
    /** After a wrong chord: the selected notes that belong to it, shown next to the correction. */
    val keptNotes get()=if(result==false && activity.kind==LearningKind.KEYBOARD)notes.intersect(activity.expectedNotes) else null
    val activity get()=variedActivity(module.activities[index],encounter)
    val completed get()=progress.completedIn(module)
    val complete get()=progress.complete(module)
    val canCheck get()=!saving && allowed && when(activity.kind) {
        LearningKind.KEYBOARD -> notes.isNotEmpty()
        LearningKind.EAR -> listened && answer!=null
        LearningKind.CHOICE -> answer!=null
        LearningKind.RHYTHM -> taps.size>=8
        LearningKind.SING -> sung
        else -> true
    }
}
private data class LearningForm(
    val index:Int?=null,val encounter:Int=0,val notes:Set<Int> = emptySet(),val answer:String?=null,val listened:Boolean=false,
    val result:Boolean?=null,val saving:Boolean=false,val error:String?=null,val hints:Int=0,
    val mode:LearningMode=LearningMode.HOME,val steps:Boolean=false,val settings:Boolean=false,
    val preferences:LearningPreferences=LearningPreferences(),val feedback:String?=null,val taps:List<Double> = emptyList(),
    val plan:List<Int> = emptyList(),val done:List<String> = emptyList(),val difficult:Set<String> = emptySet(),val titles:Map<String,String> = emptyMap(),val successSerial:Int=0,
    val singing:Boolean=false,val sung:Boolean=false,val heardMidi:Int?=null,val singStep:Int=0,val holdSeconds:Double=0.0,val micError:String?=null,val micDenied:Boolean=false,
    val postponed:Set<String> = emptySet(),val resumeIndex:Int?=null,
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
    data object StartSinging:LearningEvent
    data object StopSinging:LearningEvent
    data object MicrophoneDenied:LearningEvent
    data object Check:LearningEvent
    /** "Je le ferai plus tard": moves on from a sung activity without passing it. */
    data object Postpone:LearningEvent
    data object Next:LearningEvent
    data object RequestReview:LearningEvent
    data object Sync:LearningEvent
}

@OptIn(ExperimentalCoroutinesApi::class,ExperimentalUuidApi::class)
class LearningViewModel(
    observeMember:ObserveCurrentMemberUseCase,private val repository:LearningRepository,growth:GrowthRepository,
    private val tickObjective:TickObjectiveUseCase,private val dates:DateProvider,private val audio:PlayLearningExample,
    private val snapshots:SnapshotStore?=null,moduleId:String=LearningCatalog.DEFAULT,
    private val trackSinging:TrackSingingUseCase?=null,private val playReferenceNote:PlayReferenceNoteUseCase?=null,
):ViewModel() {
    private var listening:Job?=null
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
        LearningUiState(today=dates.today(),loading=m==null,allowed=m?.profile?.instrument==module.instrument,memberId=m?.id,module=module,
            index=f.index?:p.nextIn(module,f.postponed),encounter=f.encounter,progress=p,notes=f.notes,answer=f.answer,listened=f.listened,
            result=f.result,saving=f.saving,error=f.error,moduleReviews=checks.orEmpty().filter { it.memberId==m?.id }.associate { it.objectiveId to it.status },reviewLoaded=checks!=null,reviewStatus=c?.status,feedback=c?.feedback,
            reviewDue=p.dueIn(module,dates.today()).isNotEmpty(),mode=f.mode,hints=f.hints,showSteps=f.steps,showSettings=f.settings,
            preferences=f.preferences,feedbackText=f.feedback,taps=f.taps,sessionDone=f.done,sessionDifficult=f.difficult,sessionTitles=f.titles,successSerial=f.successSerial,
            comfortMidi=(m?.profile?.voicePart?:VoicePart.ALTO).comfortMidi,singing=f.singing,sung=f.sung,heardMidi=f.heardMidi,singStep=f.singStep,holdSeconds=f.holdSeconds,micError=f.micError,micDenied=f.micDenied,postponed=f.postponed,resumeIndex=f.resumeIndex)
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),LearningUiState(module=module))

    init {
        member.filterNotNull().onEach { m ->
            val preferences=snapshots?.load("learning-settings-${m.id}",LearningPreferences.serializer())?:LearningPreferences()
            val postponed=snapshots?.load(postponedKey(m.id),POSTPONED)?:emptySet()
            val resume=snapshots?.load(resumeKey(m.id),Resume.serializer())
            form.update { it.copy(preferences=preferences,postponed=it.postponed+postponed,resumeIndex=it.resumeIndex?:resume?.index) }
            repository.sync(m.id)
        }.launchIn(viewModelScope)
    }
    private fun persist() {
        val id=state.value.memberId?:return
        val f=form.value
        val resume = if (f.result == true) {
            val next = state.value.reached
            Resume(next, state.value.progress.attempts.count { it.activityId == module.activities[next].id })
        } else Resume(f.index?:state.value.index,f.encounter,f.notes,f.answer,f.hints)
        snapshots?.save(resumeKey(id),Resume.serializer(),resume)
        form.update { it.copy(resumeIndex=resume.index) }
    }
    private fun resumeKey(memberId:String)="learning-resume-$memberId-${module.id}"
    fun onEvent(event:LearningEvent) {
        if(state.value.saving)return
        when(event) {
            is LearningEvent.Open -> open(event.index)
            LearningEvent.ChallengeExample -> audio.broken(module.challenge().example)
            LearningEvent.Begin -> {
                val plan=state.value.progress.dailyPlan(module,dates.today(),state.value.postponed).let { if(state.value.progress.returning(dates.today()))it.take(1) else it }
                form.update { it.copy(plan=plan,done=emptyList(),difficult=emptySet(),titles=emptyMap()) }; open(plan.first())
            }
            LearningEvent.Resume -> {
                val id=state.value.memberId?:return
                val saved=snapshots?.load(resumeKey(id),Resume.serializer())
                if(saved!=null && saved.index in module.activities.indices && saved.index<=state.value.reached) {
                    form.update { it.copy(index=saved.index,encounter=saved.encounter,notes=saved.notes,answer=saved.answer,hints=saved.hints,mode=LearningMode.ACTIVITY,result=null,feedback=null,taps=emptyList()) }
                } else open(state.value.reached)
            }
            LearningEvent.Home -> { stopSinging(); audio.stop(); persist(); form.update { it.copy(mode=LearningMode.HOME,taps=emptyList()) } }
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
            LearningEvent.ListenBroken -> { audio.broken(state.value.activity.example.ifEmpty { state.value.activity.expectedNotes.sorted() }.ifEmpty { state.value.singTargets });form.update { it.copy(listened=true) } }
            LearningEvent.ListenSelection -> audio(state.value.notes.sorted())
            LearningEvent.Metronome -> audio.metronome(state.value.activity.bpm)
            LearningEvent.Tap -> form.update { it.copy(taps=(it.taps+clock.elapsedNow().inWholeMilliseconds/1000.0).takeLast(8),result=null) }
            LearningEvent.ResetTaps -> form.update { it.copy(taps=emptyList(),result=null,feedback=null) }
            LearningEvent.StartSinging -> startSinging()
            LearningEvent.StopSinging -> stopSinging()
            LearningEvent.MicrophoneDenied -> form.update { it.copy(micError="Autorise le micro dans les réglages pour chanter l'exercice.",micDenied=true) }
            LearningEvent.Check -> check()
            LearningEvent.Postpone -> postpone()
            LearningEvent.Next -> next()
            LearningEvent.RequestReview -> requestReview()
            LearningEvent.Sync -> state.value.memberId?.let { viewModelScope.launch { repository.sync(it) } }
        }
    }
    private fun open(index:Int) {
        if(index !in module.activities.indices || index>state.value.reached && !state.value.complete)return
        stopSinging()
        audio.stop()
        val encounter=state.value.progress.attempts.count { it.activityId==module.activities[index].id }
        form.update { it.copy(index=index,encounter=encounter,notes=emptySet(),answer=null,listened=false,result=null,hints=0,mode=LearningMode.ACTIVITY,steps=false,feedback=null,taps=emptyList(),error=null,sung=false,heardMidi=null,singStep=0,holdSeconds=0.0,micError=null,micDenied=false) }
        persist()
    }
    private fun next() {
        if(state.value.activity.id !in state.value.completed)return
        advance()
    }
    private fun postpone() {
        val s=state.value
        if(!s.canPostpone)return
        val id=s.activity.id
        form.update { it.copy(index=s.index,postponed=it.postponed+id,difficult=it.difficult+id,titles=it.titles+(id to s.activity.title)) }
        savePostponed()
        advance()
    }
    private fun savePostponed() {
        val id=state.value.memberId?:return
        snapshots?.save(postponedKey(id),POSTPONED,form.value.postponed)
    }
    private fun postponedKey(memberId:String)="learning-postponed-$memberId-${module.id}"
    /** To the next step of the session plan, or of the module, or to the summary. */
    private fun advance() {
        val f=form.value
        val planPosition=f.plan.indexOf(state.value.index)
        if(planPosition>=0) {
            if(planPosition==f.plan.lastIndex) { audio.stop();form.update { it.copy(mode=LearningMode.SUMMARY) };return }
            open(f.plan[planPosition+1])
        } else if(state.value.index<module.activities.lastIndex)open(state.value.index+1)
        else { audio.stop();form.update { it.copy(mode=LearningMode.SUMMARY) } }
    }
    private fun startSinging() {
        val s=state.value
        val target=s.activity.sing?:return
        val track=trackSinging?:return
        if(!s.allowed || listening?.isActive==true)return
        // The microphone would hear the example otherwise; only the other voice plays, earphones advised.
        audio.stop()
        target.accompanimentSemitones?.let { playReferenceNote?.invoke(s.comfortMidi+it,(target.secondsEach*target.semitones.size+ACCOMPANIMENT_MARGIN).toInt()) }
        val index=s.index
        form.update { it.copy(index=index,singing=true,sung=false,heardMidi=null,singStep=0,holdSeconds=0.0,micError=null,micDenied=false,result=null,feedback=null) }
        listening=track(s.singTargets,target.secondsEach)
            .onEach { p ->
                form.update { it.copy(heardMidi=p.heardMidi,singStep=p.completedSteps,holdSeconds=p.holdSeconds) }
                if(p.isSuccess) {
                    form.update { it.copy(singing=false,sung=true,singStep=target.semitones.size) }
                    stopSinging()
                }
            }
            // Refused permission, microphone used by a call… all end the take the same way.
            .catch { form.update { it.copy(singing=false,micError="Micro indisponible. Vérifie l'autorisation, puis réessaie.") } }
            .launchIn(viewModelScope)
    }
    private fun stopSinging() {
        if(listening?.isActive==true)audio.stop()
        listening?.cancel();listening=null
        form.update { if(it.singing)it.copy(singing=false) else it }
    }
    private fun check() {
        val s=state.value;if(!s.canCheck)return
        val id=s.memberId?:return
        val timing=rhythmError(s.taps,s.activity.bpm)
        val passed=if(s.activity.kind==LearningKind.RHYTHM)timing!=null && timing<=0.18 else if(s.activity.kind==LearningKind.SING)s.sung else PianoBasics.check(s.activity,s.notes,s.answer)
        form.update { it.copy(index=s.index,saving=true,error=null) }
        viewModelScope.launch {
            try {
                repository.record(LearningAttempt(Uuid.random().toString(),id,s.activity.id,PianoBasics.VERSION,passed,dates.today(),"${s.activity.id}:${s.encounter}",s.hints))
                val feedback=if(passed && s.activity.kind==LearningKind.LESSON)"Leçon terminée." else if(passed)"Activité réussie${if(s.hints>0)" avec aide" else ""}." else if(timing!=null)"Écart moyen : ${(timing*100).toInt()} %. Vise des intervalles plus réguliers, puis recommence." else correction(s.activity,s.notes,s.answer)
                if(passed && s.activity.id in form.value.postponed) { form.update { it.copy(postponed=it.postponed-s.activity.id) };savePostponed() }
                form.update { it.copy(saving=false,result=passed,feedback=feedback,done=if(passed)(it.done+s.activity.id).distinct() else it.done,
                    difficult=if(!passed || s.hints>0)it.difficult+s.activity.id else it.difficult,titles=it.titles+(s.activity.id to s.activity.title),successSerial=it.successSerial+if(passed)1 else 0) }
                if(passed && s.preferences.rewardSound)audio.reward()
                persist()
                // A lesson is read, not answered: "J'ai compris" goes straight on, once the success is counted.
                if(passed && s.activity.kind==LearningKind.LESSON && withTimeoutOrNull(2_000) { state.first { s.activity.id in it.completed } }!=null) advance()
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
    override fun onCleared(){listening?.cancel();audio.stop()}
}
private const val ACCOMPANIMENT_MARGIN=2.0
private val POSTPONED=SetSerializer(String.serializer())
