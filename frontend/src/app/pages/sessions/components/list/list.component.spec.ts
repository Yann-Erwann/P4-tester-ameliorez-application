import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { provideRouter } from '@angular/router';
import { expect } from '@jest/globals';
import { SessionService } from 'src/app/core/service/session.service';

import { ListComponent } from './list.component';
import { BehaviorSubject, of } from 'rxjs';
import { SessionSummary } from '../../../../core/models/session.interface';
import { SessionApiService } from '../../../../core/service/session-api.service';

describe('[integration] J-I02 ListComponent', () => {
  let component: ListComponent;
  let fixture: ComponentFixture<ListComponent>;
  let http: HttpTestingController;

  const mockSessionService = {
    sessionInformation: {
      admin: true
    }
  }

  beforeEach(async () => {
    mockSessionService.sessionInformation.admin = true;
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
    http.expectOne('api/session?view=summary').flush([{
      id: 1,
      name: 'Baseline yoga',
      description: 'Baseline session',
      date: '2026-12-01',
    }]);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Baseline yoga');
  });

  it('shows create and edit only to administrators while keeping detail available', () => {
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('[routerLink="create"]')).not.toBeNull();
    expect(element.textContent).toContain('Edit');
    mockSessionService.sessionInformation.admin = false;
    fixture.detectChanges();
    expect(element.querySelector('[routerLink="create"]')).toBeNull();
    expect(element.textContent).not.toContain('Edit');
    expect(element.textContent).toContain('Detail');
  });

  it('renders an empty list', () => {
    component.sessions$ = of([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.item')).toHaveLength(0);
  });

  it('keeps cards attached to their IDs when sessions are refreshed, reordered and removed', () => {
    const first: SessionSummary = { id: 1, name: 'First', description: 'Practice', date: '2026-12-01' };
    const second = { ...first, id: 2, name: 'Second' };
    const sessions = new BehaviorSubject([first, second]);
    component.sessions$ = sessions.asObservable();
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    const cards = element.querySelectorAll('.item');
    sessions.next([{ ...second, name: 'Updated second' }, { ...first }]);
    fixture.detectChanges();
    const reordered = element.querySelectorAll('.item');
    expect(reordered[0]).toBe(cards[1]);
    expect(reordered[1]).toBe(cards[0]);
    expect(reordered[0].textContent).toContain('Updated second');
    sessions.next([{ ...first }]);
    fixture.detectChanges();
    expect(element.querySelectorAll('.item')).toHaveLength(1);
    expect(element.querySelector('.item')).toBe(cards[0]);
  });

  it('cancels the async session request when leaving the list', () => {
    component.sessions$ = TestBed.inject(SessionApiService).summaries();
    fixture.detectChanges();
    const request = http.expectOne('api/session?view=summary');
    fixture.destroy();
    expect(request.cancelled).toBe(true);
  });
});
