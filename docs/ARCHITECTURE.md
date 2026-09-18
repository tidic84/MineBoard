# Architecture du prototype

Le module `core` contient les règles sans dépendance à Minecraft. `TableSession` gère le lobby, les places et la révision. Chaque jeu implémente `Game` et remplit une vue filtrée : main privée, actions légales et pièces 3D. Le serveur applique une action générique `{type, cible}` ; le client n’encode plus les verbes d’un jeu. La vue publique du bloc utilise un destinataire nul et ne contient aucune main.

Pour Minecraft 1.21.1, le bloc, ses données publiques, les paquets, la gestion des sessions, le rendu, la caméra et l'écran sont actuellement rangés dans le module Fabric. Leur package historique `fr.mineboard.fabric` ne signifie pas qu'ils dépendent tous de Fabric. Les dépendances au loader sont limitées aux points d'entrée et aux adaptateurs de transport.

## Adaptateurs

- Fabric : `MineBoard`, `FabricNetworking`, `MineBoardClient`.
- NeoForge : son propre `MineBoard`, ainsi que `NeoClient`.
- `TableNetworking.setSender` raccorde l'envoi de paquets serveur ; `handle`, `tick`, `disconnect` et `stop` sont appelés par les hooks du loader.
- `ClientTransport` raccorde l'envoi client et applique les vues reçues dans le même écran sur les deux loaders.

Le build NeoForge utilise une tâche `syncSharedSources` pour compiler les sources communes sans les dupliquer dans le dépôt. Son dossier `build/generated/shared` est une sortie de build, à ne pas modifier. Les assets et traductions proviennent du même dossier de ressources ; `fabric.mod.json` est exclu du JAR NeoForge.

Les builds sont séparés pour ne pas charger Fabric Loom et Architectury Loom dans le même classloader Gradle. Depuis la racine :

```powershell
.\gradlew.bat build
.\gradlew.bat -p neoforge-1.21.1 build
```

Architectury est utilisé comme outil de build pour remapper les noms Yarn vers les noms attendus par NeoForge. Le mod n'a pas de dépendance d'exécution à Architectury API.

## Interface

Le centre horizontal de la main est toujours `width / 2`. Sa hauteur prend en compte la plus basse des cartes de l'éventail, son inclinaison et son ombre, puis réserve 12 pixels avant la barre d'aide. Le rendu et le test de survol partagent les mêmes positions. Un menu ouvert possède la priorité d'affichage et d'interaction sur sa surface. La caméra reste indépendante de la position serveur du personnage. La molette zoome sur le plateau sauf au-dessus de la main ; + et − font de même. La distance suit `Game.boardSpan()` et l'emprise des pièces 3D, pour des tapis plus grands plus tard.

Les cartes 3D occupent 8×12 seizièmes, pas un carré : `Layouts` espace la grille mémoire en rectangle. Les dames dessinent un damier et des pions (`cell` / `token`) au lieu de réutiliser les faces du paquet.

## Limites

Les sessions sont transitoires, à deux places. La fermeture d'une place annule la manche ; la fermeture d'un spectateur ne la modifie pas.

## Minecraft 26.2

Les sources communes sont dans `minecraft-26.2`, en noms officiels. `fabric-26.2` utilise Fabric Loom 1.17.20 et `neoforge-26.2` ModDevGradle 2.0.147. Les deux builds utilisent Java 25 et le wrapper Gradle 9.5.1 de `fabric-26.2`, séparé du wrapper 1.21.1. Aucun remappage Yarn ni dépendance Architectury n'est nécessaire pour 26.2.

Le renderer extrait une liste d'états de cartes et leurs transformations depuis le bloc, puis soumet ces états sans relire la partie pendant le rendu. Les cartes utilisent le composant `ITEM_MODEL` et des définitions dans `assets/mineboard/items`. Les modèles existants sont réutilisés ; le build remplace seulement la référence de tranche par une texture originale dans l'atlas des objets.

L'interface utilise `GuiGraphicsExtractor`, une pile de transformations 2D et des strates pour respecter la priorité de la carte sélectionnée et du menu. Le mixin caméra intervient à la fin de `alignWithEntity`, avant le calcul du frustum. Les hooks de main et HUD sont exclusivement clients. Les points d'entrée de chaque loader enregistrent les contenus, les transports et les événements de cycle de vie.
