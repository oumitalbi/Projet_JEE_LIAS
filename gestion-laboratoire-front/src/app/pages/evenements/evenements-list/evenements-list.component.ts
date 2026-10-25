import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { Observable, concat, finalize, last, of } from 'rxjs';
import { Router } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { Evenement, EvenementRequest, StatutEvenement, TypeEvenement } from '../../../core/models/evenement.model';
import { Membre } from '../../../core/models/membre.model';
import { AuthService } from '../../../core/services/auth.service';
import { EvenementService } from '../../../core/services/evenement.service';
import { MembreService } from '../../../core/services/membre.service';

type StatutFiltre = 'TOUS' | StatutEvenement;

@Component({
  selector: 'app-evenements-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './evenements-list.component.html',
  styleUrl: './evenements-list.component.scss'
})
export class EvenementsListComponent implements OnInit {
  evenements: Evenement[] = [];
  evenementsCharges: Evenement[] = [];
  membres: Membre[] = [];
  selectedEvenement: Evenement | null = null;
  selectedType: TypeEvenement | 'TOUS' = 'TOUS';
  selectedStatut: StatutFiltre = 'TOUS';
  selectedAnnee = 'TOUTES';
  selectedEdition = 'TOUTES';
  mesEvenements = false;
  canViewCancelled = false;
  isLoading = false;
  isDetailLoading = false;
  loadError = false;
  isDetailOpen = false;
  isFormOpen = false;
  isSaving = false;
  isCancelling = false;
  isRegistering = false;
  isManagingOrganisateurs = false;
  isSavingOrganisateurs = false;
  confirmerAnnulation = false;
  motifAnnulation = '';
  formError = '';
  fieldErrors: Record<string, string> = {};
  actionError = '';
  organizerManagerError = '';
  managedOrganisateurIds: number[] = [];
  showSuccessToast = false;
  currentUserId: number | null = null;
  eventForm!: FormGroup;
  selectedOrganisateurIds: number[] = [];
  organisateurSearch = '';
  modeForm: 'create' | 'edit' = 'create';

  readonly typesEvenement: { value: TypeEvenement; label: string; badgeClass: string }[] = [
    { value: 'CONFERENCE', label: 'Conférence', badgeClass: 'badge-blue' },
    { value: 'SEMINAIRE', label: 'Séminaire', badgeClass: 'badge-purple' },
    { value: 'WORKSHOP', label: 'Workshop', badgeClass: 'badge-orange' }
  ];
  readonly statutsEvenement: { value: StatutEvenement; label: string; badgeClass: string }[] = [
    { value: 'A_VENIR', label: 'À venir', badgeClass: 'badge-green' },
    { value: 'EN_COURS', label: 'En cours', badgeClass: 'badge-blue' },
    { value: 'PASSE', label: 'Passé', badgeClass: 'badge-orange' },
    { value: 'ANNULE', label: 'Annulé', badgeClass: 'badge-gray' }
  ];

  constructor(
    private evenementService: EvenementService,
    private membreService: MembreService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.authService.getUserId();
    this.eventForm = this.fb.group({
      titre: ['', [Validators.required, Validators.maxLength(200)]],
      description: [''],
      type: ['', Validators.required],
      dateDebut: ['', Validators.required],
      dateFin: ['', Validators.required],
      lieu: ['', Validators.required],
      edition: ['']
    }, { validators: this.dateRangeValidator });
    this.loadEvenements();
    this.loadMembres();
    this.checkCancelledAccess();
    const evenementId = Number(this.route.snapshot.queryParamMap.get('evenementId'));
    if (Number.isInteger(evenementId) && evenementId > 0) {
      this.openDetailById(evenementId);
    }
  }

  get currentMembre(): Membre | undefined {
    return this.membres.find(membre => membre?.id === this.currentUserId);
  }

  canOpenDiscussion(evenement: Evenement): boolean {
    const member = this.currentMembre;
    if (!member) return false;
    const supervisor = ['ADMIN', 'DIRECTEUR', 'VICE_DIRECTEUR'].includes(member.role || '')
      || member.roleSysteme === 'ADMIN';
    return supervisor
      || evenement?.organisateurs?.some(person => person?.id === this.currentUserId) === true
      || evenement?.participants?.some(person => person?.id === this.currentUserId) === true;
  }

