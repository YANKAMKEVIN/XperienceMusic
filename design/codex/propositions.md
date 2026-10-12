# Xperience Music — Propositions d’écrans

Les trois planches présentent neuf écrans portrait, de gauche à droite dans l’ordre demandé. Les données et paroles sont fictives. Les vues Semaine et Bibliothèque montrent les actions d’un administrateur ; la progression concerne une chantre ; la gestion des membres est réservée à Admin++.

## Direction commune

Fond nuit #0B0A14, halo violet doux en haut, surfaces #15131F et #221F32, contours #2E2A44, texte #F4F3FA et secondaire #A9A6BD. Les accents #9B7BFF et #6E98FF accompagnent le dégradé #B46CFF → #6D5BFF → #3D7BFF. Le vert #6FD3A0 signifie réussite ou état opérationnel. Typographie sans-serif, cartes arrondies et espacements généreux reprennent la connexion validée. Le monogramme reste un repère ponctuel ; le grand bloc logotype et signature n’est pas répété sur chaque écran.

Les vagues sont un motif sonore : contexte de semaine, écoute, progression et retour du micro. Elles ne doivent jamais gêner la lecture. Les boutons secondaires restent sobres pour que le dégradé signale les actions principales. Les maquettes sont des propositions raster : les couleurs exactes, contrastes AA, tailles de texte adaptatives et cibles tactiles d’au moins 48 px devront être vérifiés dans Compose sur appareils réels. Les contours de téléphone appartiennent à la présentation, pas à l’interface.

## Planche 1 — Semaine, Bibliothèque, Détail d’un chant

### Semaine

- **Mise en page et hiérarchie :** marque compacte, titre et période, état hors connexion, deux cartes de rendez-vous, puis setlist. La liste numérotée est le contenu principal ; les dates restent immédiatement repérables.
- **Composants :** bandeau hors connexion conditionnel, cartes mardi/dimanche, tonalités et tempos par chant, lecture rapide individuelle et « Tout lire », bouton « Modifier la setlist ». Mini-lecteur fixé au-dessus des trois onglets.
- **Vagues et dégradé :** une courte séparation sonore sous les rendez-vous ; accents violet/bleu sur les tonalités et l’onglet actif. L’édition reste secondaire.
- **À valider :** setlist commune ou distincte pour mardi et dimanche ; affichage des dates et horaires ; état vide avec « Publier la setlist » ; sens du bandeau lorsque seule une partie des fichiers est téléchargée. Masquer l’action d’édition aux membres.

### Bibliothèque

- **Mise en page et hiérarchie :** titre, ajout administrateur, recherche et filtres précèdent une liste verticale. Le titre du chant domine les métadonnées tonalité/tempo.
- **Composants :** champ de recherche, filtres tonalité/tempo/déjà joué, cartes ouvrant le détail, bouton « + », mini-lecteur et onglets persistants. Prévoir le libellé accessible « Ajouter un chant » pour le bouton icône.
- **Vagues et dégradé :** aucune vague ; dégradé réservé à l’ajout et au monogramme. Les cartes restent presque noires.
- **À valider :** combinaison des filtres, définition de « déjà joué », tri et remise à zéro ; états sans résultat et chargement ; visibilité de l’ajout uniquement pour les administrateurs.

### Détail d’un chant

- **Mise en page et hiérarchie :** titre et tonalité, transposition, notation des accords, extrait des paroles, enregistrements, pistes par voix, puis accès au mode scène. Contenu défilant ; le bloc paroles n’est qu’un aperçu du chant complet.
- **Composants :** retour, boutons −/+ par demi-ton, segments Nashville/Do-Ré-Mi/C-D-E, accords ancrés au-dessus des syllabes, trois versions dont une référence, commandes de lecture et téléchargement, quatre pistes vocales séparées.
- **Vagues et dégradé :** aucune vague pour préserver l’alignement musical ; segment actif et bouton « Mode scène » en dégradé.
- **À valider :** notation Nashville pour les accords mineurs et altérations, choix dièses/bémols, persistance de la transposition, lecture et téléchargement des pistes vocales. Les pistes ne comptent pas dans la limite de trois versions. Prévoir l’accès administrateur à l’édition du chant et des versions, non développé dans ces neuf vues.

