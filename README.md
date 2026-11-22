# Yoga — application et tests

Application de studio de yoga : frontend Angular 19 et backend Spring Boot 3.5.5.
Le backend utilise Java 21, Maven 3.9.3 et MySQL. Les outils Java sont sélectionnés
avec `mise`, le frontend utilise Node 20 et npm. Conserver les données et volumes
locaux existants.

## Rapports de couverture

Les rapports HTML de la version `v1.0.0` sont consultables sur [GitHub Pages](https://yann-erwann.github.io/P4-tester-ameliorez-application/).
Pour une lecture hors ligne, extraire le ZIP et ouvrir son fichier `index.html`.

| Rapport | Consultation | Téléchargement |
| --- | --- | --- |
| Jest — tests frontend | [Voir le rapport](https://yann-erwann.github.io/P4-tester-ameliorez-application/v1.0.0/jest/) | [Télécharger le ZIP](https://github.com/Yann-Erwann/P4-tester-ameliorez-application/releases/download/v1.0.0/coverage-jest-v1.0.0.zip) |
| JaCoCo — tests backend | [Voir le rapport](https://yann-erwann.github.io/P4-tester-ameliorez-application/v1.0.0/jacoco/) | [Télécharger le ZIP](https://github.com/Yann-Erwann/P4-tester-ameliorez-application/releases/download/v1.0.0/coverage-jacoco-v1.0.0.zip) |
| Cypress — 10 parcours simulés et E-R01 sur API réelle | [Voir le rapport](https://yann-erwann.github.io/P4-tester-ameliorez-application/v1.0.0/cypress/) | [Télécharger le ZIP](https://github.com/Yann-Erwann/P4-tester-ameliorez-application/releases/download/v1.0.0/coverage-cypress-v1.0.0.zip) |

### Nombre de tests réussis dans `v1.0.0`

| Suite | Tests unitaires | Tests d’intégration | Tests de bout en bout (E2E) | Autres contrôles | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Jest · frontend | 22 | 47 | 0 | 0 | 69 |
| JUnit · backend | 27 | 39 | 0 | 2 | 68 |
| Cypress · navigateur | 0 | 0 | 11 | 0 | 11 |
| **Total** | **49** | **86** | **11** | **2** | **148** |

- **Jest** combine les composants et services Angular dans 47 tests d’intégration (68,12 %). Les appels HTTP y reçoivent des réponses simulées. Les 22 tests unitaires incluent le test de l’utilitaire de configuration de `TestBed`.
- **JUnit** exécute 34 tests avec un contexte Spring et une base H2 en mémoire, et 5 avec MySQL, soit 39 intégrations (57,35 %). Les deux autres contrôles portent sur l’architecture avec ArchUnit et sur le gestionnaire d’exceptions avec un contrôleur de test et MockMvc.
- **Cypress** exécute 10 parcours d’écran avec des réponses API simulées. Le onzième, **E-R01**, relie le navigateur Angular au serveur Spring et à MySQL pour tester la connexion, la consultation d’une séance et du compte, puis la déconnexion.

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

La grille d’évaluation demande au moins 30 % de tests d’intégration dans chacune
des suites Jest et JUnit. Les README frontend et backend détaillent les scénarios.

Pour inclure **E-R01** dans le rapport Cypress, démarrer Spring avec MySQL
et vérifier qu’un compte administrateur et au moins une séance associée à un
enseignant existent. Avec Spring sur le port 8080, lancer **depuis `frontend/`** :

```bash
# Premier terminal
npm run e2e:serve -- --proxy-config proxy.conf.json

# Deuxième terminal, après la compilation Angular
CYPRESS_realApi=true npm run e2e:ci
```

Le rapport Cypress de `v1.0.0` couvre **11 tests**, dont E-R01 dans `real-api.cy.ts`.
La commande sans `CYPRESS_realApi=true` exécute les 10 parcours avec API simulée.
Si Spring utilise un autre port, adapter la cible du proxy en suivant le
[README frontend](frontend/README.md#contrôle-complémentaire-avec-springmysql-réels).

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