  openDiscussion(evenement: Evenement): void {
    this.router.navigate(['/dashboard/messagerie'], { queryParams: { evenementId: evenement.id } });
  }

  get canCreate(): boolean {
    const membre = this.currentMembre;
    if (!membre || membre.compteActif === false || membre.compteGele === true) return false;
    const role = membre.roleSysteme || membre.role || this.authService.getUserRole();
    return membre.statut === 'PERMANENT'
      || ['ADMIN', 'DIRECTEUR'].includes(role);
  }

  get canRegister(): boolean {
    const membre = this.currentMembre;
    return !!membre && membre.compteActif !== false && membre.compteGele !== true
      && ['PERMANENT', 'ASSOCIE'].includes(membre.statut);
  }

  get anneesDisponibles(): number[] {
    return [...new Set(this.evenementsCharges
      .map(evenement => evenement.annee ?? this.parseDate(evenement.dateDebut)?.getFullYear())
      .filter((annee): annee is number => typeof annee === 'number'))]
      .sort((a, b) => b - a);
  }

  get editionsDisponibles(): string[] {
    return [...new Set(this.evenementsCharges
      .map(evenement => evenement.edition?.trim())
      .filter((edition): edition is string => !!edition))]
      .sort((a, b) => a.localeCompare(b, 'fr'));
  }

  private checkCancelledAccess(): void {
    this.evenementService.getEvenementsAnnules().subscribe({
      next: () => this.canViewCancelled = true,
      error: error => {
        if (error?.status !== 403) {
          console.error('Erreur lors de la vérification des droits sur les événements annulés', error);
        }
        this.canViewCancelled = false;
      }
    });
  }

