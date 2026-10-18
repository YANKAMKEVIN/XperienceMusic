# Parcours piano guidé

Le parcours conserve le thème nuit, les dégradés, les cartes et les boutons de l’application.
Il comprend cinq modules reliés aux objectifs existants : accords (`piano-c2`), gammes et repères
(`piano-c1`), rythme et tempo (`piano-c3`), lecture de grille (`piano-c4`) et transposition (`piano-c5`).
Les identifiants des dix activités historiques sont conservés ; les réussites précédentes restent acquises.

## Expérience

L’accueil indique le temps estimé (deux minutes par activité) et le premier objectif de la séance : une courte suite de découvertes et de
pratique, avec une révision échue si disponible. Une seule activité est affichée à la fois.
Le plan reste accessible via « Voir les étapes ». Un bilan distingue les activités travaillées
et celles à reprendre, avec les choix terminer ou continuer librement.

La reprise conserve l’activité, la variante, la sélection et les indices déjà demandés sur le
même téléphone. Une interruption ne retire aucun progrès. Le prochain exercice est enregistré
lorsqu’une activité est réussie. La taille du texte suit le système, avec un agrandissement
supplémentaire facultatif. Vibrations, son des touches, son de réussite et réduction des animations
sont réglables dans « Confort ». Le son de réussite est désactivé par défaut.

Le clavier défilant couvre Do4 à Do6 et utilise des touches tactiles d’au moins 48 dp de large.
Les notes sélectionnées sonnent si l’option est active. L’accord complet et les notes séparées
sont également disponibles. Les états à conserver ou corriger sont indiqués par du texte et
par les informations du lecteur d’écran, pas seulement par la couleur.

## Pratique et révision

La première rencontre avec une activité historique reste identique. Les rencontres suivantes
font tourner la fondamentale des accords et la qualité majeur/mineur des écoutes. Les renversements
et les octaves sont contrôlés ; une note supplémentaire rend la réponse incorrecte.

Après une erreur, la sélection reste en place. La correction précise les notes justes, les notes
en trop, un décalage de tierce ou une erreur d’octave. Trois niveaux d’aide sont disponibles :
une piste, un repérage plus direct, puis la solution et sa démonstration sonore.
Les réussites avec aide sont conservées comme pratique, sans les présenter comme une maîtrise autonome.

Les révisions sont calculées par activité à partir de l’historique. Une erreur ou une réussite
avec aide revient après un jour. Les réussites autonomes s’espacent à 3, 7, 14 puis 30 jours.
Répéter plusieurs fois le même jour n’augmente pas artificiellement cet intervalle.
Le rythme comporte huit appuis à 72 BPM, évalués sur la régularité des intervalles (tolérance moyenne
18 %) : cela ne mesure pas le jeu au piano ni un alignement avec un clic extérieur.

Les leçons lues et la pratique physique restent déclaratives. La fin d’un module permet de
demander une évaluation réelle, mais ne valide automatiquement ni compétence ni niveau.
Les nouveaux modules sont des introductions guidées ; l’objectif complet est vérifié par le responsable.

## Prises audio et retours

Une fois le module terminé, le membre peut enregistrer son instrument (3 secondes à 5 minutes),
réécouter, refaire et envoyer sa prise au responsable. La prise est liée à l’objectif ; le responsable
la retrouve dans « À valider » avec le critère, les boutons de décision et le commentaire existants.
Le retour et le statut sont visibles dans le module du membre.

La prise est privée : seul son propriétaire et un Admin++ peuvent lire les métadonnées et le fichier.
Un admin contenu n’accède pas aux prises des autres membres. Après validation, la prise ne peut plus
être remplacée via l’API. Une demande en répétition reste possible sans enregistrement.
L’envoi demande une connexion. Un fichier envoyé dont la demande a échoué reste disponible pour relancer
la demande sans refaire la prise. Une prise non envoyée n’est pas conservée après fermeture de son écran.

## Stockage et déploiement

