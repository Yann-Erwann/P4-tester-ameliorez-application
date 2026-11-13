import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterTestingModule, } from '@angular/router/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { expect } from '@jest/globals';
import { SessionService } from '../../../../core/service/session.service';

import { DetailComponent } from './detail.component';


describe('[integration] J-I02 DetailComponent', () => {
  let component: DetailComponent;
  let fixture: ComponentFixture<DetailComponent>;
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
        DetailComponent,
        RouterTestingModule,
        HttpClientTestingModule,
        MatSnackBarModule,
        ReactiveFormsModule
      ],
      providers: [
        { provide: SessionService, useValue: mockSessionService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } }
        }
      ],
    })
      .compileComponents();
    fixture = TestBed.createComponent(DetailComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne('api/session/1').flush({
      id: 1,
      name: 'Baseline yoga',
      description: 'Baseline session',
      date: '2026-12-01',
      teacher_id: 7,
      users: [],
    });
    http.expectOne('api/teacher/7').flush({
      id: 7,
      firstName: 'Audit',
      lastName: 'Teacher',
    });
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Baseline Yoga');
    expect(fixture.nativeElement.textContent).toContain('Audit TEACHER');
  });

  it('hides session and teacher sections until their data is available', () => {
    const element: HTMLElement = fixture.nativeElement;
    component.teacher = undefined;
    fixture.detectChanges();
    expect(element.querySelector('mat-card-subtitle')).toBeNull();
    expect(element.querySelector('mat-card')).not.toBeNull();
    component.session = undefined;
    fixture.detectChanges();
    expect(element.querySelector('mat-card')).toBeNull();
  });

  it('deletes the session from the administrator action and returns to the list', () => {
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const notify = jest.spyOn(fixture.debugElement.injector.get(MatSnackBar), 'open').mockImplementation();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).not.toContain('Participate');
    element.querySelector<HTMLButtonElement>('button[color="warn"]')!.click();
    const request = http.expectOne('api/session/1');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
    expect(navigate).toHaveBeenCalledWith(['sessions']);
    expect(notify).toHaveBeenCalledWith('Session deleted !', 'Close', { duration: 3000 });
  });

  it('switches the participation actions and refreshes the session after each mutation', () => {
    component.isAdmin = false;
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).not.toContain('Delete');
    expect(element.textContent).not.toContain('Do not participate');
    element.querySelector<HTMLButtonElement>('button[color="primary"]')!.click();
    const participate = http.expectOne('api/session/1/participate/1');
    expect(participate.request.method).toBe('POST');
    participate.flush(null);
    http.expectOne('api/session/1').flush({ ...component.session!, users: [1] });
    http.expectOne('api/teacher/7').flush({ id: 7, firstName: 'Audit', lastName: 'Teacher' });
    fixture.detectChanges();
    expect(element.textContent).toContain('Do not participate');
    expect(element.textContent).toContain('1 attendees');
    expect(element.querySelector('button[color="primary"]')).toBeNull();
    element.querySelector<HTMLButtonElement>('button[color="warn"]')!.click();
    const unParticipate = http.expectOne('api/session/1/participate/1');
    expect(unParticipate.request.method).toBe('DELETE');
    unParticipate.flush(null);
    http.expectOne('api/session/1').flush({ ...component.session!, users: [] });
    http.expectOne('api/teacher/7').flush({ id: 7, firstName: 'Audit', lastName: 'Teacher' });
    fixture.detectChanges();
    expect(element.textContent).not.toContain('Do not participate');
    expect(element.textContent).toContain('0 attendees');
    expect(element.querySelector('button[color="primary"]')).not.toBeNull();
  });
});

