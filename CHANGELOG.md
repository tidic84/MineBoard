# Versions de développement

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
