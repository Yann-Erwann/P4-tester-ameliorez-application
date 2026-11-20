import { defineConfig } from 'cypress';
import { rmSync } from 'node:fs';
import { resolve } from 'node:path';

const ciScreenshots = 'coverage/e2e/screenshots';

export default defineConfig({
  video: false,
  screenshotsFolder: ciScreenshots,
  reporter: 'junit',
  e2e: {
    baseUrl: 'http://127.0.0.1:4203',
    supportFile: 'cypress/support/e2e.ts',
    setupNodeEvents(on: Cypress.PluginEvents, config: Cypress.PluginConfigOptions): Cypress.PluginConfigOptions {
      if (!config.env['realApi']) {
        config.excludeSpecPattern = [config.excludeSpecPattern].flat().concat('cypress/e2e/real-api.cy.ts');
      }

      // The coverage plugin reads NYC configuration when its module is loaded.
      process.env['YOGA_CYPRESS_MODE'] = config.isTextTerminal ? 'run' : 'open';
      const nyc: { 'report-dir': string; 'temp-dir': string } = require('./nyc.config.cjs');
      const reportDir = resolve(config.projectRoot, nyc['report-dir']);
      if (!config.isTextTerminal && config.screenshotsFolder &&
          resolve(config.projectRoot, config.screenshotsFolder) === resolve(config.projectRoot, ciScreenshots)) {
        config.screenshotsFolder = resolve(reportDir, 'screenshots');
      }
      config.reporterOptions = { mochaFile: resolve(reportDir, 'results/results-[hash].xml'), ...config.reporterOptions };
      if (config.isTextTerminal) {
        rmSync(resolve(config.projectRoot, nyc['temp-dir']), { recursive: true, force: true });
        rmSync(resolve(reportDir, 'results'), { recursive: true, force: true });
      }
      const registerCodeCoverageTasks = require('@cypress/code-coverage/task') as typeof import('@cypress/code-coverage/task').default;
      registerCodeCoverageTasks(on, config);
      return config;
    },
  },
});