## Planche 2 — Lecteur plein écran, Mode scène, Ma progression

### Lecteur plein écran

- **Mise en page et hiérarchie :** identification de la version et du chant en haut, grande zone de marque au centre, disponibilité hors connexion, progression et commandes en bas. Pas d’onglets dans cette vue modale.
- **Composants :** fermeture vers le mini-lecteur, menu, durée écoulée/totale, curseur, précédent/pause/suivant, répétition et volume. Le texte « Lecture en arrière-plan » rappelle la continuité écran éteint.
- **Vagues et dégradé :** vagues centrales associées à l’écoute, sans remplacer une forme d’onde temporelle ; dégradé sur le monogramme, la progression et la commande principale.
- **À valider :** précédent/suivant entre chants ou versions ; modes de répétition ; vitesse de lecture ; téléchargement en cours et fichiers indisponibles. Vérifier la lecture écran éteint et les commandes système sur Android et iOS. Réduire ou arrêter l’animation selon les préférences d’accessibilité.

### Mode scène

- **Mise en page et hiérarchie :** plein écran dédié aux paroles et accords, petit en-tête avec tonalité et position dans la setlist. Le passage actif est blanc ; la suite est atténuée. Commandes de lecture visuelle et changement de chant en bas.
- **Composants :** texte agrandi, accords alignés, A−/A+, défilement automatique avec vitesse, précédent/suivant et titre du prochain chant. Prévoir un accès manuel au défilement et à la reprise.
- **Vagues et dégradé :** aucune vague ; accents violet sobres sur les accords. Le halo reste doux, sans grande action dégradée dans la zone de lecture.
- **À valider :** lisibilité à distance sur pupitre, contraste du passage atténué, taille maximale, écran maintenu allumé, comportement du défilement après interaction et lors d’un changement de chant. La capture affiche des barres système : tester leur masquage pour le véritable mode immersif.

### Ma progression

- **Mise en page et hiérarchie :** prénom, voix et niveau, carte de régularité, puis exercices. Les statuts sont écrits pour rester compréhensibles indépendamment de la couleur.
- **Composants :** pastilles voix/niveau, indicateurs de jours, objectif hebdomadaire, cartes « À faire », « Validé », « À retravailler », accès au retour de Mélanie, bouton « Commencer l’exercice », onglet actif Progression.
- **Vagues et dégradé :** petite vague à côté de l’identité vocale ; dégradé sur l’action de démarrage. Vert réservé au statut validé.
- **À valider :** règles de niveau et de régularité, classement des exercices, exercice choisi par l’action principale, consultation des retours Admin++. Le compteur « 2 / 3 » désigne une activité hebdomadaire à définir ; la planche ne montre qu’un exercice validé. Définir la navigation pour les membres non chantres.

## Planche 3 — Exercice au micro, Éditeur de setlist, Gestion des membres

### Exercice au micro

- **Mise en page et hiérarchie :** consigne et note cible, grande jauge centrée sur zéro, justesse instantanée, temps tenu et commande d’arrêt. Le retour reste utilisable sans lire une courbe complexe.
- **Composants :** note/fréquence, graduation en cents, zone de tolérance verte, curseur et valeur signée, chronomètre, barre de progression, état micro et validation automatique. La vue représente une tentative en cours.
- **Vagues et dégradé :** les vagues signalent l’activité sonore ; la jauge fournit la mesure. Le dégradé accompagne le temps écoulé et le bouton d’arrêt ; le vert indique la justesse.
- **À valider :** convention d’octave (« La3 · 220 Hz » est ici la notation scientifique), note adaptée à la voix, tolérance en cents, durée illustrative de huit secondes, interruptions et bruit ambiant. Prévoir permission refusée, absence de signal, résultat et nouvel essai. Distinguer validation automatique de cet exercice et validation pédagogique par Admin++ selon le type d’exercice.

