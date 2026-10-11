# XperienceMusic

Application du groupe de louange **Xperience Music** : Android et iPhone, en Kotlin
Multiplatform et Compose Multiplatform.

## Ce que fait l'app

- **Cette semaine** : la setlist de la répétition (mardi) et de la prestation (dimanche),
  téléchargeable pour écouter hors connexion.
- **Chants** : paroles et accords en chiffres (Nashville), Do-Ré-Mi ou C-D-E, transposables ;
  jusqu'à 3 versions enregistrées par chant (une référence) et des pistes par voix ;
  mode scène plein écran avec défilement automatique.
- **Écoute** : mini-lecteur qui suit d'un écran à l'autre, écran éteint compris.
- **Ma progression** : parcours des chantres avec exercices validés au micro (justesse et
  tenue de note analysées sur le téléphone, rien n'est enregistré).
- **Rôles** : membre (lecture), admin contenu (setlists, chants, versions), Admin++
  (en plus : membres et invitations).

## Structure

| Module | Rôle |
|---|---|
| `core/music` | Théorie musicale en Kotlin pur : accords, Nashville, transposition, détection de hauteur (YIN), son de référence |
| `shared` | L'app : `domain` (règles), `data` (Supabase, mode démo, copies hors ligne), `presentation` (ViewModels), `ui` (Compose) |
| `androidApp`, `iosApp` | Points d'entrée Android et iOS |
| `supabase` | Schéma de la base, règles d'accès et leurs tests |

## Lancer l'app

- Android : `./gradlew :androidApp:installDebug`
- iOS : ouvrir `iosApp/iosApp.xcodeproj` dans Xcode

Sans configuration, l'app tourne en **mode démo** (données locales, code de connexion
`123456`). Pour la brancher sur le vrai serveur : [docs/supabase-setup.md](docs/supabase-setup.md).

Gradle tourne avec Java 21 (`gradle/gradle-daemon-jvm.properties`), récupéré
automatiquement si besoin.

## Tests

```bash
./gradlew :core:music:allTests :shared:testAndroidHostTest
```

Les règles d'accès de la base se testent sur un Postgres jetable : voir
[docs/supabase-setup.md](docs/supabase-setup.md#ce-que-garantit-le-serveur). Les deux
tournent à chaque push via GitHub Actions (`.github/workflows/ci.yml`).
