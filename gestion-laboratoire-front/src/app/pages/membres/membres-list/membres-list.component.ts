import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MembreService } from '../../../core/services/membre.service';
import { AuthService } from '../../../core/services/auth.service';
import { Membre } from '../../../core/models/membre.model';
import { Router } from '@angular/router';

@Component({
  selector: 'app-membres-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './membres-list.component.html',
  styleUrl: './membres-list.component.scss'
})
export class MembresListComponent implements OnInit {
  membres: Membre[] = [];
  selectedMembre: Membre | null = null;
  isModalOpen: boolean = false;
  isSendingResetLink = false;
  resetLinkMessage = '';
  resetLinkError = '';

  constructor(
    private membreService: MembreService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  get canSendResetLink(): boolean {
    return this.authService.getUserRole() === 'ADMIN';
  }

  get canManageRoles(): boolean {
    return ['ADMIN', 'DIRECTEUR'].includes(this.authService.getUserRole());
  }

  ngOnInit(): void {
    this.membreService.getMembres().subscribe({
      next: (response: any) => {
        // Extraction sécurisée au cas où l'API renvoie un wrapper (ex: { content: [...] } ou { data: [...] })
        if (Array.isArray(response)) {
          this.membres = response;
        } else if (response && response.content) {
          this.membres = response.content;
        } else if (response && response.data) {
          this.membres = response.data;
        } else {
          this.membres = [];
        }
        
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur lors du chargement des membres', err);
      }
    });
  }

  openModal(membre: Membre): void {
    this.selectedMembre = membre;
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
    setTimeout(() => this.selectedMembre = null, 300); // delay to allow animation to finish
  }

  openGovernance(): void {
    this.closeModal();
    this.router.navigate(['/dashboard/gouvernance']);
  }

  sendResetLink(): void {
    const membre = this.selectedMembre;
    if (!membre || !this.canSendResetLink || this.isSendingResetLink) return;
    this.isSendingResetLink = true;
    this.resetLinkMessage = '';
    this.resetLinkError = '';
    this.membreService.envoyerLienReinitialisationMotDePasse(membre.id).subscribe({
      next: response => {
        this.resetLinkMessage = response.message;
        this.isSendingResetLink = false;
        this.cdr.detectChanges();
      },
      error: error => {
        console.error('Impossible d’envoyer le lien de réinitialisation au membre', error);
        this.resetLinkError = error?.error?.message || 'Le lien n’a pas pu être envoyé. Réessayez plus tard.';
        this.isSendingResetLink = false;
        this.cdr.detectChanges();
      }
    });
  }
}