  loadEvenements(): void {
    this.isLoading = true;
    this.loadError = false;
    let request;
    if (this.selectedStatut === 'ANNULE') {
      request = this.evenementService.getEvenementsAnnules();
    } else if (this.mesEvenements && this.currentUserId !== null) {
      request = this.evenementService.getEvenementsParParticipant(this.currentUserId);
    } else if (this.selectedStatut === 'A_VENIR') {
      request = this.evenementService.getEvenementsAVenir();
    } else {
      request = this.evenementService.getEvenements();
    }

    request.pipe(finalize(() => {
      this.isLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: data => {
        this.evenementsCharges = Array.isArray(data)
          ? data.filter(evenement => evenement !== null && typeof evenement?.id === 'number')
          : [];
        this.appliquerFiltres();
      },
      error: error => {
        console.error('Erreur lors du chargement des événements', error);
        this.loadError = true;
        this.evenementsCharges = [];
        this.evenements = [];
      }
    });
  }

  loadMembres(): void {
    this.membreService.getMembres().subscribe({
      next: data => {
        this.membres = Array.isArray(data) ? data.filter(membre => !!membre) : [];
      },
      error: error => console.error('Erreur lors du chargement des membres', error)
    });
  }

  setTypeFilter(type: TypeEvenement | 'TOUS'): void {
    this.selectedType = type;
    this.appliquerFiltres();
  }

  setStatutFilter(statut: StatutFiltre): void {
    this.selectedStatut = statut;
    this.loadEvenements();
  }

  setAnneeFilter(annee: string): void {
    this.selectedAnnee = annee;
    this.appliquerFiltres();
  }

  setEditionFilter(edition: string): void {
    this.selectedEdition = edition;
    this.appliquerFiltres();
  }

  setMesEvenements(value: boolean): void {
    this.mesEvenements = value;
    this.loadEvenements();
  }

  private appliquerFiltres(): void {
    this.evenements = this.evenementsCharges.filter(evenement => {
      const typeMatch = this.selectedType === 'TOUS' || evenement.type === this.selectedType;
      const statutMatch = this.selectedStatut === 'TOUS' || evenement.statut === this.selectedStatut;
      const annee = evenement.annee ?? this.parseDate(evenement.dateDebut)?.getFullYear();
      const anneeMatch = this.selectedAnnee === 'TOUTES' || annee === Number(this.selectedAnnee);
      const editionMatch = this.selectedEdition === 'TOUTES'
        || evenement.edition?.trim() === this.selectedEdition;
      const myMatch = !this.mesEvenements || evenement.participants?.some(
        participant => participant?.id === this.currentUserId
      ) === true;
      return typeMatch && statutMatch && anneeMatch && editionMatch && myMatch;
    });
  }

  getTypeLabel(type: TypeEvenement | null | undefined): string {
    return this.typesEvenement.find(option => option.value === type)?.label || 'Événement';
  }

  getTypeBadgeClass(type: TypeEvenement | null | undefined): string {
    return this.typesEvenement.find(option => option.value === type)?.badgeClass || 'badge-gray';
  }

  getStatusLabel(status: StatutEvenement | null | undefined): string {
    return this.statutsEvenement.find(option => option.value === status)?.label || 'Statut indisponible';
  }

  getStatusBadgeClass(status: StatutEvenement | null | undefined): string {
    return this.statutsEvenement.find(option => option.value === status)?.badgeClass || 'badge-gray';
  }

  getInitials(membre: Pick<Membre, 'prenom' | 'nom'> | null | undefined): string {
    const initials = `${membre?.prenom?.trim()?.charAt(0) || ''}${membre?.nom?.trim()?.charAt(0) || ''}`;
    return initials || '?';
  }

  getFullName(membre: Pick<Membre, 'prenom' | 'nom'> | null | undefined): string {
    return `${membre?.prenom || ''} ${membre?.nom || ''}`.trim() || 'Membre';
  }

  getDateRange(evenement: Evenement): string {
    const debut = this.parseDate(evenement?.dateDebut);
    const fin = this.parseDate(evenement?.dateFin);
    if (!debut || !fin) return 'Date à confirmer';

    const debutDate = this.formatDate(debut);
    const finDate = this.formatDate(fin);
    const debutHeure = this.formatTime(debut);
    const finHeure = this.formatTime(fin);
    if (debut.toDateString() === fin.toDateString()) {
      return `${debutDate} · ${debutHeure} – ${finHeure}`;
    }
    if (debut.getMonth() === fin.getMonth() && debut.getFullYear() === fin.getFullYear()) {
      return `Du ${debut.getDate()} au ${finDate}`;
    }
    return `Du ${debutDate} au ${finDate}`;
  }

  getLongDateRange(evenement: Evenement): string {
    const debut = this.parseDate(evenement?.dateDebut);
    const fin = this.parseDate(evenement?.dateFin);
    if (!debut || !fin) return 'Date à confirmer';
    return `${this.formatLongDate(debut)} · ${this.formatTime(debut)} – ${this.formatLongDate(fin)} · ${this.formatTime(fin)}`;
  }

  private parseDate(value: string | null | undefined): Date | null {
    if (!value) return null;
    const localDate = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(value);
    const date = localDate
      ? new Date(
        Number(localDate[1]), Number(localDate[2]) - 1, Number(localDate[3]),
        Number(localDate[4]), Number(localDate[5]), Number(localDate[6] || 0)
      )
      : new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  private formatDate(date: Date): string {
    return new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }).format(date);
  }

