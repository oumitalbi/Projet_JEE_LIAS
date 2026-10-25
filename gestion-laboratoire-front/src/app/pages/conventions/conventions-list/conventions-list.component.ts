import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import {
  Convention,
  ConventionRequest,
  Partenaire,
  PartenaireRequest,
  StatutConvention,
  TypePartenaire
} from '../../../core/models/convention.model';
import { Evenement } from '../../../core/models/evenement.model';
import { ConventionService } from '../../../core/services/convention.service';
import { DocumentService } from '../../../core/services/document.service';
import { EvenementService } from '../../../core/services/evenement.service';
import { MembreService } from '../../../core/services/membre.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-conventions-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DatePipe, RouterLink],
  templateUrl: './conventions-list.component.html',
  styleUrl: './conventions-list.component.scss'
})
export class ConventionsListComponent implements OnInit {
  conventions: Convention[] = [];
  filteredConventions: Convention[] = [];
  partenaires: Partenaire[] = [];
  evenements: Evenement[] = [];
  selectedConvention: Convention | null = null;
  conventionForm!: FormGroup;
  partenaireForm!: FormGroup;
  statutFilter = 'TOUS';
  partenaireFilter = 'TOUS';
  anneeFilter = 'TOUTES';
  archived = false;
  detailOpen = false;
  formOpen = false;
  partnerFormOpen = false;
  isLoading = false;
  isSaving = false;
  isUploading = false;
  isAttaching = false;
  isActionLoading = false;
  selectedFile: File | null = null;
  pendingDocumentId: number | null = null;
  selectedActivityId = '';
  errorMessage = '';
  notice = '';
  noticeIsError = false;
  uploadError = '';
  showConfirmation = false;
  confirmationTitle = '';
  confirmationText = '';
  confirmationAction: (() => void) | null = null;
  isDirector = false;
  isManager = false;
  userStatus = '';

  readonly partnerTypes: { value: TypePartenaire; label: string }[] = [
    { value: 'ACADEMIQUE', label: 'Académique' },
    { value: 'INDUSTRIEL', label: 'Industriel' },
    { value: 'INSTITUTIONNEL', label: 'Institutionnel' }
  ];

  constructor(
    private readonly conventionService: ConventionService,
    private readonly documentService: DocumentService,
    private readonly evenementService: EvenementService,
    private readonly membreService: MembreService,
    private readonly authService: AuthService,
    private readonly fb: FormBuilder,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loadConventions();
    this.loadPartners();
    this.membreService.getMonProfil().subscribe({
      next: member => {
        this.userStatus = member?.statut || '';
        const functionalRole = member?.role || '';
        const systemRole = member?.roleSysteme || this.authService.getUserRole();
        this.isDirector = functionalRole === 'DIRECTEUR' || systemRole === 'DIRECTEUR';
        this.isManager = this.isDirector
          || functionalRole === 'VICE_DIRECTEUR'
          || functionalRole === 'ADMIN'
          || ['ADMIN', 'VICE_DIRECTEUR'].includes(systemRole);
        this.cdr.detectChanges();
      },
      error: error => {
        console.error('Impossible de charger le profil utilisateur des conventions', error);
        this.userStatus = 'INCONNU';
      }
    });
    this.evenementService.getEvenements().subscribe({
      next: events => {
        this.evenements = (events || []).filter(event => event.statut !== 'ANNULE');
        this.cdr.detectChanges();
      },
      error: error => console.error('Impossible de charger les événements associés', error)
    });
  }

  get years(): string[] {
    return [...new Set(this.conventions
      .map(convention => convention.dateSignature?.slice(0, 4))
      .filter((year): year is string => !!year))].sort((a, b) => Number(b) - Number(a));
  }

  get availableActivities(): Evenement[] {
    const linkedIds = new Set(this.selectedConvention?.activites.map(activity => activity.id) || []);
    return this.evenements.filter(event => !linkedIds.has(event.id));
  }

  private initForms(convention?: Convention): void {
    this.conventionForm = this.fb.group({
      titre: [convention?.titre || '', [Validators.required, Validators.maxLength(200)]],
      description: [convention?.description || ''],
      dateSignature: [convention?.dateSignature || '', Validators.required],
      dateExpiration: [convention?.dateExpiration || '', Validators.required],
      partenaireId: [convention?.partenaire?.id || '', Validators.required]
    }, { validators: form => {
      const start = form.get('dateSignature')?.value;
      const end = form.get('dateExpiration')?.value;
      return start && end && end < start ? { invalidPeriod: true } : null;
    } });

    this.partenaireForm = this.fb.group({
      nom: ['', [Validators.required, Validators.maxLength(200)]],
      type: ['', Validators.required],
      pays: ['', Validators.maxLength(120)],
      contactEmail: ['', [Validators.email, Validators.maxLength(255)]]
    });
  }

