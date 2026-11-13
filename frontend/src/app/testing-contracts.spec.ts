import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { appConfig } from './app.config';
import { SessionService } from './core/service/session.service';
import { AuthService } from './core/service/auth.service';
import { SessionApiService } from './core/service/session-api.service';
import { UserService } from './core/service/user.service';
import { TeacherService } from './core/service/teacher.service';
import { AuthGuard } from './guards/auth.guard';
import { UnauthGuard } from './guards/unauth.guard';
import { Observable } from 'rxjs';
import { SessionInformation } from './core/models/sessionInformation.interface';
import { Session } from './core/models/session.interface';

const identity: SessionInformation = {
  id: 7, username: 'member@example.invalid', firstName: 'Test', lastName: 'Member',
  admin: false, token: 'synthetic-token', type: 'Bearer',
};

describe('[unit] J-U01 session state and route decisions', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter([])] }));

  it('publishes initial, connected and disconnected state and clears identity', () => {
    const service = TestBed.inject(SessionService);
    const states: boolean[] = [];
    const subscription = service.$isLogged().subscribe(state => states.push(state));
    service.logIn(identity);
    expect(service.sessionInformation).toEqual(identity);
    service.logOut();
    expect(states).toEqual([false, true, false]);
    expect(service.sessionInformation).toBeUndefined();
    subscription.unsubscribe();
  });

  it.each([false, true])('protects private routes (connected: %s)', connected => {
    const router = TestBed.inject(Router);
    const navigate = jest.spyOn(router, 'navigate').mockResolvedValue(true);
    if (connected) TestBed.inject(SessionService).logIn(identity);
    expect(TestBed.inject(AuthGuard).canActivate()).toBe(connected);
    if (connected) expect(navigate).not.toHaveBeenCalled();
    else expect(navigate).toHaveBeenCalledWith(['login']);
  });

  it.each([false, true])('protects guest routes with the existing redirect (connected: %s)', connected => {
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    if (connected) TestBed.inject(SessionService).logIn(identity);
    expect(TestBed.inject(UnauthGuard).canActivate()).toBe(!connected);
    if (connected) expect(navigate).toHaveBeenCalledWith(['rentals']);
    else expect(navigate).not.toHaveBeenCalled();
  });
});

describe('[integration] J-I01 services, HttpClient and application JWT interceptor', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [...appConfig.providers, provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it.each([false, true])('sends the current token only when connected (connected: %s)', connected => {
    if (connected) TestBed.inject(SessionService).logIn(identity);
    TestBed.inject(HttpClient).get('/probe').subscribe();
    const request = http.expectOne('/probe');
    expect(request.request.headers.get('Authorization')).toBe(connected ? 'Bearer synthetic-token' : null);
    request.flush({});
  });

  it('posts registration fields and login credentials without inventing a role', () => {
    const auth = TestBed.inject(AuthService);
    const registration = { email: identity.username, firstName: 'Test', lastName: 'Member', password: 'Secret!123' };
    auth.register(registration).subscribe();
    const register = http.expectOne('/api/auth/register');
    expect(register.request.method).toBe('POST');
    expect(register.request.body).toEqual(registration);
    register.flush(null);
    auth.login({ email: registration.email, password: registration.password }).subscribe(response => expect(response).toEqual(identity));
    const login = http.expectOne('/api/auth/login');
    expect(login.request.body).toEqual({ email: registration.email, password: registration.password });
    login.flush(identity);
  });

  it.each([0, 1, 2, 3, 4, 5, 6])('J-I03 preserves session contract case %s', index => {
    const service = TestBed.inject(SessionApiService);
    const session: Session = { id: 1, name: 'Practice', date: new Date('2026-12-01'), teacher_id: 7,
      description: 'Yoga', users: [], createdAt: new Date(), updatedAt: new Date() };
    const operations: { call: () => Observable<unknown>; method: string; path: string; body: Session | Session[] | null }[] = [
      { call: () => service.all(), method: 'GET', path: 'api/session', body: [session] },
      { call: () => service.detail('1'), method: 'GET', path: 'api/session/1', body: session },
      { call: () => service.create(session), method: 'POST', path: 'api/session', body: session },
      { call: () => service.update('1', session), method: 'PUT', path: 'api/session/1', body: session },
      { call: () => service.delete('1'), method: 'DELETE', path: 'api/session/1', body: null },
      { call: () => service.participate('1', '7'), method: 'POST', path: 'api/session/1/participate/7', body: null },
      { call: () => service.unParticipate('1', '7'), method: 'DELETE', path: 'api/session/1/participate/7', body: null },
    ];
    {
      const operation = operations[index];
      operation.call().subscribe(response => expect(response).toEqual(operation.body));
      const request = http.expectOne(operation.path);
      expect(request.request.method).toBe(operation.method);
      if (['POST', 'PUT'].includes(operation.method)) expect(request.request.body).toEqual(operation.path.includes('participate') ? null : session);
      request.flush(operation.body);
    }
  });

  it('reads teachers and accounts and deletes an account using their existing contracts', () => {
    const teacher = TestBed.inject(TeacherService);
    teacher.all().subscribe(response => expect(response).toEqual([]));
    http.expectOne('api/teacher').flush([]);
    teacher.detail('7').subscribe();
    http.expectOne('api/teacher/7').flush({ id: 7 });
    const user = TestBed.inject(UserService);
    user.getById('7').subscribe();
    http.expectOne('api/user/7').flush({ id: 7 });
    user.delete('7').subscribe();
    const deletion = http.expectOne('api/user/7');
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
  });

  it.each([401, 403, 404, 409, 500])('propagates HTTP %s instead of reporting a success', status => {
    const success = jest.fn();
    const failure = jest.fn();
    TestBed.inject(SessionApiService).all().subscribe({ next: success, error: failure });
    http.expectOne('api/session').flush({}, { status, statusText: 'Failure' });
    expect(success).not.toHaveBeenCalled();
    expect(failure).toHaveBeenCalledWith(expect.objectContaining({ status }));
  });
});
