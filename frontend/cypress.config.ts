import { defineConfig } from 'cypress';
import { rmSync } from 'node:fs';
import { resolve } from 'node:path';

export default defineConfig({
  video: false,
  screenshotsFolder: 'coverage/e2e/screenshots',
  reporter: 'junit',
  reporterOptions: { mochaFile: 'coverage/e2e/results/results-[hash].xml', toConsole: false },
  e2e: {
    baseUrl: 'http://127.0.0.1:4203',
    supportFile: 'cypress/support/e2e.ts',
    setupNodeEvents(on: Cypress.PluginEvents, config: Cypress.PluginConfigOptions): Cypress.PluginConfigOptions {
      if (!config.env['realApi']) {
        config.excludeSpecPattern = [config.excludeSpecPattern].flat().concat('cypress/e2e/real-api.cy.ts');
      }
      // Never combine a new run with old coverage or failed JUnit artifacts.
      for (const directory of ['coverage/e2e/.nyc_output', 'coverage/e2e/results']) {
        rmSync(resolve(config.projectRoot, directory), { recursive: true, force: true });
      }
      const registerCodeCoverageTasks = require('@cypress/code-coverage/task') as typeof import('@cypress/code-coverage/task').default;
      registerCodeCoverageTasks(on, config);
      return config;
    },
  },
});