Les essais sont enregistrés localement avant synchronisation et restent utilisables hors connexion
sur le téléphone déjà connecté. Le marqueur d’attente permet de relancer la synchronisation.
Les tentatives Supabase sont privées et immuables. Variante et nombre d’indices sont ajoutés avec
des valeurs par défaut pour les anciennes données. La déconnexion efface aussi les essais locaux
pas encore synchronisés, les préférences, les reprises et les prises de préécoute.

Appliquer les migrations dans l’ordre :

1. `20261018000000_recording_integrity_and_private_audio.sql`
2. `20261018010000_learning_attempts.sql`
3. `20261018020000_learning_feedback_and_recordings.sql`
4. `20261018030000_objective_recording_history.sql`

Les tests couvrent les réponses musicales, les variantes, les révisions, la reprise sans pénalité,
la synchronisation, la confidentialité et le gel des prises validées.
Vérifications à faire sur Android et iPhone : clavier et défilement, sons, autorisation du micro,
prise et écoute par le responsable, mode hors connexion, grand texte, lecteur d’écran et réduction
système des animations. Les builds seuls ne valident pas cette expérience sur un appareil réel.

## Maîtrise, objectifs et défis

Les statuts distinguent découverte, pratique, consolidation et validation par le responsable.
Un bouton explique la raison du statut. La fin des exercices, même sans erreur, ne remplace
jamais la validation sur l’instrument. La liste des modules affiche aussi le statut de chaque compétence à partir des décisions du responsable,
sans déduire une validation des seules réussites aux exercices.

Un bilan facultatif de trois questions propose un point de départ sans débloquer ni valider
une étape. Le conseil et l’objectif de un à trois jours de pratique par semaine restent locaux,
par membre, dans les préférences existantes. Le compteur porte sur les sept derniers jours,
un jour distinct avec un essai enregistré comptant une fois ; il ne mesure pas une durée de jeu.
Après sept jours sans essai, la séance est limitée à une activité pour une reprise douce.

Chaque module propose un défi concret, une grille ou des consignes, trois étapes et des critères
d’écoute. La démonstration sonore énonce les notes successivement : ce n’est pas une piste
d’accompagnement rythmée. L’envoi reste lié à l’objectif et à sa validation humaine.
Le retour du responsable est présenté comme le prochain travail ; son champ de commentaire
invite à nommer une réussite, un point précis et l’essai attendu.

L’historique conserve chaque prise **envoyée**, datée, avec écoute séparée. Les prises précédentes
non conservées par les versions antérieures ne peuvent pas être récupérées. La migration récupère
la prise actuelle existante. La table courante garde la dernière preuve d’évaluation ; la table
historique est alimentée par un trigger et ne permet aux membres ni modification ni suppression.
Les règles privées propriétaire/Admin++ s’appliquent aux deux. Les anciens fichiers restent
protégés contre la suppression par leur propriétaire ; la modération Admin++ reste possible.

## Atelier piano : correction au micro

Depuis « Gammes et repères », « Essayer la correction automatique » ouvre un atelier local.
Il propose Do, Sol et Fa majeur sur une octave, en montée ou en descente, avec une main, sans
pédale ni accompagnement. Le tempo (40, 60 ou 80 BPM) règle l’exemple sonore et les repères
visuels. Ce guide ne mesure pas une synchronisation exacte au métronome. Le profil piano peut
l’essayer avant la fin du module. Aucun envoi serveur ni validation de compétence n’est déclenché.
Aucune migration supplémentaire n’est nécessaire.

### Avant la prise

Le bouton « Préparer le micro » demande l’autorisation, arrête toute lecture, puis ouvre un test
de signal. Le membre joue une note pour vérifier le niveau. Le bouton de départ s’active après
au moins 150 ms cumulées de son non saturé ; cela confirme un signal, pas son origine ni la
qualité d’un piano. Un signal saturé bloque le départ. Le test expire après 20 secondes et
n’est pas conservé. Le compte à rebours de trois secondes est visuel et annulable ; ses données
micro sont elles aussi exclues de l’enregistrement. Aucun son de l’app n’est joué pendant la prise.

