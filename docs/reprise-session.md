# Reprise de session : Xperience Music

Colle ce texte au début d'une nouvelle session Claude Code ouverte dans `~/dev/XperienceMusic`.

---

On continue l'app **Xperience Music** (KMP + Compose Multiplatform, Android + iOS) pour le groupe
de louange de mon église. Réponds-moi toujours en français. Lis d'abord `README.md`,
`docs/supabase-setup.md` et `git log --oneline -20` pour te remettre dans le contexte.

## Où on en est

Tout est enregistré dans git (voir `git log` pour le dernier commit).

- **Direction artistique validée.** Fond nuit #0B0A14, dégradé violet → bleu (#B46CFF → #6D5BFF → #3D7BFF),
  monogramme XM, vagues lumineuses (`ui/brand/`). Les maquettes de Codex sont dans `design/codex/`.
- **Connexion.**
  - Code e-mail à 6 chiffres, saisi dans 6 cases qui se figent pendant la vérification et deviennent rouges si le code est faux.
  - Champ e-mail en pilule, avec la flèche d'envoi intégrée.
  - Le lien du mail passe par une page relais en https, `https://yankamkevin.github.io/xperience-music-connexion/`
    (dépôt `YANKAMKEVIN/xperience-music-connexion`), car Gmail retire les liens `xperiencemusic://`.
- **Accueil (onboarding).** Une question par écran en 4 étapes : prénom en grand et centré, rôle,
  voix (colonnes de hauteur) et niveau.
- **Navigation.** Onglets dans une pilule flottante ; le mini-lecteur est une carte flottante au-dessus.
  Lecteur plein écran, mode scène sans barres système.
- **Setlists.** Une setlist par dimanche. Les semaines à venir peuvent être publiées à l'avance,
  les semaines passées restent lisibles. Pas de filtre tempo/BPM en V1.
- **États vides.** Illustration lumineuse ; les membres voient un compte à rebours jusqu'à la répétition.
- **Ma progression (chantres).**
  - 5 paliers validés au micro : souffle, justesse, intervalles, gammes, harmonie.
  - Un échauffement du jour et une série de jours d'entraînement (un jour de pause toléré par semaine).
  - **Ta voix sur le chant de la semaine** : le chantre s'enregistre avec sa piste dans les écouteurs et envoie
    l'enregistrement. L'Admin++ l'écoute dans « À écouter » et répond « Valider » ou « À retravailler »
    avec un commentaire.
