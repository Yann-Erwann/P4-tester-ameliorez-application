/// <reference types="cypress" />

// No cy.intercept: this smoke test crosses the configured Angular proxy,
// Spring authentication/JWT, controllers/services, and the existing MySQL data.
describe('E-R01 real Spring/MySQL integration', () => {
  it('logs in, reads sessions/teachers and account, then logs out without changing data', () => {
    const email = String(Cypress.env('adminEmail') || 'yoga@studio.com');
    const password = String(Cypress.env('adminPassword') || 'test!1234');
    cy.visit('/login');
    cy.get('[formControlName=email]').type(email);
    cy.get('[formControlName=password]').type(password, { log: false });
    cy.contains('button', 'Submit').click();
    cy.location('pathname').should('eq', '/sessions');
    cy.contains('button', 'Create').should('be.visible');
    cy.get('.item').first().contains('button', 'Detail').click();
    cy.get('.description').should('be.visible');
    cy.get('mat-card-subtitle').should('be.visible');
    cy.get('button[mat-icon-button]').click();
    cy.location('pathname').should('eq', '/sessions');
    cy.contains('Account').click(); cy.contains(email).should('be.visible');
    cy.contains('You are admin').should('be.visible');
    cy.contains('Logout').click(); cy.location('pathname').should('eq', '/login');
  });
});
