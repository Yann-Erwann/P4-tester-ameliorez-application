const reportDir = process.env.YOGA_CYPRESS_MODE === 'open' ? 'coverage/e2e-open' : 'coverage/e2e';

module.exports = {
  extends: '@istanbuljs/nyc-config-typescript',
  all: true,
  include: ['src/app/**/*.ts'],
  exclude: ['**/*.spec.ts'],
  'temp-dir': reportDir + '/.nyc_output',
  'report-dir': reportDir,
  reporter: ['text', 'html', 'lcov', 'json', 'json-summary'],
  'check-coverage': true,
  statements: 80,
  branches: 80,
  functions: 80,
  lines: 80,
};
