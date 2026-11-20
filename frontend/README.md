# Yoga — frontend Angular 19

Depuis la racine du dépôt, entrer dans `frontend/`. Utiliser Node 20 et npm
(versions vérifiées : Node 20.19.2, npm 9.2.0), puis installer les dépendances
à partir du lockfile :

```bash
cd frontend
npm ci
npm start
```

`npm start` utilise `ng serve` et le fichier versionné `proxy.conf.json` pour
transmettre les requêtes `/api` à Spring sur le port 8080. Le
[README backend](../backend/README.md) décrit le démarrage de Spring et de MySQL.

## Partie 2 — Jest : unitaires et intégration

```bash
npm test -- --ci --runInBand
npm run test:coverage
npm run lint
```

Jest utilise `jest.config.js`, `setup-jest.ts` et l’environnement Angular/JSDOM.
Pour travailler sur un seul test, utiliser par exemple
`npm test -- --runInBand src/app/pages/not-found/not-found.component.spec.ts`.
La commande `npm run test:coverage` active la mesure et les seuils pour la suite complète.
La collecte inclut **tous** les fichiers `src/app/**/*.ts`, même non importés par
un test, en excluant uniquement les fichiers de tests. Les interfaces ne
produisent pas de code exécutable ; démarrage, polyfills et configuration de
l’outillage sont hors de ce périmètre applicatif. Les seuils globaux bloquants
sont **80 % pour instructions, branches, fonctions et lignes**, séparément.

Rapports produits dans `coverage/jest/` : `index.html`, `lcov.info`,
`coverage-final.json`, `coverage-summary.json` et, par `test:coverage`,
`test-results.json`. Les rapports générés restent exclus de Git.

| Catégorie | Scénarios et frontière de test |
| --- | --- |
| `[unit]` J-U01 | État de session et décisions des gardes ; navigation espionnée, sans écran distant. |
| `[unit]` J-U02 | Durée de vie des requêtes des composants, templates remplacés ; annulation à la destruction et absence d’effet tardif. |
| `[unit]` J-U03 | Création des services et contrôles isolés conservés. |
| `[integration]` J-I01/J-I03 | Services réels, configuration HttpClient et intercepteur JWT réels, avec HttpTestingController uniquement à la frontière réseau. Méthodes, chemins, corps, jeton et propagation des erreurs. |
| `[integration]` J-I02 | Composant + vrai template + formulaires/routeur/services Angular ; requêtes contrôlées par HttpTestingController, sans serveur Spring. Conditions, listes, actions, navigation et erreurs déjà gérées par l’application. |

Les cas paramétrés comptent individuellement. La dernière suite comporte
**69 tests réussis, dont 47 intégrations (68,12 %)**, au-dessus des 30 % demandés.
Après chaque évolution, recalculer le ratio depuis le rapport réellement exécuté :

```bash
node -e 'const r=require("./coverage/jest/test-results.json");const tests=r.testResults.flatMap(s=>s.assertionResults).filter(t=>t.status==="passed");const integration=tests.filter(t=>t.fullName.includes("[integration]"));const ratio=integration.length/tests.length;console.log({total:tests.length,integration:integration.length,percent:100*ratio});if(ratio<0.30)process.exit(1)'
```

Cette classification décrit les collaborateurs réellement présents : un test
avec HttpTestingController n’est pas un appel à Spring/MySQL. Les tests de
durée de vie qui remplacent le template ne sont pas comptés comme intégrations.

La grille fournie par l’utilisateur comporte quatre étapes : Jest, Cypress,
Mockito/JUnit et README. Les identifiants de ce README sont les scénarios de
mise en œuvre de cette grille. La frontière réseau et la classification de
chaque famille de tests sont indiquées pour rendre le ratio vérifiable.

## Cypress — écrans et couverture E2E

Sous Linux, installer les dépendances système de Cypress, notamment Xvfb et les
bibliothèques GTK/GBM/NSS/audio. Cypress 15.2.0 est déclaré dans le lockfile.
Vérifier son installation avec `npx cypress verify`.

Dans un premier terminal :

```bash
npm run e2e:serve
```

Le serveur de test instrumenté écoute sur `http://127.0.0.1:4203`. Il utilise la
configuration Angular `e2e` et `cypress/coverage.webpack.ts`. Le code applicatif
reste inchangé ; l’instrumentation est limitée à cette construction de test.

