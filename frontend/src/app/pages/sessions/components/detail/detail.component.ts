import { Component, DestroyRef, ErrorHandler, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, filter, map, startWith, switchMap, tap } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { Teacher } from '../../../../core/models/teacher.interface';
import { SessionService } from '../../../../core/service/session.service';
import { TeacherService } from '../../../../core/service/teacher.service';
import { Session } from '../../../../core/models/session.interface';
import { SessionApiService } from '../../../../core/service/session-api.service';
import { MaterialModule } from "../../../../shared/material.module";
import { CommonModule } from "@angular/common";

@Component({
  selector: 'app-detail',
  imports: [CommonModule, MaterialModule],
  templateUrl: './detail.component.html',
  styleUrls: ['./detail.component.scss']
})
export class DetailComponent implements OnInit {
  public session: Session | undefined;
  public teacher: Teacher | undefined;
  public isParticipate = false;
  public isAdmin = false;
  public sessionId: string;
  public userId: string;

  private destroyRef = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  private refresh = new Subject<void>();
  private requestedTeacherId?: number;
  private errorHandler = inject(ErrorHandler);
  private sessionService = inject(SessionService);
  private sessionApiService = inject(SessionApiService);
  private teacherService = inject(TeacherService);
  private matSnackBar = inject(MatSnackBar);
  private router = inject(Router);

  constructor() {
    this.sessionId = this.route.snapshot.paramMap.get('id')!;
    this.isAdmin = this.sessionService.sessionInformation!.admin;
    this.userId = this.sessionService.sessionInformation!.id.toString();
  }

  ngOnInit(): void {
    this.refresh.pipe(
      startWith(undefined),
      switchMap(() => this.sessionApiService.detail(this.sessionId).pipe(
        catchError(error => {
          this.errorHandler.handleError(error);
          return EMPTY;
        }),
      )),
      tap(session => {
        this.session = session;
        this.isParticipate = session.users.includes(Number(this.userId));
      }),
      map(session => session.teacher_id),
      filter(id => id !== this.requestedTeacherId),
      switchMap(id => {
        this.requestedTeacherId = id;
        this.teacher = undefined;
        return this.teacherService.detail(String(id)).pipe(
          catchError(error => {
            this.requestedTeacherId = undefined;
            this.errorHandler.handleError(error);
            return EMPTY;
          }),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(teacher => this.teacher = teacher);
  }

  public back(): void {
    window.history.back();
  }

  public delete(): void {
    this.sessionApiService
      .delete(this.sessionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
          this.matSnackBar.open('Session deleted !', 'Close', { duration: 3000 });
          this.router.navigate(['sessions']);
        }
      );
  }

  public participate(): void {
    this.sessionApiService.participate(this.sessionId, this.userId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refresh.next());
  }

  public unParticipate(): void {
    this.sessionApiService.unParticipate(this.sessionId, this.userId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refresh.next());
  }

}
