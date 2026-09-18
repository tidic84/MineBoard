# MineBoard

Prototype de jeu de cartes en 3D dans Minecraft : tapis posé dans le monde, deux places, lobby, caméra animée et main privée.

## État du prototype

**Minecraft 1.21.1, Java 21 : builds Fabric et NeoForge alpha.4.** Fabric a été essayé en jeu ; NeoForge a passé la compilation, le démarrage client et une partie à deux joueurs via `runDedicated` (NeoPlayerOne / NeoPlayerTwo).

**Minecraft 26.2, Java 25 : ports Fabric et NeoForge alpha.4.** Compilation, chargement client et démarrage serveur vérifiés. La validation visuelle et une partie complète à deux joueurs restent à effectuer en 26.2. Chaque version Minecraft et chaque loader possède son propre JAR.

- Un tapis en bois sombre et feutre vert, à poser sur un bloc ou une table de décoration.
- Deux places virtuelles et jusqu'à 16 participants/spectateurs connectés à la table.
- Lobby : prendre place, se déclarer prêt, distribution par l'hôte.
- Caméra locale animée : vue d'ensemble, vue assise selon la place, zoom, mouvement doux près des bords, retour au centre.
- 40 faces originales de cartes avec épaisseur, symboles de couleur et dos commun ; chaque face existe en deux exemplaires.
- Sept cartes par joueur, main en éventail, sélection souris/clavier et défilement des grandes mains.
- Coups vérifiés sur le serveur, main adverse cachée dans les paquets comme dans les données publiques du bloc.
- Animation de distribution dans la main et animation de pose sur la table.

## Installer et jouer

1. Installer Fabric Loader **0.16.14 ou ultérieur**, Minecraft **1.21.1** et Fabric API pour **1.21.1**.
2. Ajouter `fabric-1.21.1/build/libs/mineboard-fabric-1.21.1-0.1.0-alpha.4.jar` dans `mods` sur **le serveur et les deux clients**. Ne pas utiliser le JAR `sources`.
3. En créatif, chercher « MineBoard » dans les blocs fonctionnels, ou utiliser `/give @s mineboard:table`.
4. Poser le tapis sur une surface dégagée. Prévoir deux blocs libres au-dessus et autour pour la caméra.
5. Chaque joueur fait un clic droit sur le tapis, clique sur **Prendre place**, puis **Je suis prêt**.
6. L'hôte change de jeu dans le lobby, puis lance la manche.
7. **Défausse** : même couleur ou même valeur. **Mémoire** : cliquer les cartes du tapis. **Défausse à effets** : 0 passe, 1 inverse, 2 fait piocher deux cartes. **Dames** : clic pour choisir un pion puis une case. **Draft** : les deux joueurs choisissent une carte en même temps.

Pour NeoForge : utiliser **NeoForge 21.1.219 ou ultérieur pour Minecraft 1.21.1** et le fichier `neoforge-1.21.1/build/libs/mineboard-neoforge-1.21.1-0.1.0-alpha.4.jar` sur le serveur et les clients. Fabric API et Architectury API ne sont pas requis sur NeoForge. Choisir le JAR correspondant au loader ; ne pas installer les deux JAR à la fois. La connexion de clients Fabric à un serveur NeoForge n'est pas prise en charge par ce prototype.

Pour **Minecraft 26.2**, utiliser Java **25** et l'un de ces fichiers :

| Loader | Dépendances utilisées pour la validation | Fichier |
|---|---|---|
| Fabric | Loader 0.19.5 + Fabric API 0.160.0+26.2 | `fabric-26.2/build/libs/mineboard-fabric-26.2-0.1.0-alpha.4.jar` |
| NeoForge | NeoForge 26.2.0.88 | `neoforge-26.2/build/libs/mineboard-neoforge-26.2-0.1.0-alpha.4.jar` |

Installer le même JAR sur le serveur et les clients. Alpha.4 aligne les quatre JAR sur le catalogue de jeux, le zoom et le damier.

Recette : trois papiers sur la première ligne, puis planche / tapis vert / planche sur la deuxième.

### Commandes

| Action | Contrôle |
|---|---|
| Choisir une carte | Survol, flèches gauche/droite ou molette sur la main |
| Zoom sur le plateau | Molette hors de la main, ou + / − |
| Jouer | Clic sur la carte, Entrée ou bouton Jouer |
| Piocher et terminer son tour | P ou bouton Piocher |
| Alterner vue assise / vue d'ensemble | V |
| Activer/désactiver le mouvement aux bords | M |
| Ouvrir/replier le menu de partie | Tab ou bouton du menu |
| Quitter la table | Échap ou bouton Quitter |

Les textes suivent la langue Minecraft (français et anglais). Les autres joueurs qui ouvrent une table pleine peuvent regarder sans voir les mains privées.

La main est centrée sur toute la fenêtre, indépendamment du panneau latéral. Une marge tient compte de l'inclinaison des cartes pour préserver la barre d'aide, y compris pendant la distribution. Le menu se replie automatiquement au début de la manche pour laisser la table et la main visibles ; le bouton Piocher reste accessible.

## Règles de cette première version

Cinq jeux originaux, choisis dans le lobby par l'hôte.