  loadConventions(): void {
    this.isLoading = true;
    this.conventionService.getConventions({ archives: this.archived }).pipe(
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: conventions => {
        this.conventions = conventions || [];
        this.applyFilters();
      },
      error: error => {
        this.conventions = [];
        this.filteredConventions = [];
        this.showError(error, 'Impossible de charger les conventions.');
      }
    });
  }

  loadPartners(): void {
    this.conventionService.getPartners().subscribe({
      next: partners => {
        this.partenaires = partners || [];
        this.cdr.detectChanges();
      },
      error: error => {
        console.error('Impossible de charger les partenaires', error);
        this.showError(error, 'Impossible de charger la liste des partenaires.');
      }
    });
  }

  applyFilters(): void {
    this.filteredConventions = this.conventions.filter(convention =>
      (this.statutFilter === 'TOUS' || convention.statut === this.statutFilter)
      && (this.partenaireFilter === 'TOUS' || convention.partenaire?.id.toString() === this.partenaireFilter)
      && (this.anneeFilter === 'TOUTES' || convention.dateSignature?.slice(0, 4) === this.anneeFilter)
    );
  }

  toggleArchives(): void {
    this.archived = !this.archived;
    this.statutFilter = 'TOUS';
    this.loadConventions();
  }

  openDetail(convention: Convention): void {
    this.detailOpen = true;
    this.selectedConvention = convention;
    this.pendingDocumentId = null;
    this.uploadError = '';
    this.conventionService.getConvention(convention.id).subscribe({
      next: detail => {
        this.selectedConvention = detail;
        this.cdr.detectChanges();
      },
      error: error => {
        this.detailOpen = false;
        this.showError(error, 'Impossible de charger le détail de cette convention.');
      }
    });
  }

  closeDetail(): void {
    this.detailOpen = false;
    this.selectedConvention = null;
    this.pendingDocumentId = null;
  }

  openCreateForm(): void {
    this.errorMessage = '';
    this.initForms();
    this.formOpen = true;
  }

  openEditForm(): void {
    if (!this.selectedConvention) return;
    this.errorMessage = '';
    this.initForms(this.selectedConvention);
    this.formOpen = true;
  }

  closeForm(): void {
    this.formOpen = false;
    this.errorMessage = '';
  }

  saveConvention(): void {
    if (this.conventionForm.invalid) {
      this.conventionForm.markAllAsTouched();
      this.errorMessage = 'Vérifiez les champs obligatoires et la période de validité.';
      return;
    }
    const value = this.conventionForm.getRawValue();
    const request: ConventionRequest = {
      titre: value.titre.trim(),
      description: value.description?.trim() || '',
      dateSignature: value.dateSignature,
      dateExpiration: value.dateExpiration,
      partenaireId: Number(value.partenaireId)
    };
    const id = this.selectedConvention?.id;
    this.isSaving = true;
    const operation = id
      ? this.conventionService.updateConvention(id, request)
      : this.conventionService.createConvention(request);
    operation.pipe(finalize(() => {
      this.isSaving = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: convention => {
        this.formOpen = false;
        this.selectedConvention = convention;
        this.notice = id ? 'Convention modifiée.' : 'Convention créée.';
        this.noticeIsError = false;
        this.loadConventions();
        if (id) this.openDetail(convention);
      },
      error: error => this.showFormError(error)
    });
  }

  openPartnerForm(): void {
    this.partenaireForm.reset();
    this.partnerFormOpen = true;
    this.errorMessage = '';
  }

  closePartnerForm(): void {
    this.partnerFormOpen = false;
    this.errorMessage = '';
  }

  savePartner(): void {
    if (this.partenaireForm.invalid) {
      this.partenaireForm.markAllAsTouched();
      this.errorMessage = 'Complétez le nom et le type du partenaire.';
      return;
    }
    const value = this.partenaireForm.getRawValue();
    const request: PartenaireRequest = {
      nom: value.nom.trim(),
      type: value.type,
      pays: value.pays?.trim() || '',
      contactEmail: value.contactEmail?.trim() || ''
    };
    this.isSaving = true;
    this.conventionService.addPartner(request).pipe(finalize(() => {
      this.isSaving = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: partner => {
        this.partenaires = [...this.partenaires, partner].sort((a, b) => a.nom.localeCompare(b.nom));
        this.conventionForm.patchValue({ partenaireId: partner.id });
        this.partnerFormOpen = false;
        this.notice = 'Partenaire ajouté.';
        this.noticeIsError = false;
      },
      error: error => this.showFormError(error)
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] || null;
    this.uploadError = '';
  }

  uploadAndAttachDocument(): void {
    if (!this.selectedConvention || !this.selectedFile) return;
    if (this.selectedFile.type !== 'application/pdf' && !this.selectedFile.name.toLowerCase().endsWith('.pdf')) {
      this.uploadError = 'Le document de convention doit être un fichier PDF.';
      return;
    }
    this.isUploading = true;
    this.uploadError = '';
    this.documentService.uploader(this.selectedFile, 'CONVENTION', null).pipe(
      finalize(() => {
        this.isUploading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: document => {
        this.pendingDocumentId = document.id;
        this.selectedFile = null;
        this.attachPendingDocument();
      },
      error: error => this.showError(error, 'Le dépôt du document a échoué.')
    });
  }

  attachPendingDocument(): void {
    if (!this.selectedConvention || !this.pendingDocumentId) return;
    this.isAttaching = true;
    this.uploadError = '';
    this.conventionService.attachDocument(this.selectedConvention.id, this.pendingDocumentId).pipe(
      finalize(() => {
        this.isAttaching = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: convention => {
        this.selectedConvention = convention;
        this.pendingDocumentId = null;
        this.notice = 'Document attaché à la convention.';
        this.noticeIsError = false;
        this.loadConventions();
      },
      error: error => {
        this.uploadError = `${this.messageFrom(error, 'Le document a été déposé, mais son attachement a échoué.')} Vous pouvez réessayer sans le déposer à nouveau.`;
      }
    });
  }

  downloadDocument(documentId: number, filename: string): void {
    this.documentService.telecharger(documentId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        link.click();
        window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      error: error => this.showError(error, 'Le téléchargement du document a échoué.')
    });
  }

  addActivity(): void {
    const eventId = Number(this.selectedActivityId);
    if (!this.selectedConvention || !eventId) return;
    this.runAction(
      this.conventionService.addActivity(this.selectedConvention.id, eventId),
      'Activité associée.'
    );
    this.selectedActivityId = '';
  }

  removeActivity(eventId: number): void {
    if (!this.selectedConvention) return;
    this.runAction(
      this.conventionService.removeActivity(this.selectedConvention.id, eventId),
      'Lien d’activité retiré.'
    );
  }

  archive(convention: Convention): void {
    this.confirm('Archiver la convention', 'Elle restera conservée dans les archives institutionnelles.', () =>
      this.runAction(this.conventionService.archive(convention.id), 'Convention archivée.')
    );
  }

  restore(convention: Convention): void {
    this.confirm('Restaurer la convention', 'Elle redeviendra disponible dans la liste courante.', () =>
      this.runAction(this.conventionService.restore(convention.id), 'Convention restaurée.')
    );
  }

  archivePartner(partner: Partenaire): void {
    this.confirm('Archiver le partenaire', 'Le partenaire sera conservé et ne pourra plus être choisi pour une nouvelle convention.', () => {
      this.isActionLoading = true;
      this.conventionService.archivePartner(partner.id).pipe(finalize(() => {
        this.isActionLoading = false;
        this.cdr.detectChanges();
      })).subscribe({
        next: () => {
          this.partenaires = this.partenaires.filter(item => item.id !== partner.id);
          this.notice = 'Partenaire archivé.';
          this.noticeIsError = false;
        },
        error: error => this.showError(error, 'Le partenaire n’a pas pu être archivé.')
      });
    });
  }

  private runAction(operation: import('rxjs').Observable<Convention>, success: string): void {
    this.isActionLoading = true;
    operation.pipe(finalize(() => {
      this.isActionLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: convention => {
        this.selectedConvention = convention;
        this.notice = success;
        this.noticeIsError = false;
        this.loadConventions();
      },
      error: error => this.showError(error, 'Cette action n’a pas pu être effectuée.')
    });
  }

  private confirm(title: string, text: string, action: () => void): void {
    this.confirmationTitle = title;
    this.confirmationText = text;
    this.confirmationAction = action;
    this.showConfirmation = true;
  }

  executeConfirmation(): void {
    const action = this.confirmationAction;
    this.showConfirmation = false;
    this.confirmationAction = null;
    action?.();
  }

  cancelConfirmation(): void {
    this.showConfirmation = false;
    this.confirmationAction = null;
  }

  statusLabel(status: StatutConvention): string {
    return ({
      BROUILLON: 'Brouillon',
      A_VENIR: 'À venir',
      ACTIVE: 'Active',
      EXPIREE: 'Expirée',
      ARCHIVEE: 'Archivée'
    } as Record<StatutConvention, string>)[status];
  }

  partnerTypeLabel(type?: string | null): string {
    return this.partnerTypes.find(option => option.value === type)?.label || 'Partenaire';
  }

  formatDate(value?: string | null): string {
    if (!value) return 'Date à confirmer';
    const date = new Date(`${value}T00:00:00`);
    return Number.isNaN(date.getTime())
      ? 'Date à confirmer'
      : new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }).format(date);
  }

  private showFormError(error: any): void {
    const fields = error?.error?.fields || error?.error?.errors;
    this.errorMessage = fields && typeof fields === 'object'
      ? Object.entries(fields).map(([field, value]) => `${field} : ${value}`).join(' · ')
      : this.messageFrom(error, 'La requête n’a pas pu être enregistrée.');
  }

  private showError(error: any, fallback: string): void {
    this.notice = this.messageFrom(error, fallback);
    this.noticeIsError = true;
    this.cdr.detectChanges();
  }

  private messageFrom(error: any, fallback: string): string {
    return error?.error?.message || error?.error?.detail || fallback;
  }
}