### Éditeur de setlist

- **Mise en page et hiérarchie :** semaine et événements, liste ordonnée au centre, ajout, puis bloc brouillon/publication. La publication est l’action la plus forte.
- **Composants :** poignées de déplacement, numéros, titres, sélecteurs de tonalité, menus par chant, ajout et bouton « Publier la setlist ». Les tonalités sont propres à cette setlist.
- **Vagues et dégradé :** aucune vague ; dégradé limité à la publication. Les commandes d’organisation restent sur surfaces sombres.
- **À valider :** sauvegarde du brouillon, édition après publication, conflits entre administrateurs, changement des dates et suppression d’une ligne. Prévoir des actions « Monter/Descendre » accessibles en complément du glisser et des messages de publication réussie ou échouée. Accès Admin contenu et Admin++.

### Gestion des membres

- **Mise en page et hiérarchie :** effectif et rôle courant, invitation par e-mail, recherche, liste des membres et rappel des droits. Les initiales remplacent les photos pour garder une présentation calme.
- **Composants :** champ e-mail, invitation, recherche, nom/rôle/voix, accès au changement de rôle, invitation en attente et renvoi. Le rappel synthétique complète les libellés ; Admin++ hérite aussi des droits de contenu.
- **Vagues et dégradé :** aucune vague ; dégradé sur « Inviter », accents violet/bleu pour les avatars et états d’attente.
- **À valider :** choix du rôle à l’invitation, expiration/renvoi, doublons, révocation d’accès et garde-fou pour le dernier Admin++. Clarifier l’effectif : vingt membres actifs ou invitations incluses. L’écran est réservé à Admin++.

## Prompts et méthode de génération

Outil utilisé : génération d’images intégrée, avec `s24-connexion.png` et `AppIcon-1024.png` comme références visuelles. Une génération par planche, puis corrections ciblées de palette et d’iconographie. Aucun écran de connexion n’a été recréé.

Le socle des trois prompts demandait : « Planche de trois maquettes mobiles portrait côte à côte, frontales, mêmes proportions 415:900, français lisible, fond #0B0A14 et halo violet doux, surfaces #15131F/#221F32, contours #2E2A44, texte #F4F3FA/#A9A6BD, dégradé #B46CFF → #6D5BFF → #3D7BFF, accents #9B7BFF/#6E98FF, succès #6FD3A0, sans-serif sobre, coins 16 px, beaucoup d’air ; reprendre strictement les références ; vagues fines uniquement aux emplacements utiles ; aucun symbole religieux. »

- **Planche 1 :** Semaine du 13 au 18 octobre, rendez-vous mardi/dimanche, hors connexion, quatre chants avec tonalités et lectures, modification et mini-lecteur ; Bibliothèque avec recherche, trois filtres et ajout ; détail « Ta lumière », transposition, trois notations, paroles et accords, trois enregistrements, quatre pistes vocales et mode scène.
- **Planche 2 :** Lecteur « Ta lumière », référence téléchargée, monogramme et vagues, progression et commandes ; mode scène avec grands accords/paroles, position 2/4, taille et défilement, précédent/suivant ; progression de Sarah, alto niveau 2, régularité, quatre exercices et trois statuts.
- **Planche 3 :** Exercice de tenue La3 à 220 Hz, jauge en cents, +3 cents, 6,2/8 secondes et micro actif ; éditeur avec quatre chants déplaçables et tonalités, ajout, brouillon et publication ; vingt membres, invitation e-mail, recherche, rôles, prénoms et invitation en attente.
- **Corrections ciblées :** remplacer l’icône d’église de la carte Culte par un calendrier ; uniformiser les pastilles de tonalité en violet/bleu ; remplacer les accents orange d’attente/brouillon par violet et l’avatar vert par bleu, en conservant la composition et les textes.