Dans un second terminal :

```bash
npm run e2e:ci
```

La configuration Cypress est `cypress.config.ts`, le support de collecte est
`cypress/support/e2e.ts`, et NYC lit `nyc.config.cjs`. Cypress découvre les specs
dans `cypress/e2e/` et ses sous-dossiers. Le parcours `real-api.cy.ts` s’active
avec `CYPRESS_realApi=true`. La suite `screens.cy.ts`
utilise le vrai navigateur et les vrais composants/services Angular, avec
`cy.intercept()` à la frontière HTTP pour des scénarios déterministes.

| Cas | Écran et comportement |
| --- | --- |
| E-01 / E-02 | Inscription nominale et adresse refusée. |
| E-03 | Connexion refusée et visibilité du mot de passe de connexion. |
| E-04 | Garde des routes privées et écran introuvable. |
| E-05 / E-06 | Liste/détail membre, participation/retrait et liste vide/déconnexion. |
| E-07 / E-08 | Création, édition, suppression admin et retour sans sauvegarde. |
| E-09 / E-10 | Compte administrateur, retour et suppression du compte membre. |

Rapports E2E : `coverage/e2e/index.html`, `lcov.info`, `coverage-summary.json`,
rapports JUnit dans `coverage/e2e/results/` et captures d’échec dans
`coverage/e2e/screenshots/`. `npm run e2e:coverage` régénère les rapports et
**échoue si aucune ligne applicative n’a été mesurée ou si l’un des quatre
indicateurs descend sous 80 %**. Le runner interactif (`npm run cypress:open`)
utilise `coverage/e2e-open/` pour sa couverture et ses artefacts. La
couverture E2E n’est pas fusionnée avec Jest pour atteindre artificiellement un
seuil : chaque partie dispose de son propre rapport et contrôle.

### Contrôle complémentaire avec Spring/MySQL réels

Depuis `frontend/`, avec Spring sur le port **8080**, démarrer le serveur de test
avec le proxy fourni dans le dépôt. Si `e2e:serve` occupe déjà le port 4203,
le relancer avec cette commande.

```bash
npm run e2e:serve -- --proxy-config proxy.conf.json
```

Si Spring écoute sur **8082**, copier `proxy.conf.json` hors du dépôt, par exemple
dans `/tmp/yoga-proxy.json`, puis remplacer la valeur de `target` dans la copie
par `http://127.0.0.1:8082`. Utiliser ensuite
`npm run e2e:serve -- --proxy-config /tmp/yoga-proxy.json`.
Ce même proxy fonctionne avec `npm start -- --port 4202 --proxy-config /tmp/yoga-proxy.json`
pour le serveur de développement.

Le backend et MySQL doivent être prêts. Le contrôle réel est en lecture seule et
nécessite un compte administrateur et au moins une séance avec un enseignant.
L’utilisateur de démonstration documenté dans le README backend convient aux
données locales. D’autres identifiants peuvent être fournis par les variables
`CYPRESS_adminEmail` et `CYPRESS_adminPassword`, sans les commiter.

```bash
# Les dix parcours contrôlés et le parcours réel, avec rapport commun E2E :
CYPRESS_realApi=true npm run e2e:ci
```

`real-api.cy.ts` ne contient aucun `cy.intercept` : connexion, JWT, liste,
détail/enseignant, compte et déconnexion traversent le proxy Angular, les
contrôleurs/services Spring et MySQL. Ce contrôle n’insère ni ne supprime de
données. La suite complète a passé **11 tests sans échec ni test ignoré**.
La couverture E2E reste indépendante de celle de Jest. Chaque `cypress run`
nettoie ses données de couverture brutes et ses résultats JUnit avant la collecte.
Les dernières mesures E2E sont : 98,21 % instructions, 94,54 % branches,
100 % fonctions et 98,19 % lignes.

## Comportements applicatifs conservés

Cette partie modifie les tests, leur configuration et la documentation. Les
validateurs de longueur d’origine, l’affichage du mot de passe d’inscription,
la redirection existante de la garde invitée et la gestion limitée des erreurs
des formulaires de séance ne sont pas corrigés silencieusement par les tests.
Le build production a un avertissement de taille initiale, distinct de la
couverture et du résultat de `ng serve`.
