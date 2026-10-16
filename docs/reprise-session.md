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
- **Parcours de croissance** Débutant → Confirmé → Pro. Chantres : paliers 1-3 pour Confirmé, 1-5 pour Pro.
  Musiciens : objectifs par instrument (`data/seed/MusicianObjectives.kt`), cochés puis validés par
  l'Admin++. Ensuite le membre demande son passage ; l'Admin++ confirme (le serveur change le niveau) ou
  renvoie avec un commentaire, dans « À valider ». L'Admin++ corrige un niveau dans « Gérer les membres ».
- **Vidéo TikTok de présentation** (30 s, avec la musique du groupe) dans `video/`. Rendu : `npm run render`.

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
3. **Moi (Supabase)** : lancer `20261015000000_service_planning.sql` puis `20261016000000_growth_path.sql`.
4. **Installation sur mon S24** — à tester : micro (« Ma voix »), import accords.app, collage d'un chant avec les accords au-dessus (Galaxy S24, débogage sans fil ; je l'active quand tu le demandes).
   Tu as le droit d'installer sur le S24, mais **pas** sur le S21+ (`RFCR40827ZZ`).
   Commande : `~/Library/Android/sdk/platform-tools/adb`.
5. **Plus tard.**
   - Passer l'envoi des mails de Gmail à **Brevo** (ou Resend) avant d'inviter le groupe.
   - Notifications quand une setlist est publiée (Firebase).
   - Test sur un vrai iPhone (il faut un compte Apple Developer pour TestFlight).
   - Version Android release signée et numéro de version, pour diffuser l'APK au groupe.
   - Pages d'aide : une pour les membres, une pour le responsable des chants.
   - Ligne « Cette semaine tu sers… › » en haut de Semaine.
   - Relire avec le groupe les objectifs des musiciens (ébauche à ajuster).

## Règles à respecter

- Ne tape jamais mes identifiants ni mes codes de connexion à ma place.
- Ne me demande jamais la clé `service_role` / `sb_secret_` ni le mot de passe de la base.
  Les clés restent dans `local.properties` (non versionné).
- Pour une version démo sans Supabase, compile avec `ORG_GRADLE_PROJECT_xmDemo=true`. Le code démo est 123456.
- Architecture : Clean Architecture + MVVM ; chaque écran a son trio Screen / ScreenContent / Preview ;
  les fonctions `derive` sont pures ; il y a des tests à chaque couche.
  Les tests de la base se lancent avec Docker (voir `docs/supabase-setup.md`).
- La chaîne d'outils : Kotlin 2.3.21, CMP 1.11, AGP 9, démon Gradle sur JDK 21, supabase-kt figé en 3.6.0.
