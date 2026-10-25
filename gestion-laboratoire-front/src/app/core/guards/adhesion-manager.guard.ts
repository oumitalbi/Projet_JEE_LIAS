import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { MembreService } from '../services/membre.service';

export const adhesionManagerGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(MembreService).getMonProfil().pipe(
    map(membre => {
      const eligible = membre?.compteActif !== false
        && membre?.compteGele !== true
        && membre?.statut !== 'DOCTORANT'
        && membre?.statut !== 'ANCIEN'
        && membre?.statut !== 'RETRAITE'
        && (membre?.roleSysteme === 'ADMIN' || membre?.role === 'ADMIN' || membre?.role === 'DIRECTEUR' || membre?.roleSysteme === 'DIRECTEUR');
      return eligible || router.createUrlTree(['/dashboard']);
    }),
    catchError(() => of(router.createUrlTree(['/dashboard'])))
  );
};
