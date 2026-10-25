import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { MembreService } from '../../../../core/services/membre.service';

export interface NavItem {
  label: string;
  icon: string;
  route: string;
  badge?: number;
  hideForDoctorants?: boolean;
  managerOnly?: boolean;
  reportsOnly?: boolean;
}

export interface NavGroup {
  category: string;
  items: NavItem[];
  adminOnly?: boolean;
  reportsOnly?: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent implements OnInit {
  @Input() collapsed = false;
  @Output() toggleSidebar = new EventEmitter<void>();

  userRole: string = '';
  userStatus = '';
  functionalRole = '';
  compteActif = false;
  compteGele = true;

  navGroups: NavGroup[] = [
    {
      category: 'Général',
      items: [
        { label: 'Tableau de bord', icon: 'home', route: '/dashboard' }
      ]
    },
    {
      category: 'Mon Espace',
      items: [
        { label: 'Messagerie', icon: 'mail', route: '/dashboard/messagerie', hideForDoctorants: true },
        { label: 'Mes Notifications', icon: 'bell', route: '/dashboard/notifications', hideForDoctorants: true }
      ]
    },
    {
      category: 'Recherche & Production',
      items: [
        { label: 'Publications', icon: 'file-text', route: '/dashboard/publications' },
        { label: 'Événements', icon: 'calendar', route: '/dashboard/evenements' },
        { label: 'Calendrier', icon: 'calendar', route: '/dashboard/calendrier' },
        { label: 'Conventions', icon: 'handshake', route: '/dashboard/conventions' }
      ]
    },
    {
      category: 'Ressources & Opérations',
      items: [
        { label: 'Documents', icon: 'folder', route: '/dashboard/documents' },
        { label: 'Logistique', icon: 'package', route: '/dashboard/logistique', hideForDoctorants: true },
        { label: 'Réunions & PV', icon: 'users', route: '/dashboard/reunions', hideForDoctorants: true }
      ]
    },
    {
      category: 'Administration',
      adminOnly: true,
      items: [
        { label: 'Membres & Équipes', icon: 'user-cog', route: '/dashboard/membres' },
        { label: 'Demandes d\'adhésion', icon: 'inbox', route: '/dashboard/adhesions', managerOnly: true },
        { label: 'Gouvernance', icon: 'landmark', route: '/dashboard/gouvernance' }
      ]
    },
    {
      category: 'Pilotage',
      reportsOnly: true,
      items: [
        { label: 'Rapports annuels', icon: 'bar-chart', route: '/dashboard/rapports', reportsOnly: true }
      ]
    }
  ];

  constructor(
    private authService: AuthService,
    private membreService: MembreService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    // Get user role from AuthService
    this.ngZone.run(() => {
      this.userRole = this.authService.getUserRole();
      this.cdr.detectChanges();
    });

    // Subscribe to auth state changes for reactive updates
    this.authService.currentUser$.subscribe(user => {
      this.ngZone.run(() => {
        if (user && user.role) {
          this.userRole = user.role;
        }
        this.cdr.detectChanges();
      });
    });
    this.membreService.getMonProfil().subscribe({
      next: membre => {
        this.userStatus = membre?.statut || '';
        this.functionalRole = membre?.role || '';
        this.compteActif = membre?.compteActif === true;
        this.compteGele = membre?.compteGele === true;
        this.cdr.detectChanges();
      },
      error: error => console.error('Impossible de charger le statut du membre pour la navigation', error)
    });
  }

  get isAdmin(): boolean {
    return this.userStatus !== 'DOCTORANT' && this.userRole !== 'DOCTORANT'
      && (this.userRole === 'ADMIN'
        || this.userRole === 'DIRECTEUR'
        || this.functionalRole === 'ADMIN'
        || this.functionalRole === 'DIRECTEUR');
  }

  get isAdhesionManager(): boolean {
    return this.isAdmin && this.compteActif && !this.compteGele
      && !['DOCTORANT', 'ANCIEN', 'RETRAITE'].includes(this.userStatus);
  }

  get isRapportManager(): boolean {
    return this.compteActif && !this.compteGele
      && !['DOCTORANT', 'ANCIEN', 'RETRAITE'].includes(this.userStatus)
      && (['ADMIN', 'DIRECTEUR', 'VICE_DIRECTEUR'].includes(this.functionalRole)
        || this.userRole === 'ADMIN');
  }

  onToggle(): void {
    this.toggleSidebar.emit();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
