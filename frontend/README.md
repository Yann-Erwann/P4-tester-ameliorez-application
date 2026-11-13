# Yoga — frontend Angular 19

Depuis la racine du dépôt, entrer dans `frontend/`. Utiliser Node 20 et npm
(versions vérifiées : Node 20.19.2, npm 9.2.0), puis installer les dépendances
à partir du lockfile :

```bash
cd frontend
npm ci
npm start
```

`npm start` utilise `ng serve`. Le serveur et son proxy doivent viser le backend
Spring démarré selon [son README](../backend/README.md). Si les ports usuels sont
occupés, conserver les autres applications et utiliser un proxy temporaire hors
du dépôt, par exemple `/tmp/yoga-real-proxy.json` pointant `/api` vers Spring 8082 :

```bash
npm start -- --host 127.0.0.1 --port 4202 --proxy-config /tmp/yoga-real-proxy.json
```

## Partie 2 — Jest : unitaires et intégration

```bash
npm test -- --ci --runInBand
npm run test:coverage
npm run lint
```

Jest utilise `jest.config.js`, `setup-jest.ts` et l’environnement Angular/JSDOM.
La collecte inclut **tous** les fichiers `src/app/**/*.ts`, même non importés par
un test, en excluant uniquement les fichiers de tests. Les interfaces ne
produisent pas de code exécutable, démarrage, polyfills et configuration de
l’outillage sont hors de ce périmètre applicatif. Les seuils globaux bloquants
sont **80 % pour instructions, branches, fonctions et lignes**, séparément.

Rapports produits dans `coverage/jest/` : `index.html`, `lcov.info`,
`coverage-final.json`, `coverage-summary.json` et, par `test:coverage`,
`test-results.json`. Les rapports générés restent exclus de Git.

| Catégorie | Scénarios et frontière de test |
| --- | --- |
| `[unit]` J-U01 | État de session et décisions des gardes, navigation espionnée, sans écran distant. |
| `[unit]` J-U02 | Durée de vie des requêtes des composants, templates remplacés, annulation à la destruction et absence d’effet tardif. |
| `[unit]` J-U03 | Création des services et contrôles isolés conservés. |
| `[integration]` J-I01/J-I03 | Services réels, configuration HttpClient et intercepteur JWT réels, avec HttpTestingController uniquement à la frontière réseau. Méthodes, chemins, corps, jeton et propagation des erreurs. |
| `[integration]` J-I02 | Composant + vrai template + formulaires/routeur/services Angular, requêtes contrôlées par HttpTestingController, sans serveur Spring. Conditions, listes, actions, navigation et erreurs déjà gérées par l’application. |

Les cas paramétrés comptent individuellement. La dernière suite comporte
**69 tests réussis, dont 47 intégrations (68,12 %)**, au-dessus des 30 % demandés.
Après chaque évolution, recalculer le ratio depuis le rapport réellement exécuté :

```bash
node -e 'const r=require("./coverage/jest/test-results.json");const tests=r.testResults.flatMap(s=>s.assertionResults).filter(t=>t.status==="passed");const integration=tests.filter(t=>t.fullName.includes("[integration]"));const ratio=integration.length/tests.length;console.log({total:tests.length,integration:integration.length,percent:100*ratio});if(ratio<0.30)process.exit(1)'
```

Cette classification décrit les collaborateurs réellement présents : un test
avec HttpTestingController n’est pas un appel à Spring/MySQL. Les tests de
durée de vie qui remplacent le template ne sont pas comptés comme intégrations.

Le « plan de testing fourni » mentionné dans le sujet n’est pas présent dans le
dépôt au début de cette partie. Les identifiants ci-dessus et ci-dessous sont
une matrice de travail explicite, à rapprocher de ce plan lorsqu’il est disponible ;
ils ne constituent pas une affirmation de conformité à un document absent.
