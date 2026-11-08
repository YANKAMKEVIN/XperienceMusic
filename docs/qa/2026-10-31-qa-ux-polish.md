# QA des 20 retouches UX (branche `feat/ux-polish`) sur Galaxy S24 (31 octobre 2026)

- **Version** : `feat/ux-polish` (`b56316c`), build démo (`ORG_GRADLE_PROJECT_xmDemo=true`), installée avec `install -r`.
- **Appareil** : Galaxy S24 `RFCX80Y0FVX`, police 1.0, animations système normales.
- **Méthode** : pilotage `adb`, enregistrement vidéo de chaque animation puis découpage image par image (20 à 30 images/s), lecture de l'arbre `uiautomator`. Aucun plantage dans `logcat -b crash`. L'app a été arrêtée en fin de test, « Réduire les animations » remis sur non.

## Bilan

| # | Retouche | Verdict |
|---|---|---|
| 1 | Boutons en dégradé à l'appui | OK. L'état appuyé paraît un peu terne, proche d'un bouton désactivé (avis) |
| 2 | Fondu lecture / chargement / pause | OK |
| 3 | Mini-lecteur qui glisse | OK à l'entrée et à la sortie, le titre reste affiché pendant la sortie |
| 4 | Rond de chargement retardé | Non testable : la démo charge instantanément |
| 5 | Lignes de Chants qui glissent au filtre | OK. Les lignes se chevauchent pendant le mouvement (léger) |
| 6 | Setlist : chants qui glissent | **À corriger** : au retrait, « Ajouter des chants » remonte d'un coup et recouvre la ligne qui s'efface |
| 7 | Tonalités en pastille, titres coupés | OK |
| 8 | Planning : le mois glisse | **À corriger** : seul le titre glisse, les cartes du mois changent d'un coup ; les deux titres se superposent à mi-course |
| 9 | Pastilles sur une ligne | OK sur les données démo (pas de nom long, grande police non testée) |
| 10 | Tonalité qui défile en transposant | OK, vers le haut avec « + », vers le bas avec « − » |
| 11 | Notation en fondu | OK |
| 12 | État vide d'un chant supprimé | Non atteignable sur l'appareil (supprimer un chant ramène à la liste) |
| 13 | Niveau des membres en 48 dp | Cible OK (144 px = 48 dp). **À corriger** : niveau et rôle n'ont ni la même taille de texte ni le même retrait ; le rôle renvoyé à la ligne est décalé de 14 px |
| 14 | Ma progression et « Réduire les animations » | OK : égaliseur figé, sections sans transition. Reste le fondu de 250 ms de la pastille d'onglet, avec un instant où les deux onglets sont gris |
| 15 | Carte « Aujourd'hui » en jetons | OK, mêmes valeurs qu'avant (vérifié dans le code) |
| 16 | Touches du piano en jetons | OK, mêmes valeurs qu'avant (vérifié dans le code) |
| 17 | Champs, menus arrondis à 12 dp | OK sur la recherche et le menu ⋮ ; message en bas d'écran non déclenché |
| 18 | Semaine passée estompée | Non testable : la démo n'a pas de semaine passée |
| 19 | Vitesse du mode scène qui se déplie | OK |
| 20 | Cartes de choix de l'accueil | OK, teinte puis coche en fondu |

## Vus en passant (pas dans les 20 retouches)

Corrigés le 3 novembre 2026, un commit par point, **pas encore vérifiés sur téléphone**.

- Mode scène : la coupure entre syllabes laisse un espace visible (« Ressusci té ! », « l'éterni té ! ») et la première ligne perd ses accents sous l'en-tête en défilant (« A toi la gloire, O »).
  - **Corrigé** : dans un mot coupé par un accord (« Ressus[5]ci[1]té »), la marge après l'accord se réduit au strict minimum et, si l'accord dépasse encore la syllabe, un trait d'union comble l'espace (« Ressusci-té ») au lieu d'un blanc — `ChordChartView.kt`, aussi dans le détail du chant. Le haut des paroles s'estompe sur 16 dp dès qu'elles défilent sous la barre : une ligne disparaît en fondu au lieu de perdre d'abord ses accents — `StageScreen.kt`.
- Planning : double point dans « attendues le 25 oct.. ».
  - **Corrigé** : « Les réponses étaient attendues le 25 oct. : tu peux encore répondre… », sur le modèle du message de « Mes dispos » — `strings.xml`.
- Ma progression : le titre passe sur deux lignes à côté de la pastille « 0 jour ».
  - **Corrigé** : le titre des en-têtes d'onglet reste sur une ligne et descend au besoin jusqu'à 18 sp avant de couper — `BrandHeader.kt`.
- Semaine : « Dimanche 1 nov. » se coupe avant « nov. ».
  - **Corrigé** : espace insécable entre le jour et le mois dans toutes les dates abrégées (« 1 nov. », « 25 oct. », « dim. 8 nov. ») — `Formatting.kt`, tests mis à jour.
- Accueil : la description « Chantre » se coupe avant « voix » quand la coche apparaît.
  - **Corrigé** : la place de la coche (24 dp) est réservée en permanence, le texte ne bouge plus — `ChoiceCards.kt`.
- Semaine : le bouton d'un chant en lecture affiche une pause mais TalkBack lit toujours « Écouter la référence ».
  - **Corrigé** : le libellé (« Mettre en pause… » ou « Écouter la référence de… ») est porté par le bouton lui-même et non plus par l'icône remplacée, pour que le nœud d'accessibilité change avec l'icône — `WeekScreen.kt`. Cause non reproduite dans le code (les deux libellés existaient déjà) : à confirmer sur l'A17 avec TalkBack.
