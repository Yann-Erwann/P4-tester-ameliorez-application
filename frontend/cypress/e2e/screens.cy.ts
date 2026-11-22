/// <reference types="cypress" />

const member = { id: 7, username: 'member@example.invalid', firstName: 'Test', lastName: 'Member',
  admin: false, token: 'synthetic-token', type: 'Bearer' };
const teacher = { id: 3, firstName: 'Test', lastName: 'Teacher' };
const initial = { id: 1, name: 'Morning yoga', date: '2026-12-01', teacher_id: 3,
  description: 'Practice together', users: [] as number[], createdAt: '2026-11-01', updatedAt: '2026-11-01' };

// Browser flows use the real Angular router, forms, services and interceptor.
// Only the HTTP boundary is simulated; real Spring/MySQL smoke tests are separate.
describe('E-S01 screen journeys with controlled API responses', () => {
  let sessions: typeof initial[];
  let admin = false;
  beforeEach(() => {
    const session: typeof initial = { ...initial, users: [] };
    const participationPath = `/api/session/${session.id}/participate/${member.id}`;
    sessions = [session]; admin = false;
    cy.intercept('POST', '**/api/auth/login', request => request.reply({ ...member, admin })).as('login');
    cy.intercept('POST', '**/api/auth/register', { message: 'User registered successfully!' }).as('register');
    cy.intercept('GET', '**/api/teacher', [teacher]).as('teachers');
    cy.intercept('GET', '**/api/teacher/*', teacher).as('teacher');
    cy.intercept({ method: 'GET', pathname: '/api/session', query: { view: 'summary' } }, request => {
      request.reply(sessions.map(({ id, name, date, description }) => ({ id, name, date, description })));
    }).as('sessions');
    cy.intercept('GET', /\/api\/session\/\d+$/, request => request.reply(sessions.find(s => s.id === Number(request.url.split('/').pop())) || {})).as('detail');
    cy.intercept('GET', '**/api/user/*', request => request.reply({ ...member, email: member.username, admin })).as('account');
    cy.intercept('DELETE', '**/api/user/*', { statusCode: 200, body: '' }).as('deleteAccount');
    cy.intercept('POST', '**/api/session', request => {
      const created = { ...initial, ...request.body, id: 2, users: [] };
      sessions.push(created); request.reply(created);
    }).as('create');
    cy.intercept('PUT', /\/api\/session\/\d+$/, request => {
      const session = sessions.find(s => s.id === Number(request.url.split('/').pop()))!;
      Object.assign(session, request.body); request.reply(session);
    }).as('update');
    cy.intercept('DELETE', /\/api\/session\/\d+$/, request => {
      sessions = sessions.filter(s => s.id !== Number(request.url.split('/').pop())); request.reply({ statusCode: 200, body: '' });
    }).as('deleteSession');
    cy.intercept('POST', '**/api/session/*/participate/*', request => {
      expect(new URL(request.url).pathname).to.equal(participationPath);
      session.users.push(member.id);
      request.reply({ statusCode: 200, body: '' });
    }).as('participate');
    cy.intercept('DELETE', '**/api/session/*/participate/*', request => {
      expect(new URL(request.url).pathname).to.equal(participationPath);
      session.users = session.users.filter(userId => userId !== member.id);
      request.reply({ statusCode: 200, body: '' });
    }).as('unparticipate');
  });

  function login(asAdmin = false): void {
    admin = asAdmin;
    cy.visit('/login');
    cy.get('[formControlName=email]').type(member.username);
    cy.get('[formControlName=password]').type('Secret!123');
    cy.contains('button', 'Submit').click();
    cy.wait('@login'); cy.wait('@sessions');
    cy.location('pathname').should('eq', '/sessions');
  }

  it('E-01 registers valid details then returns to login', () => {
    cy.visit('/register'); cy.contains('button', 'Submit').should('be.disabled');
    cy.get('[formControlName=firstName]').type('First'); cy.get('[formControlName=lastName]').type('Last');
    cy.get('[formControlName=email]').type('new@example.invalid'); cy.get('[formControlName=password]').type('Secret!123');
    cy.contains('button', 'Submit').click();
    cy.wait('@register').its('request.body').should('include', { firstName: 'First', lastName: 'Last' });
    cy.location('pathname').should('eq', '/login');
  });

  it('E-02 displays the existing duplicate-registration error', () => {
    cy.intercept('POST', '**/api/auth/register', { statusCode: 400 }).as('duplicate');
    cy.visit('/register');
    for (const [key, value] of Object.entries({ firstName: 'First', lastName: 'Last', email: member.username, password: 'Secret!123' }))
      cy.get(`[formControlName=${key}]`).type(value);
    cy.contains('button', 'Submit').click(); cy.wait('@duplicate'); cy.get('.error').should('be.visible');
  });

  it('E-03 refuses incorrect credentials and toggles password visibility', () => {
    cy.intercept('POST', '**/api/auth/login', { statusCode: 401 }).as('refused');
    cy.visit('/login'); cy.contains('button', 'Submit').should('be.disabled');
    cy.get('[formControlName=email]').type(member.username); cy.get('[formControlName=password]').type('Incorrect!123');
    cy.get('button[mat-icon-button]').click(); cy.get('[formControlName=password]').should('have.attr', 'type', 'text');
    cy.get('button[mat-icon-button]').click(); cy.get('[formControlName=password]').should('have.attr', 'type', 'password');
    cy.contains('button', 'Submit').click(); cy.wait('@refused'); cy.get('.error').should('be.visible');
  });

  it('E-04 guards a private route and displays the not-found screen', () => {
    cy.visit('/me'); cy.location('pathname').should('eq', '/login');
    cy.visit('/unknown-evaluation-route'); cy.contains('Page not found !').should('be.visible');
  });

  it('E-05 opens a session, participates and withdraws as a member', () => {
    login(); cy.contains('button', 'Create').should('not.exist'); cy.contains('button', 'Edit').should('not.exist');
    cy.contains('button', 'Detail').click(); cy.wait('@detail'); cy.wait('@teacher');
    cy.contains('Test TEACHER').should('be.visible'); cy.contains('0 attendees').should('be.visible');
    cy.contains('button', 'Participate').click(); cy.wait('@participate'); cy.wait('@detail');
    cy.contains('1 attendees').should('be.visible'); cy.contains('button', 'Do not participate').click(); cy.wait('@unparticipate');
    cy.contains('0 attendees').should('be.visible'); cy.get('button[mat-icon-button]').click();
    cy.location('pathname').should('eq', '/sessions');
  });

  it('E-06 shows an empty session list and logs out', () => {
    sessions = []; login(); cy.get('.item').should('not.exist'); cy.contains('Logout').click();
    cy.location('pathname').should('eq', '/login'); cy.contains('Register').should('be.visible');
  });

  it('E-07 creates, updates and deletes a session as administrator', () => {
    login(true); cy.contains('button', 'Create').click(); cy.wait('@teachers');
    cy.contains('button', 'Save').should('be.disabled');
    cy.get('[formControlName=name]').type('Evening yoga'); cy.get('[formControlName=date]').type('2026-12-02');
    cy.get('mat-select').click(); cy.contains('mat-option', 'Test Teacher').click();
    cy.get('[formControlName=description]').type('Evening practice'); cy.contains('button', 'Save').click(); cy.wait('@create');
    cy.get('.item').filter(':contains("Evening yoga")').contains('button', 'Edit').click(); cy.wait('@detail'); cy.wait('@teachers');
    cy.get('[formControlName=name]').clear().type('Updated yoga'); cy.contains('button', 'Save').click(); cy.wait('@update');
    cy.get('.item').filter(':contains("Updated yoga")').contains('button', 'Detail').click(); cy.wait('@detail');
    cy.contains('button', 'Delete').click(); cy.wait('@deleteSession'); cy.location('pathname').should('eq', '/sessions');
    cy.contains('.item', 'Updated yoga').should('not.exist');
  });

  it('E-08 returns from the create form without saving', () => {
    login(true); cy.contains('button', 'Create').click(); cy.wait('@teachers');
    cy.get('[formControlName=name]').type('Unsaved value'); cy.get('button[mat-icon-button]').click();
    cy.location('pathname').should('eq', '/sessions'); cy.contains('.item', 'Unsaved value').should('not.exist');
  });

  it('E-09 displays the administrator account and its return navigation', () => {
    login(true); cy.contains('Account').click(); cy.wait('@account'); cy.contains('You are admin').should('be.visible');
    cy.contains('Delete my account:').should('not.exist'); cy.get('button[mat-icon-button]').click();
    cy.location('pathname').should('eq', '/sessions');
  });

  it('E-10 deletes the current member account then returns to login', () => {
    login(); cy.contains('Account').click(); cy.wait('@account'); cy.contains('Delete my account:').should('be.visible');
    cy.get('button[color=warn]').click(); cy.wait('@deleteAccount'); cy.location('pathname').should('eq', '/login');
  });
});
