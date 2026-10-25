import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  loginForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  showPassword = false;
  showForgotPassword = false;
  isForgotSubmitting = false;
  forgotMessage = '';
  forgotError = '';
  currentYear = new Date().getFullYear();

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  get f() {
    return this.loginForm.controls;
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleForgotPassword(): void {
    this.showForgotPassword = !this.showForgotPassword;
    this.forgotMessage = '';
    this.forgotError = '';
  }

  requestPasswordReset(): void {
    const email = String(this.f['email'].value || '').trim();
    this.f['email'].markAsTouched();
    if (this.f['email'].invalid || this.isForgotSubmitting) return;

    this.isForgotSubmitting = true;
    this.forgotError = '';
    this.forgotMessage = '';
    this.authService.demanderReinitialisationMotDePasse(email).subscribe({
      next: response => {
        this.forgotMessage = response.message;
        this.isForgotSubmitting = false;
      },
      error: error => {
        console.error('Impossible d’envoyer la demande de réinitialisation', error);
        this.forgotError = error.status === 0
          ? 'Impossible de contacter le serveur. Réessayez plus tard.'
          : 'La demande n’a pas pu être traitée. Réessayez plus tard.';
        this.isForgotSubmitting = false;
      }
    });
  }

  onSubmit(): void {
    // Mark all fields as touched to trigger validation messages
    Object.values(this.loginForm.controls).forEach(control => {
      control.markAsTouched();
    });

    if (this.loginForm.invalid) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        if (err.status === 401 || err.status === 403) {
          this.errorMessage = 'Identifiants incorrects. Veuillez réessayer.';
        } else if (err.status === 0) {
          this.errorMessage = 'Impossible de contacter le serveur. Vérifiez votre connexion.';
        } else {
          this.errorMessage = 'Une erreur est survenue. Veuillez réessayer plus tard.';
        }
      }
    });
  }
}
