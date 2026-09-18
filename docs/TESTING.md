# Validation du prototype

## Automatisé

`gradlew build` compile le serveur et le client, exécute les tests du cœur, puis produit le JAR Fabric remappé contenant le cœur.

`TableGameTest` vérifie la défausse : lancement, hôte, confidentialité, coups périmés, pioche, recyclage et 100 parties. `MemoryGameTest`, `EffectsGameTest`, `CheckersGameTest`, `DraftGameTest` et `LayoutsTest` couvrent les autres jeux et la géométrie du plateau (paires, effets 0/2, ouverture des dames, draft simultané, cartes mémoire sans superposition, pions distincts des cartes).

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
