import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterTestingModule, } from '@angular/router/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { expect } from '@jest/globals';
import { SessionService } from '../../../../core/service/session.service';

import { DetailComponent } from './detail.component';


describe('DetailComponent', () => {
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
});

