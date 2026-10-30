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
  - Filtre par tonalité sous la recherche (« Toutes », puis chaque tonalité du catalogue), caché
    tant que tous les chants sont dans la même tonalité.
  - **Importer depuis accords.app** : dans l'éditeur d'un nouveau chant, on colle le lien d'un chant
    (par exemple `accords.app/JEM414`). L'app lit cette seule page (le ChordPro intégré par Nuxt) et
    remplit titre, auteur, tonalité, paroles et accords, avec une section « Source » (copyright et
    lien) en bas. Seuls les liens vers un chant d'accords.app sont acceptés.
  - **Décision :** on ne récupère pas tout le catalogue d'accords.app. Le site ne donne aucune
    licence de réutilisation et les chants (JEM, traductions comprises) sont sous droits. Chaque
    import est un chant choisi par un admin, source gardée. Un import complet seulement si Rephi
    donne son accord.
  - **Accords au-dessus des paroles** : un chant collé avec les accords sur la ligne du dessus (Word,
    PDF, WhatsApp) est converti tout seul ; tapé à la main, un bouton « Placer les accords dans les
    paroles » apparaît. Les titres « Refrain : », « Couplet 2 »… deviennent des sections.
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
    dans l'éditeur de chant, pas d'annulation d'un passage de niveau confirmé.

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
   - Passer l'envoi des mails de Gmail à **Brevo** (ou Resend) avant d'inviter le groupe.
   - Notifications quand une setlist est publiée (Firebase).
   - Test sur un vrai iPhone (il faut un compte Apple Developer pour TestFlight).
   - Version Android release signée et numéro de version, pour diffuser l'APK au groupe.
   - Pages d'aide : une pour les membres, une pour le responsable des chants.
   - Ligne « Cette semaine tu sers… › » en haut de Semaine.

## Règles à respecter

- Ne tape jamais mes identifiants ni mes codes de connexion à ma place.
- Ne me demande jamais la clé `service_role` / `sb_secret_` ni le mot de passe de la base.
  Les clés restent dans `local.properties` (non versionné).
- Pour une version démo sans Supabase, compile avec `ORG_GRADLE_PROJECT_xmDemo=true`. Le code démo est 123456.
- Architecture : Clean Architecture + MVVM ; chaque écran a son trio Screen / ScreenContent / Preview ;
  les fonctions `derive` sont pures ; il y a des tests à chaque couche.
  Les tests de la base se lancent avec Docker (voir `docs/supabase-setup.md`).
- La chaîne d'outils : Kotlin 2.3.21, CMP 1.11, AGP 9, démon Gradle sur JDK 21, supabase-kt figé en 3.6.0.
