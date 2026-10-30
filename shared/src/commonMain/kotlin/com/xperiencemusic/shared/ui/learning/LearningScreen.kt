package com.xperiencemusic.shared.ui.learning

import com.xperiencemusic.shared.ui.components.choiceState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.learning.*
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.presentation.learning.*
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.MicrophoneDeniedNotice
import com.xperiencemusic.shared.ui.platform.rememberMicrophonePermission
import com.xperiencemusic.shared.ui.progress.ExerciseStatusBadge
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LearningScreen(onBack:()->Unit,moduleId:String=LearningCatalog.DEFAULT,onModule:(String)->Unit={},onRecord:()->Unit={},onScaleCoach:()->Unit={},
    viewModel:LearningViewModel=koinViewModel(key="learning-$moduleId") { parametersOf(moduleId) }) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LearningScreenContent(state,viewModel::onEvent,onBack,onModule,onRecord, audioReview={ member, objective -> ObjectiveAudioReview(member, objective) }, onScaleCoach=onScaleCoach)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreenContent(state:LearningUiState,onEvent:(LearningEvent)->Unit,onBack:()->Unit,onModule:(String)->Unit={},onRecord:()->Unit={},audioReview:@Composable (String,String)->Unit={_,_->},onScaleCoach:()->Unit={}) {
    val density=LocalDensity.current
    val haptic=LocalHapticFeedback.current
    LaunchedEffect(state.successSerial) {
        if(state.successSerial>0 && state.preferences.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    CompositionLocalProvider(LocalDensity provides Density(density.density,density.fontScale*if(state.preferences.largeText)1.2f else 1f)) {
        Scaffold(topBar={ TopAppBar(title={Text(state.module.title,style=MaterialTheme.typography.titleLarge)},navigationIcon={
            IconButton(onClick={ if(state.mode==LearningMode.HOME)onBack() else onEvent(LearningEvent.Home) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Retour") }
        },actions={TextButton(onClick={onEvent(LearningEvent.Settings)}) {Text("Confort")}},windowInsets=WindowInsets(0)) },
            // The main action stays on screen however long the hints and corrections above it grow.
            bottomBar={ if(!state.loading && state.allowed && state.mode==LearningMode.ACTIVITY) ActivityAction(state,onEvent) },
            contentWindowInsets=WindowInsets(0)) { padding ->
            if(state.loading) Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.Center){CircularProgressIndicator()}
            else if(!state.allowed)Text(if(state.module.instrument==Instrument.SINGER)"Ces modules sont proposés au parcours chantre." else "Ces modules sont proposés au parcours piano.",Modifier.padding(padding).padding(Spacing.l))
            else key(state.mode,state.index,state.module.id) {
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),verticalArrangement=Arrangement.spacedBy(Spacing.m)) {
                    when(state.mode) {
                        LearningMode.HOME -> HomeContent(state,onEvent,onModule,onRecord,audioReview,onScaleCoach)
                        LearningMode.ACTIVITY -> {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                Text("Étape ${state.index+1} / ${state.module.activities.size}",style=MaterialTheme.typography.labelLarge)
                                TextButton(onClick={onEvent(LearningEvent.Steps)}){Text("Voir les étapes")}
                            }
                            ActivityCard(state,onEvent)
                            TextButton(onClick={onEvent(LearningEvent.Home)}){Text("Faire une pause · progression conservée")}
                        }
                        LearningMode.SUMMARY -> {
                            AppCard {
                                Text("Une séance de plus !",style=MaterialTheme.typography.headlineMedium)
                                Text("${state.sessionDone.size} activité(s) travaillée(s)",Modifier.padding(top=Spacing.m))
                                state.sessionDone.forEach { id -> Text("✓ ${state.sessionTitles[id] ?: LearningCatalog.findActivity(id)?.title.orEmpty()}",Modifier.padding(top=Spacing.s)) }
                                if(state.sessionDifficult.isNotEmpty()) {
                                    Text("À reprendre tranquillement",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=Spacing.m))
                                    state.sessionDifficult.forEach { id ->Text(state.sessionTitles[id] ?: LearningCatalog.findActivity(id)?.title.orEmpty()) }
                                }
                                Text(if(state.sessionDifficult.isEmpty()) "Tu as travaillé sans difficulté signalée. La prochaine séance t’aidera à entretenir ces repères." else "Les prochaines révisions tiendront compte de tes difficultés. Tu peux aussi les reprendre depuis les étapes.",Modifier.padding(top=Spacing.m))
                                Text("Prochaine étape : ${state.module.activities[state.reached].title}",Modifier.padding(top=Spacing.s))
                            }
                            GradientButton(onClick={onEvent(LearningEvent.Home)}){Text("Terminer ma séance")}
                            OutlinedButton(onClick={onEvent(LearningEvent.Open(state.reached))}){Text("Continuer librement")}
                        }
                    }
                    state.error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                }
            }
        }
        if(state.showSteps) AlertDialog(onDismissRequest={onEvent(LearningEvent.Steps)},title={Text("Les étapes")},text={
            Column(Modifier.verticalScroll(rememberScrollState())) {
                state.module.activities.forEachIndexed { i,a ->
                    val unlocked=state.complete || i<=state.reached
                    val later=a.id in state.postponed && a.id !in state.completed
                    TextButton(onClick={onEvent(LearningEvent.Open(i))},enabled=unlocked) {
                        Text("${if(a.id in state.completed)"✓" else "${i+1}."} ${a.title}${if(later)" · à reprendre" else if(!unlocked)" · à découvrir ensuite" else ""}")
                    }
                }
            }
        },confirmButton={TextButton(onClick={onEvent(LearningEvent.Steps)}){Text("Fermer")}})
        if(state.showSettings) AlertDialog(onDismissRequest={onEvent(LearningEvent.Settings)},title={Text("Ton confort")},text={
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Setting("Son des touches",state.preferences.touchSound){onEvent(LearningEvent.Preferences(state.preferences.copy(touchSound=it)))}
                Setting("Son de réussite",state.preferences.rewardSound){onEvent(LearningEvent.Preferences(state.preferences.copy(rewardSound=it)))}
                Setting("Vibrations légères",state.preferences.haptics){onEvent(LearningEvent.Preferences(state.preferences.copy(haptics=it)))}
                Setting("Réduire les animations",state.preferences.reduceMotion){onEvent(LearningEvent.Preferences(state.preferences.copy(reduceMotion=it)))}
                Setting("Texte plus grand",state.preferences.largeText){onEvent(LearningEvent.Preferences(state.preferences.copy(largeText=it)))}
                Text("Le texte conserve aussi les réglages de taille du téléphone. Les animations suivent les réglages système.",style=MaterialTheme.typography.bodySmall)
            }
        },confirmButton={TextButton(onClick={onEvent(LearningEvent.Settings)}){Text("C’est réglé")}})
    }
}
/** The whole row is the switch: TalkBack reads its label and state, and a tap on the label toggles it. */
@Composable private fun Setting(label:String,value:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=48.dp).toggleable(value,role=Role.Switch,onValueChange=onChange),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f));Switch(value,onCheckedChange=null)}
}

