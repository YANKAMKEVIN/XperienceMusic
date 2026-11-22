# Aide : Xperience Music pour le responsable des chants

Cette page s'adresse aux responsables : **Admin contenu** (chants, versions, setlists) et **Admin++** (en plus :
membres, exercices, voix et niveaux). Tout ce que voit un membre est décrit dans
[l'aide des membres](membres.md).

## Publier la setlist de la semaine

1. Dans Semaine, touche « Publier la setlist » (ou « Modifier la setlist » si elle existe déjà).
2. Sans recherche, la liste propose le répertoire du groupe, avec « Chanté le … ». Une recherche couvre tout
   le catalogue, paroles comprises.
3. Un chant absent de la bibliothèque s'ajoute tout de suite avec son titre et son lien YouTube (ajout rapide).
4. Range les chants dans l'ordre avec les flèches, vérifie les dates de répétition et de service, et ajoute un
   message si besoin (il s'affiche en haut de Semaine).
5. Les semaines à venir peuvent être publiées à l'avance ; les semaines passées restent lisibles mais ne se
   modifient plus.

« Déposer la répétition » (dans Semaine) envoie l'enregistrement de la répétition à tout le groupe. L'app
compresse l'audio avant l'envoi (la limite est de 50 Mo par fichier).

## Ajouter ou corriger un chant

- Dans Chants, le + ouvre l'éditeur ; dans la fiche, « Modifier le chant ».
- **Importer depuis accords.app** : colle le lien d'un chant (par exemple `accords.app/JEM414`). Titre, auteur,
  tonalité, paroles et accords se remplissent ; une section « Source » garde le copyright et le lien.
  Vérifie, puis enregistre.
- **Accords au-dessus des paroles** : un chant collé depuis Word, un PDF ou WhatsApp est converti tout seul.
  Tapé à la main, le bouton « Placer les accords dans les paroles » apparaît. « Refrain : », « Couplet 2 »…
  deviennent des sections.
- Les accords inconnus sont signalés sous le texte ; l'aperçu montre le rendu final.
- Le champ « Lien YouTube » donne l'image en haut de la fiche.
- « Ajouter une version » enregistre jusqu'à 3 versions par chant (dont une de référence) et des pistes par voix.

## Le planning (Admin contenu et Admin++)

1. Dans Planning, ouvre un mois (jusqu'à deux mois à l'avance), avec une date limite de réponse si tu veux.
2. Chacun répond « Je sers » ou « Pas dispo ». Les rappels relancent ceux qui n'ont pas répondu.
3. Touche une date : la liste du groupe permet d'ajouter ou de retirer quelqu'un. Les rôles manquants
   (chant, piano, basse, batterie) sont signalés.
4. « Valider le mois » fige les réponses des membres. Tu peux encore ajuster les équipes après.

## Admin++ : membres

- Menu de « Mon compte » › Responsable › « Membres du groupe ».
- « Inviter quelqu'un » : saisis l'adresse e-mail, puis préviens la personne qu'elle peut se connecter.
- Change le rôle d'un membre, corrige son niveau ou retire-le du groupe. Tu ne peux pas modifier ton propre accès.

## Admin++ : voix et objectifs

- « Voix à écouter » : écoute les enregistrements des chantres sur le chant de la semaine, puis « Valider »
  (un message permet d'annuler) ou « À retravailler » avec un commentaire. Tu ne valides pas tes propres prises.
- « Objectifs à valider » : les objectifs cochés par les membres et les demandes de passage de niveau.
  « Voir ses objectifs » déplie ce que la personne a validé. Un passage de niveau confirmé change le niveau
  sur le serveur et ne s'annule pas depuis l'app : en cas d'erreur, corrige le niveau dans « Membres du groupe ».

## Bon à savoir

- La tonalité n'apparaît pas dans la liste des chants, car elle change d'une répétition à l'autre. La setlist
  montre celle de la fiche du chant : si le groupe chante un chant dans une autre tonalité, change-la dans
  l'éditeur du chant.
- Pour les réglages du serveur (Supabase, e-mails, migrations), voir [la mise en place](../supabase-setup.md).
