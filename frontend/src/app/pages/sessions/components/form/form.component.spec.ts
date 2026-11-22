import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {  ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { expect } from '@jest/globals';
import { SessionService } from 'src/app/core/service/session.service';
import { SessionApiService } from '../../../../core/service/session-api.service';
import { TeacherService } from '../../../../core/service/teacher.service';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { By } from '@angular/platform-browser';
import { MatSelect } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject } from 'rxjs';
import { Teacher } from '../../../../core/models/teacher.interface';

import { FormComponent } from './form.component';
import { NotFoundComponent } from '../../../not-found/not-found.component';

describe('[integration] J-I02 FormComponent', () => {
  let component: FormComponent;
  let fixture: ComponentFixture<FormComponent>;
  let http: HttpTestingController;

  const mockSessionService = {
    sessionInformation: {
      admin: true
    }
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        FormComponent,
        RouterTestingModule,
        HttpClientTestingModule,
        MatCardModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        ReactiveFormsModule,
        MatSnackBarModule,
        MatSelectModule,
        BrowserAnimationsModule
      ],
      providers: [
        { provide: SessionService, useValue: mockSessionService },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } } },
        SessionApiService
      ],
    })
      .compileComponents();

    fixture = TestBed.createComponent(FormComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne('api/teacher').flush([]);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.sessionForm?.invalid).toBe(true);
  });

  it.each([false, true])('returns to sessions without saving when back is clicked (update: %s)', async (update) => {
    const router = TestBed.inject(Router);
    router.resetConfig([
      { path: 'sessions', component: NotFoundComponent },
      { path: 'sessions/create', component: NotFoundComponent },
      { path: 'sessions/update/:id', component: NotFoundComponent },
    ]);
    await router.navigateByUrl(update ? '/sessions/update/1' : '/sessions/create');
    if (update) {
      component.ngOnInit();
      http.expectOne('api/session/1').flush({ id: 1, name: 'Yoga', date: '2026-12-01', teacher_id: 7, description: 'Practice', users: [] });
    }
    component.sessionForm!.patchValue({ name: 'Unsaved change' });
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    element.querySelector<HTMLButtonElement>('button[routerLink="/sessions"]')!.click();
    await fixture.whenStable();

    expect(router.url).toBe('/sessions');
    http.expectNone(request => ['POST', 'PUT', 'DELETE'].includes(request.method));
  });

  it('shows the create title and saves the form values', () => {
    const element: HTMLElement = fixture.nativeElement;
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const notify = jest.spyOn(fixture.debugElement.injector.get(MatSnackBar), 'open').mockImplementation();
    expect(element.textContent).toContain('Create session');
    expect(element.textContent).not.toContain('Update session');
    const values = { name: 'Practice', date: '2026-12-01', teacher_id: 7, description: 'Yoga practice' };
    component.sessionForm!.setValue(values);
    fixture.detectChanges();
    expect(element.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(false);
    element.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    const request = http.expectOne('api/session');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(values);
    request.flush(null, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('conflicts with existing data');
    expect(component.sessionForm!.value).toEqual(values);
    expect(navigate).not.toHaveBeenCalled();
    component.submit();
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')).toBeNull();
    http.expectOne('api/session').flush({ ...values, id: 1, users: [] });
    expect(navigate).toHaveBeenCalledWith(['sessions']);
    expect(notify).toHaveBeenCalledWith('Session created !', 'Close', { duration: 3000 });
  });

  it('shows the update title, waits for the form and saves changes', () => {
    jest.spyOn(TestBed.inject(Router), 'url', 'get').mockReturnValue('/sessions/update/1');
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    jest.spyOn(fixture.debugElement.injector.get(MatSnackBar), 'open').mockImplementation();
    component.sessionForm = undefined;
    component.ngOnInit();
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Update session');
    expect(element.textContent).not.toContain('Create session');
    expect(element.querySelector('form')).toBeNull();
    http.expectOne('api/session/1').flush({ id: 1, name: 'Yoga', date: '2026-12-01', teacher_id: 7, description: 'Practice', users: [1] });
    fixture.detectChanges();
    // Recreating the async block subscribes to the teacher request again.
    http.expectOne('api/teacher').flush([]);
    fixture.detectChanges();
    expect(element.querySelector('form')).not.toBeNull();
    component.sessionForm!.patchValue({ name: 'Updated yoga' });
    element.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    const request = http.expectOne('api/session/1');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.name).toBe('Updated yoga');
    expect(request.request.body.users).toBeUndefined();
    request.flush(null, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('conflicts with existing data');
    expect(component.sessionForm!.value.name).toBe('Updated yoga');
    expect(navigate).not.toHaveBeenCalled();
    component.submit();
    http.expectOne('api/session/1').flush({ id: 1, ...request.request.body });
    expect(navigate).toHaveBeenCalledWith(['sessions']);
  });

  it('shows a general save error when the server fails', () => {
    component.submit();
    http.expectOne('api/session').flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Unable to save');
  });

  it('keeps teacher options attached to their IDs through refresh, reorder and removal', () => {
    const first: Teacher = { id: 7, firstName: 'First', lastName: 'Teacher', createdAt: new Date(), updatedAt: new Date() };
    const second = { ...first, id: 8, firstName: 'Second' };
    const teachers = new BehaviorSubject([first, second]);
    component.teachers$ = teachers.asObservable();
    fixture.detectChanges();
    const select = fixture.debugElement.query(By.directive(MatSelect)).componentInstance as MatSelect;
    const options = select.options.toArray();
    expect(options.map(option => option.value)).toEqual([7, 8]);
    teachers.next([{ ...second, firstName: 'Updated' }, { ...first }]);
    fixture.detectChanges();
    expect(select.options.first).toBe(options[1]);
    expect(select.options.last).toBe(options[0]);
    expect(select.options.first.viewValue).toContain('Updated');
    teachers.next([{ ...first }]);
    fixture.detectChanges();
    expect(select.options.toArray()).toEqual([options[0]]);
    teachers.next([]);
    fixture.detectChanges();
    expect(select.options.length).toBe(0);
  });

  it('cancels the async teacher request when leaving the form', () => {
    component.teachers$ = TestBed.inject(TeacherService).all();
    fixture.detectChanges();
    const request = http.expectOne('api/teacher');
    fixture.destroy();
    expect(request.cancelled).toBe(true);
  });
});
