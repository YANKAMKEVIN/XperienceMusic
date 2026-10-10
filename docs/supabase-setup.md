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

## 3. T'inviter comme Admin++

Toujours dans le SQL Editor, avec **ton** adresse e-mail :

```sql
insert into public.invitations (email, role) values ('ton.email@exemple.com', 'SUPER_ADMIN');
```

Ensuite, c'est depuis cette table que tu invites les autres membres (`MEMBER` par défaut,
`CONTENT_ADMIN` pour la personne qui gère les chants). Un écran d'administration dans l'app
viendra plus tard.

## 4. Configurer la connexion par code e-mail

**Authentication → Email Templates → Magic Link** : remplace le contenu par un message qui
affiche le code, par exemple :

```html
<h2>Ton code XperienceMusic</h2>
<p>Entre ce code dans l'application : <strong>{{ .Token }}</strong></p>
<p>Il expire dans une heure.</p>
```

Fais de même pour le modèle **Confirm signup** (première connexion).

> L'envoi d'e-mails intégré de Supabase est limité (quelques e-mails par heure). Pour 20
> personnes ça suffit au début ; au-delà, branche un SMTP (Brevo, Resend…) dans
> **Project Settings → Authentication → SMTP**.

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
