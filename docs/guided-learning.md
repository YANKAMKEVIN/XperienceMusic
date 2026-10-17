# Premier parcours guidé : Accords de base

Le module piano reprend les cartes, couleurs, typographies et boutons de l’application.
Il est accessible dans Ma progression, pour les profils piano, et remplace la case de l’objectif
`piano-c2` par une entrée « Apprendre ». Les autres objectifs conservent leur fonctionnement.

## Version 1

Dix activités : leçon majeur, construction de Do majeur, leçon mineur, deux écoutes corrigées,
construction de Do mineur, leçon renversements, construction du premier renversement,
entraînement I–IV–V–I sur l’instrument et défi Fa majeur au clavier.

Les exemples sont des sons synthétisés, pas des enregistrements de piano. Les réponses clavier
sont contrôlées note par note, avec l’octave ; une note supplémentaire rend la réponse incorrecte.
Les deux activités d’écoute ne révèlent l’accord qu’après la réponse.

Une activité terminée débloque la suivante. Les leçons et la pratique physique sont déclaratives ;
elles ne sont pas des preuves de maîtrise. Les essais et leurs résultats sont conservés.
Le défi est proposé à nouveau trois jours après sa dernière réussite.

La fin du module permet de demander une évaluation en répétition sur l’objectif existant.
Le responsable utilise « À valider » pour évaluer le jeu réel, répondre et commenter.
Ce jalon ne propose pas encore d’enregistrement audio rattaché à une compétence ni de
reconnaissance automatique des accords joués au piano.

## Sauvegarde

Les essais sont versionnés et enregistrés localement avant toute synchronisation. Hors réseau,
on peut poursuivre le module sur le téléphone déjà connecté. Les essais non synchronisés portent
un indicateur et un bouton de reprise. Ils sont également repris à l’ouverture du module.
Les tentatives sont privées et immuables sur Supabase. Elles n’accordent aucune compétence ni niveau.
La déconnexion efface les données locales, y compris les essais pas encore synchronisés.

Appliquer `supabase/migrations/20261018010000_learning_attempts.sql` après les migrations précédentes.
Sans cette migration, la pratique locale reste possible mais les essais attendent leur synchronisation.

## Vérifications

Les tests couvrent les réponses musicales, les versions de contenu, les déblocages, la révision,
la sauvegarde hors ligne, la reprise idempotente et les réponses réseau après déconnexion.
Les règles SQL vérifient l’isolation entre membres et l’immutabilité des tentatives.
Prévoir une vérification sur Android et iPhone : clavier, son, grand texte, lecteur d’écran,
rotation, reprise après fermeture, réseau coupé, demande et retour du responsable.
