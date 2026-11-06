import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { provideRouter } from '@angular/router';
import { expect } from '@jest/globals';
import { SessionService } from 'src/app/core/service/session.service';

import { ListComponent } from './list.component';

describe('ListComponent', () => {
  let component: ListComponent;
  let fixture: ComponentFixture<ListComponent>;
  let http: HttpTestingController;

  const mockSessionService = {
    sessionInformation: {
      admin: true
    }
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ListComponent,
        HttpClientTestingModule,
        MatCardModule,
        MatIconModule
      ],
      providers: [
        provideRouter([]),
        { provide: SessionService, useValue: mockSessionService }
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(ListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne('api/session').flush([{
      id: 1,
      name: 'Baseline yoga',
      description: 'Baseline session',
      date: '2026-12-01',
      teacher_id: 7,
      users: [],
    }]);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Baseline yoga');
  });
});
