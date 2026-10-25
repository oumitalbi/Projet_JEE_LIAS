import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Membre } from '../../../core/models/membre.model';
import {
  EquipeMandat, FonctionMandat, HistoriqueProfil, HistoriqueRole, Mandat,
  PeriodeAffiliation
} from '../../../core/models/mandat.model';
import { MandatService } from '../../../core/services/mandat.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-gouvernance-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './gouvernance-list.component.html',
  styleUrl: './gouvernance-list.component.scss'
})
export class GouvernanceListComponent implements OnInit {
  mandats: Mandat[] = [];
  membres: Membre[] = [];
  filteredMandats: Mandat[] = [];
  selectedMember: Membre | null = null;
  memberMandats: Mandat[] = [];
  affiliations: PeriodeAffiliation[] = [];
  roleHistory: HistoriqueRole[] = [];
  profileHistory: HistoriqueProfil[] = [];

  isLoading = false;
  isSaving = false;
  isHistoryLoading = false;
  isManaging = false;
  isDetailOpen = false;
  isFormOpen = false;
  isCloseFormOpen = false;
  isLifecycleOpen = false;
  errorMessage = '';
  successMessage = '';
  labCreationDate = '';
  selectedRole = 'TOUS';
  selectedTeam = 'TOUTES';
  selectedYear = 'TOUTES';
  selectedMandat: Mandat | null = null;
  editingMandat: Mandat | null = null;
  lifecycleAction: 'RETRAITE' | 'DESACTIVER' | 'REACTIVER' = 'DESACTIVER';
  reactivationStatus = 'PERMANENT';
  currentMember: Membre | null = null;
  mandatForm!: FormGroup;
  closeForm!: FormGroup;

  readonly roles: { value: FonctionMandat; label: string }[] = [
    { value: 'DIRECTEUR', label: 'Directeur' },
    { value: 'VICE_DIRECTEUR', label: 'Vice-directeur' },
    { value: 'CHEF_EQUIPE', label: "Chef d'équipe" }
  ];
  readonly teams: EquipeMandat[] = ['ISDIAC', 'SIMA', 'SDTIC', 'ILIAS'];

