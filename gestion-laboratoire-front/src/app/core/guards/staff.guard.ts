import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MembreService } from '../services/membre.service';
import { catchError, map, of } from 'rxjs';

export const staffGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (authService.getUserRole() === 'DOCTORANT') return router.createUrlTree(['/dashboard']);
  return inject(MembreService).getMonProfil().pipe(
    map(membre => membre?.statut === 'DOCTORANT' ? router.createUrlTree(['/dashboard']) : true),
    catchError(() => of(router.createUrlTree(['/dashboard'])))
  );
};
