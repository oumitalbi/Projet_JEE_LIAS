import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Reunion, ReunionRequest, ReunionMembre, StatutReunion } from '../../../core/models/reunion.model';
import { Membre } from '../../../core/models/membre.model';
import { ReunionService } from '../../../core/services/reunion.service';
import { MembreService } from '../../../core/services/membre.service';
import { DocumentService } from '../../../core/services/document.service';
import { AuthService } from '../../../core/services/auth.service';
import { parseLocalDateTime, formatDateTimeRange } from '../../../core/utils/date-format.util';

@Component({
  selector: 'app-reunions-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './reunions-list.component.html',
  styleUrl: './reunions-list.component.scss'
})
export class ReunionsListComponent implements OnInit {
  reunions: Reunion[] = [];
  filteredReunions: Reunion[] = [];
  membres: Membre[] = [];
  selectedReunion: Reunion | null = null;
  reunionForm!: FormGroup;
  selectedParticipantIds: number[] = [];
  userId: number | null = null;
  userRole = '';
  userFunctionalRole = '';
  userStatus = '';
  statusFilter = 'TOUS';
  periodFilter = 'TOUTES';
  teamFilter = 'TOUTES';
  onlyMine = false;
  showArchived = false;
  detailOpen = false;
  formOpen = false;
  isLoading = false;
  isSaving = false;
  isUploadingPv = false;
  isAttachingPv = false;
  actionLoading = false;
  formError = '';
  successMessage = '';
  noticeError = false;
  pvError = '';
  selectedPvFile: File | null = null;
  pendingPvDocumentId: number | null = null;
  participantToAdd = '';
  showActionConfirm = false;
  confirmAction: (() => void) | null = null;
  confirmTitle = '';
  confirmMessage = '';

  constructor(
    private readonly reunionService: ReunionService,
    private readonly membreService: MembreService,
    private readonly documentService: DocumentService,
    private readonly authService: AuthService,
    private readonly route: ActivatedRoute,
    private readonly fb: FormBuilder,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.userId = this.authService.getUserId();
    this.userRole = this.authService.getUserRole();
    this.initForm();
    this.loadReunions();
    const reunionId = Number(this.route.snapshot.queryParamMap.get('reunionId'));
    if (Number.isInteger(reunionId) && reunionId > 0) {
      this.openDetailById(reunionId);
    }
    this.membreService.getMonProfil().subscribe({
      next: membre => {
        this.userStatus = membre?.statut || '';
        this.userFunctionalRole = membre?.role || '';
        this.cdr.detectChanges();
      },
      error: error => console.error('Impossible de charger le profil pour les permissions de réunion', error)
    });
    this.membreService.getMembres().subscribe({
      next: membres => {
        this.membres = (membres || []).filter(membre =>
          membre.compteActif !== false
          && membre.compteGele !== true
          && ['PERMANENT', 'ASSOCIE'].includes(membre.statut)
        );
        this.cdr.detectChanges();
      },
      error: error => console.error('Impossible de charger la liste des membres', error)
    });
  }

  get isDoctorant(): boolean {
    return this.userStatus === 'DOCTORANT' || this.userRole === 'DOCTORANT';
  }

  get canPlan(): boolean {
    return !this.isDoctorant && (
      ['ADMIN', 'DIRECTEUR', 'VICE_DIRECTEUR'].includes(this.userRole)
      || ['ADMIN', 'DIRECTEUR', 'VICE_DIRECTEUR', 'CHEF_EQUIPE'].includes(this.userFunctionalRole)
    );
  }

  get canSeeArchives(): boolean {
    return ['ADMIN', 'DIRECTEUR', 'VICE_DIRECTEUR'].includes(this.userRole)
      || ['ADMIN', 'DIRECTEUR', 'VICE_DIRECTEUR'].includes(this.userFunctionalRole);
  }

  get teams(): string[] {
    return [...new Set(this.reunions.map(reunion => reunion.organisateur?.equipe)
      .filter((team): team is string => !!team))].sort();
  }

