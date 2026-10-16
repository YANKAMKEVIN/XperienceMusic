# Brancher XperienceMusic sur Supabase

Sans configuration, l'app tourne en **mode démo** : données locales, code de connexion `123456`.
Ces étapes la branchent sur un vrai projet Supabase partagé par tout le groupe.

## 1. Créer le projet

1. Crée un compte sur [supabase.com](https://supabase.com) et un nouveau projet
   (région **Europe (Paris ou Frankfurt)**, plan gratuit pour commencer).
2. Garde le mot de passe de la base de données dans un gestionnaire de mots de passe.

## 2. Créer les tables et les règles d'accès

Dans le tableau de bord : **SQL Editor → New query**, colle tout le contenu de
[`supabase/migrations/20261010000000_initial_schema.sql`](../supabase/migrations/20261010000000_initial_schema.sql)
puis **Run**.

Cela crée les tables (membres, invitations, chants, versions, setlists, progression), les règles
par rôle et le stockage privé `audio`.

Puis, de la même façon, exécute **dans l'ordre** les autres fichiers de
[`supabase/migrations/`](../supabase/migrations/) (un fichier = une requête). Quand l'app évolue,
seuls les nouveaux fichiers sont à exécuter :

| Fichier | Ce qu'il change |
| --- | --- |
| `20261012000000_setlists_per_week.sql` | une setlist par dimanche : on peut publier les semaines suivantes à l'avance, republier un dimanche le remplace, et un admin peut supprimer une semaine |
| `20261013000000_practice_days.sql` | les jours d'entraînement de chaque chantre, pour la série affichée dans « Ma progression » |
| `20261014000000_voice_submissions.sql` | les voix envoyées par les chantres sur les chants de la semaine, que l'Admin++ valide ou renvoie « à retravailler » |
| `20261015000000_service_planning.sql` | le planning des vendredis et dimanches : mois ouverts par les admins, réponses de chacun |
| `20261016000000_growth_path.sql` | le parcours Débutant → Confirmé → Pro : demandes de passage, objectifs des musiciens, et seul l'Admin++ change un niveau après l'accueil |

## 3. T'inviter comme Admin++

Toujours dans le SQL Editor, avec **ton** adresse e-mail :

```sql
insert into public.invitations (email, role) values ('ton.email@exemple.com', 'SUPER_ADMIN');
```

C'est la seule invitation à faire en SQL. Ensuite, tout se passe dans l'app :
**Ma progression → Gérer les membres** (Admin++ seulement). Tu y invites les autres par leur
adresse e-mail, tu changes leur rôle (Membre par défaut, Admin contenu pour la personne qui gère
les chants), tu annules une invitation en attente ou tu retires quelqu'un du groupe.

## 4. Configurer la connexion par code e-mail

1. **Authentication → SMTP** : branche un envoi d'e-mails (Gmail avec un mot de passe
   d'application, Brevo, Resend…). Sans SMTP personnalisé, les modèles ne sont pas modifiables.
2. **Authentication → Providers → Email** : mets **Email OTP Length** à **6** (l'app attend 6 chiffres).
3. **Authentication → Email Templates** : mets le **même** contenu dans **Confirm signup**
   (première connexion) et **Magic Link** (connexions suivantes), avec l'objet
   « Ton code Xperience Music » :

```html
<h2>Ton code Xperience Music</h2>
<p style="font-size:32px;font-weight:bold;letter-spacing:6px">{{ .Token }}</p>
<p><a href="https://yankamkevin.github.io/xperience-music-connexion/#email={{ .Email }}&code={{ .Token }}">Ouvrir Xperience Music</a></p>
<p>Le code expire dans une heure. Si tu n'as rien demandé, ignore ce message.</p>
```

Le lien passe par une page relais en https
([dépôt `xperience-music-connexion`](https://github.com/YANKAMKEVIN/xperience-music-connexion)) car
Gmail retire les liens `xperiencemusic://`. La page rouvre l'app avec le code rempli ; l'adresse et
le code sont après « # » et ne sont jamais envoyés au serveur.

## 5. Donner les clés à l'app

**Project Settings → API** : copie l'**URL du projet** et la **clé publishable**
(`sb_publishable_…`, ou l'ancienne clé `anon`). Ne copie **jamais** la clé `service_role` / secret.

Ajoute-les dans `local.properties` à la racine du projet (ce fichier n'est pas versionné) :

```properties
supabase.url=https://xxxxxxxx.supabase.co
supabase.publishableKey=sb_publishable_xxxxxxxx
```

Reconstruis l'app : elle passe automatiquement du mode démo à Supabase.

## Ce que garantit le serveur

Les règles sont appliquées par la base elle-même, pas seulement cachées dans l'app :

- un compte non invité peut se connecter mais **ne voit rien** ;
- un membre lit tout mais ne peut rien modifier ni supprimer ;
- un admin contenu gère setlists, chants et versions, dans la limite de **3 versions par chant**
  (une référence + deux alternatives, toujours exactement une référence) ;
- seul l'Admin++ invite, change les rôles et peut renvoyer un exercice « à retravailler ».

Ces règles sont vérifiées par [`supabase/tests/rls_test.sql`](../supabase/tests/rls_test.sql) :

```bash
docker run -d --name xm-pg -e POSTGRES_PASSWORD=pg postgres:16-alpine
for f in supabase/tests/supabase_stubs.sql supabase/migrations/*.sql supabase/tests/rls_test.sql; do docker exec -i xm-pg psql -U postgres -v ON_ERROR_STOP=1 -q < "$f"; done
docker rm -f xm-pg
```

## À faire plus tard

- **Remplacer Gmail par Brevo ou Resend pour l'envoi des codes**, avant d'inviter tout le groupe.
  Avec le SMTP Gmail, chaque code envoyé à un membre apparaît aussi dans les messages envoyés du
  compte Gmail de l'admin. Un service dédié (gratuit à ce volume) envoie depuis une adresse
  « Xperience Music » et garde les codes privés.