@Composable private fun HomeContent(s:LearningUiState,event:(LearningEvent)->Unit,onModule:(String)->Unit,onRecord:()->Unit,audioReview:@Composable (String,String)->Unit,onScaleCoach:()->Unit) {
    AppCard {
        Text("TA SÉANCE DU JOUR",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
        Text("Un petit pas, à ton rythme",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(top=Spacing.s))
        val plan=s.progress.dailyPlan(s.module,s.today,s.postponed).let { if(s.progress.returning(s.today))it.take(1) else it }
        Text("Environ ${plan.size*2} minutes · ${plan.size} activité(s)",Modifier.padding(top=Spacing.s))
        Text("Objectif : ${s.module.activities[plan.first()].title}",Modifier.padding(top=Spacing.s))
        if(s.progress.returning(s.today)) Text("Heureux de te retrouver. Reprends doucement avec une courte révision : tes progrès sont conservés.",Modifier.padding(top=Spacing.s))
        Text("${s.completed.size} / ${s.module.activities.size} activités terminées",Modifier.padding(top=Spacing.m))
        LinearProgressIndicator(progress={s.completed.size.toFloat()/s.module.activities.size},modifier=Modifier.fillMaxWidth().padding(top=Spacing.s))
        GradientButton(onClick={event(LearningEvent.Begin)},modifier=Modifier.padding(top=Spacing.m)){Text(if(s.completed.isNotEmpty())"Continuer ma séance" else "Commencer ma séance")}
        TextButton(onClick={event(LearningEvent.Resume)}){Text("Reprendre · ${s.module.activities[s.reached].title}")}
        TextButton(onClick={event(LearningEvent.Steps)}){Text("Explorer les étapes")}
    }
    if(s.progress.pending>0) AppCard {
        Text("${s.progress.pending} essai(s) conservé(s) sur ce téléphone, en attente d’envoi.")
        TextButton(onClick={event(LearningEvent.Sync)}){Text("Synchroniser")}
    }
    AppCard {
        val mastery=s.progress.mastery(s.module,s.reviewStatus,s.today)
        var explain by remember { mutableStateOf(false) }
        Text(mastery.label,style=MaterialTheme.typography.titleMedium)
        TextButton(onClick={explain=!explain}) { Text("Comprendre ce statut") }
        if(explain) Text(mastery.explanation,style=MaterialTheme.typography.bodySmall)
        Text(if(s.module.instrument==Instrument.SINGER)"Les exercices à l’écran et ta voix en situation sont évalués séparément." else "Les exercices à l’écran et le jeu sur l’instrument sont évalués séparément.",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=Spacing.s))
        s.reviewStatus?.let {ExerciseStatusBadge(it,Modifier.padding(top=Spacing.s))}
        s.feedback?.let {
            Text("Mon prochain travail",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=Spacing.s))
            Text(it,Modifier.padding(top=Spacing.s))
            if(s.reviewStatus==ExerciseStatus.TO_REWORK) Text("Reprends cette consigne, puis envoie une nouvelle prise au responsable.",style=MaterialTheme.typography.bodySmall)
        }
        if(s.complete) s.memberId?.let { audioReview(it, s.module.objectiveId) }
        if(s.complete && s.reviewStatus!=ExerciseStatus.VALIDATED) {
            OutlinedButton(onClick=onRecord,modifier=Modifier.padding(top=Spacing.m)){Text(if(s.module.instrument==Instrument.SINGER)"Enregistrer ma voix pour le responsable" else "Enregistrer mon jeu pour le responsable")}
            TextButton(onClick={event(LearningEvent.RequestReview)},enabled=s.reviewLoaded && !s.saving && s.reviewStatus!=ExerciseStatus.SUBMITTED){Text("Demander une évaluation en répétition")}
            if(!s.reviewLoaded)Text("Une connexion est nécessaire pour envoyer la demande.",style=MaterialTheme.typography.bodySmall)
        }
    }
    if(s.module.id=="piano-scales") AppCard {
        Text("UNE CORRECTION AU MICRO",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
        Text("Ta gamme, note par note",style=MaterialTheme.typography.titleLarge)
        Text("Joue une montée de Do majeur à une main. L’app compare les notes après la prise et propose un passage à reprendre.")
        OutlinedButton(onClick=onScaleCoach) {Text("Essayer la correction automatique")}
        Text("Essai local et expérimental · sans pédale · validation finale par le responsable.",style=MaterialTheme.typography.bodySmall)
    }
    JourneyGoal(s,event)
    // The starting-point quiz only knows the piano modules.
    if(s.module.instrument!=Instrument.PIANO) Unit
    else if(s.preferences.startingPoint==null) StartingPoint(s,event)
    else AppCard {
        Text("Mon point de départ conseillé : ${LearningCatalog.module(s.preferences.startingPoint).title}")
        TextButton(onClick={onModule(s.preferences.startingPoint)}) {Text("Explorer ce module")}
        TextButton(onClick={event(LearningEvent.Preferences(s.preferences.copy(startingPoint=null)))}) {Text("Refaire le petit bilan")}
    }
    AppCard {
        val challenge=s.module.challenge()
        Text("DÉFI MUSICAL",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
        Text(challenge.title,style=MaterialTheme.typography.titleLarge)
        challenge.steps.forEachIndexed { i,step -> Text("${i+1}. $step",Modifier.padding(top=Spacing.s)) }
        Text("À écouter : ${challenge.criteria}",Modifier.padding(top=Spacing.s))
        if(s.module.instrument==Instrument.PIANO) {
            TextButton(onClick={event(LearningEvent.ChallengeExample)}) { Text("Écouter les notes de la grille") }
            Text("Démonstration note par note : le rythme d’accompagnement reste à travailler sur ton instrument.",style=MaterialTheme.typography.bodySmall)
        }
        if(s.complete && s.reviewStatus!=ExerciseStatus.VALIDATED) OutlinedButton(onClick=onRecord) { Text("Enregistrer mon défi") }
        else if(!s.complete) Text("Prépare ce défi avec les activités du module ; l’envoi au responsable s’ouvrira ensuite.",style=MaterialTheme.typography.bodySmall)
    }
    Text(if(s.module.instrument==Instrument.SINGER)"Mon parcours chantre" else "Mon parcours piano",style=MaterialTheme.typography.titleLarge)
    LearningCatalog.modulesFor(s.module.instrument).forEach { m ->
        AppCard(onClick={onModule(m.id)}) {
            Text(m.title,style=MaterialTheme.typography.titleMedium)
            Text("${s.progress.completedIn(m).size} / ${m.activities.size} activités",style=MaterialTheme.typography.bodySmall)
            Text(s.progress.mastery(m,s.moduleReviews[m.objectiveId],s.today).label,style=MaterialTheme.typography.bodySmall)
        }
    }
    if(s.progress.attempts.isNotEmpty()) AppCard {
        Text("Mes derniers essais",style=MaterialTheme.typography.titleMedium)
        s.progress.attempts.filter { a->s.module.activities.any {it.id==a.activityId} }.takeLast(5).reversed().forEach {
            Text("${it.day} · ${LearningCatalog.findActivity(it.activityId)?.title.orEmpty()} · ${if(it.passed)"réussi${if(it.hints>0)" avec aide" else ""}" else "à reprendre"}",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=Spacing.s))
        }
    }
}

