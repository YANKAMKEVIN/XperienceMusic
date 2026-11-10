# Xperience Music — Brief : avatars des membres

## Le produit
Application mobile (Android + iOS) du groupe de louange d'une église, environ 20 membres : chantres,
pianistes, bassistes, batteurs. Interface en français, thème sombre uniquement.

Images jointes, à respecter :
- `semaine-actuelle.png` : capture de l'app réelle. `AppIcon-1024.png` : icône.

## Direction artistique validée
- Fond nuit presque noir #0B0A14. Surfaces #15131F et #221F32.
- Dégradé de marque violet → indigo → bleu : #B46CFF → #6D5BFF → #3D7BFF. Accent violet #9B7BFF, bleu #6E98FF.
- Ambiance douce, lumineuse, moderne. Pas de croix, colombe ni vitraux.

## Ce qu'on te demande
Des **avatars par défaut** : chaque membre en choisit un dans l'app tant qu'il n'a pas mis sa photo.
Ils s'affichent dans un **cercle**, de 40 px (en-tête) à 120 px (profil) : le visage doit rester lisible
en tout petit.

**16 images carrées 1024 × 1024**, une par fichier, enregistrées dans ce dossier :

| Rôle | Garçons | Filles |
|---|---|---|
| Chantre (micro) | `chantre-garcon-1.png`, `chantre-garcon-2.png` | `chantre-fille-1.png`, `chantre-fille-2.png` |
| Piano (clavier) | `piano-garcon-1.png`, `piano-garcon-2.png` | `piano-fille-1.png`, `piano-fille-2.png` |
| Basse (guitare basse) | `basse-garcon-1.png`, `basse-garcon-2.png` | `basse-fille-1.png`, `basse-fille-2.png` |
| Batterie (baguettes) | `batterie-garcon-1.png`, `batterie-garcon-2.png` | `batterie-fille-1.png`, `batterie-fille-2.png` |

Pour chaque image :
- **Même style pour les 16** : illustration 2D/3D douce et stylisée (pas de photo réaliste), jeunes
  adultes souriants, cadrage buste centré, visage dans le tiers haut, assez d'air autour pour un
  recadrage en cercle.
- L'instrument se reconnaît d'un coup d'œil mais reste petit (micro à la main, clavier au premier plan,
  manche de basse, baguettes).
- Fond : dégradé de marque doux ou halo violet sur nuit, **sans texte, sans logo, sans cadre ni cercle
  dessiné**.
- Varier d'une image à l'autre : teintes de peau (majorité de peaux noires et métisses, quelques
  peaux claires), coiffures (tresses, locks, afro, carré, crâne rasé, foulard…), tenues, lunettes ou non.
  Les deux variantes d'un même rôle doivent être nettement différentes.

Ensuite, assemble une planche de contrôle `planche-avatars.png` (les 16 avatars recadrés en cercles,
4 × 4, sur fond #0B0A14, avec le nom du fichier sous chacun) et écris `notes.md` en français : le style
retenu et tout ce qui t'a posé problème.