  constructor(
    private mandatService: MandatService,
    private authService: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.isLoading = true;
    this.mandatService.getTous().subscribe({
      next: data => {
        this.mandats = data ?? [];
        this.applyFilters();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: err => {
        this.errorMessage = this.getError(err, 'Impossible de charger les mandats.');
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
    this.mandatService.getMembres().subscribe({
      next: data => {
        this.membres = (data ?? []).filter(membre => membre.compteActif !== false
          && membre.statut !== 'DOCTORANT' && membre.statut !== 'ANCIEN' && membre.statut !== 'RETRAITE');
        const id = this.authService.getUserId();
        this.currentMember = (data ?? []).find(membre => membre.id === id) ?? null;
        this.isManaging = this.currentMember?.roleSysteme === 'ADMIN'
          || this.currentMember?.role === 'ADMIN' || this.currentMember?.role === 'DIRECTEUR';
        this.cdr.detectChanges();
      },
      error: err => {
        this.errorMessage = this.getError(err, 'Impossible de charger les membres.');
        this.cdr.detectChanges();
      }
    });
    this.mandatService.getDateCreationLaboratoire().subscribe({
      next: response => this.labCreationDate = response?.dateCreation ?? '',
      error: err => {
        this.errorMessage = this.getError(err, 'Impossible de charger la date de création du laboratoire.');
        this.cdr.detectChanges();
      }
    });
  }

  private initForms(): void {
    this.mandatForm = this.fb.group({
      membreId: [null, Validators.required],
      responsabilite: ['DIRECTEUR', Validators.required],
      equipe: [null],
      dateDebut: [this.todayIso(), Validators.required],
      description: ['']
    });
    this.closeForm = this.fb.group({
      dateFin: [this.todayIso(), Validators.required],
      motif: ['', [Validators.required, Validators.maxLength(1000)]]
    });
    this.mandatForm.get('responsabilite')?.valueChanges.subscribe((role: FonctionMandat) => {
      const teamControl = this.mandatForm.get('equipe');
      if (role === 'CHEF_EQUIPE') {
        teamControl?.setValidators(Validators.required);
      } else {
        teamControl?.clearValidators();
        teamControl?.setValue(null);
      }
      teamControl?.updateValueAndValidity();
    });
  }

  get organigramme(): Mandat[] {
    const active = this.mandats.filter(mandat => mandat.actif);
    return active
      .filter(mandat => mandat.responsabilite === 'DIRECTEUR' || mandat.responsabilite === 'VICE_DIRECTEUR'
        || mandat.responsabilite === 'CHEF_EQUIPE')
      .sort((a, b) => this.roleOrder(a.responsabilite) - this.roleOrder(b.responsabilite));
  }

  get activeMandateCount(): number {
    return this.mandats.filter(mandat => mandat.actif).length;
  }

  get years(): string[] {
    return [...new Set(this.mandats.map(mandat => this.getYear(mandat.dateDebut)))]
      .filter(Boolean).sort((a, b) => Number(b) - Number(a));
  }

  get eligibleMembers(): Membre[] {
    const team = this.mandatForm?.get('equipe')?.value;
    if (this.mandatForm?.get('responsabilite')?.value !== 'CHEF_EQUIPE' || !team) return this.membres;
    return this.membres.filter(membre => membre.equipeLaboratoire === team);
  }

  applyFilters(): void {
    this.filteredMandats = this.mandats.filter(mandat =>
      (this.selectedRole === 'TOUS' || mandat.responsabilite === this.selectedRole)
      && (this.selectedTeam === 'TOUTES' || mandat.equipe === this.selectedTeam)
      && (this.selectedYear === 'TOUTES' || this.getYear(mandat.dateDebut) === this.selectedYear)
    );
  }

  onFilterChanged(): void {
    this.applyFilters();
  }

  openCreateForm(): void {
    this.errorMessage = '';
    this.editingMandat = null;
    this.mandatForm.get('responsabilite')?.enable();
    this.mandatForm.get('membreId')?.enable();
    this.isFormOpen = true;
  }

  openEditForm(mandat: Mandat): void {
    this.errorMessage = '';
    this.editingMandat = mandat;
    this.mandatForm.reset({
      membreId: mandat.membre.id,
      responsabilite: mandat.responsabilite,
      equipe: mandat.equipe,
      dateDebut: mandat.dateDebut.slice(0, 10),
      description: mandat.description ?? ''
    });
    this.mandatForm.get('responsabilite')?.disable();
    this.mandatForm.get('membreId')?.disable();
    this.isFormOpen = true;
  }

  closeCreateForm(): void {
    this.isFormOpen = false;
    this.editingMandat = null;
    this.mandatForm.get('responsabilite')?.enable();
    this.mandatForm.get('membreId')?.enable();
    this.mandatForm.reset({
      membreId: null, responsabilite: 'DIRECTEUR', equipe: null, dateDebut: this.todayIso(), description: ''
    });
  }

  saveMandat(): void {
    if (this.mandatForm.invalid || this.isSaving) {
      this.mandatForm.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    this.errorMessage = '';
    const formValue = this.mandatForm.getRawValue();
    const mandatEnEdition = this.editingMandat;
    const wasEditing = mandatEnEdition !== null;
    const request = mandatEnEdition
      ? this.mandatService.modifier(mandatEnEdition.id, {
          dateDebut: formValue.dateDebut,
          equipe: formValue.equipe,
          description: formValue.description
        })
      : this.mandatService.creer(formValue);
    request.subscribe({
      next: mandat => {
        if (this.editingMandat) this.replaceMandat(mandat);
        else this.mandats = [mandat, ...this.mandats.filter(existing => existing.id !== mandat.id)];
        this.applyFilters();
        this.isSaving = false;
        this.closeCreateForm();
        this.successMessage = wasEditing ? 'Le mandat a été corrigé.' : 'Le mandat a été attribué.';
        this.cdr.detectChanges();
      },
      error: err => {
        this.errorMessage = this.getError(err, 'Impossible d’attribuer le mandat.');
        this.isSaving = false;
        this.cdr.detectChanges();
      }
    });
  }

  openCloseForm(mandat: Mandat): void {
    this.selectedMandat = mandat;
    this.closeForm.reset({ dateFin: this.todayIso(), motif: '' });
    this.isCloseFormOpen = true;
  }

  closeMandat(): void {
    if (!this.selectedMandat || this.closeForm.invalid || this.isSaving) {
      this.closeForm.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    const { dateFin, motif } = this.closeForm.getRawValue();
    this.mandatService.cloturer(this.selectedMandat.id, dateFin, motif).subscribe({
      next: updated => {
        this.replaceMandat(updated);
        this.isSaving = false;
        this.isCloseFormOpen = false;
        this.selectedMandat = null;
        this.successMessage = 'Le mandat a été clôturé et conservé dans l’historique.';
        this.cdr.detectChanges();
      },
      error: err => {
        this.errorMessage = this.getError(err, 'Impossible de clôturer le mandat.');
        this.isSaving = false;
        this.cdr.detectChanges();
      }
    });
  }

  openMemberHistory(member: Membre): void {
    this.selectedMember = member;
    this.isDetailOpen = true;
    this.isHistoryLoading = true;
    this.memberMandats = [];
    this.affiliations = [];
    this.roleHistory = [];
    this.profileHistory = [];
    this.mandatService.getParMembre(member.id).subscribe({
      next: data => { this.memberMandats = data ?? []; this.cdr.detectChanges(); },
      error: err => { this.errorMessage = this.getError(err, 'Impossible de charger les mandats du membre.'); this.cdr.detectChanges(); }
    });
    this.mandatService.getAffiliations(member.id).subscribe({
      next: data => { this.affiliations = data ?? []; this.cdr.detectChanges(); },
      error: err => { this.errorMessage = this.getError(err, 'Impossible de charger les affiliations.'); this.cdr.detectChanges(); }
    });
    this.mandatService.getHistoriqueRoles(member.id).subscribe({
      next: data => { this.roleHistory = data ?? []; this.cdr.detectChanges(); },
      error: err => { this.errorMessage = this.getError(err, 'Impossible de charger l’historique des rôles.'); this.cdr.detectChanges(); }
    });
    this.mandatService.getHistoriqueProfil(member.id).subscribe({
      next: data => {
        this.profileHistory = data ?? [];
        this.isHistoryLoading = false;
        this.cdr.detectChanges();
      },
      error: err => {
        this.errorMessage = this.getError(err, 'Impossible de charger les modifications du profil.');
        this.isHistoryLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  closeDetail(): void {
    this.isDetailOpen = false;
    this.selectedMember = null;
  }

  openLifecycle(action: 'RETRAITE' | 'DESACTIVER' | 'REACTIVER', member: Membre): void {
    this.selectedMember = member;
    this.lifecycleAction = action;
    this.reactivationStatus = 'PERMANENT';
    this.isLifecycleOpen = true;
  }

  confirmLifecycle(): void {
    if (!this.selectedMember || this.isSaving) return;
    const fullName = `${this.selectedMember.prenom ?? ''} ${this.selectedMember.nom ?? ''}`.trim();
    const actionName = this.lifecycleAction === 'RETRAITE' ? 'mettre à la retraite'
      : this.lifecycleAction === 'DESACTIVER' ? 'désactiver' : 'réactiver';
    if (!window.confirm(`Confirmer : ${actionName} ${fullName} ?`)) return;

    this.isSaving = true;
    const id = this.selectedMember.id;
    const request = this.lifecycleAction === 'RETRAITE' ? this.mandatService.mettreEnRetraite(id)
      : this.lifecycleAction === 'DESACTIVER' ? this.mandatService.desactiver(id)
        : this.mandatService.reactiver(id, this.reactivationStatus);
    request.subscribe({
      next: member => {
        this.isSaving = false;
        this.isLifecycleOpen = false;
        this.successMessage = `Le cycle de vie de ${fullName} a été mis à jour.`;
        this.membres = this.membres.filter(existing => existing.id !== member.id);
        if (member.compteActif !== false && member.statut !== 'RETRAITE' && member.statut !== 'ANCIEN'
            && member.statut !== 'DOCTORANT') {
          this.membres = [...this.membres, member];
        }
        this.selectedMember = member;
        this.cdr.detectChanges();
      },
      error: err => {
        this.errorMessage = this.getError(err, 'Impossible de mettre à jour le statut du membre.');
        this.isSaving = false;
        this.cdr.detectChanges();
      }
    });
  }

  getInitials(member: { nom?: string | null; prenom?: string | null }): string {
    return `${member.prenom?.charAt(0) ?? ''}${member.nom?.charAt(0) ?? ''}`.toUpperCase();
  }

  getMemberFromMandate(mandat: Mandat): Membre {
    return this.membres.find(member => member.id === mandat.membre.id) ?? {
      id: mandat.membre.id,
      nom: mandat.membre.nom,
      prenom: mandat.membre.prenom,
      email: '',
      role: mandat.responsabilite,
      statut: '',
      equipeLaboratoire: mandat.membre.equipeLaboratoire,
      photo: mandat.membre.photo
    };
  }

  getFullName(member: { nom?: string | null; prenom?: string | null } | null): string {
    return member ? `${member.prenom ?? ''} ${member.nom ?? ''}`.trim() : '';
  }

  roleLabel(role: string): string {
    return this.roles.find(option => option.value === role)?.label ?? role.replaceAll('_', ' ');
  }

  formatDate(date: string | null | undefined): string {
    if (!date) return 'En cours';
    return new Date(`${date.slice(0, 10)}T12:00:00`).toLocaleDateString('fr-FR', {
      day: 'numeric', month: 'long', year: 'numeric'
    });
  }

  getYear(date: string | null): string {
    return date?.slice(0, 4) ?? '';
  }

  private roleOrder(role: FonctionMandat): number {
    return role === 'DIRECTEUR' ? 0 : role === 'VICE_DIRECTEUR' ? 1 : 2;
  }

  todayIso(): string {
    const today = new Date();
    return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  }

  private replaceMandat(updated: Mandat): void {
    this.mandats = this.mandats.map(mandat => mandat.id === updated.id ? updated : mandat);
    this.applyFilters();
  }

  private getError(error: unknown, fallback: string): string {
    if (typeof error === 'object' && error !== null && 'error' in error) {
      const body = (error as { error?: unknown }).error;
      if (typeof body === 'string') return body;
      if (typeof body === 'object' && body !== null && 'message' in body
          && typeof (body as { message?: unknown }).message === 'string') {
        return (body as { message: string }).message;
      }
    }
    return fallback;
  }
}