@Composable private fun ActivityAction(s:LearningUiState,event:(LearningEvent)->Unit) {
    val a=s.activity
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(start=Spacing.l,end=Spacing.l,top=Spacing.s),verticalArrangement=Arrangement.spacedBy(Spacing.s)) {
        // The answer to "Vérifier" sits right above it: never under the fold, and read out by TalkBack.
        s.feedbackText?.let { feedback -> CheckFeedback(feedback,s.result==true,s.keptNotes) }
        if(s.result==true) GradientButton(onClick={event(LearningEvent.Next)},modifier=Modifier.fillMaxWidth()){Text("Continuer")}
        else GradientButton(onClick={event(LearningEvent.Check)},enabled=s.canCheck,modifier=Modifier.fillMaxWidth()){Text(when {s.saving->"Sauvegarde…";a.kind==LearningKind.LESSON->"J’ai compris";a.kind==LearningKind.PRACTICE->"J’ai effectué cet entraînement";a.kind==LearningKind.SING && !s.sung->"Chante d’abord l’exercice";else->"Vérifier"})}
        // A sung activity never locks the session: no earphones, a noisy room or a shy microphone can wait.
        if(s.canPostpone && !s.sung) TextButton(onClick={event(LearningEvent.Postpone)},modifier=Modifier.fillMaxWidth()){Text("Je le ferai plus tard")}
    }
}

