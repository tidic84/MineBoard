# Validation du prototype

## Automatisé

`gradlew build` compile le serveur et le client, exécute les tests du cœur, puis produit le JAR Fabric remappé contenant le cœur.

`powershell -NoProfile -File tools/Validate-Assets.ps1` contrôle les textures locales, les correspondances des 27 modèles de plateau entre 1.21.1 et 26.2, les six silhouettes d'échecs distinctes et l'absence d'intersection des volumes des pions, pièces d'échecs et plaques. Lancer aussi ce contrôle après `tools/Generate-Assets.ps1`.

`TableGameTest` vérifie la défausse : lancement, hôte, confidentialité, coups périmés, pioche, recyclage et 100 parties. `MemoryGameTest`, `EffectsGameTest`, `CheckersGameTest`, `DraftGameTest`, `AgesGameTest`, `ChessGameTest` et `LayoutsTest` couvrent les autres jeux et la géométrie du plateau (paires, effets 0/2, ouverture des dames, draft simultané, cités des âges, échecs, cartes mémoire sans superposition, pions distincts des cartes). `PieceMotionTest` couvre le glissement des pièces et le saut des cartes.

## Parcours manuel à deux clients

Utiliser un monde jetable, en créatif, avec une zone dégagée.

1. Donner et poser `mineboard:table`. Le modèle et les cartes ne doivent pas avoir de texture manquante.
2. Ouvrir la table : caméra fluide, panneau à droite, monde toujours visible.
3. Prendre une place avec chaque client : places distinctes, noms synchronisés, points de vue opposés.
4. Vérifier que Distribuer est désactivé avant que les deux joueurs soient prêts.
5. Distribuer : chaque client reçoit sept cartes, l'autre main reste cachée.
6. Jouer une carte valide et une invalide. Seule la première modifie la défausse et le tour.
7. Vérifier clavier, souris, molette et une main de plus de neuf cartes.
8. Déplacer la souris aux bords puis au centre : mouvement limité et retour doux ; pas de mouvement sur le panneau ni pendant la sélection des cartes.
9. Tester V, M et une petite résolution avec GUI scale élevé.
10. Quitter : retour de la caméra et des commandes normales ; l'autre joueur revient au lobby.
11. Tester déconnexion, destruction du tapis, changement de dimension et mort. Aucun écran/caméra ne doit rester bloqué.
12. Tester plusieurs tables : actions et mains ne doivent jamais passer d'une table à l'autre.

Les essais unitaires ne remplacent pas la validation visuelle ni un essai réseau entre deux vrais clients. Consulter le compte rendu de livraison pour les contrôles effectivement réalisés.

## Contrôles effectués pour alpha.2 — 18 septembre 2026

- Build Fabric 1.21.1 réussi après séparation du transport réseau.
- 11 tests JUnit réussis, dont 100 parties jusqu'à la victoire, pioche épuisée, recyclage et départ d'un spectateur.
- Vérification géométrique des mains de 1 à 9 cartes visibles, sur des largeurs de 320 à 1280 et hauteurs de 240 à 720 pixels GUI : marge sous l'éventail d'au moins 12 pixels, coins et ombre inclus.
- Build NeoForge 21.1.219 réussi, sans référence à Fabric API dans les sources communes compilées pour NeoForge.
- Client NeoForge lancé : ressources du mod chargées, absence d'erreur de modèle ou de texture MineBoard dans les logs de chargement.
- JAR final installé sur un serveur NeoForge standard, limité à `127.0.0.1:25576` : démarrage complet, placement de `mineboard:table`, lecture de son NBT et arrêt propre avec sauvegarde.

Le profil `runServer` Architectury Loom charge un JAR Minecraft fusionné : `RuntimeDistCleaner` refuse `MinecraftClient` pendant l'enregistrement des paquets NeoForge. Utiliser `gradlew -p neoforge-1.21.1 runDedicated` (serveur standard, port `25576`) ou copier le JAR remappé dans une installation dédiée. Ne pas confondre ce test avec une partie réseau complète sur NeoForge.

La disposition alpha.2 doit encore être appréciée visuellement en jeu. Le centrage, les marges et les limites de clic ont été vérifiés dans le code ; aucune nouvelle capture d'écran de la main n'a été validée pendant cette itération.

## Contrôles effectués pour alpha.3 — 18 septembre 2026

- Builds Fabric 26.2 et NeoForge 26.2 réussis sous Java 25, avec sources communes du moteur et du client 26.2.
- Vérification du cœur par `:core:test` : résultat Gradle à jour, 11 tests, zéro échec et zéro erreur. Le moteur n'a pas changé pendant ce portage.
- Démarrage des clients Fabric 0.19.5 / API 0.160.0+26.2 et NeoForge 26.2.0.88 : chargement des ressources terminé, sans erreur de modèle MineBoard après correction de l'atlas des cartes. Les hooks caméra, main et HUD ne provoquent pas d'erreur de transformation au chargement.
- Serveur Fabric de développement sur `127.0.0.1:25577` : démarrage complet, chargement du chunk, placement de `mineboard:table`, lecture du NBT, arrêt par `stop` avec sauvegarde.
- Serveur NeoForge de développement sur `127.0.0.1:25579` : mêmes vérifications réussies. Son entrée standard est raccordée pour permettre les commandes console et son dossier `run-server` est séparé des clients.
- Les premiers démarrages ont signalé les avertissements habituels des profils hors ligne (Realms/authentification) et des compteurs Windows. Aucun de ces avertissements n'a empêché le chargement du mod.

