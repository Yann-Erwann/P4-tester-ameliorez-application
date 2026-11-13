import { Type } from '@angular/core';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { MeComponent } from './components/me/me.component';
import { SessionService } from './core/service/session.service';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { DetailComponent } from './pages/sessions/components/detail/detail.component';
import { FormComponent } from './pages/sessions/components/form/form.component';

describe('[unit] J-U02 Component request lifetime', () => {
  let http: HttpTestingController;
  const navigate = jest.fn().mockResolvedValue(true);
  const open = jest.fn();
  const session = {
    id: 1, name: 'Yoga', description: 'Practice', date: '2026-12-01',
    teacher_id: 7, users: [1],
  };

  beforeEach(() => {
    navigate.mockClear();
    open.mockClear();
  });

  async function createComponent<T>(component: Type<T>, update = false): Promise<ComponentFixture<T>> {
    await TestBed.configureTestingModule({
      imports: [component, HttpClientTestingModule],
      providers: [
        { provide: Router, useValue: { url: update ? '/sessions/update/1' : '/sessions/create', navigate } },
        { provide: MatSnackBar, useValue: { open } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } } },
      ],
    }).overrideComponent(component, { set: { template: '' } }).compileComponents();
    TestBed.inject(SessionService).logIn({
      id: 1, admin: true, token: 'test-token', type: 'Bearer',
      username: 'test@example.invalid', firstName: 'Test', lastName: 'Yoga',
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(component);
    fixture.detectChanges();
    return fixture;
  }

  function expectCancelled(fixture: { destroy(): void }, url: string): void {
    const request = http.expectOne(url);
    fixture.destroy();
    expect(request.cancelled).toBe(true);
    expect(() => request.flush({})).toThrow();
    expect(navigate).not.toHaveBeenCalled();
    expect(open).not.toHaveBeenCalled();
  }

  afterEach(() => http.verify());

  it('cancels login on departure without logging in or navigating', async () => {
    const fixture = await createComponent(LoginComponent);
    TestBed.inject(SessionService).logOut();
    fixture.componentInstance.submit();
    expectCancelled(fixture, '/api/auth/login');
    expect(TestBed.inject(SessionService).isLogged).toBe(false);
    expect(fixture.componentInstance.onError).toBe(false);
  });

  it('cancels registration on departure without navigating', async () => {
    const fixture = await createComponent(RegisterComponent);
    fixture.componentInstance.submit();
    expectCancelled(fixture, '/api/auth/register');
    expect(fixture.componentInstance.onError).toBe(false);
  });

  it('cancels account loading on departure', async () => {
    const fixture = await createComponent(MeComponent);
    expectCancelled(fixture, 'api/user/1');
    expect(fixture.componentInstance.user).toBeUndefined();
  });

  it('cancels account deletion on departure without logging out or notifying', async () => {
    const fixture = await createComponent(MeComponent);
    http.expectOne('api/user/1').flush({ id: 1 });
    fixture.componentInstance.delete();
    expectCancelled(fixture, 'api/user/1');
    expect(TestBed.inject(SessionService).isLogged).toBe(true);
  });

  it('cancels session loading without starting a teacher lookup', async () => {
    const fixture = await createComponent(DetailComponent);
    expectCancelled(fixture, 'api/session/1');
    expect(fixture.componentInstance.session).toBeUndefined();
    http.expectNone('api/teacher/7');
  });

  it('cancels a teacher lookup started after the session response', async () => {
    const fixture = await createComponent(DetailComponent);
    http.expectOne('api/session/1').flush(session);
    expectCancelled(fixture, 'api/teacher/7');
    expect(fixture.componentInstance.teacher).toBeUndefined();
  });

  it.each(['delete', 'participate', 'unParticipate'] as const)(
    'cancels session action %s without notification, navigation or refresh', async (action) => {
      const fixture = await createComponent(DetailComponent);
      http.expectOne('api/session/1').flush(session);
      http.expectOne('api/teacher/7').flush({ id: 7 });
      fixture.componentInstance[action]();
      expectCancelled(fixture, action === 'delete' ? 'api/session/1' : 'api/session/1/participate/1');
      http.expectNone('api/session/1');
    },
  );

  it('cancels form loading without initializing the edit form', async () => {
    const fixture = await createComponent(FormComponent, true);
    expectCancelled(fixture, 'api/session/1');
    expect(fixture.componentInstance.sessionForm).toBeUndefined();
  });

  it.each([false, true])('cancels form saving on departure (update: %s)', async (update) => {
    const fixture = await createComponent(FormComponent, update);
    if (update) {
      http.expectOne('api/session/1').flush(session);
    }
    fixture.componentInstance.submit();
    expectCancelled(fixture, update ? 'api/session/1' : 'api/session');
  });
});
