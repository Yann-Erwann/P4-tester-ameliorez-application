import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { expect } from '@jest/globals';
import { Router } from '@angular/router';

import { RegisterComponent } from './register.component';

describe('[integration] J-I02 RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RegisterComponent,
        BrowserAnimationsModule,
        HttpClientTestingModule,
        ReactiveFormsModule,
        MatCardModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.error')).toBeNull();
  });

  it.each([
    ['firstName', 3, 20],
    ['lastName', 3, 20],
    ['password', 6, 40],
  ] as const)('checks the length of %s before sending a registration', (field, min, max) => {
    component.form.setValue({ email: 'yoga@example.invalid', firstName: 'Alice', lastName: 'Jones', password: 'Secret!123' });
    const input: HTMLInputElement = fixture.nativeElement.querySelector(`[formControlName="${field}"]`);
    const submit: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    for (const [value, valid] of [
      ['', false], [' '.repeat(min), false], ['a'.repeat(min - 1), false],
      ['9'.repeat(min), true], ['a'.repeat(max), true], ['a'.repeat(max + 1), false],
    ] as const) {
      input.value = value;
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      expect(submit.disabled).toBe(!valid);
      if (!valid) {
        fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
        http.expectNone('/api/auth/register');
      }
    }
  });

  it('submits registration and opens login on success', () => {
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const registration = { email: 'test@example.invalid', firstName: 'Test', lastName: 'Yoga', password: 'Secret!123' };
    const passwordInput: HTMLInputElement = fixture.nativeElement.querySelector('[formControlName="password"]');
    expect(passwordInput.type).toBe('password');
    component.form.setValue(registration);
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    const request = http.expectOne('/api/auth/register');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(registration);
    request.flush(null);
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('shows the error branch when registration fails', () => {
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    component.form.setValue({ email: 'taken@example.invalid', firstName: 'Test', lastName: 'Yoga', password: 'Secret!123' });
    component.submit();
    http.expectOne('/api/auth/register').flush({}, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.error')?.textContent).toContain('An error occurred');
    expect(navigate).not.toHaveBeenCalled();
  });
});
