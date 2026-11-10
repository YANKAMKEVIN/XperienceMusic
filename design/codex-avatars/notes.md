# Avatars Xperience Music

## Style retenu

16 jeunes adultes souriants en illustration 3D douce et stylisée : volumes arrondis, matières mates, yeux expressifs, lumière diffuse et liserés violet/bleu. Fonds nuit avec halo violet et indigo, vêtements dans la palette de marque. Aucun texte, logo, cadre ou symbole religieux dans les avatars.

Les carnations noires et métisses sont majoritaires, avec quelques carnations claires. Coiffures, lunettes et tenues distinguent les variantes. Chaque ligne de la planche correspond à un rôle : chant, piano, basse, batterie ; colonnes garçon 1, garçon 2, fille 1, fille 2.

## Fabrication et contrôle

- Outil intégré de génération d’images ; exactement 16 appels, un par avatar.
- 16 PNG carrés normalisés en 1024 × 1024 par rééchantillonnage Lanczos. Les sorties natives étaient de taille supérieure ; aucune retouche de contenu.
- Planche : 2048 × 2240, grille 4 × 4, masques circulaires sur fond #0B0A14, noms complets sous chaque avatar.
- Contrôle visuel de la planche : visages dégagés, silhouettes distinctes, instruments reconnaissables. Les instruments restent secondaires par rapport aux visages.

## Difficultés et limites

Les générations indépendantes présentent de petites différences de proportions et de matière malgré une consigne commune. Les pianistes sont légèrement plus éloignés pour laisser apparaître les mains et le clavier. Les touches arrivent au bord inférieur du cercle ; le bas des instruments et des bustes est volontairement coupé par le masque.

À 40 px, le visage, la coiffure et la couleur dominent ; les cordes, les touches, les doigts et les montures fines perdent forcément du détail. Le rôle se lit mieux à 120 px. Les détails des instruments sont illustratifs, pas des schémas techniques. Aucun blocage de génération rencontré.

## Prompts utilisés

Consigne commune (anglais, transmise à chaque appel) :

> Use case: stylized-concept. Create ONE default member avatar for Xperience Music mobile app, square 1024x1024. Consistent premium soft stylized 3D illustration, matte clay-like smooth surfaces, simplified friendly young adult, warm smile, large expressive eyes, no photorealism. Centered bust portrait, face in upper third, whole hair with generous headroom, face large and readable at 40px. Compact recognizable instrument in lower central area, all important content safe inside circular crop. Night background #0B0A14 with subtle soft violet-indigo halo, palette #B46CFF #6D5BFF #3D7BFF, clothing dark #15131F #221F32 accented lavender. Soft studio illumination, gentle violet blue rim light. No text, logo, watermark, frame, drawn circle, religious symbol. 

Pour les appels 2 à 16, ajout de cohérence :

> Match a unified avatar family: friendly rounded proportions, softly sculpted face, restrained detail, dark night violet halo. One person only.

Sujets par fichier (ajoutés individuellement à la consigne commune, avec demande du nom de sortie) :

- **chantre-garcon-1.png** : young Black man with deep brown skin, short rounded afro, no glasses, violet sweatshirt, holding one small handheld microphone near chest.
- **chantre-garcon-2.png** : light olive skin man, shaved head, round glasses, indigo overshirt, holding small handheld microphone.
- **chantre-fille-1.png** : deep brown Black woman, long black braids gathered back, lavender blouse, holding small handheld microphone.
- **chantre-fille-2.png** : mixed race caramel skin woman, short curly auburn bob, thin round glasses, blue sweatshirt, holding small handheld microphone.
- **piano-garcon-1.png** : dark brown Black man, short neat black locks, indigo sweater, hands on compact clearly visible black and white piano keyboard across lower chest.
- **piano-garcon-2.png** : mixed race warm tan man, short wavy dark hair, rectangular glasses, lavender jacket, hands on compact clearly visible black and white piano keyboard across lower chest.
- **piano-fille-1.png** : dark brown Black woman, rounded natural afro, blue blouse, hands on compact clearly visible black and white piano keyboard across lower chest.
- **piano-fille-2.png** : fair skin woman, straight dark chin length bob, violet sweater, hands on compact clearly visible black and white piano keyboard across lower chest.
- **basse-garcon-1.png** : deep brown Black man, clean shaved head, subtle short beard, blue shirt, holding compact electric bass with visible four strings and four tuning pegs, neck diagonal beside shoulder, small body at lower chest.
- **basse-garcon-2.png** : mixed race caramel skin man, high curly black top haircut, glasses, violet jacket, holding compact electric bass with visible four strings and four tuning pegs, neck diagonal beside shoulder, small body at lower chest.
- **basse-fille-1.png** : deep brown Black woman, long tied back locks, lavender shirt, holding compact electric bass with visible four strings and four tuning pegs, neck diagonal beside shoulder, small body at lower chest.
- **basse-fille-2.png** : warm medium brown woman, violet wrapped headscarf, blue sweater, holding compact electric bass with visible four strings and four tuning pegs, neck diagonal beside shoulder, small body at lower chest.
- **batterie-garcon-1.png** : dark brown Black man, small rounded afro, round glasses, lavender sweatshirt, holding exactly two wooden drumsticks crossed clearly in front of chest.
- **batterie-garcon-2.png** : fair skin man, tousled copper hair, indigo shirt, holding exactly two wooden drumsticks crossed clearly in front of chest.
- **batterie-fille-1.png** : deep brown Black woman, braided hair in high bun, violet jacket, holding exactly two wooden drumsticks crossed clearly in front of chest.
- **batterie-fille-2.png** : mixed race golden brown woman, short natural curls, round glasses, blue blouse, holding exactly two wooden drumsticks crossed clearly in front of chest.


## Dans le dépôt

Les PNG de Codex ont été convertis en WebP (1024 px, qualité 90) pour garder le dépôt léger. L’app
embarque des copies de 256 px dans `shared/src/commonMain/composeResources/drawable/avatar_*.webp`.
