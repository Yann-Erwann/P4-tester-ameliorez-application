# Contribuer au projet

Ces conventions encadrent les contributions au dépôt. Vérifier le socle demandé
et l’état réel dans les manifestes, configurations et README concernés avant
de choisir une commande ou une API. Yoga utilise Angular 19 et Spring Boot 3 ;
le sujet demande Java 21 et Maven 3.9.3, sélectionnés avec `mise`.
Ces conventions n’imposent aucun hook ni réécriture de l’historique.

## Périmètre et exigences

Le frontend fourni et son lockfile ne doivent pas être modifiés sans autorisation
explicite. La même protection s’applique au mock fourni, y compris au chemin
historique `frontend/ressources/mockoon/rental-oc.json` s’il est présent. Sa lecture
reste autorisée ; ne pas changer son comportement par un contournement.

Conserver les données, volumes et fichiers d’environnement existants. Ne pas
ajouter de scripts au dépôt ni recréer de dossier `scripts/` : documenter les
commandes dans les README concernés. Ne pas suivre les secrets ou les artefacts
générés, tels que les répertoires de compilation, couverture ou dépendances.

Pour une exigence nouvelle ou modifiée, consigner le critère d’acceptation et
son mode de vérification dans la documentation suivie ou dans la PR. Distinguer
demandé, implémenté, testé et restant à faire. Relier les résultats automatiques
et les vérifications humaines aux exigences de la contribution.

Une CI vérifie ses seuls contrôles programmés, pas toutes les instructions en
langage naturel.

## Commits

Utiliser uniquement un titre en anglais, sans corps, sous la forme :

```text
type(scope): describe the resulting change
```

| Type | Usage |
| --- | --- |
| `feat` | Nouvelle fonctionnalité. |
| `fix` | Correction d’un défaut. |
| `refactor` | Restructuration préservant le comportement attendu. |
| `perf` | Amélioration dont le bénéfice est mesuré. |
| `test` | Infrastructure de tests ou couverture transversale. |
| `ci` | Vérifications et publication automatisées. |
| `docs` | Documentation. |
| `build` | Dépendances et configuration de construction. |
| `style` | Présentation du code sans changement fonctionnel. |
| `chore` | Maintenance ne relevant pas des types précédents. |

Le scope désigne une responsabilité : par exemple `project`, `git`, `testing`,
`http`, `security`, `database`, `docker`, ou une fonctionnalité réelle du dépôt.
Un scope supplémentaire doit être compréhensible et expliqué dans la PR lorsque
son sens n’est pas évident. Ne pas importer les domaines métier d’un autre projet.

```text
fix(security): reject an invalid authentication token
test(database): verify transaction rollbacks
docs(project): document local startup requirements
```

Les commits utilisent uniquement l’identité Git actuelle de l’utilisateur,
`Yann-Erwann Le Calvez <flashhy@hotmail.com>`, pour l’auteur et le committer.
Ne pas ajouter de co-auteur, de signature d’agent ou d’identité de bot. Vérifier
l’identité effective avant chaque commit et signaler tout écart avant création.

## Atomicité et validation avant chaque commit

Définir le découpage avant de préparer l’index. Un commit couvre une responsabilité
cohérente et laisse le projet compilable. Conserver ensemble l’implémentation,
les tests pertinents et les adaptations indispensables ; respecter les dépendances.
Séparer nettoyage, typage, migration de dépendances et corrections fonctionnelles
indépendantes. Un découpage global par dossier ou langage ne suffit pas.

Avant chaque commit demandé :

1. Examiner `git status --short`, le diff et les fichiers non suivis retenus.
   Respecter le périmètre demandé et signaler les dépendances nécessaires
   laissées hors commit.
2. Exécuter les contrôles adaptés à la responsabilité et au risque. Vérifier les
   commandes dans les manifestes actuels, puis relever résultats et limites.
3. Indexer uniquement les fichiers ou hunks concernés. Examiner le diff réellement
   indexé avec `git diff --cached` et `git diff --cached --check` ; vérifier le
   périmètre, les secrets et les fichiers générés.
