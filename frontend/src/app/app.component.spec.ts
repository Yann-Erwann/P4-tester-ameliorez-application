import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterTestingModule } from '@angular/router/testing';
import { expect } from '@jest/globals';

import { AppComponent } from './app.component';
import { SessionService } from './core/service/session.service';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';


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
});
