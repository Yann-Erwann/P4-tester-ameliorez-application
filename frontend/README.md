# Yoga

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 19.2.16.

## Start the project

Git clone:

> git clone https://github.com/OpenClassrooms-Student-Center/P5-Full-Stack-testing

Go inside folder:

> cd yoga

Install dependencies:

> npm install

Launch Front-end:

> npm run start;


### Test

#### E2E

Launching e2e test:

> npm run e2e

Generate coverage report (you should launch e2e test before):

> npm run e2e:coverage

Report is available here:

> front/coverage/lcov-report/index.html

#### Unitary test

Launching test:

> npm run test

for following change:

> npm run test:watch

### Vérifications avant les améliorations

Depuis `frontend/`, exécuter les tests unitaires sans surveillance :

```bash
npm test -- --ci --runInBand
```

Jest utilise `jest.config.js` et l’environnement Angular initialisé dans
`setup-jest.ts`. Les composants standalone sont importés dans les tests ; les
requêtes sont simulées par les outils HTTP de test Angular. Ces tests ne
nécessitent pas le backend ou MySQL et ne remplacent pas les parcours réels.
