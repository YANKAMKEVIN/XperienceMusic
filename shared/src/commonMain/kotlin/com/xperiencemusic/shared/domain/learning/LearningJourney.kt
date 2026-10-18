package com.xperiencemusic.shared.domain.learning

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

enum class Mastery(val label:String,val explanation:String) {
    DISCOVER("À découvrir", "Commence par les repères et les premières activités."),
    PRACTISING("En pratique", "Tu as commencé les exercices. La pratique régulière construit tes repères."),
    CONSOLIDATING("À consolider", "Certaines activités demandent encore de l’aide ou une révision. Une réussite à l’écran ne valide pas le jeu réel."),
    VALIDATED("Validée par le responsable", "Le responsable a confirmé la compétence sur ton instrument. Continue à l’entretenir.")
}
fun LearningProgress.mastery(module:LearningModule,status:ExerciseStatus?,today:LocalDate):Mastery {
    if(status==ExerciseStatus.VALIDATED)return Mastery.VALIDATED
    val latest=attempts.filter { a -> a.version==PianoBasics.VERSION && module.activities.any { it.id==a.activityId } }.groupBy { it.activityId }.values.map { it.last() }
    if(latest.any { !it.passed || it.hints>0 })return Mastery.CONSOLIDATING
    if(status==ExerciseStatus.TO_REWORK || complete(module) || dueIn(module,today).isNotEmpty())return Mastery.CONSOLIDATING
    return if(completedIn(module).isEmpty())Mastery.DISCOVER else Mastery.PRACTISING
}
fun LearningProgress.weeklyDays(today:LocalDate):Int = attempts.map { it.day }.distinct().count { it.daysUntil(today) in 0..6 }
fun LearningProgress.returning(today:LocalDate):Boolean = attempts.maxOfOrNull { it.day }?.daysUntil(today)?.let { it>=7 }==true

data class MusicalChallenge(val title:String,val steps:List<String>,val criteria:String,val example:List<Int>)
fun LearningModule.challenge():MusicalChallenge = when(id) {
    "piano-scales" -> MusicalChallenge("Des repères fluides",listOf("Écoute les notes de Do majeur.","Joue une octave lentement, mains séparées, puis ensemble si tu es à l’aise.","Enregistre une prise sans forcer le tempo."),"Écoute la régularité et vérifie le doigté avec le responsable.",listOf(60,62,64,65,67,69,71,72))
    "piano-rhythm" -> MusicalChallenge("Accompagner sans accélérer",listOf("Écoute le clic à 72 BPM.","Joue C | Am | F | G, quatre temps par accord, pendant trois tours.","Garde la pulsation pendant les changements et enregistre ta prise."),"Une pulsation stable et des changements sans arrêt.",listOf(60,64,67,69,72,76,65,69,72,67,71,74))
    "piano-transpose" -> MusicalChallenge("Suivre un changement de tonalité",listOf("Joue C | F | G | C.","Rejoue un ton plus haut : D | G | A | D.","Enregistre les deux versions et explique le déplacement des degrés."),"Les relations I–IV–V sont conservées dans les deux tonalités.",listOf(60,65,67,60,62,67,69,62))
    "piano-chart" -> MusicalChallenge("Accompagner un refrain",listOf("Lis C | Am | F | G avant de commencer.","Compte quatre temps par accord et fais deux tours sans t’arrêter.","Enregistre puis demande une autre grille au responsable."),"Anticiper les changements. Une grille nouvelle sera nécessaire à l’évaluation finale.",listOf(60,64,67,69,72,76,65,69,72,67,71,74))
    else -> MusicalChallenge("Des accords qui s’enchaînent",listOf("Écoute les notes des accords C puis F puis G.","Sur le piano, joue C | F | G | C, quatre temps par accord, à ton tempo.","Fais deux tours puis enregistre celui qui représente le mieux ton travail."),"Des notes propres, une main détendue et des transitions régulières.",listOf(60,64,67,65,69,72,67,71,74,60,64,67))
}
