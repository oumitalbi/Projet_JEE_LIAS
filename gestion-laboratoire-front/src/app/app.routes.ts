import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { staffGuard } from './core/guards/staff.guard';
import { adhesionManagerGuard } from './core/guards/adhesion-manager.guard';
import { rapportManagerGuard } from './core/guards/rapport-manager.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./pages/landing-page/landing-page.component').then(m => m.LandingPageComponent),
    title: 'LIAS — Accueil'
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent),
    title: 'LIAS — Connexion'
  },
  {
    path: 'demande-adhesion',
    loadComponent: () => import('./pages/demande-adhesion/candidature.component').then(m => m.CandidatureComponent),
    title: 'LIAS — Demande d’adhésion'
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./pages/reset-password/reset-password.component').then(m => m.ResetPasswordComponent),
    title: 'LIAS — Réinitialiser le mot de passe'
  },
  {
    path: 'conventions',
    pathMatch: 'full',
    redirectTo: '/dashboard/conventions'
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/dashboard/layout/dashboard-layout.component').then(m => m.DashboardLayoutComponent),
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/dashboard/home/dashboard-home.component').then(m => m.DashboardHomeComponent),
        title: 'LIAS — Tableau de bord'
      },
      {
        path: 'membres',
        loadComponent: () => import('./pages/membres/membres-list/membres-list.component').then(m => m.MembresListComponent),
        title: 'LIAS — Membres'
      },
      {
        path: 'mon-profil',
        loadComponent: () => import('./pages/mon-profil/mon-profil.component').then(m => m.MonProfilComponent),
        title: 'LIAS — Mon Profil'
      },
      {
        path: 'messagerie',
        loadComponent: () => import('./pages/messagerie/messagerie.component').then(m => m.MessagerieComponent),
        title: 'LIAS — Messagerie'
      },
      {
        path: 'notifications',
        loadComponent: () => import('./pages/notifications/notifications-list.component').then(m => m.NotificationsListComponent),
        title: 'LIAS — Mes Notifications'
      },
      {
        path: 'calendrier',
        loadComponent: () => import('./pages/calendrier/calendrier.component').then(m => m.CalendrierComponent),
        title: 'LIAS — Calendrier'
      },
      {
        path: 'publications',
        loadComponent: () => import('./pages/publications/publications-list/publications-list.component').then(m => m.PublicationsListComponent),
        title: 'LIAS — Publications'
      },
      {
        path: 'evenements',
        loadComponent: () => import('./pages/evenements/evenements-list/evenements-list.component').then(m => m.EvenementsListComponent),
        title: 'LIAS — Événements'
      },
      {
        path: 'documents',
        loadComponent: () => import('./pages/documents/documents-list/documents-list.component').then(m => m.DocumentsListComponent),
        title: 'LIAS — Documents'
      },
      {
        path: 'reunions',
        loadComponent: () => import('./pages/reunions/reunions-list/reunions-list.component').then(m => m.ReunionsListComponent),
        title: 'LIAS — Réunions & PV'
      },
      {
        path: 'conventions',
        loadComponent: () => import('./pages/conventions/conventions-list/conventions-list.component').then(m => m.ConventionsListComponent),
        title: 'LIAS — Conventions'
      },
      {
        path: 'logistique',
        loadComponent: () => import('./pages/logistique/logistique-list/logistique-list.component').then(m => m.LogistiqueListComponent),
        title: 'LIAS — Logistique'
      },
      {
        path: 'gouvernance',
        canActivate: [staffGuard],
        loadComponent: () => import('./pages/gouvernance/gouvernance-list/gouvernance-list.component').then(m => m.GouvernanceListComponent),
        title: 'LIAS — Gouvernance'
      },
      {
        path: 'adhesions',
        canActivate: [adhesionManagerGuard],
        loadComponent: () => import('./pages/demande-adhesion/adhesions-list.component').then(m => m.AdhesionsListComponent),
        title: 'LIAS — Demandes d’adhésion'
      },
      {
        path: 'rapports',
        canActivate: [rapportManagerGuard],
        loadComponent: () => import('./pages/rapports/rapports-annuels.component').then(m => m.RapportsAnnuelsComponent),
        title: 'LIAS — Rapports annuels'
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
