# Yoga — application et tests

Application de studio de yoga : frontend Angular 19 et backend Spring Boot 3.5.5.
Le backend utilise Java 21, Maven 3.9.3 et MySQL. Les outils Java sont sélectionnés
avec `mise` ; le frontend utilise Node 20 et npm. Conserver les données et volumes
locaux existants.

## Installer et lancer

```bash
git clone https://github.com/Yann-Erwann/P4-tester-ameliorez-application.git
cd P4-tester-ameliorez-application
mise install java@21 maven@3.9.3
```

Depuis `backend/`, vérifier le fichier d’environnement local puis démarrer Docker
et le backend :

```bash
mise exec java@21 maven@3.9.3 -- mvn spring-boot:run
```

Dans un second terminal, depuis `frontend/` :

```bash
npm ci
npm start
```

Le [README backend](backend/README.md) décrit MySQL, les données de démonstration,
Docker Compose et les ports alternatifs. Le [README frontend](frontend/README.md)
décrit le proxy et les dépendances système Cypress, notamment Xvfb sous Linux.
Les deux services doivent être prêts pour les parcours contre l’API réelle.

## Partie 2 — exécuter les tests et générer les rapports

| Partie | Commande depuis son dossier | Rapports et seuils |
| --- | --- | --- |
| Frontend Jest | `npm run test:coverage` | `frontend/coverage/jest/index.html`, JSON et LCOV ; ≥ 80 % instructions, branches, fonctions et lignes. |
| Frontend Cypress | Démarrer `npm run e2e:serve`, puis `npm run e2e:ci` dans un autre terminal | `frontend/coverage/e2e/index.html`, JSON, LCOV et JUnit ; ≥ 80 % pour chacun des quatre indicateurs. |
| Backend JUnit/Mockito/H2 | `mise exec java@21 maven@3.9.3 -- mvn -B clean verify` | `backend/target/site/jacoco/index.html` et `target/surefire-reports` ; ≥ 80 % instructions, branches, complexité, méthodes et classes, avec seuil existant de 90 % des lignes par package conservé. |
| Backend avec MySQL | Même commande avec `YOGA_MYSQL_TEST_URL` configurée selon le README backend | Cinq intégrations supplémentaires, fixtures transactionnelles annulées ; schéma existant seulement validé. |

Les rapports de couverture Jest et E2E sont indépendants. Les DTO backend sont
hors du périmètre demandé et exclus de JaCoCo ; le code généré Lombok est marqué
et filtré, le code métier écrit à la main reste mesuré. Les commandes échouent
si un test échoue ou si un indicateur mesuré descend sous son seuil.

Les READMEs détaillés relient les scénarios à la grille d’évaluation fournie et
expliquent le comptage réel des tests d’intégration. Résultats de référence :
Jest **69 tests, dont 47 intégrations (68,12 %)** ; backend avec MySQL **68 tests,
dont 39 intégrations (57,35 %)**. Le minimum demandé est 30 % pour chaque suite.

Cypress distingue les écrans avec API simulée par `cy.intercept` du contrôle
complémentaire sans interception contre Spring/MySQL. Pour ce dernier, voir les
prérequis et la commande `CYPRESS_realApi=true npm run e2e:ci` dans le README
frontend : le serveur instrumenté doit utiliser le proxy visant le vrai backend.

Les artefacts générés restent dans les dossiers ignorés `coverage/` et `target/`.
Les conventions Git et les limites d’intervention sont dans
[CONTRIBUTING.md](CONTRIBUTING.md).
