import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { MembreService } from '../services/membre.service';

export const rapportManagerGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(MembreService).getMonProfil().pipe(
    map(membre => {
      const roleAutorise = ['ADMIN', 'DIRECTEUR', 'VICE_DIRECTEUR'].includes(membre?.role ?? '')
        || membre?.roleSysteme === 'ADMIN';
      const compteAutorise = membre?.compteActif === true
        && membre?.compteGele !== true
        && !['DOCTORANT', 'ANCIEN', 'RETRAITE'].includes(membre?.statut ?? '');
      return roleAutorise && compteAutorise || router.createUrlTree(['/dashboard']);
    }),
    catchError(() => of(router.createUrlTree(['/dashboard'])))
  );
};
