import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { SessionService } from 'src/app/core/service/session.service';

import { MeComponent } from './me.component';

describe('[integration] J-I02 MeComponent', () => {
  let component: MeComponent;
  let fixture: ComponentFixture<MeComponent>;
  let http: HttpTestingController;

  const mockSessionService = {
    sessionInformation: {
      admin: true,
      id: 1
    }
  }
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MeComponent,
        MatSnackBarModule,
        HttpClientTestingModule,
        MatCardModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule
      ],
      providers: [{ provide: SessionService, useValue: mockSessionService }],
    })
      .compileComponents();

    fixture = TestBed.createComponent(MeComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne('api/user/1').flush({
      id: 1,
      email: 'audit@example.invalid',
      firstName: 'Audit',
      lastName: 'Yoga',
      admin: true,
      createdAt: '2026-11-01T00:00:00',
    });
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Audit YOGA');
  });

  it('hides account information until a user is available', () => {
    component.user = undefined;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Name:');
    expect(fixture.nativeElement.querySelector('button[color="warn"]')).toBeNull();
  });

  it('shows the admin message or the account deletion action according to the user', () => {
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('You are admin');
    expect(element.querySelector('button[color="warn"]')).toBeNull();
    component.user = { ...component.user!, admin: false };
    fixture.detectChanges();
    expect(element.textContent).not.toContain('You are admin');
    expect(element.textContent).toContain('Delete my account:');
    const remove = jest.spyOn(component, 'delete').mockImplementation(() => {});
    element.querySelector<HTMLButtonElement>('button[color="warn"]')!.click();
    expect(remove).toHaveBeenCalledTimes(1);
  });
});