Cette validation porte sur les profils de développement, pas sur une installation des JAR finaux dans un launcher standard. Le parcours visuel à deux clients ci-dessus reste à faire en 26.2, notamment le survol des cartes, la distribution, la superposition du menu, les transitions caméra et la confidentialité réseau en partie réelle. Vulkan, shaders et autres mods de caméra n'ont pas été testés.

## Contrôles effectués pour alpha.4 — 19 septembre 2026

- 25 tests JUnit du cœur, zéro échec : défausse, mémoire sans superposition rectangulaire, dames avec pions dédiés, draft, effets, `hasTurnAction`.
- TableScreen 1.21.1 et 26.2 : boutons de tour lus dans `view.buttons()`, plus de Piocher/Jouer en dur.
- Compilation Fabric 1.21.1 (main + client) et NeoForge 1.21.1.
- Grille mémoire : emprise 8×12 seizièmes, cases disjointes, dans le tapis.
- Dames : 64 cases + 24 pions `cell`/`token`, plus de faces 0–39.
- Caméra : zoom molette / + −, cadrage `boardSpan`.
- Versions Gradle et README en `0.1.0-alpha.4`. JAR produits et métadonnées internes à `0.1.0-alpha.4` : Fabric 1.21.1, NeoForge 1.21.1, Fabric 26.2, NeoForge 26.2.

## Revue des corrections — 19 septembre 2026

- 34 tests JUnit réussis, dont neuf nouveaux cas : lobby et départ du draft, révisions de choix simultanés, prises et rafles obligatoires, désélection, clic des cartes tournées, cadrage et espacement du draft complet.
- Builds Fabric et NeoForge, en 1.21.1 et 26.2, réussis. Les quatre JAR restent en alpha.4.
- Génération complète des assets dans un répertoire de test séparé, suivie de `Validate-Assets.ps1` : succès. Deux régénérations des textures de cases et de pions donnent les mêmes SHA-256.
- Client Fabric 1.21.1 : chargement des ressources, entrée dans un monde plat de test séparé, ouverture du tapis et cycle jusqu'au lobby Draft sans erreur. Boutons Prêt / Jeu / Distribuer / Quitter distincts à 240 pixels GUI de hauteur.
- Inspection visuelle dans Minecraft des modèles de damier, pions, dames et cartes, à l'aide d'une scène locale d'entités `item_display` utilisant les modèles et les échelles du jeu. Cette scène vérifie les assets ; elle ne remplace pas un essai complet du rendu de partie et du réseau.
- Le deuxième client a planté deux fois dans `glfw.dll` (`EXCEPTION_ACCESS_VIOLATION`) lors de son activation. Aucun nouvel essai multijoueur complet n'est donc revendiqué. Le rendu en jeu de 26.2 reste à contrôler ; sa compilation a été vérifiée.

## Revue après la fin de Grok — 19 septembre 2026

- 67 tests JUnit, zéro échec, erreur ou test ignoré. Les 17 régressions ajoutées couvrent notamment le roque sous attaque de pion, les rois espacés de deux cases, les faux matériels insuffisants, la priorité du mat, les identifiants des mains cachées, le commerce limité aux quantités disponibles, la récupération gratuite et les animations de prises.
- Parties complètes simulées de Cités des âges pour chacun des effectifs de 3 à 7 joueurs.
- Builds finaux réussis : Fabric 1.21.1 et NeoForge 1.21.1 sous Java 21 ; Fabric 26.2 et NeoForge 26.2 sous Java 25. Les JAR restent en alpha.4 dans les dossiers `build/libs` respectifs.
- `Validate-Assets.ps1` réussi : 27 correspondances de modèles entre versions, textures présentes, six silhouettes d'échecs différentes et volumes sans intersection. Le validateur utilise explicitement des nombres flottants pour ne pas arrondir les coordonnées mixtes entières/décimales.
- Génération complète dans `build/review/asset-regeneration`, puis validation : succès. Les 21 nouveaux modèles ont la même géométrie JSON que les sources ; leurs 21 textures sont identiques octet pour octet.
- Clés de traduction FR/EN identiques, avec les noms des 78 cartes distinctes et les 25 effets spéciaux. `git diff --check` réussi.
- Aperçus des volumes JSON et textures examinés : `build/review/chess-models.png` et `build/review/board-models.png`. Régénération par `python tools/Preview-Models.py` avec Pillow sous Windows. Cet aperçu applique un éclairage simplifié et une couleur moyenne par face ; il ne reproduit pas le moteur Minecraft.

Aucun client Minecraft ni essai multijoueur n'a été lancé pendant cette revue. Les captures et essais en jeu des sections précédentes sont historiques. Restent à valider en jeu : cadrage des cités à sept joueurs, survol des cartes à grande échelle GUI, sélection des pièces hautes, transitions caméra et transport réseau. Les illustrations de cités distinguent les familles ; les informations propres à chaque carte sont affichées au survol.
