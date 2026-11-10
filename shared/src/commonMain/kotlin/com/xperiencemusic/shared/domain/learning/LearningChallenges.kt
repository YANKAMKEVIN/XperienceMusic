package com.xperiencemusic.shared.domain.learning

import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.LearningModule
import com.xperiencemusic.shared.domain.model.MusicalChallenge

/** The challenge prepared at the end of a module, before sending a take to the responsable. */
fun LearningModule.challenge(): MusicalChallenge = if (instrument == Instrument.SINGER) {
    SingerLearning.challenge(this)
} else {
    when (id) {
        "piano-scales" -> MusicalChallenge(
            title = "Des repères fluides",
            steps = listOf(
                "Écoute les notes de Do majeur.",
                "Joue une octave lentement, mains séparées, puis ensemble si tu es à l’aise.",
                "Enregistre une prise sans forcer le tempo.",
            ),
            criteria = "Écoute la régularité et vérifie le doigté avec le responsable.",
            example = listOf(60, 62, 64, 65, 67, 69, 71, 72),
        )
        "piano-rhythm" -> MusicalChallenge(
            title = "Accompagner sans accélérer",
            steps = listOf(
                "Écoute le clic à 72 BPM.",
                "Joue C | Am | F | G, quatre temps par accord, pendant trois tours.",
                "Garde la pulsation pendant les changements et enregistre ta prise.",
            ),
            criteria = "Une pulsation stable et des changements sans arrêt.",
            example = listOf(60, 64, 67, 69, 72, 76, 65, 69, 72, 67, 71, 74),
        )
        "piano-transpose" -> MusicalChallenge(
            title = "Suivre un changement de tonalité",
            steps = listOf(
                "Joue C | F | G | C.",
                "Rejoue un ton plus haut : D | G | A | D.",
                "Enregistre les deux versions et explique le déplacement des degrés.",
            ),
            criteria = "Les relations I–IV–V sont conservées dans les deux tonalités.",
            example = listOf(60, 65, 67, 60, 62, 67, 69, 62),
        )
        "piano-chart" -> MusicalChallenge(
            title = "Accompagner un refrain",
            steps = listOf(
                "Lis C | Am | F | G avant de commencer.",
                "Compte quatre temps par accord et fais deux tours sans t’arrêter.",
                "Enregistre puis demande une autre grille au responsable.",
            ),
            criteria = "Anticiper les changements. Une grille nouvelle sera nécessaire à l’évaluation finale.",
            example = listOf(60, 64, 67, 69, 72, 76, 65, 69, 72, 67, 71, 74),
        )
        else -> MusicalChallenge(
            title = "Des accords qui s’enchaînent",
            steps = listOf(
                "Écoute les notes des accords C puis F puis G.",
                "Sur le piano, joue C | F | G | C, quatre temps par accord, à ton tempo.",
                "Fais deux tours puis enregistre celui qui représente le mieux ton travail.",
            ),
            criteria = "Des notes propres, une main détendue et des transitions régulières.",
            example = listOf(60, 64, 67, 65, 69, 72, 67, 71, 74, 60, 64, 67),
        )
    }
}
