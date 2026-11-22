import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators } from '@angular/forms';
import { nonBlankText } from '../../shared/non-blank-text.validator';
import { Router } from '@angular/router';
import { AuthService } from '../../core/service/auth.service';
import { RegisterRequest } from '../../core/models/registerRequest.interface';
import { MaterialModule } from "../../shared/material.module";
@Component({
  selector: 'app-register',
  imports: [MaterialModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  private destroyRef = inject(DestroyRef);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);
  private router = inject(Router);
  public onError = false;

  public form = this.fb.group({
    email: [
      '',
      [
        Validators.required,
        Validators.email
      ]
    ],
    firstName: [
      '',
      [
        Validators.required,
        nonBlankText,
        Validators.minLength(3),
        Validators.maxLength(20)
      ]
    ],
    lastName: [
      '',
      [
        Validators.required,
        nonBlankText,
        Validators.minLength(3),
        Validators.maxLength(20)
      ]
    ],
    password: [
      '',
      [
        Validators.required,
        nonBlankText,
        Validators.minLength(6),
        Validators.maxLength(40)
      ]
    ]
  });


  public submit(): void {
    if (this.form.invalid) {
      return;
    }

    const registerRequest = this.form.value as RegisterRequest;
    this.authService.register(registerRequest)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (_: void) => this.router.navigate(['/login']),
        error: _ => this.onError = true,
      }
    );
  }

}