  initForm(reunion?: Reunion): void {
    this.selectedParticipantIds = reunion?.participants?.map(participant => participant.id) || [];
    if (!reunion && this.userId && !this.selectedParticipantIds.includes(this.userId)) {
      this.selectedParticipantIds.push(this.userId);
    }
    this.reunionForm = this.fb.group({
      titre: [reunion?.titre || '', [Validators.required, Validators.maxLength(200)]],
      ordreDuJour: [reunion?.ordreDuJour || '', Validators.required],
      dateDebut: [this.toInputDate(reunion?.dateDebut), Validators.required],
      dateFin: [this.toInputDate(reunion?.dateFin), Validators.required],
      lieu: [reunion?.lieu || '', [Validators.required, Validators.maxLength(255)]]
    }, { validators: form => {
      const start = parseLocalDateTime(form.get('dateDebut')?.value);
      const end = parseLocalDateTime(form.get('dateFin')?.value);
      return start && end && end < start ? { dateFinInvalide: true } : null;
    } });
  }

  loadReunions(): void {
    this.isLoading = true;
    const request = this.showArchived
      ? this.reunionService.getReunions(true)
      : this.onlyMine && this.userId
        ? this.reunionService.getReunionsDuMembre(this.userId)
        : this.reunionService.getReunions(false);
    request.pipe(
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: reunions => {
        this.reunions = reunions || [];
        this.applyFilters();
      },
      error: error => {
        console.error('Erreur chargement réunions', error);
        this.reunions = [];
        this.filteredReunions = [];
        this.successMessage = '';
      }
    });
  }

  applyFilters(): void {
    const now = new Date();
    this.filteredReunions = this.reunions.filter(reunion => {
      const start = parseLocalDateTime(reunion.dateDebut);
      const matchesStatus = this.statusFilter === 'TOUS' || reunion.statut === this.statusFilter;
      const matchesTeam = this.teamFilter === 'TOUTES' || reunion.organisateur?.equipe === this.teamFilter;
      const matchesPeriod = this.periodFilter === 'TOUTES'
        || (this.periodFilter === 'A_VENIR' && !!start && start >= now)
        || (this.periodFilter === 'PASSEES' && !!start && start < now);
      const matchesMine = !this.onlyMine
        || reunion.organisateur?.id === this.userId
        || reunion.participants?.some(participant => participant.id === this.userId);
      return matchesStatus && matchesTeam && matchesPeriod && matchesMine;
    });
  }

  toggleArchived(): void {
    this.showArchived = !this.showArchived;
    this.statusFilter = 'TOUS';
    this.loadReunions();
  }

  setMineOnly(enabled: boolean): void {
    this.onlyMine = enabled;
    this.loadReunions();
  }

  openDetail(reunion: Reunion): void {
    this.openDetailById(reunion.id, reunion);
  }

  private openDetailById(id: number, reunion?: Reunion): void {
    this.detailOpen = true;
    this.selectedReunion = reunion ?? null;
    this.pvError = '';
    this.pendingPvDocumentId = null;
    this.reunionService.getReunion(id).subscribe({
      next: detail => {
        this.selectedReunion = detail;
        this.cdr.detectChanges();
      },
      error: error => {
        this.detailOpen = false;
        this.setError(error, 'Impossible de charger le détail de cette réunion.');
      }
    });
  }

  closeDetail(): void {
    this.detailOpen = false;
    this.selectedReunion = null;
    this.pendingPvDocumentId = null;
    this.pvError = '';
  }

  openCreateForm(): void {
    this.formError = '';
    this.initForm();
    this.formOpen = true;
  }

  openEditForm(): void {
    if (!this.selectedReunion) return;
    this.formError = '';
    this.initForm(this.selectedReunion);
    this.formOpen = true;
  }

  closeForm(): void {
    this.formOpen = false;
    this.formError = '';
  }

  toggleParticipant(membreId: number, checked: boolean): void {
    this.selectedParticipantIds = checked
      ? [...new Set([...this.selectedParticipantIds, membreId])]
      : this.selectedParticipantIds.filter(id => id !== membreId);
  }

  saveReunion(): void {
    this.formError = '';
    if (this.reunionForm.invalid || this.selectedParticipantIds.length === 0) {
      this.reunionForm.markAllAsTouched();
      this.formError = this.selectedParticipantIds.length === 0
        ? 'Sélectionnez au moins un participant.'
        : 'Vérifiez les champs obligatoires et les dates.';
      return;
    }
    const request: ReunionRequest = {
      ...this.reunionForm.getRawValue(),
      participantsIds: this.selectedParticipantIds
    };
    const id = this.selectedReunion?.id;
    this.isSaving = true;
    const operation = id
      ? this.reunionService.modifier(id, request)
      : this.reunionService.planifier(request);
    operation.pipe(finalize(() => {
      this.isSaving = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: reunion => {
        this.formOpen = false;
        this.selectedReunion = reunion;
        this.successMessage = id ? 'Réunion modifiée.' : 'Réunion planifiée.';
        this.noticeError = false;
        this.loadReunions();
        if (id) this.openDetail(reunion);
      },
      error: error => this.setFormError(error)
    });
  }

  addParticipant(): void {
    const id = Number(this.participantToAdd);
    if (!this.selectedReunion || !id) return;
    this.runAction(this.reunionService.ajouterParticipant(this.selectedReunion.id, id), 'Participant ajouté.');
    this.participantToAdd = '';
  }

  removeParticipant(participant: ReunionMembre): void {
    if (!this.selectedReunion) return;
    this.runAction(
      this.reunionService.retirerParticipant(this.selectedReunion.id, participant.id),
      'Participant retiré.'
    );
  }

  changeStatus(status: StatutReunion): void {
    if (!this.selectedReunion) return;
    this.askConfirmation(
      'Changer le statut',
      `Confirmer le passage au statut « ${this.statusLabel(status)} » ?`,
      () => this.runAction(this.reunionService.changerStatut(this.selectedReunion!.id, status), 'Statut mis à jour.')
    );
  }

  archive(reunion: Reunion): void {
    this.askConfirmation('Archiver la réunion', 'La réunion et son PV seront conservés dans les archives.', () =>
      this.runAction(this.reunionService.archiver(reunion.id), 'Réunion archivée.')
    );
  }

  unarchive(reunion: Reunion): void {
    this.askConfirmation('Désarchiver la réunion', 'La réunion redeviendra visible dans la liste active.', () =>
      this.runAction(this.reunionService.desarchiver(reunion.id), 'Réunion désarchivée.')
    );
  }

  private askConfirmation(title: string, message: string, action: () => void): void {
    this.confirmTitle = title;
    this.confirmMessage = message;
    this.confirmAction = action;
    this.showActionConfirm = true;
  }

  executeConfirmedAction(): void {
    const action = this.confirmAction;
    this.showActionConfirm = false;
    this.confirmAction = null;
    action?.();
  }

  cancelConfirmation(): void {
    this.showActionConfirm = false;
    this.confirmAction = null;
  }

  private runAction(operation: import('rxjs').Observable<Reunion>, message: string): void {
    this.actionLoading = true;
    operation.pipe(finalize(() => {
      this.actionLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: reunion => {
        this.selectedReunion = reunion;
        this.successMessage = message;
        this.noticeError = false;
        this.loadReunions();
      },
      error: error => this.setError(error, 'Cette action n’a pas pu être effectuée.')
    });
  }

  onPvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedPvFile = input.files?.[0] || null;
    this.pvError = '';
  }

  uploadAndAttachPv(): void {
    if (!this.selectedReunion || !this.selectedPvFile) return;
    this.isUploadingPv = true;
    this.pvError = '';
    this.documentService.uploader(this.selectedPvFile, 'PV', null).pipe(
      finalize(() => {
        this.isUploadingPv = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: document => {
        this.pendingPvDocumentId = document.id;
        this.selectedPvFile = null;
        this.attachPendingPv();
      },
      error: error => this.setError(error, 'Le dépôt du PV a échoué.')
    });
  }

  attachPendingPv(): void {
    if (!this.selectedReunion || !this.pendingPvDocumentId) return;
    this.isAttachingPv = true;
    this.pvError = '';
    this.reunionService.attacherPv(this.selectedReunion.id, this.pendingPvDocumentId).pipe(
      finalize(() => {
        this.isAttachingPv = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: reunion => {
        this.selectedReunion = reunion;
        this.pendingPvDocumentId = null;
        this.successMessage = 'PV attaché à la réunion.';
        this.noticeError = false;
        this.loadReunions();
      },
      error: error => {
        this.pvError = `${this.errorMessage(error, 'Le document a été déposé, mais son attachement a échoué.')} Réessayez sans le déposer à nouveau.`;
      }
    });
  }

  downloadPv(id: number, filename: string): void {
    this.documentService.telecharger(id).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = filename;
        anchor.click();
        window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      error: error => this.setError(error, 'Le téléchargement du PV a échoué.')
    });
  }

  getAvailableParticipants(): Membre[] {
    const participantIds = new Set(this.selectedReunion?.participants?.map(member => member.id) || []);
    return this.membres.filter(member => !participantIds.has(member.id));
  }

  getStatusOptions(reunion: Reunion): StatutReunion[] {
    if (reunion.statut === 'PLANIFIEE') return ['EN_COURS', 'ANNULEE'];
    if (reunion.statut === 'EN_COURS') return ['TERMINEE', 'ANNULEE'];
    return [];
  }

  statusLabel(status: StatutReunion): string {
    return ({
      PLANIFIEE: 'Planifiée',
      EN_COURS: 'En cours',
      TERMINEE: 'Terminée',
      ANNULEE: 'Annulée',
      ARCHIVEE: 'Archivée'
    } as Record<StatutReunion, string>)[status];
  }

  formatRange(start?: string | null, end?: string | null): string {
    return formatDateTimeRange(start, end);
  }

  formatLong(value?: string | null): string {
    const date = parseLocalDateTime(value);
    return date ? new Intl.DateTimeFormat('fr-FR', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit'
    }).format(date) : 'Date à confirmer';
  }

  initials(member?: ReunionMembre | null): string {
    return `${member?.prenom?.charAt(0) || ''}${member?.nom?.charAt(0) || ''}`.toUpperCase() || '?';
  }

  fullName(member?: ReunionMembre | null): string {
    return `${member?.prenom || ''} ${member?.nom || ''}`.trim() || 'Membre';
  }

  canUploadPv(reunion: Reunion): boolean {
    return reunion.peutGererPv && !reunion.archivee
      && (reunion.statut === 'EN_COURS' || reunion.statut === 'TERMINEE');
  }

  canEdit(reunion: Reunion): boolean {
    return reunion.peutModifier && !reunion.archivee
      && (reunion.statut === 'PLANIFIEE' || reunion.statut === 'EN_COURS');
  }

  canManageParticipants(reunion: Reunion): boolean {
    return reunion.peutModifier && !reunion.archivee
      && (reunion.statut === 'PLANIFIEE' || reunion.statut === 'EN_COURS');
  }

  canRemoveParticipant(member: ReunionMembre): boolean {
    return !!this.selectedReunion && this.canManageParticipants(this.selectedReunion)
      && member.id !== this.selectedReunion.organisateur?.id;
  }

  private toInputDate(value?: string | null): string {
    const date = parseLocalDateTime(value);
    if (!date) return '';
    const pad = (part: number) => part.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  private setFormError(error: any): void {
    const fields = error?.error?.fields || error?.error?.errors;
    if (fields && typeof fields === 'object') {
      this.formError = Object.entries(fields).map(([field, message]) => `${field} : ${message}`).join(' · ');
    } else {
      this.formError = this.errorMessage(error, 'La réunion n’a pas pu être enregistrée.');
    }
  }

  private setError(error: any, fallback: string): void {
    this.successMessage = this.errorMessage(error, fallback);
    this.noticeError = true;
    this.cdr.detectChanges();
  }

  private errorMessage(error: any, fallback: string): string {
    return error?.error?.message || error?.error?.detail || fallback;
  }
}
