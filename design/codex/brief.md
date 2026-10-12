# Xperience Music — Brief de propositions d'écrans

## Le produit
Application mobile (Android + iOS, Compose Multiplatform) pour le groupe de louange d'une église, environ 20 membres.
Répétition le mardi, culte le dimanche. Elle remplace WhatsApp, où l'on repostait chaque semaine les enregistrements et la liste des chants.
Langue de l'interface : français. Thème sombre uniquement (salles de répétition et églises peu éclairées).

### Rôles
- **Membre** : consulte tout, n'édite rien.
- **Admin contenu** : gère setlists, chants (paroles + accords), enregistrements, suppressions.
- **Admin++** : tout ce qui précède + valide les exercices des chantres + gère les membres (invitations, rôles).

### Fonctions, écran par écran
1. **Semaine** (onglet 1) : la setlist de la semaine — répétition mardi, culte dimanche ; ordre des chants, tonalité de chacun, bouton d'écoute rapide ; bandeau « hors connexion » si pas de réseau ; admin : bouton « Publier / modifier la setlist ».
2. **Bibliothèque** (onglet 2) : tous les chants, recherche, filtres (tonalité, tempo, déjà joué) ; admin : « Ajouter un chant ».
3. **Détail d'un chant** : paroles avec accords au-dessus des syllabes. Accords stockés en chiffres Nashville, affichables au choix « 1-4-5 », « Do-Ré-Mi » ou « C-D-E », avec transposition (−/+ demi-tons). Enregistrements : au plus 3 versions (1 **référence** + 2 alternatives), plus des **pistes par voix** (soprano / alto / ténor / basse) qui ne comptent pas dans les 3. Bouton « Mode scène ».
4. **Lecteur** : mini-lecteur au-dessus des onglets + lecteur plein écran (lecture écran éteint, disponible hors connexion, téléchargé).
5. **Mode scène** : plein écran, gros texte paroles + accords, lisible sur un pupitre, défilement, chant suivant / précédent de la setlist.
6. **Ma progression** (onglet 3, chantres uniquement) : niveau, voix (soprano/alto/ténor/basse), exercices de chant classés, série / régularité, statut des exercices (à faire, validé, « à retravailler » renvoyé par l'Admin++).
7. **Exercice au micro** : exemple « tenir une note » — note cible, accordeur en direct (jauge de justesse, cents), minuteur de tenue, validation automatique.
8. **Admin** : éditeur de setlist (glisser pour réordonner, choisir la tonalité), éditeur de chant, gestion des membres (inviter par e-mail, rôle).

## Direction artistique VALIDÉE (à respecter strictement)
Voir l'image jointe `s24-connexion.png` (écran de connexion validé sur un vrai téléphone) et `AppIcon-1024.png` (icône).
- Fond nuit presque noir **#0B0A14**, halo violet très doux en haut d'écran. Surfaces **#15131F** et **#221F32**, contours **#2E2A44**.
- Dégradé de marque violet → indigo → bleu : **#B46CFF → #6D5BFF → #3D7BFF**. Accent violet **#9B7BFF**, bleu **#6E98FF**. Texte blanc cassé **#F4F3FA**, texte secondaire **#A9A6BD**. Succès **#6FD3A0**.
- Monogramme **XM** : X blanc, M en dégradé dont la jambe droite est une croche.
- Logotype « XPERIENCE » (gras, blanc) sur « M U S I C » (violet espacé). Signature « RÉPÉTER · ENREGISTRER · GRANDIR ».
- Motif signature : **vagues sonores lumineuses** (fines sinusoïdes en dégradé, plus amples au centre, s'effaçant sur les bords).
- Typographie sans-serif moderne, sobre ; coins arrondis ~16 px ; beaucoup d'air ; premium, calme, spirituel sans clichés (pas de croix, colombe ni vitraux).
- Accessibilité : contrastes AA, cibles tactiles ≥ 48 px.

## Ce qu'on te demande
Des **propositions d'écrans** dans cette même DA, pour les écrans 1 à 8, sous forme de planches d'images portrait (maquettes de téléphone côte à côte, 3 écrans par planche max, textes en français lisibles, données réalistes : chants de louange francophones génériques, prénoms variés).
Le motif des vagues doit revenir de façon discrète et utile (lecteur, accordeur, en-têtes), pas partout.
Ne PAS refaire l'écran de connexion : il est validé.
