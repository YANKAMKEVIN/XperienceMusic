package com.xperiencemusic.shared.data.seed

import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.Objective

/**
 * What a musician shows the super admin, in rehearsal or at a service, to move up. Ids are stored
 * on the server: keep them when rewording.
 */
internal object MusicianObjectives {
    private fun o(id: String, instrument: Instrument, level: Level, order: Int, title: String, description: String) =
        Objective(id, instrument, level, order, title, description)

    val all: List<Objective> = listOf(
        // Piano · vers Confirmé
        o("piano-c1", Instrument.PIANO, Level.CONFIRMED, 1, "Les accords de la setlist", "Jouer tous les accords de la setlist du dimanche sans hésiter, en suivant la grille."),
        o("piano-c2", Instrument.PIANO, Level.CONFIRMED, 2, "Tenir le tempo", "Garder un tempo régulier sur un chant entier, du couplet au dernier refrain."),
        o("piano-c3", Instrument.PIANO, Level.CONFIRMED, 3, "Transposer d'un ton", "Rejouer un chant connu un ton plus haut ou plus bas, à la demande."),
        o("piano-c4", Instrument.PIANO, Level.CONFIRMED, 4, "Intro et fin", "Lancer un chant par une intro et le terminer proprement, sans signal du responsable."),
        // Piano · vers Pro
        o("piano-p1", Instrument.PIANO, Level.PRO, 1, "Accords enrichis", "Utiliser renversements et accords de septième pour rendre la grille plus riche, sans gêner les voix."),
        o("piano-p2", Instrument.PIANO, Level.PRO, 2, "Jouer sans grille", "Accompagner un chant connu sans regarder la grille."),
        o("piano-p3", Instrument.PIANO, Level.PRO, 3, "Louange libre", "Accompagner un temps de louange libre du vendredi, en suivant le chantre."),
        o("piano-p4", Instrument.PIANO, Level.PRO, 4, "Mener le groupe", "Diriger les enchaînements entre les chants pendant un service."),
        // Basse · vers Confirmé
        o("bass-c1", Instrument.BASS, Level.CONFIRMED, 1, "Les fondamentales", "Jouer la fondamentale de chaque accord de la setlist, au bon moment."),
        o("bass-c2", Instrument.BASS, Level.CONFIRMED, 2, "Caler avec la batterie", "Jouer en place avec la grosse caisse sur un chant entier."),
        o("bass-c3", Instrument.BASS, Level.CONFIRMED, 3, "Suivre les nuances", "Jouer moins fort sur les couplets doux et monter avec le groupe."),
        o("bass-c4", Instrument.BASS, Level.CONFIRMED, 4, "Les fins de chant", "Terminer un chant en même temps que le groupe, sans flottement."),
        // Basse · vers Pro
        o("bass-p1", Instrument.BASS, Level.PRO, 1, "Lignes de passage", "Relier les accords par des notes de passage qui servent le chant."),
        o("bass-p2", Instrument.BASS, Level.PRO, 2, "Jouer sans grille", "Accompagner un chant connu sans regarder la grille."),
        o("bass-p3", Instrument.BASS, Level.PRO, 3, "Louange libre", "Suivre un temps de louange libre du vendredi en trouvant la tonalité à l'oreille."),
        o("bass-p4", Instrument.BASS, Level.PRO, 4, "Changements de tonalité", "Suivre une modulation en direct sans perdre le groupe."),
        // Batterie · vers Confirmé
        o("drums-c1", Instrument.DRUMS, Level.CONFIRMED, 1, "Un rythme régulier", "Tenir un rythme de base régulier sur un chant entier, sans accélérer."),
        o("drums-c2", Instrument.DRUMS, Level.CONFIRMED, 2, "Les transitions", "Marquer le passage couplet → refrain par un break simple."),
        o("drums-c3", Instrument.DRUMS, Level.CONFIRMED, 3, "Jouer doux", "Accompagner un chant calme aux balais ou en jouant très léger."),
        o("drums-c4", Instrument.DRUMS, Level.CONFIRMED, 4, "Les fins de chant", "Arrêter le groupe proprement sur la dernière mesure."),
        // Batterie · vers Pro
        o("drums-p1", Instrument.DRUMS, Level.PRO, 1, "Varier les grooves", "Adapter le rythme à chaque chant de la setlist (ballade, 6/8, rythme enlevé)."),
        o("drums-p2", Instrument.DRUMS, Level.PRO, 2, "Construire la dynamique", "Faire monter l'intensité sur un chant, du premier couplet au dernier refrain."),
        o("drums-p3", Instrument.DRUMS, Level.PRO, 3, "Louange libre", "Suivre un temps de louange libre du vendredi sans plan écrit."),
        o("drums-p4", Instrument.DRUMS, Level.PRO, 4, "Tenir le groupe", "Garder le tempo du groupe pendant tout un service, même quand il s'emballe."),
    )
}