**Défausse** : inspirée des jeux de couleurs, **pas une implémentation complète du UNO** : nombres 0 à 9 dans quatre couleurs, deux exemplaires par carte. Piocher termine le tour. Une pioche vide recycle la défausse sauf la carte du dessus.

**Défausse à effets** : mêmes cartes. 0 passe le tour adverse, 1 inverse le sens (à deux joueurs, équivaut à un passage), 2 donne deux cartes à l'adversaire et le passe.

**Mémoire** : grille 4×4, huit paires. Une paire reste au joueur qui rejoue ; un échec reste visible jusqu'au prochain retournement.

**Dames** : 12 pions chacun, déplacements en diagonale, prises obligatoires, dame au bout du plateau.

**Draft** : sept cartes, choix simultané, le reste passe à l'adversaire. Le plus haut total de chiffres gagne.

L'hôte peut proposer une nouvelle manche.

**Quitter sa place, se déconnecter, mourir ou s'éloigner de plus de huit blocs annule la manche et remet les joueurs restants au lobby.** Les parties ne sont pas sauvegardées après redémarrage ou déchargement du bloc. Les spectateurs peuvent quitter sans annuler la partie. Le personnage reste à sa position réelle ; les places et la caméra sont virtuelles, sans siège physique ni bras animés à ce stade. Le serveur ne rend pas les joueurs invulnérables.

Le tapis est un premier modèle fonctionnel, sans animation de boîte qui s'ouvre. La caméra doit disposer d'espace libre : l'évitement des murs n'est pas encore implémenté. Les raccourcis V/M/P/+/− sont fixes dans ce prototype. Le paquet de pioche est une représentation visuelle simplifiée. L'interface ne permet pas encore de cliquer directement sur la pioche 3D.

Les faces mémoire restent celles du paquet numéroté ; un jeu de cartes dédié viendra plus tard. Un plateau plus grand (plusieurs blocs) est préparé côté caméra (`boardSpan`) mais le tapis tient encore sur un seul bloc.

## Développer

```powershell
.\gradlew.bat build
.\gradlew.bat :core:test
.\gradlew.bat :fabric-1.21.1:runClient
.\gradlew.bat :fabric-1.21.1:runClientTwo
.\gradlew.bat -p neoforge-1.21.1 build
.\gradlew.bat -p neoforge-1.21.1 runClient
.\gradlew.bat -p neoforge-1.21.1 runClientTwo
.\gradlew.bat -p neoforge-1.21.1 runDedicated
```

`runServer` NeoForge 1.21.1 ne démarre pas : Architectury Loom charge un JAR Minecraft fusionné, et le serveur dédié refuse `MinecraftClient`. `runDedicated` installe un serveur NeoForge standard (port `25576`, hors ligne) et y copie le JAR remappé. Le client de fumée `runSmokeClient` se connecte à cette adresse.

Les profils client utilisent `PlayerOne` et `PlayerTwo`, avec des dossiers séparés. Pour un essai multijoueur local, ouvrir un monde de développement au LAN puis connecter le deuxième client à `localhost:PORT` (port affiché dans le chat). Ces profils hors ligne sont uniquement destinés au développement.

Pour 26.2, définir `JAVA_HOME` vers un JDK 25 et utiliser le wrapper Gradle dédié :

```powershell
.\fabric-26.2\gradlew.bat -p fabric-26.2 build
.\fabric-26.2\gradlew.bat -p neoforge-26.2 build
.\fabric-26.2\gradlew.bat -p fabric-26.2 runClient
.\fabric-26.2\gradlew.bat -p fabric-26.2 runClientTwo
.\fabric-26.2\gradlew.bat -p neoforge-26.2 runClient
.\fabric-26.2\gradlew.bat -p neoforge-26.2 runClientTwo
```

Les serveurs de développement 26.2 utilisent `runServer` et un dossier `run-server` séparé des clients. Accepter l'EULA dans ce dossier avant le premier démarrage.

Les wrappers Gradle sont inclus. Les dépendances sont téléchargées au premier build. Les textures sont versionnées ; leur régénération facultative sous Windows utilise `powershell -NoProfile -File tools/Generate-Assets.ps1`.

## Structure et prochaines cibles

- `core` : règles Java indépendantes de Minecraft, vues filtrées par destinataire et tests JUnit.
- `fabric-1.21.1/src/main` : bloc, enregistrement et réseau serveur.
- `fabric-1.21.1/src/client` : caméra, rendu 3D et interactions.
- `tools/Generate-Assets.ps1` : source reproductible des modèles et textures.
- `neoforge-1.21.1` : adaptateurs NeoForge et build réutilisant les sources et les assets 1.21.1.
- `docs/TESTING.md` : vérifications automatiques et parcours manuel.
- `docs/ARCHITECTURE.md` : partage des sources et organisation des adaptateurs.

`minecraft-26.2` contient le bloc, le réseau et le client communs aux builds `fabric-26.2` et `neoforge-26.2`, avec le nouveau rendu par extraction. Ils réutilisent le moteur de règles, les modèles et les traductions de 1.21.1. Aucune compatibilité avec les shaders ou mods de caméra tiers n'est annoncée sans essai.
