# Reprise de session : Xperience Music

Colle ce texte au début d'une nouvelle session Claude Code ouverte dans `~/dev/XperienceMusic`.

---

On continue l'app **Xperience Music** (KMP + Compose Multiplatform, Android + iOS) pour le groupe
de louange de mon église. Réponds-moi toujours en français. Lis d'abord `README.md`,
`docs/supabase-setup.md` et `git log --oneline -20` pour te remettre dans le contexte.

## Où on en est

Tout est enregistré dans git. Dernier commit : `b7b93d3` (envoi de voix et écoute par l'Admin++).

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
- **Vidéo TikTok de présentation** (30 s, avec la musique du groupe) dans `video/`. Rendu : `npm run render`.

## Reste à faire

1. ~~**Moi (Supabase, SQL Editor).** Lancer les migrations `practice_days` et `voice_submissions`.~~ Fait le 13 octobre 2026.
2. **Toi.** Vérifier en démo les écrans « Ma voix » et « À écouter » sur le simulateur **iPhone 16 Pro**
   (`5450413B-6EA8-4EE1-A74C-EFFE36AAF118`), compilé dans `iosApp/build/dd16`.
   - N'utilise pas l'iPhone 17 Pro : une autre session y lance une app vélo.
   - Fais une capture avant chaque série de touches.
   - Le micro plante sur simulateur : teste-le sur mon S24.
3. **Installation sur mon S24** (Galaxy S24, débogage sans fil ; je l'active quand tu le demandes).
   Tu as le droit d'installer sur le S24, mais **pas** sur le S21+ (`RFCR40827ZZ`).
   Commande : `~/Library/Android/sdk/platform-tools/adb`.
4. **Plus tard.**
   - Passer l'envoi des mails de Gmail à **Brevo** (ou Resend) avant d'inviter le groupe.
   - Notifications quand une setlist est publiée (Firebase).
   - Test sur un vrai iPhone.

## Règles à respecter

- Ne tape jamais mes identifiants ni mes codes de connexion à ma place.
- Ne me demande jamais la clé `service_role` / `sb_secret_` ni le mot de passe de la base.
  Les clés restent dans `local.properties` (non versionné).
- Pour une version démo sans Supabase, compile avec `ORG_GRADLE_PROJECT_xmDemo=true`. Le code démo est 123456.
- Architecture : Clean Architecture + MVVM ; chaque écran a son trio Screen / ScreenContent / Preview ;
  les fonctions `derive` sont pures ; il y a des tests à chaque couche.
  Les tests de la base se lancent avec Docker (voir `docs/supabase-setup.md`).
- La chaîne d'outils : Kotlin 2.3.21, CMP 1.11, AGP 9, démon Gradle sur JDK 21, supabase-kt figé en 3.6.0.
