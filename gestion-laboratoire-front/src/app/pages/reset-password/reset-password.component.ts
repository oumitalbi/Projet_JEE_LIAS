import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss'
})
export class ResetPasswordComponent {
  readonly token: string;
  readonly form: FormGroup;
  isSubmitting = false;
  successMessage = '';
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (this.token) {
      this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
    }
    this.form = this.fb.group({
      motDePasse: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(128)]],
      confirmationMotDePasse: ['', Validators.required]
    }, { validators: this.passwordsMatch });
    if (!this.token) {
      this.errorMessage = 'Le lien est incomplet. Demandez un nouveau lien depuis la page de connexion.';
    }
  }

  get controls() {
    return this.form.controls;
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || !this.token || this.isSubmitting) return;
    this.isSubmitting = true;
    this.errorMessage = '';
    this.authService.reinitialiserMotDePasse(
      this.token,
      this.controls['motDePasse'].value || '',
      this.controls['confirmationMotDePasse'].value || ''
    ).subscribe({
      next: response => {
        this.successMessage = response.message;
        this.isSubmitting = false;
        this.form.disable();
      },
      error: error => {
        console.error('La réinitialisation du mot de passe a échoué', error);
        this.errorMessage = error?.error?.message
          || 'Le lien est invalide ou expiré. Demandez-en un nouveau depuis la page de connexion.';
        this.isSubmitting = false;
      }
    });
  }

  private passwordsMatch(control: AbstractControl): ValidationErrors | null {
    const password = control.get('motDePasse')?.value;
    const confirmation = control.get('confirmationMotDePasse')?.value;
    return password && confirmation && password !== confirmation ? { passwordsMismatch: true } : null;
  }
}