@Composable private fun CheckFeedback(feedback:String,passed:Boolean,kept:Set<Int>?) {
    Column(Modifier.fillMaxWidth().heightIn(max=160.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .verticalScroll(rememberScrollState()).padding(Spacing.m).semantics(mergeDescendants=true){liveRegion=LiveRegionMode.Polite},
        verticalArrangement=Arrangement.spacedBy(Spacing.xs)) {
        Text(feedback,style=MaterialTheme.typography.bodyMedium,color=if(passed)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        kept?.let { Text("À conserver : ${it.sorted().joinToString(" · "){note->noteLabel(note)}.ifEmpty{"cherche d’abord la fondamentale"}}",style=MaterialTheme.typography.bodySmall) }
    }
}

/** Big answer buttons: two side by side ("Majeur" / "Mineur"), longer lists one per line. */
@Composable private fun ChoiceButtons(choices:List<String>,selected:String?,enabled:Boolean,onPick:(String)->Unit) {
    val button:@Composable (String,Modifier)->Unit = { choice,modifier ->
        val isSelected=selected==choice
        OutlinedButton(onClick={onPick(choice)},enabled=enabled,
            colors=ButtonDefaults.outlinedButtonColors(containerColor=if(isSelected)MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
            border=BorderStroke(if(isSelected)2.dp else 1.dp,if(isSelected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
            modifier=modifier.heightIn(min=56.dp).semantics{this.selected=isSelected}) { Text(choice,textAlign=TextAlign.Center) }
    }
    if(choices.size<=2) Row(Modifier.fillMaxWidth().selectableGroup(),horizontalArrangement=Arrangement.spacedBy(Spacing.s)) { choices.forEach { button(it,Modifier.weight(1f)) } }
    else Column(Modifier.fillMaxWidth().selectableGroup(),verticalArrangement=Arrangement.spacedBy(Spacing.s)) { choices.forEach { button(it,Modifier.fillMaxWidth()) } }
}

@Composable private fun ActivityCard(s:LearningUiState,event:(LearningEvent)->Unit) {
    val a=s.activity
    AppCard {
        Column((if(s.preferences.reduceMotion)Modifier else Modifier.animateContentSize()),verticalArrangement=Arrangement.spacedBy(Spacing.m)) {
            Text(when(a.kind){LearningKind.LESSON->"COMPRENDRE";LearningKind.EAR->"ÉCOUTER";LearningKind.KEYBOARD->"CONSTRUIRE";LearningKind.PRACTICE->if(s.module.instrument==Instrument.SINGER)"AVEC TA VOIX" else "SUR TON INSTRUMENT";LearningKind.CHOICE->"FAIRE LE LIEN";LearningKind.RHYTHM->"SENTIR LE TEMPO";LearningKind.SING->"CHANTER"},style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
            Text(a.title,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.semantics{heading()})
            Text(a.instruction,style=MaterialTheme.typography.bodyLarge)
            if(a.kind in listOf(LearningKind.LESSON,LearningKind.PRACTICE))Text(a.explanation)
            if(a.example.isNotEmpty()) {
                OutlinedButton(onClick={event(LearningEvent.Listen)},enabled=!s.saving){Text(if(a.example.size>1 && a.choices.isNotEmpty())"Écouter les notes" else "Écouter l’accord")}
                TextButton(onClick={event(LearningEvent.ListenBroken)},enabled=!s.saving){Text("Écouter note par note")}
            }
            if(a.kind in listOf(LearningKind.EAR,LearningKind.CHOICE)) {
                ChoiceButtons(s.choiceOptions,s.answer,enabled=!s.saving){event(LearningEvent.Answer(it))}
                if(a.kind==LearningKind.EAR && !s.listened)Text("Écoute avant de choisir.",style=MaterialTheme.typography.bodySmall)
            }
            if(a.kind==LearningKind.KEYBOARD) {
                PianoKeyboard(s){event(LearningEvent.Key(it))}
                Text("Ta sélection : ${s.notes.sorted().joinToString(" · "){noteLabel(it)}.ifEmpty{"aucune"}}",style=MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick={event(LearningEvent.ListenSelection)},enabled=s.notes.isNotEmpty()){Text("Écouter ma sélection")}
            }
            if(a.kind==LearningKind.SING) SingPanel(s,event)
            if(a.kind==LearningKind.RHYTHM) {
                OutlinedButton(onClick={event(LearningEvent.Metronome)}){Text("Écouter 8 clics · ${a.bpm} BPM")}
                Button(onClick={event(LearningEvent.Tap)},modifier=Modifier.fillMaxWidth().heightIn(min=100.dp)){Text("Frapper la pulsation · ${s.taps.size}/8")}
                TextButton(onClick={event(LearningEvent.ResetTaps)}){Text("Recommencer les appuis")}
            }
            if(a.kind !in listOf(LearningKind.LESSON,LearningKind.PRACTICE) && s.result!=true) {
                TextButton(onClick={event(LearningEvent.Hint)},enabled=s.hints<3){Text(if(s.hints==0)"Un indice" else if(s.hints<3)"Un peu plus d’aide" else "Les aides sont affichées")}
                if(s.hints>0) Text(when(s.hints) {
                    1->when(a.kind){LearningKind.KEYBOARD->"Cherche la fondamentale, puis compte les demi-tons. Majeur : 4 puis 3 ; mineur : 3 puis 4.";LearningKind.EAR->"Réécoute note par note et concentre-toi sur la distance entre les deux premières notes.";LearningKind.RHYTHM->"Relâche la main et garde un mouvement régulier. Le premier appui fixe le départ.";LearningKind.SING->"Écoute d’abord tes notes, respire, puis chante sans pousser : la note doit être stable avant de compter.";else->"Reprends la règle dans la leçon précédente."}
                    2->if(a.kind==LearningKind.KEYBOARD)"Les notes manquantes sont maintenant repérées sur le clavier. Tu gardes ta sélection." else a.explanation
                    else->a.explanation
                },style=MaterialTheme.typography.bodyMedium)
                if(s.hints>=3 && a.expectedNotes.isNotEmpty())OutlinedButton(onClick={event(LearningEvent.ListenBroken)}){Text("Écouter la démonstration")}
            }
            if(a.kind==LearningKind.PRACTICE)Text(if(s.module.instrument==Instrument.SINGER)"Cette étape est déclarative. Seul le responsable peut valider la compétence en situation." else "Cette étape est déclarative. Seul le responsable peut valider la compétence sur ton instrument.",style=MaterialTheme.typography.bodySmall)
        }
    }
}

/** Sung activity: the notes to sing from the singer's comfort note, the microphone, and the live pitch. */
@Composable private fun SingPanel(s:LearningUiState,event:(LearningEvent)->Unit) {
    val target=s.activity.sing?:return
    val permission=rememberMicrophonePermission()
    val notes=s.singTargets
    Text("À chanter : ${notes.joinToString(" · "){noteLabel(it)}} · ${formatSeconds(target.secondsEach)} chacune",style=MaterialTheme.typography.titleMedium)
    OutlinedButton(onClick={event(LearningEvent.ListenBroken)},enabled=!s.singing && !s.saving){Text(if(notes.size>1)"Écouter mes notes" else "Écouter ma note")}
    if(target.accompanimentSemitones!=null) Text("Mets des écouteurs : l’autre voix joue pendant que tu chantes, et le micro ne doit entendre que toi.",style=MaterialTheme.typography.bodySmall)
    if(s.singing) {
        OutlinedButton(onClick={event(LearningEvent.StopSinging)},modifier=Modifier.fillMaxWidth()){Text("Arrêter l’écoute")}
        Text("J’entends : ${s.heardMidi?.let{noteLabel(it)}?:"…"}",style=MaterialTheme.typography.bodyLarge,modifier=Modifier.semantics{liveRegion=LiveRegionMode.Polite})
        if(notes.size>1) Text("Note ${(s.singStep+1).coerceAtMost(notes.size)} / ${notes.size}",style=MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(progress={(s.singStep+(s.holdSeconds/target.secondsEach).coerceIn(0.0,1.0)).toFloat()/notes.size},modifier=Modifier.fillMaxWidth())
    } else if(s.sung) Text("Bien tenu ! Tu peux vérifier l’activité.",color=MaterialTheme.colorScheme.primary)
    else Button(onClick={permission.request { if(it)event(LearningEvent.StartSinging) else event(LearningEvent.MicrophoneDenied) }},enabled=!s.saving && s.result!=true,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Text("Chanter maintenant")}
    s.micError?.let { if(s.micDenied)MicrophoneDeniedNotice(it,permission.openSettings) else Text(it,color=MaterialTheme.colorScheme.error) }
    if(s.canPostpone && !s.sung) Text("Pas d’écouteurs ou trop de bruit autour de toi ? « Je le ferai plus tard » ouvre l’étape suivante. Cette activité restera à reprendre : elle ne compte pas comme réussie.",style=MaterialTheme.typography.bodySmall)
}
private fun formatSeconds(seconds:Double)=if(seconds==seconds.toInt().toDouble())"${seconds.toInt()} s" else "${seconds.toString().replace('.',',')} s"

@Composable private fun PianoKeyboard(s:LearningUiState,onKey:(Int)->Unit) {
    val whites=(60..84).filter {it%12 in listOf(0,2,4,5,7,9,11)}
    val blacks=(61..83).filter {it%12 in listOf(1,3,6,8,10)}
    val scroll=rememberScrollState()
    val density=LocalDensity.current
    val haptic=LocalHapticFeedback.current
    LaunchedEffect(s.activity.id,s.encounter) {
        val first=s.activity.expectedNotes.minOrNull()?:60
        scroll.scrollTo(with(density){(72.dp*whites.count{it<first}.coerceAtLeast(0)).roundToPx()})
    }
    Text("Fais glisser le clavier pour explorer les deux octaves.",style=MaterialTheme.typography.bodySmall)
    Box(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
        Box(Modifier.width(72.dp*whites.size).height(184.dp)) {
            Row {
                whites.forEach { midi -> PianoKey(midi,s,Modifier.width(72.dp).height(180.dp),false){
                    if(s.preferences.haptics)haptic.performHapticFeedback(HapticFeedbackType.LongPress);onKey(midi)
                } }
            }
            blacks.forEach { midi ->
                val boundary=whites.count{it<midi}
                PianoKey(midi,s,Modifier.offset(x=72.dp*boundary-24.dp).width(48.dp).height(108.dp),true){
                    if(s.preferences.haptics)haptic.performHapticFeedback(HapticFeedbackType.LongPress);onKey(midi)
                }
            }
        }
    }
}
@Composable private fun PianoKey(midi:Int,s:LearningUiState,modifier:Modifier,black:Boolean,onClick:()->Unit) {
    val selected=midi in s.notes
    val wrong=s.result==false && selected && midi !in s.activity.expectedNotes
    val missing=s.hints>=2 && midi in s.activity.expectedNotes && !selected
    val color=when {wrong->MaterialTheme.colorScheme.error;s.result==true && selected->Color(0xFF6FD3A0);selected->MaterialTheme.colorScheme.primary;missing->MaterialTheme.colorScheme.tertiary;black->Color(0xFF161320);else->Color(0xFFF4F3FA)}
    val label=noteLabel(midi)
    val keyColor by animateColorAsState(color, tween(if(s.preferences.reduceMotion)0 else 140), label="key-color")
    Box(modifier.background(keyColor,RoundedCornerShape(bottomStart=6.dp,bottomEnd=6.dp)).border(1.dp,Color(0xFF555164),RoundedCornerShape(bottomStart=6.dp,bottomEnd=6.dp))
        .toggleable(selected,enabled=!s.saving,role=Role.Checkbox,onValueChange={onClick()})
        .semantics {contentDescription=label;stateDescription=when{wrong->"sélectionnée, à corriger";selected->"sélectionnée";missing->"indice : note à ajouter";else->"non sélectionnée"}},contentAlignment=Alignment.BottomCenter) {
        Text(label,color=if(black && !selected && !missing)Color.White else Color(0xFF161320),style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(bottom=10.dp))
    }
}
@Preview @Composable private fun LearningPreview()=AppTheme {
    LearningScreenContent(LearningUiState(loading=false,allowed=true,index=1,mode=LearningMode.ACTIVITY),{},{})
}

@Composable private fun JourneyGoal(s:LearningUiState,event:(LearningEvent)->Unit) {
    AppCard {
        Text("Mon rythme",style=MaterialTheme.typography.titleMedium)
        Text("${s.progress.weeklyDays(s.today)} jour(s) de pratique ces 7 derniers jours · objectif ${s.preferences.weeklyTarget}")
        Text("Une journée compte dès qu’un exercice est enregistré. Plusieurs séances le même jour comptent une fois.",style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(Spacing.s)) {
            listOf(1,2,3).forEach { target -> FilterChip(selected=s.preferences.weeklyTarget==target,onClick={event(LearningEvent.Preferences(s.preferences.copy(weeklyTarget=target)))},label={Text("$target j / sem.")},modifier=Modifier.choiceState(s.preferences.weeklyTarget==target)) }
        }
        Text("Un repère personnel, à ajuster librement. Tu peux reprendre à tout moment.",style=MaterialTheme.typography.bodySmall)
    }
}
@Composable private fun StartingPoint(s:LearningUiState,event:(LearningEvent)->Unit) {
    var stage by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    AppCard {
        Text("Trouver mon point de départ",style=MaterialTheme.typography.titleMedium)
        Text("Trois repères facultatifs. Aucune compétence n’est validée ni aucune étape sautée.",style=MaterialTheme.typography.bodySmall)
        val questions=listOf("Quelles notes composent Do majeur ?","Combien de croches durent une noire ?","C–F–G un ton plus haut devient…")
        val answers=listOf(listOf("Do–Mi–Sol","Do–Mi♭–Sol"),listOf("4","2"),listOf("D–G–A","D–F–A"))
        if(stage<3) {
            Text(questions[stage],Modifier.padding(top=Spacing.s))
            answers[stage].forEachIndexed { index,answer -> OutlinedButton(onClick={if(index==listOf(0,1,0)[stage])score++;stage++}) {Text(answer)} }
        } else {
            val recommendation=when {score==0->"piano-chords";score<3->"piano-rhythm";else->"piano-chart"}
            Text("$score / 3 repères retrouvés. Suggestion : ${LearningCatalog.module(recommendation).title}. Tu peux explorer chaque module à ton rythme.")
            TextButton(onClick={event(LearningEvent.Preferences(s.preferences.copy(startingPoint=recommendation)))}) {Text("Conserver ce conseil")}
        }
        TextButton(onClick={event(LearningEvent.Preferences(s.preferences.copy(startingPoint=LearningCatalog.DEFAULT)))}) {Text("Je préfère commencer de zéro")}
    }
}
