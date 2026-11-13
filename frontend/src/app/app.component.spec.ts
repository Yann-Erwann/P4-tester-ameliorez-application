import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterTestingModule } from '@angular/router/testing';
import { expect } from '@jest/globals';

import { AppComponent } from './app.component';
import { SessionService } from './core/service/session.service';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { NotFoundComponent } from './pages/not-found/not-found.component';


describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        AppComponent,
        RouterTestingModule,
        HttpClientTestingModule,
        MatToolbarModule
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('switches navigation between logged-out and logged-in states and logs out', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const element: HTMLElement = fixture.nativeElement;
    const service = TestBed.inject(SessionService);
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    expect(element.textContent).toContain('Login');
    expect(element.textContent).toContain('Register');
    expect(element.textContent).not.toContain('Logout');

    service.logIn({ id: 1, admin: false, token: 'test', type: 'Bearer', username: 'test@example.invalid', firstName: 'Test', lastName: 'Yoga' });
    fixture.detectChanges();
    expect(element.textContent).toContain('Sessions');
    expect(element.textContent).toContain('Account');
    expect(element.textContent).not.toContain('Register');
    Array.from(element.querySelectorAll<HTMLElement>('.link')).find(link => link.textContent === 'Logout')!.click();
    fixture.detectChanges();
    expect(service.isLogged).toBe(false);
    expect(element.textContent).toContain('Login');
    expect(navigate).toHaveBeenCalledWith(['']);
  });

  it('releases the async session-state subscription on destruction', () => {
    const unsubscribe = jest.fn();
    jest.spyOn(TestBed.inject(SessionService), '$isLogged').mockReturnValue(new Observable<boolean>(subscriber => {
      subscriber.next(false);
      return unsubscribe;
    }));
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    fixture.destroy();
    expect(unsubscribe).toHaveBeenCalledTimes(1);
  });

  it('marks only the current guest page as active and updates it on navigation', async () => {
    const router = TestBed.inject(Router);
    router.resetConfig([
      { path: 'login', component: NotFoundComponent },
      { path: 'register', component: NotFoundComponent },
    ]);
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    await router.navigateByUrl('/login');
    await fixture.whenStable();
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    const login = element.querySelector<HTMLAnchorElement>('[routerLink="/login"]')!;
    const register = element.querySelector<HTMLAnchorElement>('[routerLink="/register"]')!;
    expect(login.classList.contains('active')).toBe(true);
    expect(login.getAttribute('aria-current')).toBe('page');
    expect(register.classList.contains('active')).toBe(false);
    expect(register.hasAttribute('aria-current')).toBe(false);

    register.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(router.url).toBe('/register');
    expect(register.classList.contains('active')).toBe(true);
    expect(register.getAttribute('aria-current')).toBe('page');
    expect(login.classList.contains('active')).toBe(false);
    expect(login.hasAttribute('aria-current')).toBe(false);
  });

  it('marks Sessions active on a nested session route and switches to Account', async () => {
    const router = TestBed.inject(Router);
    router.resetConfig([
      { path: 'sessions/detail/:id', component: NotFoundComponent },
      { path: 'me', component: NotFoundComponent },
    ]);
    TestBed.inject(SessionService).logIn({ id: 1, admin: false, token: 'test', type: 'Bearer', username: 'test@example.invalid', firstName: 'Test', lastName: 'Yoga' });
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    await router.navigateByUrl('/sessions/detail/1');
    await fixture.whenStable();
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    const sessions = element.querySelector<HTMLElement>('[routerLink="sessions"]')!;
    const account = element.querySelector<HTMLElement>('[routerLink="me"]')!;
    expect(sessions.classList.contains('active')).toBe(true);
    expect(sessions.getAttribute('aria-current')).toBe('page');
    expect(account.classList.contains('active')).toBe(false);

    account.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(router.url).toBe('/me');
    expect(account.classList.contains('active')).toBe(true);
    expect(account.getAttribute('aria-current')).toBe('page');
    expect(sessions.classList.contains('active')).toBe(false);
    expect(sessions.hasAttribute('aria-current')).toBe(false);
  });
});