4. Vérifier l’auteur et le committer :

   ```bash
   git var GIT_AUTHOR_IDENT
   git var GIT_COMMITTER_IDENT
   ```

5. Créer le commit avec un titre conforme, sans corps ni co-auteur.

Ne pas prétendre rendre atomique un historique déjà créé par une série de
correctifs. Aucun amend, rebase ou force-push sans instruction explicite.

## Commandes de vérification

Privilégier les wrappers lorsqu’ils existent. Ce starter n’a actuellement pas
de Maven Wrapper ; utiliser les versions du sujet avec `mise`, comme décrit
dans le [README backend](backend/README.md).

Depuis `backend/` :

```bash
mise exec java@21 maven@3.9.3 -- mvn -B verify
```

Après suppression ou déplacement de classes, employer `clean verify` pour écarter
les anciennes classes. Le backend Yoga ne contient actuellement aucun test
Java automatisé : une réussite de Maven prouve la compilation et la construction
du JAR, sans valider les accès SQL, les autorisations ou les parcours utilisateur.

`ARCH-001` impose controller → service → repository. Vérifier si un test
l’automatise avant d’annoncer sa couverture ; aucun `ArchitectureTests` n’est
actuellement fourni dans ce starter. Ne pas désactiver un contrôle existant
pour accepter une dépendance non conforme.

Pour Angular, lire `frontend/package.json`, `angular.json` et les configurations
de test. Les scripts déclarés comprennent `start`, `test`, `lint` et les cibles
Cypress ; aucun script npm `build` n’est actuellement déclaré. Vérifier qu’un
script dispose bien du builder et des dépendances nécessaires avant de l’annoncer
fonctionnel. Les tests unitaires sont configurés avec Jest.

Si le frontend reste protégé, exécuter les contrôles qui produisent des fichiers
dans une copie isolée, sans installer de dépendances ni générer d’artefacts dans
`frontend/`. Distinguer compilation, tests avec réponses simulées et parcours
contre le backend réel. Respecter les contraintes et commandes du README
[frontend](frontend/README.md) lorsqu’elles correspondent à la configuration.

Pour Docker/Compose, identifier le fichier et l’environnement utilisés, puis
valider avec `config --quiet` afin de ne pas afficher les secrets résolus.
Préserver le nom du projet et les volumes. Un conteneur démarré ne prouve pas que
l’application est prête ; vérifier le service ou le parcours concerné.

Pour les contrats HTTP, confronter le contrat disponible, les consommateurs et
les réponses du serveur. Ne pas présumer la présence d’OpenAPI, d’un mock ou d’une
interface de documentation. Dans ce dépôt, la collection Postman backend est une
source existante à examiner.

Pour une documentation seule, relire commandes, liens et dossiers d’exécution et
contrôler le diff. Ne pas lancer les suites applicatives sans effet sur leur code.

## Issues, branches, PR et publication

Lorsqu’une issue GitHub existe, employer son vrai numéro :
`<type>/<issue-number>-<short-description>`. Sans issue distante, utiliser
`<type>/<short-description>`. Un identifiant de plan local ne constitue pas un
numéro d’issue GitHub ; ne pas ajouter de référence fictive.

Décrire le problème, le comportement obtenu, le périmètre et les éventuelles
évolutions de contrat. Indiquer les exigences concernées, prérequis, validations
réellement exécutées et critères restant ouverts. `Refs #<numéro>` convient à une
contribution partielle ; `Closes #<numéro>` à celle qui satisfait les critères.
Employer le modèle de PR s’il existe, sans inventer un modèle absent.

Pour un transfert de commit, vérifier ses dépendances sur la cible, les conflits
et les équivalents existants. Un commit atomique peut dépendre d’un autre ; son
transfert isolé n’est pas garanti. Après résolution des conflits, relire le résultat
et exécuter les contrôles adaptés sur la cible.

Avant une publication demandée, vérifier branche cible, état des vérifications
et règles de fusion réellement actives. La présence d’un fichier de workflow ou
d’une configuration de protection dans le dépôt ne démontre pas son activation.
