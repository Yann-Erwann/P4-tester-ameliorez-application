# Yoga — application et tests

Application de studio de yoga : frontend Angular 19 et backend Spring Boot 3.5.5.
Le backend utilise Java 21, Maven 3.9.3 et MySQL. Les outils Java sont sélectionnés
avec `mise`, le frontend utilise Node 20 et npm. Conserver les données et volumes
locaux existants.

## Rapports de couverture

Les rapports HTML de la version `v0.0.2` sont disponibles au format ZIP.
Extraire chaque archive, puis ouvrir son fichier `index.html`.

- [Jest — tests frontend](https://github.com/Yann-Erwann/P4-tester-ameliorez-application/releases/download/v0.0.2/coverage-jest-v0.0.2.zip)
- [JaCoCo — tests backend](https://github.com/Yann-Erwann/P4-tester-ameliorez-application/releases/download/v0.0.2/coverage-jacoco-v0.0.2.zip)
- [Cypress — parcours d’écran](https://github.com/Yann-Erwann/P4-tester-ameliorez-application/releases/download/v0.0.2/coverage-cypress-v0.0.2.zip)

## Installer et lancer

```bash
git clone https://github.com/Yann-Erwann/P4-tester-ameliorez-application.git
cd P4-tester-ameliorez-application
mise install java@21 maven@3.9.3
```

**Terminal 1.** Démarrer Docker avec Compose sur la machine qui héberge le
projet. Depuis la racine du clone, lancer Spring avec le fichier
`backend/.env` fourni. Spring démarre MySQL via `compose.yaml` et écoute sur le port 8080.

```bash
cd backend
mise exec java@21 maven@3.9.3 -- mvn spring-boot:run
```

**Terminal 2.** Depuis la racine du clone, utiliser Node 20.19.2 et npm pour
installer les dépendances du lockfile et lancer Angular.

```bash
cd frontend
npm ci
npm start
```

Ouvrir l’application sur `http://localhost:4200`. Avec VS Code Remote, transférer
le port 4200 de la VM dans l’onglet **Ports**, puis ouvrir l’adresse locale
indiquée sur le Mac. Angular relaie les requêtes `/api` vers Spring.

Le [README backend](backend/README.md) décrit MySQL, les données de démonstration,
Docker Compose et les ports alternatifs. Le [README frontend](frontend/README.md)
décrit le proxy et les dépendances système Cypress, notamment Xvfb sous Linux.
Les deux services doivent être prêts pour les parcours contre l’API réelle.

## Partie 2 — exécuter les tests et générer les rapports

| Partie | Commande et dossier de travail | Rapports et seuils |
| --- | --- | --- |
| Frontend Jest | Dans `frontend/`, exécuter `npm run test:coverage` | `frontend/coverage/jest/index.html`, JSON et LCOV. Minimum de 80 % pour les instructions, branches, fonctions et lignes. |
| Frontend Cypress | Dans `frontend/`, lancer `npm run e2e:serve` et attendre la compilation sur le port 4203. Garder ce terminal ouvert et exécuter `npm run e2e:ci` dans un deuxième terminal depuis `frontend/`. | `frontend/coverage/e2e/index.html`, JSON, LCOV et résultats JUnit. Minimum de 80 % pour chacun des quatre indicateurs. |
| Backend JUnit/Mockito/H2 | Dans `backend/`, exécuter `mise exec java@21 maven@3.9.3 -- mvn -B clean verify` | `backend/target/site/jacoco/index.html` et `backend/target/surefire-reports/`. Par package, minimum de 90 % des lignes et de 80 % pour les instructions, branches, complexité, méthodes et classes. |
| Backend avec MySQL | Même commande avec `YOGA_MYSQL_TEST_URL` configurée selon le README backend | Cinq intégrations supplémentaires, fixtures transactionnelles annulées, schéma existant seulement validé. |

Les rapports de couverture Jest et E2E sont indépendants. Les DTO backend sont
hors du périmètre demandé et exclus de JaCoCo, le code généré Lombok est marqué
et filtré, le code métier écrit à la main reste mesuré. Les commandes échouent
si un test échoue ou si un indicateur mesuré descend sous son seuil.

Les READMEs détaillés relient les scénarios à la grille d’évaluation fournie et
expliquent le comptage réel des tests d’intégration. Résultats de référence :
Jest **69 tests, dont 47 intégrations (68,12 %)** backend avec MySQL **68 tests,
dont 39 intégrations (57,35 %)**. Le minimum demandé est 30 % pour chaque suite.

Cypress distingue les écrans avec API simulée par `cy.intercept` du contrôle
complémentaire sans interception contre Spring/MySQL. Pour ce dernier, voir les
prérequis et la commande `CYPRESS_realApi=true npm run e2e:ci` dans le README
frontend : le serveur instrumenté doit utiliser le proxy visant le vrai backend.

## Lire les trois rapports de couverture

Après chaque commande de test, ouvrir le fichier `index.html` de la suite
correspondante dans un navigateur. Les chemins du tableau partent de la racine
du dépôt. Pour transmettre un rapport, copier son dossier complet afin de
conserver les feuilles de style et les pages détaillées.

- **Jest et Cypress** affichent chacun leur synthèse **All files**. Lire les quatre
  pourcentages séparément. **Statements** désigne les instructions exécutées,
  **Branches** les issues de décision parcourues, **Functions** les fonctions
  appelées et **Lines** les lignes exécutées. Cliquer sur un dossier puis sur un
  fichier pour repérer les lignes en rouge ou les branches manquantes.
- **JaCoCo** donne la synthèse sur la ligne **Total**. Les colonnes **Cov.**
  indiquent les pourcentages des instructions et des branches. Pour les lignes,
  méthodes, classes et la complexité, lire les colonnes **Missed** et le total
  correspondant. Par exemple, 2 méthodes manquées sur 20 donnent 90 % de
  couverture. Cliquer sur un package pour vérifier ses seuils, puis sur une
  classe pour voir les lignes exécutées, partielles ou manquées.

Les artefacts générés restent dans les dossiers ignorés `coverage/` et `target/`.
Les conventions Git et les limites d’intervention sont dans
[CONTRIBUTING.md](CONTRIBUTING.md).