- **Chants.**
  - **Refonte de la liste (17 novembre 2026, PR 12, fusionnée)**, pensée pour ~2000 chants.
    Deux onglets : « Notre répertoire » (chants d'une setlist conservée ou avec un enregistrement, rangés
    en « Prochain service », « Chantés récemment », puis de A à Z) et « Tout » (par lettre, en-têtes
    collants, index A–Z à droite). L'écran s'ouvre sur le répertoire s'il y en a un. Lignes compactes
    sans carte : titre, auteur, note de musique s'il y a de l'audio. La recherche reste fixe en haut,
    couvre tout le catalogue et lit aussi les paroles (à partir de 3 lettres).
  - **Décision de Kevin :** la tonalité n'apparaît plus dans la liste et son filtre est supprimé
    (elle change d'une répétition à l'autre) ; elle reste dans la fiche du chant.
  - **Vérifié sur le S24 avec la vraie base (17 novembre 2026)** : onglets, index A–Z (tap et glissement),
    recherche dans les paroles. Deux corrections au passage : l'app ne chargeait que les 1000 premiers
    chants (limite de lignes de Supabase ; le chargement se fait maintenant par pages, 1987 chants),
    et les derniers résultats d'une recherche restaient cachés derrière le clavier.
  - **Filtre par thème (21 novembre 2026, branche `claude/suite`, pas encore vu à l'écran)** : migration
    `20261121000000_song_themes.sql` (colonne `songs.themes`, liste de textes vide par défaut) et script
    `supabase/scripts/song_themes_import.py` qui prépare le SQL des étiquettes à partir d'un CSV/JSON
    (lien accords.app ou titre), sans jamais se connecter à la base. **À faire par Kevin** : appliquer
    la migration puis l'import (étapes dans `docs/supabase-setup.md`, étape 6). Dans Chants, une rangée
    de thèmes apparaît sous les onglets dès qu'un chant en a un ; elle restreint les onglets et la
    recherche. Avant la migration, l'app lit les thèmes à part, ignore l'échec et cache le filtre.
  - **Liste allégée (21 novembre 2026, branche `claude/suite`, pas encore vue à l'écran)** : la liste ne
    charge plus que titre, auteur, tonalité, versions, pistes et liens (toujours par pages de 1000) ; les
    paroles d'un chant arrivent quand on ouvre sa fiche (« Les paroles se chargent… » en attendant, s'il
    n'en a pas de copie). La première recherche de 3 lettres ou plus charge toutes les paroles une fois
    par session (par pages aussi) : les titres trouvés s'affichent tout de suite, les paroles dès
    qu'elles arrivent. Choix : recherche sur le téléphone plutôt que `ilike` ou plein texte côté serveur,
    car les accords sont collés dans les mots (`Sei[2]gneur`) et les accents doivent être ignorés comme
    avant ; ça marche aussi hors ligne et sans changer la base. « Télécharger » la semaine charge aussi
    les paroles de ses chants, pour le mode scène hors ligne. Les paroles déjà lues restent sur le
    téléphone (`snapshots/song_charts.json`) ; la copie d'une version précédente garde les siennes.
    L'éditeur attend les paroles avant d'ouvrir un chant, pour ne jamais les effacer.
  - **Suites du 17 novembre 2026 (PR 13, fusionnée)** :
    une bulle affiche la lettre sous le doigt à côté de l'index A–Z ; dans la fiche d'un chant, le bouton
    « Écouter l'original » est remplacé par l'image de la vidéo YouTube, cliquable, au-dessus du titre
    (Coil 3 pour charger l'image ; simple lien si l'adresse n'est pas une vidéo) ; l'onglet Progrès
    n'affiche plus un écran à moitié chargé puis remplacé (l'écran partiel, gardé pour le hors-ligne,
    n'apparaît que si les données ont plus de 1,5 s de retard). Vérifié sur le S24 : bulle et miniature.
    Pas vérifié à l'écran : le clignotement de Progrès (intermittent) et l'ouverture de YouTube au clic.
  - **Haut de la fiche d'un chant (17 novembre 2026, PR 14, fusionnée)** : choix de Kevin,
    variante B. L'image de la vidéo YouTube est un bandeau en haut, avec le titre et l'auteur posés
    dessus ; sans vidéo, le titre reste seul (et un lien qui n'est pas une vidéo devient une ligne
    « Écouter l'original »). Tonalité et notation sont regroupées dans un seul cadre. **Tranché
    par Kevin le 21 novembre 2026 : on garde cette variante (B).** La variante A (titre d'abord, vignette
    en petite ligne dessous) est abandonnée.
  - **Écart de transposition visible (PR 17, fusionnée le 18 novembre 2026)** : sous la tonalité de la fiche,
    « +2 demi-tons » (lu par TalkBack) ; en mode scène, « +2 · origine Ré majeur » sous la tonalité quand
    le chant est transposé. Pas vu à l'écran.
  - **Importer depuis accords.app** : dans l'éditeur d'un nouveau chant, on colle le lien d'un chant
    (par exemple `accords.app/JEM414`). L'app lit cette seule page (le ChordPro intégré par Nuxt) et
    remplit titre, auteur, tonalité, paroles et accords, avec une section « Source » (copyright et
    lien) en bas. Seuls les liens vers un chant d'accords.app sont acceptés.
  - **Import groupé :** Kevin a confirmé le 17 novembre 2026 avoir déjà obtenu l’autorisation
    de Rephi pour la récupération groupée. La sélection demandée est la catégorie « adoration »
    (`https://accords.app/recherche?tags=,adoration`), puis le catalogue complet
    (`https://accords.app/recherche?tags=`). Conserver les sources et crédits, et utiliser
    `tools/rephi-import/` pour préparer le SQL sans écraser les chants déjà présents.
  - **Accords au-dessus des paroles** : un chant collé avec les accords sur la ligne du dessus (Word,
    PDF, WhatsApp) est converti tout seul ; tapé à la main, un bouton « Placer les accords dans les
    paroles » apparaît. Les titres « Refrain : », « Couplet 2 »… deviennent des sections.
- **Premier chargement hors ligne (PR 16, fusionnée le 18 novembre 2026)** : sans réseau ni copie sur le
  téléphone, Chants et Semaine tournaient sans fin. Ils affichent maintenant « Impossible de charger… »
  avec « Réessayer » ; le bandeau hors ligne n'apparaît que sur une copie à l'écran et a son propre bouton
  « Réessayer ». Adapté à la nouvelle bibliothèque en fusionnant `main` le 18 novembre. Pas vu à l'écran.
- **Bandeau d'échec de chargement (PR 15, fusionnée le 18 novembre 2026)** : il a sa propre surface sous la
  barre d'état du S24 au lieu de passer sous l'horloge.
- **Planning (4ᵉ onglet).** Vendredis (louange libre, sans setlist ni répétition) et dimanches. L'admin
  ouvre un mois, chacun répond « Je sers / Pas dispo » dans « Mes dispos », l'admin ajuste et valide.
  Rappels locaux (sans Firebase) : mois validé, veille de répétition (dimanches), veille de service,
  relance des réponses manquantes.
- **Parcours de croissance** Débutant → Confirmé → Pro, d'après le programme validé par Kevin
  (`data/seed/GrowthObjectives.kt`) : 7 objectifs par niveau pour piano, basse, batterie et chantres,
  chacun avec « Ce qu'on apprend » (fiche dépliable) et un critère de validation. Aucun ne cite un jour,
  un service ou la setlist. Les objectifs chantres liés à un palier au micro se valident en le
  finissant, les autres sont cochés puis validés par l'Admin++. Tous atteints → le membre demande son
  passage ; l'Admin++ confirme (le serveur change le niveau) ou renvoie avec un commentaire, dans
  « À valider ». L'Admin++ corrige un niveau dans « Gérer les membres ».
- **Parcours chantre guidé** (19 octobre 2026) : 9 modules, un par objectif chantre sans palier au micro
  (c4 à c7, p3 à p7), avec leçons, questions, écoute et exercices chantés au micro. Rien n'y valide un
  objectif : c'est toujours l'Admin++ (voir `docs/guided-learning.md`).
- **« Ma progression » refaite** (direction B, 19 octobre 2026) : anneau du niveau suivant (un segment par
  objectif), carte « Aujourd'hui » avec une seule action (voix à reprendre, palier au micro, parcours guidé,
  chant de la semaine, échauffement : voir `presentation/progress/ProgressFocus.kt`), puis onglets Objectifs /
  Parcours / Ma voix. Fiche d'objectif en bas d'écran ; outils admin dans le menu ⋮ et une bande « À traiter ».
- **Vidéo TikTok de présentation** (30 s, avec la musique du groupe) dans `video/`. Rendu : `npm run render`.
- **QA UI/UX sur le S24** (30 octobre 2026, build démo) : 40 constats (1 bloquant, 14 majeurs, 17 mineurs,
  8 suggestions) dans `docs/qa/2026-10-30-qa-s24.md`. Les captures sont restées hors git, dans
  `~/dev/XperienceMusic/docs/qa/2026-10-30-captures/`.
  - **Lot 1 (QA-01 à QA-09) corrigé** sur `fix/qa-s24-lot1`, partie de `feat/guided-piano-learning` : ViewModels
    effacés à chaque changement d'écran racine, Koin démarré dans `XperienceMusicApplication`, retour système
    dans l'accueil, confirmations (déconnexion, abandon des modifications, « Valider le mois » déplacé en haut du
    mois pour les admins), fiche d'objectif, retours d'exercice et « Arrêter » toujours visibles. Choix validés
    par Kevin le 30 octobre. Vérifié sur le S24 et l'A17 le 30 octobre : 9/9 OK, sans régression
    (`docs/qa/2026-10-30-verif-lot1.md`).
  - **Lot 2 (QA-10 à QA-15) corrigé** sur `fix/qa-s24-lot2`, partie de `fix/qa-s24-lot1` : « Je le ferai plus
    tard » sur les activités chantées, bouton « Ouvrir les réglages » après un refus du micro, grille coupée
    entre les mots, mode scène paysage avec commandes en colonne, noms et états pour TalkBack, semaine du
    dimanche le plus proche (`BandWeek`). Vérifié sur l'A17 le 30 octobre : 6/6 OK (`docs/qa/2026-10-30-verif-lot2.md`).
  - **Lot 3 (QA-16 à QA-40) corrigé** le 30 octobre, à la suite sur la même branche `fix/qa-s24-lot2`, **pas encore
    poussé** (en attente de la validation de Kevin) : un commit par constat, plus les deux défauts vus pendant la
    vérification du lot 1 (compteurs du Planning, total de l'accueil), la jauge du micro, l'écran d'explication
    avant la première demande du micro et le geste retour de `LearningScreen`. Détail : lignes « Corrigé (lot 3) »
    de `docs/qa/2026-10-30-qa-s24.md`. Choix de Kevin : barre d'onglets latérale en paysage, « le responsable »
    côté membres, onglet « Progrès », barre de progression sans total dans l'accueil. **Pas encore vérifié sur
    téléphone** (l'A17 a été débranché avant l'essai). Restent ouverts : pochette média sur iOS, champs en pilule
    dans l'éditeur de chant, pas d'annulation d'un passage de niveau confirmé (**tranché par Kevin le
    22 novembre 2026** : pas d'annulation, on corrige le niveau à la main dans « Membres du groupe »).

## Branches et PR (18 novembre 2026, soir)

- `main` contient tout jusqu'à la PR 19 (`9da505a`). Toutes les branches fusionnées sont supprimées, en local et sur GitHub.
- **Branche `fix/week-first-look` (24 novembre 2026)**, avant le premier partage de l'app : sans setlist pour la
  semaine en cours, Semaine montre toujours la dernière publiée, mais l'annonce (« Dernière setlist publiée · rien
  encore pour cette semaine ») et sa pastille porte sa date au lieu de « Cette semaine » ; « 1 chant » s'accorde ;
  le compteur hors ligne dit qu'il compte des enregistrements. Vu sur le S24. Pas encore testé : l'accueil d'un
  nouveau compte (code à 6 chiffres, 4 questions). Planning : le mois s'ouvre maintenant sur la prochaine date à
  venir (une fois par mois affiché ; l'en-tête et la bannière sont alors au-dessus, il faut remonter pour les voir),
  et une date passée sans équipe dit « Personne n'a servi ». Ce n'était pas un bug : « validé, 2 dates incomplètes »
  ne compte que les dates à venir.
- **PR 18 (fusionnée le 18 novembre 2026)** : la bande vide au-dessus du clavier dans les écrans poussés
  (« Publier la setlist » sur le S24). `MainScaffold` consomme la marge de sa barre du bas, donc
  `imePadding()` n'ajoute plus que ce que le clavier couvre au-delà. Correctif global : quelques bas
  d'écran peuvent remonter un peu.
- **PR 19 (fusionnée le 18 novembre 2026)** : choix de Kevin, « suggestions + recherche ». Dans « Ajouter
  des chants », sans recherche, seulement le répertoire (par titre, avec « Chanté le … ») ; une recherche
  couvre tout le catalogue, paroles comprises. Lignes compactes de la bibliothèque
  (`ui/components/SongListRow.kt`), sans tonalité.
- Vu sur le S24 le 18 novembre au soir : plus de liste complète (répertoire vide hors chant déjà choisi,
  donc seule l'indication de recherche), et le champ de recherche colle au clavier. Captures dans
  `docs/qa/2026-11-18-captures/` (hors git). Pas encore vu avec un répertoire de plusieurs chants.
- La CI macOS de GitHub manque parfois de runners : un job « Tests and builds » annulé après 15 min sans
  étape lancée se relance avec `gh run rerun <id> --failed`. Le merge automatique est désactivé sur le dépôt.
- Le worktree `~/dev/XperienceMusic-qa` est sur `main` sans branche (HEAD détaché).
- Hors git : captures et comptes rendus de QA sous `docs/qa/` (non suivis), dossier `tools/`, et un stash
  ancien (7 lignes dans `MainActivity.kt`, posé sur le merge de la PR 9).
- **detekt** repasse au vert (22 novembre 2026, branche `claude/suite-3`) : les 15 remarques laissées par les
  fusions de novembre sont corrigées sans changer le comportement ; les trois écarts voulus (`runRemote`, le
  défilement du mode scène, la police des titres) sont justifiés dans le code ou dans la baseline.

## Reste à faire

1. ~~**Moi (Supabase, SQL Editor).** Lancer les migrations `practice_days` et `voice_submissions`.~~ Fait le 13 octobre 2026.
2. ~~**Toi.** Vérifier en démo « Ma voix » et « À écouter ».~~ Fait le 13 octobre 2026.
   Pour les essais sur simulateur :
   - **iPhone 16 Pro Max** (`E0520F3C-45E5-4AAC-99F8-14D1A11A9293`), compilé dans `iosApp/build/dd16`.
     L'iPhone 16 Pro sert à une autre app (MEMOVERSE) et l'iPhone 17 Pro à une app vélo : n'y touche pas.
   - Connexion démo directe : `xcrun simctl openurl <udid> "xperiencemusic://connexion?email=toi%40demo.local&code=123456"`.
   - Le clavier AZERTY déforme la frappe (chiffres, @, /) : mets le texte avec `xcrun simctl pbcopy`,
     tape une lettre pour faire apparaître le menu, puis appui long → Paste.
   - Fais une capture avant chaque série de touches.
   - Le micro plante sur simulateur : teste-le sur mon S24.
3. ~~**Moi (Supabase)** : migrations planning et parcours.~~ Faites le 16 octobre 2026.
4. **Tests sur téléphone : Galaxy A17** (`adb -s adb-R5GYB2R7W2Y-HaeuqW._adb-tls-connect._tcp`).
   Ne touche pas au S21 (`RFCR40827ZZ`), et ne désinstalle pas `dev.kevin.kanto2026` sur l'A17. Le S24
   (`RFCX80Y0FVX`) sert aussi à MEMOVERSE : on peut y faire la QA, mais seulement après avoir vérifié qu'aucune
   autre session ne l'utilise. Ne laisse rien tourner en continu : une autre session en a
   besoin. C'est Kevin qui se connecte. À tester : micro (« Ma voix »), import accords.app, collage avec accords
   au-dessus, planning et rappels, parcours de croissance.
5. **Plus tard.**
   - ~~Passer l'envoi des mails de Gmail à **Brevo**.~~ Fait le 10 novembre 2026 (voir
     `docs/supabase-setup.md`, étape 4).
   - Notifications quand une setlist est publiée (Firebase).
   - Test sur un vrai iPhone (il faut un compte Apple Developer pour TestFlight).
   - Version Android release signée et numéro de version, pour diffuser l'APK au groupe.
   - Pages d'aide : une pour les membres, une pour le responsable des chants. **Première version le
     22 novembre 2026** (branche `claude/suite-3`) : `docs/aide/membres.md` et `docs/aide/responsable.md`,
     liées depuis le README. **Publiées dans l'app le 22 novembre 2026** (décision de Kevin, branche
     `claude/suite-3`, pas vu à l'écran) : écran « Aide » ouvert depuis Mon compte. Un membre lit sa page ;
     Admin contenu et Admin++ ont deux onglets, « Responsables » (ouvert d'abord) et « Membres ». Source
     unique : les fichiers de `docs/aide`, compilés dans l'app par `:shared:generateHelpPages`
     (`HelpMarkdown` lit leur Markdown simple ; ce qui est entre `<!-- hors-app -->` et
     `<!-- /hors-app -->` reste sur GitHub).
   - Point de l'onglet Progrès : **tranché par Kevin le 22 novembre 2026**, il reste jusqu'au nouvel envoi
     (comportement actuel gardé, rien à coder).
   - Ligne « Cette semaine tu sers… › » en haut de Semaine. **Faite le 22 novembre 2026** (branche
     `claude/suite-3`, pas vue à l'écran) : une carte sous l'en-tête quand le membre a répondu « Je sers »
     pour le vendredi ou le dimanche de la semaine ; elle ouvre l'équipe de son prochain service et dit
     « Équipe à confirmer par les responsables » tant que le mois n'est pas validé. **Confirmé par Kevin
     le 22 novembre 2026** : l'appui ouvre bien le détail du service (pas l'onglet Planning).
6. **Avatars et « Mon compte »** (PR 7, branche `feat/member-avatar`). La migration
   `20261110000000_member_avatars.sql` est appliquée sur Supabase le 10 novembre 2026. Testé sur le S24 :
   avatar dans Semaine, Mon compte, avatar dessiné, import de photo, retrait. Fusionnée le 10 novembre 2026.
   Reste à tester sur iPhone (sélecteur PHPicker).
7. **Refonte qualité du code** (branche `refactor/code-quality`, partie de `main`, 10 novembre 2026), après un
   audit architecture / SOLID / code mort / valeurs en dur :
   - **Apprentissage remis en couches** : use cases, `ActivityGrader`, `LearningSessionStore`, `PcmRecorder`
     commun avec `VoiceRecorder`, ViewModels découpés, textes dans `strings.xml`.
   - **Tokens d'interface** : `Sizes`, `Radius`, `AppTypography`, `AppTheme.colors`. Les gros écrans sont
     découpés par section.
   - **Couche data** : un seul `SessionGuard`, remise à zéro par `resetAtSignOut()`, `Modules.kt` découpé par
     fonctionnalité dans `di/`, rappels planifiés dans le domaine, `MainScaffold` découpé (`AppNavGraph`, `AppTabBar`).
   - **Garde-fous** : detekt avec les règles Compose (avec baselines) et `checkUiLiterals`, qui refuse les
     textes, dp, couleurs et `koinInject` de dépôts dans `ui/`.
   - **Reste à faire** :
     - tester sur téléphone : aucun changement visible n'est voulu, à vérifier ;
     - la PR 5 (dépôt de la répétition) est intégrée : ses liaisons sont dans `di/SongsModule.kt`, ses routes
       dans `AppNavGraph.kt`, et le nom « Répétition du … » est composé par l'écran à partir de `strings.xml` ;
     - choix gardé : `SongRepository` n'est pas découpé et `SyncStatus` n'est pas fusionné avec le bandeau réseau,
       pour ne pas gêner la PR 5.
8. **Dépôt de la répétition** (PR 5) : fusionnée le 10 novembre 2026, après un test complet sur le S24 avec la
   vraie base (envoi pour tout le groupe, lecture, setlist, piste alto). Corrigé pendant le test : l'app
   revenait sur Semaine et perdait le formulaire après un passage en arrière-plan.
9. **Prochains chantiers** (demandés par Kevin le 10 novembre 2026). Les deux premiers sont dans `main`
   (branche `feat/quick-song-and-audio-compression`, fusionnée, supprimée le 18 novembre 2026) :
   - Compression audio dans l'app avant l'envoi : Supabase gratuit limite à 50 Mo par fichier, soit environ
     25 min avec le Dictaphone Samsung.
   - Ajout rapide d'un chant dans « Publier la setlist » (titre + lien YouTube), et un champ lien YouTube sur
     la fiche du chant. Aujourd'hui, une bibliothèque vide bloque la publication.
   - En option : coller le message WhatsApp entier pour créer la setlist. **En attente** : Kevin enverra des
     exemples de vrais messages du groupe ; rien à faire avant.
   - QA : toujours rejouer la vraie semaine sur la vraie base, bibliothèque vide, avec une sortie de l'app en
     cours de route.

## Règles à respecter

- Ne tape jamais mes identifiants ni mes codes de connexion à ma place.
- Ne me demande jamais la clé `service_role` / `sb_secret_` ni le mot de passe de la base.
  Les clés restent dans `local.properties` (non versionné).
- Pour une version démo sans Supabase, compile avec `ORG_GRADLE_PROJECT_xmDemo=true`. Le code démo est 123456.
- Architecture : Clean Architecture + MVVM ; chaque écran a son trio Screen / ScreenContent / Preview ;
  les fonctions `derive` sont pures ; il y a des tests à chaque couche. Aucun ViewModel ne reçoit de dépôt,
  aucun composable n'injecte de dépôt ; textes dans `strings.xml`, tailles et couleurs par les tokens de
  `ui/theme`. `./gradlew detekt checkUiLiterals` le vérifie.
  Les tests de la base se lancent avec Docker (voir `docs/supabase-setup.md`).
- La chaîne d'outils : Kotlin 2.3.21, CMP 1.11, AGP 9, démon Gradle sur JDK 21, supabase-kt figé en 3.6.0.

## Polish UX (branche feat/ux-polish)

Vingt finitions cosmétiques, une par commit (`61390b4` à `ffd2846`), sans changement de comportement ni de
données. QA sur le S24 le 31 octobre (`docs/qa/2026-10-31-qa-ux-polish.md`) : 13 OK, 4 non testables en
démo (4, 12, 17 pour le message en bas d'écran, 18), 4 défauts corrigés ensuite (`0aeef2e` à `768039a`) :
le Planning fait glisser tout le mois, la partie basse de l'éditeur de setlist suit le retrait, niveau et rôle
des membres ont le même style, l'onglet de Ma progression change sans fondu en mouvement réduit. Ces quatre
corrections sont vérifiées sur l'A17 (build démo, 31 octobre au soir) ; le fondu des cartes du Planning a été
décalé ensuite pour que l'ancien et le nouveau mois ne se superposent plus, également vérifié sur l'A17.
Les six défauts « vus en passant » du compte rendu sont corrigés le 3 novembre (un commit chacun, détail sous
chaque point du compte rendu) : trait d'union au lieu d'un blanc dans un mot coupé par un accord, fondu en haut
des paroles du mode scène, double point du Planning, titre de l'en-tête sur une ligne, espace insécable dans
les dates, place de la coche réservée dans les cartes de choix, libellé du bouton lecture/pause porté par le
bouton. **Pas encore vérifiés sur téléphone** (A17 à réserver auprès de la session coordinatrice).

1. Les boutons en dégradé réagissent à l'appui (léger rétrécissement et assombrissement) — `BrandControls.kt`.
2. Lecture, chargement et pause passent de l'un à l'autre en fondu — `PlayPauseButton.kt`, `PlayerSheet.kt`.
3. Le mini-lecteur glisse en entrant et en sortant au lieu d'apparaître d'un coup — `MiniPlayer.kt`.
4. Le rond de chargement n'apparaît qu'après 300 ms, en fondu ; Chants l'affiche au premier chargement — `LoadingIndicator.kt`.
5. Dans Chants, les lignes glissent en place quand la recherche ou la tonalité filtre la liste — `LibraryScreen.kt`.
6. Éditeur de setlist : les chants déplacés, ajoutés ou retirés glissent — `SetlistEditorScreen.kt`.
7. Éditeur de setlist : tonalités en pastille comme ailleurs, titres longs coupés par « … » — `SetlistEditorScreen.kt`.
8. Planning : le mois glisse dans le sens des flèches — `PlanningScreen.kt`.
9. Pastilles de tonalité, « Aujourd’hui », réponse et personnes toujours sur une ligne — `BrandControls.kt`, `PlanningScreen.kt`.
10. Détail du chant : la tonalité monte ou descend en défilant quand on transpose — `SongDetailScreen.kt`.
11. Détail du chant : le choix de notation passe en fondu d'une option à l'autre — `SongDetailScreen.kt`.
12. Chant supprimé : vrai état vide (icône, titre, explication) au lieu d'une ligne seule — `SongDetailScreen.kt`.
13. Membres : le choix du niveau a une zone d'appui de 48 dp — `MembersScreen.kt`.
14. Ma progression respecte « Réduire les animations » (entrée, compteur, égaliseur, onglets) — `ProgressMotion.kt`.
15. Carte « Aujourd’hui » : couleurs et rayon tirés de la palette (`Palette`, `Radius.hero`) — `ProgressMotion.kt`.
16. Touches du piano de l'atelier : couleurs tirées de la palette — `LearningScreen.kt`.
17. Champs de texte, menus et messages arrondis à 12 dp, assortis aux cartes — `AppTheme.kt`.
18. Semaine passée : les titres s'estompent avec leur tonalité — `WeekScreen.kt`.
19. Mode scène : les boutons de vitesse se déplient au lieu d'apparaître d'un coup — `StageScreen.kt`.
20. Accueil et « Mon profil » : la teinte et la coche des cartes de choix arrivent en douceur — `ChoiceCards.kt`.