L’enregistrement commence au signal « À toi ». Les notes attendues servent de repères visuels
au tempo choisi. La prise s’arrête à la demande ou après 30 secondes au maximum, même si le
micro ne fournit plus de trames. Le micro est libéré avant le bilan et la réécoute. Le téléphone
reste éveillé pendant le test micro, le compte à rebours et la prise.

### Comprendre puis retravailler

Le bilan affiche les notes dans leur ordre d’exécution, avec un libellé pour chaque résultat :
repérée, à vérifier, octave, non repérée ou ajout détecté. Toucher une note sélectionne son détail
et réécoute un extrait de la prise contenant le passage, limité à six secondes. Une note non
repérée utilise le contexte des notes voisines ; les temps et découpages sont approximatifs.
Le membre peut entendre séparément la note attendue ou arrêter l’écoute.

« Travailler autour de cette note » ouvre un exercice de trois notes dans la même gamme, le
même sens et au même tempo, avec le contexte disponible aux extrémités. Ce fragment a sa propre
séquence attendue et son propre bilan ; il n’est pas comparé à une gamme entière. Un bouton
permet de revenir à la gamme complète. Les notes supplémentaires utilisent la position attendue
voisine pour proposer un fragment.

Deux essais exploitables du même exercice et au même tempo peuvent être comparés : notes
attendues retrouvées, notes de nouveau repérées et notes à vérifier encore. Le dernier bilan
exploitable sert de référence ; une prise ambiguë n’affiche pas de comparaison et ne remplace
pas cette référence. Changer de gamme, de sens, de tempo ou de fragment efface le bilan de
référence et l’audio. Ce retour compare des détections, pas une maîtrise musicale définitive.

### Analyse et limites

YIN estime une fondamentale monophonique après réduction de fréquence avec moyennage. Le moteur
écarte les fenêtres faibles, peu périodiques, trop éloignées d’une note tempérée ou hors du registre
Do3–Do7. Une note doit rester repérée au moins 180 ms. L’alignement par distance d’édition utilise
la séquence de l’exercice choisi. Il distingue note différente, octave différente, note non
repérée et ajout. L’espacement n’est commenté que lorsque toutes les notes attendues sont
retrouvées dans l’ordre, sans ajout. Le doigté, la posture et le toucher ne sont pas évalués.

Une prise trop courte, trop faible, saturée ou peu exploitable ne reçoit aucun verdict sur les
notes. Accords, pédale, bruits périodiques et harmoniques peuvent encore tromper la détection.
Les tests utilisent des signaux synthétiques à plusieurs fréquences d’échantillonnage, dont un
son avec harmoniques et décroissance ; ils ne remplacent pas un essai sur de vrais pianos.

### Données et interruptions

L’analyse et l’encodage des extraits tournent hors du thread UI. Le téléphone conserve au maximum
une prise locale et un bilan de comparaison sans audio précédent. Le test micro et le compte à
rebours ne sont jamais ajoutés à la prise. Les fichiers de préécoute (prise entière et extrait)
sont privés et écrasés ou supprimés au nouvel essai/changement d’exercice. Sortie, changement de
compte et arrière-plan annulent test, compte à rebours, capture, analyse et découpage en cours,
effacent les deux bilans, libèrent le micro et suppriment les préécoutes. Revenir ne redémarre
jamais automatiquement l’enregistrement.

Vérification manuelle Android/iPhone : refus/autorisation micro ; test faible/saturé ; annulation
et interruption du compte à rebours ; gamme correcte puis Fa♯ au lieu de Fa en Do ; octave
incorrecte ou note omise ; toucher les notes du bilan et comparer l’extrait avec la prise ;
travailler un fragment puis refaire la gamme ; changer tempo/gamme/sens ; comparer deux essais ;
sortir pendant l’analyse ou l’encodage de l’extrait. Tester sur pianos acoustiques et numériques.
Ne pas diffuser l’exemple sur une enceinte pendant la prise : le micro peut reconnaître cette source.
