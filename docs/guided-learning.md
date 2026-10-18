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
jamais la validation sur l’instrument. Les autres modules invitent à ouvrir leur propre statut
plutôt que d’inventer une validation à partir de leurs exercices.

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
