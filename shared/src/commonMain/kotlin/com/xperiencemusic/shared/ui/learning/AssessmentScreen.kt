package com.xperiencemusic.shared.ui.learning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.learning.AssessmentViewModel
import com.xperiencemusic.shared.ui.platform.rememberMicrophonePermission
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AssessmentScreen(moduleId:String,onBack:()->Unit,vm:AssessmentViewModel=koinViewModel(key="assessment-$moduleId"){parametersOf(moduleId)}) {
    val level by vm.level.collectAsStateWithLifecycle()
    val seconds by vm.recordedSeconds.collectAsStateWithLifecycle()
    val sending by vm.sending.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val permission=rememberMicrophonePermission()
    Scaffold(topBar={TopAppBar(title={Text("Mon jeu sur l’instrument")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Retour")}},windowInsets=WindowInsets(0))},contentWindowInsets=WindowInsets(0)) {padding->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.l),verticalArrangement=Arrangement.spacedBy(Spacing.m)) {
            AppCard {
                Text(vm.module.title,style=MaterialTheme.typography.headlineSmall)
                Text(vm.module.activities.last().explanation,Modifier.padding(top=Spacing.m))
                Text("Choisis un endroit calme. Pose le téléphone près de l’instrument, joue puis arrête. La prise est partagée seulement avec toi et le responsable habilité. Durée : 3 secondes à 5 minutes.",Modifier.padding(top=Spacing.m))
            }
            if(level.isRecording) {
                Text("Enregistrement · ${level.seconds.toInt()} s",style=MaterialTheme.typography.headlineSmall)
                LinearProgressIndicator(progress={level.level},modifier=Modifier.fillMaxWidth())
                GradientButton(onClick=vm::stop){Text("Arrêter")}
            } else {
                GradientButton(onClick={permission.request {if(it)vm.start() else vm.denied()}},enabled=!sending){Text(if(seconds>0)"Refaire la prise" else "Enregistrer mon jeu")}
                if(seconds>=3) {
                    OutlinedButton(onClick=vm::listen,enabled=!sending){Text("Réécouter · $seconds s")}
                    OutlinedButton(onClick=vm::send,enabled=!sending){Text(if(sending)"Envoi…" else "Envoyer pour évaluation")}
                } else if(seconds>0)Text("La prise est trop courte. Enregistre au moins 3 secondes.")
            }
            message?.let{Text(it)}
            Text("Tu peux aussi demander une évaluation en répétition sans envoyer d’enregistrement.",style=MaterialTheme.typography.bodySmall)
        }
    }
}
