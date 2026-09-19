# Versions de développement

## Revue après Grok — 19 septembre 2026

- Douze silhouettes d'échecs en volumes pleins : pion, cavalier, fou, tour, dame et roi, en deux teintes. Les cavaliers font face à l'adversaire ; case claire à droite des blancs.
- Plaques de cités en relief, sept pictogrammes de familles et pièces sans lettres provisoires. Nom, coût, production, effets et chaînages des cartes de cités au survol.
- Échecs : attaques des pions sur les cases vides, roque sans tour, récursion entre rois, faux matériels insuffisants, priorité du mat et répétitions avec prise en passant corrigés.
- Commerce : quantités de production limitées, alternatives exclusives, achats calculés avant les constructions simultanées. Récupération gratuite depuis la défausse ; cartes des étapes retirées du circuit de défausse.
- Confidentialité : identifiants neutres pour les dos de cartes, choix simultanés masqués et mélange des nouveaux âges indépendant de la révision publique.
- Interface : actions accessibles dans le menu replié, lobby jusqu'à sept places adapté aux petites hauteurs, clic sur la pioche et les cités, récupération au clavier.
- Animations : départ depuis la case quittée, prises et roque, cartes immobiles sans rebond à chaque action ; cités espacées sans faces coplanaires superposées.
- Génération complète intégrant les nouveaux assets ; validation des 27 routages de plateau et des six silhouettes distinctes.

## Cités des âges et échecs — 19 septembre 2026

- Cités des âges : plateaux avec ressource de départ, commerce payé aux voisins, copies de cartes selon le nombre de joueurs, guildes, guerre 1/3/5, sciences n²+7, étapes de merveille (y compris construction gratuite et défausse), vue des cités voisines.
- Échecs : roque, prise en passant, promotion, mat, pat, nulle (répétition, 50 coups, matériel insuffisant).
- Les cartes 3D glissent et sautent d'une pose à l'autre (défausse, draft, mémoire, cités).

## Corrections de revue — 19 septembre 2026

- Draft : ouverture du lobby et retour après un départ sans erreur ; les choix simultanés acceptent la même révision, mais un ancien choix ne peut plus jouer dans la main suivante.
- Dames : prises obligatoires et rafles réellement imposées ; recliquer le pion sélectionné le désélectionne.
- Pions reconstruits en volumes pleins, sans carrés tournés superposés ; dames à double étage, hauteur ajustée au damier.
- Cartes du draft espacées sans chevauchement ; clic des cartes tournées et cadrage corrigés. Le clic tient compte de la hauteur de chaque pièce.
- Boutons du lobby accessibles à 240 pixels GUI de hauteur.
- Régénération des assets conservant les six modèles de plateau en 1.21.1, textures déterministes et validation par `tools/Validate-Assets.ps1`.

## 0.1.0-alpha.4 — plusieurs jeux

- Moteur de table séparé des règles : session, actions génériques, pièces 3D et actions légales.
- Cinq jeux : défausse, mémoire, défausse à effets (0/1/2), dames, draft simultané.
- L'hôte change de jeu dans le lobby. La défausse simple reste le défaut.
- Grille mémoire espacée selon le rectangle 8×12 des cartes, sans superposition.
- Dames : damier et pions 3D (dames empilées), plus de cartes numérotées.
- Zoom de la caméra (molette sur la table, +/−). Le cadrage suit l'emprise du plateau.
- L'écran affiche les boutons fournis par la vue, sans verbes de défausse en dur.

## 0.1.0-alpha.3 — ports Minecraft 26.2

- Builds Fabric 26.2 et NeoForge 26.2, Java 25 et Gradle 9.5.1.
- Code Minecraft 26.2 commun aux deux loaders ; règles inchangées et partagées avec 1.21.1.
- Rendu 3D adapté à la séparation extraction/soumission des états.
- Interface transparente, événements clavier/souris et superposition du menu adaptés au nouveau rendu GUI.
- Caméra animée appliquée avant le calcul du champ visible ; masquage du HUD et de l'objet tenu adapté.
- Définitions d'objets modernes et texture de tranche propre au mod pour le nouvel atlas des objets.
- Recette adaptée au format 26.2 et profils client/serveur séparés.
- JAR 1.21.1 conservés en alpha.2. Validation multijoueur visuelle 26.2 encore à effectuer.

## 0.1.0-alpha.2

- Main centrée sur l'écran complet, indépendamment du menu latéral.
- Cartes remontées avec une marge calculée sur leurs coins inclinés et leur ombre.
- Distribution masquée sous la limite de la zone de cartes pour dégager la barre d'aide.
- Menu de partie replié automatiquement après la distribution, ouvrable avec Tab.
- Sélection et profondeur de rendu cohérentes lorsque le menu recouvre la main.
- Grandes mains et redimensionnement : sélection conservée dans la zone visible.
- Pioche épuisée : recyclage de la défausse ou passage du tour si aucune carte n'est disponible.
- Règles, paquets, sessions et présentation partagés avec l'adaptateur NeoForge 1.21.1.
- 11 tests du moteur, dont 100 parties simulées.

## 0.1.0-alpha.1

Premier prototype Fabric 1.21.1 : tapis, lobby à deux places, cartes 3D, caméra et partie multijoueur à cartes numérotées.