  private formatLongDate(date: Date): string {
    return new Intl.DateTimeFormat('fr-FR', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    }).format(date);
  }

  private formatTime(date: Date): string {
    return new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit', hour12: false }).format(date);
  }

  openDetail(evenement: Evenement): void {
    this.openDetailById(evenement.id, evenement);
  }

  private openDetailById(id: number, evenement?: Evenement): void {
    this.selectedEvenement = evenement ?? {
      id,
      titre: '',
      type: 'CONFERENCE',
      dateDebut: null,
      dateFin: null,
      lieu: '',
      organisateurs: [],
      participants: []
    };
    this.confirmerAnnulation = false;
    this.motifAnnulation = '';
    this.actionError = '';
    this.isDetailOpen = true;
    this.isDetailLoading = true;
    this.evenementService.getEvenement(id)
      .pipe(finalize(() => {
        this.isDetailLoading = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: detail => this.selectedEvenement = detail,
        error: error => {
          console.error('Erreur lors du chargement du détail de l’événement', error);
          this.actionError = 'Le détail de cet événement ne peut pas être chargé.';
        }
      });
  }

  closeDetail(): void {
    this.isDetailOpen = false;
    this.selectedEvenement = null;
    this.confirmerAnnulation = false;
    this.actionError = '';
  }

  startEdit(): void {
    if (!this.selectedEvenement?.peutModifier) return;
    this.openForm('edit', this.selectedEvenement);
  }

  startManageOrganisateurs(): void {
    if (!this.selectedEvenement?.peutModifier) return;
    this.managedOrganisateurIds = (this.selectedEvenement.organisateurs || [])
      .map(organisateur => organisateur.id);
    this.organizerManagerError = '';
    this.isManagingOrganisateurs = true;
  }

  toggleManagedOrganisateur(id: number): void {
    this.managedOrganisateurIds = this.managedOrganisateurIds.includes(id)
      ? this.managedOrganisateurIds.filter(selectedId => selectedId !== id)
      : [...this.managedOrganisateurIds, id];
  }

  saveOrganisateurManagement(): void {
    const evenement = this.selectedEvenement;
    if (!evenement?.peutModifier || this.managedOrganisateurIds.length === 0 || this.isSavingOrganisateurs) {
      this.organizerManagerError = 'Un événement doit conserver au moins un organisateur.';
      return;
    }
    const existingIds = (evenement.organisateurs || []).map(organisateur => organisateur.id);
    const toAdd = this.managedOrganisateurIds.filter(id => !existingIds.includes(id));
    const toRemove = existingIds.filter(id => !this.managedOrganisateurIds.includes(id));
    const requests: Observable<Evenement>[] = [
      ...toAdd.map(id => this.evenementService.affecterOrganisateur(evenement.id, id)),
      ...toRemove.map(id => this.evenementService.retirerOrganisateur(evenement.id, id))
    ];
    this.isSavingOrganisateurs = true;
    this.organizerManagerError = '';
    const request$ = requests.length ? concat(...requests).pipe(last()) : of(evenement);
    request$.pipe(finalize(() => {
      this.isSavingOrganisateurs = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: updated => {
        this.selectedEvenement = updated;
        this.isManagingOrganisateurs = false;
        this.loadEvenements();
      },
      error: error => {
        console.error('Erreur lors de la gestion des organisateurs', error);
        this.organizerManagerError = error?.error?.message || 'La liste des organisateurs n’a pas pu être mise à jour.';
        this.evenementService.getEvenement(evenement.id).subscribe({
          next: updated => this.selectedEvenement = updated,
          error: refreshError => console.error('Impossible de rafraîchir les organisateurs après erreur', refreshError)
        });
      }
    });
  }

  openForm(mode: 'create' | 'edit' = 'create', evenement?: Evenement): void {
    if (mode === 'create' && !this.canCreate) return;
    this.modeForm = mode;
    this.eventForm.reset({
      titre: evenement?.titre || '',
      description: evenement?.description || '',
      type: evenement?.type || '',
      dateDebut: this.toDateInput(evenement?.dateDebut),
      dateFin: this.toDateInput(evenement?.dateFin),
      lieu: evenement?.lieu || '',
      edition: evenement?.edition || ''
    });
    this.selectedOrganisateurIds = mode === 'edit'
      ? (evenement?.organisateurs || []).map(organisateur => organisateur.id)
      : this.currentUserId === null ? [] : [this.currentUserId];
    this.organisateurSearch = '';
    this.formError = '';
    this.fieldErrors = {};
    this.isFormOpen = true;
  }

  private toDateInput(value: string | null | undefined): string {
    const date = this.parseDate(value);
    if (!date) return '';
    const pad = (part: number) => String(part).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  closeForm(): void {
    if (!this.isSaving) this.isFormOpen = false;
  }

  get filteredMembres(): Membre[] {
    const query = this.organisateurSearch.trim().toLowerCase();
    return this.membres.filter(membre => {
      const eligible = membre.compteActif !== false && membre.compteGele !== true
        && !['DOCTORANT', 'RETRAITE', 'ANCIEN'].includes(membre.statut);
      const name = `${membre.prenom || ''} ${membre.nom || ''}`.toLowerCase();
      return eligible && (!query || name.includes(query));
    });
  }

  isOrganisateurSelected(id: number): boolean {
    return this.selectedOrganisateurIds.includes(id);
  }

  toggleOrganisateur(id: number): void {
    this.selectedOrganisateurIds = this.isOrganisateurSelected(id)
      ? this.selectedOrganisateurIds.filter(selectedId => selectedId !== id)
      : [...this.selectedOrganisateurIds, id];
  }

  getSelectedOrganisateurs(): Membre[] {
    return this.membres.filter(membre => this.selectedOrganisateurIds.includes(membre.id));
  }

  removeOrganisateur(id: number): void {
    this.selectedOrganisateurIds = this.selectedOrganisateurIds.filter(selectedId => selectedId !== id);
  }

  fieldError(field: string): string | null {
    return this.fieldErrors[field] || null;
  }

  submitEvenement(): void {
    this.eventForm.markAllAsTouched();
    if (this.eventForm.invalid || this.selectedOrganisateurIds.length === 0 || this.isSaving) return;

    const value = this.eventForm.getRawValue();
    const request: EvenementRequest = {
      titre: value.titre.trim(),
      description: value.description || '',
      type: value.type,
      dateDebut: this.formatDateTimeForApi(value.dateDebut),
      dateFin: this.formatDateTimeForApi(value.dateFin),
      lieu: value.lieu.trim(),
      edition: value.edition?.trim() || null,
      organisateursIds: [...this.selectedOrganisateurIds]
    };
    const evenementId = this.selectedEvenement?.id;
    if (this.modeForm === 'edit' && evenementId === undefined) return;

    this.isSaving = true;
    this.formError = '';
    this.fieldErrors = {};
    const request$ = this.modeForm === 'edit'
      ? this.evenementService.modifierEvenement(evenementId!, request)
      : this.evenementService.creerEvenement(request);

    request$.pipe(finalize(() => {
      this.isSaving = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: () => {
        this.isFormOpen = false;
        this.isDetailOpen = false;
        this.selectedEvenement = null;
        this.showToast(this.modeForm === 'edit'
          ? 'L’événement a été modifié.'
          : 'L’événement a été créé.');
        this.loadEvenements();
      },
      error: error => {
        console.error('Erreur lors de l’enregistrement de l’événement', error);
        this.mapBackendErrors(error);
      }
    });
  }

  private dateRangeValidator(control: AbstractControl): ValidationErrors | null {
    const debut = control.get('dateDebut')?.value;
    const fin = control.get('dateFin')?.value;
    return debut && fin && new Date(fin).getTime() < new Date(debut).getTime()
      ? { dateFinAvantDebut: true }
      : null;
  }

  private formatDateTimeForApi(value: string): string {
    return value.length === 16 ? `${value}:00` : value;
  }

  private mapBackendErrors(error: { error?: { message?: string; fields?: Record<string, string> } }): void {
    const fields = error?.error?.fields;
    if (fields && typeof fields === 'object') {
      this.fieldErrors = fields;
    }
    this.formError = error?.error?.message || 'L’enregistrement a échoué. Vérifiez les informations puis réessayez.';
  }

  beginAnnulation(): void {
    this.confirmerAnnulation = true;
    this.motifAnnulation = '';
    this.actionError = '';
  }

  confirmAnnulation(): void {
    if (!this.selectedEvenement?.peutAnnuler || !this.motifAnnulation.trim() || this.isCancelling) return;
    this.isCancelling = true;
    this.actionError = '';
    this.evenementService.annulerEvenement(this.selectedEvenement.id, this.motifAnnulation.trim())
      .pipe(finalize(() => {
        this.isCancelling = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: () => {
          this.closeDetail();
          this.showToast('L’événement a été annulé.');
          this.loadEvenements();
        },
        error: error => {
          console.error('Erreur lors de l’annulation de l’événement', error);
          this.actionError = error?.error?.message || 'L’annulation a échoué. Vérifiez vos droits ou réessayez.';
        }
      });
  }

  toggleInscription(): void {
    const evenement = this.selectedEvenement;
    if (!evenement || !this.canRegister || this.isRegistering) return;
    this.isRegistering = true;
    this.actionError = '';
    const request = evenement.estInscrit
      ? this.evenementService.desinscrire(evenement.id)
      : this.evenementService.inscrire(evenement.id);
    request.pipe(finalize(() => {
      this.isRegistering = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: updated => {
        this.selectedEvenement = updated;
        this.loadEvenements();
      },
      error: error => {
        console.error('Erreur lors de la modification de l’inscription', error);
        this.actionError = error?.error?.message || 'Votre inscription n’a pas pu être modifiée.';
      }
    });
  }

  private showToast(message: string): void {
    this.showSuccessToast = true;
    this.successMessage = message;
    setTimeout(() => {
      this.showSuccessToast = false;
      this.cdr.detectChanges();
    }, 3000);
  }

  successMessage = '';
}
