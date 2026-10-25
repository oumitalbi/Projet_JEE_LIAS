import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { DocumentArchive, TypeDocument } from '../../../core/models/document.model';
import { Evenement } from '../../../core/models/evenement.model';
import { Membre } from '../../../core/models/membre.model';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentService } from '../../../core/services/document.service';
import { EvenementService } from '../../../core/services/evenement.service';
import { MembreService } from '../../../core/services/membre.service';

interface DocumentGroup {
  eventLabel: string;
  dateLabel: string;
  documents: DocumentArchive[];
}

@Component({
  selector: 'app-documents-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './documents-list.component.html',
  styleUrl: './documents-list.component.scss'
})
export class DocumentsListComponent implements OnInit {
  documents: DocumentArchive[] = [];
  searchResults: DocumentArchive[] = [];
  evenements: Evenement[] = [];
  profile: Membre | null = null;
  selectedType: TypeDocument | 'TOUS' = 'TOUS';
  selectedEventId = 'TOUS';
  selectedYear = 'TOUTES';
  searchTerm = '';
  isArchivedView = false;
  isLoading = false;
  loadError = '';
  actionError = '';
  uploadError = '';
  uploadOpen = false;
  isUploading = false;
  selectedFile: File | null = null;
  documentForm!: FormGroup;
  private searchTimer: ReturnType<typeof setTimeout> | undefined;

  readonly types: { value: TypeDocument; label: string; icon: string }[] = [
    { value: 'DEMANDE_FINANCEMENT', label: 'Financement', icon: '€' },
    { value: 'PROGRAMME', label: 'Programme', icon: 'P' },
    { value: 'ATTESTATION', label: 'Attestation', icon: 'A' },
    { value: 'RAPPORT', label: 'Rapport', icon: 'R' },
    { value: 'DOCUMENT_ADMINISTRATIF', label: 'Administratif', icon: 'D' }
  ];

  constructor(
    private readonly documentService: DocumentService,
    private readonly evenementService: EvenementService,
    private readonly membreService: MembreService,
    private readonly authService: AuthService,
    private readonly fb: FormBuilder,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.documentForm = this.fb.group({
      type: ['PROGRAMME', Validators.required],
      evenementId: ['']
    });
    this.membreService.getMonProfil().subscribe({
      next: profile => {
        this.profile = profile;
        this.cdr.detectChanges();
      },
      error: error => {
        console.error('Impossible de déterminer les droits du membre pour les documents.', error);
      }
    });
    this.evenementService.getEvenements().subscribe({
      next: evenements => this.evenements = Array.isArray(evenements) ? evenements : [],
      error: error => console.error('Impossible de charger les événements pour le filtre Documents.', error)
    });
    this.loadDocuments();
  }

  get canUpload(): boolean {
    const role = this.profile?.roleSysteme || this.profile?.role || this.authService.getUserRole();
    return this.profile?.statut === 'PERMANENT'
      || ['ADMIN', 'DIRECTEUR', 'VICE_DIRECTEUR'].includes(role);
  }

  get canViewArchives(): boolean {
    const role = this.profile?.roleSysteme || this.profile?.role || this.authService.getUserRole();
    return ['ADMIN', 'DIRECTEUR', 'VICE_DIRECTEUR'].includes(role);
  }

  get years(): string[] {
    return [...new Set(this.documents
      .map(document => this.parseDate(document.dateUpload)?.getFullYear().toString())
      .filter((year): year is string => !!year))]
      .sort((left, right) => right.localeCompare(left));
  }

  get filteredDocuments(): DocumentArchive[] {
    return this.searchResults.filter(document =>
      (this.selectedType === 'TOUS' || document.typeDocument === this.selectedType)
      && (this.selectedEventId === 'TOUS'
        || document.evenementId?.toString() === this.selectedEventId)
      && (this.selectedYear === 'TOUTES'
        || this.parseDate(document.dateUpload)?.getFullYear().toString() === this.selectedYear));
  }

  get groupedDocuments(): DocumentGroup[] {
    const groups = new Map<string, DocumentGroup>();
    for (const document of this.filteredDocuments) {
      const date = this.parseDate(document.dateUpload);
      const eventLabel = document.evenementTitre?.trim() || 'Documents administratifs';
      const dateLabel = date
        ? new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(date)
        : 'Date non précisée';
      const key = `${eventLabel}|${dateLabel}`;
      const group = groups.get(key) ?? { eventLabel, dateLabel, documents: [] };
      group.documents.push(document);
      groups.set(key, group);
    }
    return [...groups.values()].sort((left, right) => {
      const leftDate = this.parseDate(left.documents[0]?.dateUpload)?.getTime() ?? 0;
      const rightDate = this.parseDate(right.documents[0]?.dateUpload)?.getTime() ?? 0;
      return rightDate - leftDate;
    });
  }

  get typeCount(): number {
    return this.filteredDocuments.length;
  }

  loadDocuments(): void {
    this.isLoading = true;
    this.loadError = '';
    const request = this.isArchivedView
      ? this.documentService.getArchives()
      : this.documentService.getDocuments();
    request.pipe(finalize(() => {
      this.isLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: documents => {
        this.documents = Array.isArray(documents) ? documents.filter(document => !!document?.id) : [];
        this.searchResults = this.documents;
        if (this.searchTerm.trim().length >= 2) {
          this.searchDocuments(this.searchTerm.trim());
        }
      },
      error: error => {
        console.error('Erreur lors du chargement des documents.', error);
        this.loadError = this.errorMessage(error, 'Impossible de charger les documents.');
        this.documents = [];
        this.searchResults = [];
      }
    });
  }

  onSearchChange(value: string): void {
    this.searchTerm = value;
    if (this.searchTimer) clearTimeout(this.searchTimer);
    const query = value.trim();
    if (query.length < 2) {
      this.searchResults = this.documents;
      return;
    }
    this.searchTimer = setTimeout(() => this.searchDocuments(query), 250);
  }

  private searchDocuments(query: string): void {
    if (this.isArchivedView) {
      this.searchResults = this.documents.filter(document =>
        document.nomFichier.toLocaleLowerCase().includes(query.toLocaleLowerCase()));
      return;
    }
    this.documentService.rechercherDocuments(query).subscribe({
      next: results => this.searchResults = Array.isArray(results) ? results : [],
      error: error => {
        console.error('Erreur lors de la recherche globale de documents.', error);
        this.actionError = this.errorMessage(error, 'La recherche de documents a échoué.');
      }
    });
  }

  setType(type: TypeDocument | 'TOUS'): void {
    this.selectedType = type;
  }

  setArchiveView(archived: boolean): void {
    this.isArchivedView = archived;
    this.loadDocuments();
  }

  openUpload(): void {
    this.uploadError = '';
    this.selectedFile = null;
    this.documentForm.reset({ type: 'PROGRAMME', evenementId: '' });
    this.uploadOpen = true;
  }

  closeUpload(): void {
    if (!this.isUploading) this.uploadOpen = false;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    this.uploadError = '';
  }

  submitUpload(): void {
    this.documentForm.markAllAsTouched();
    if (this.documentForm.invalid || !this.selectedFile) {
      this.uploadError = !this.selectedFile ? 'Veuillez sélectionner un fichier.' : 'Vérifiez les champs obligatoires.';
      return;
    }
    const type = this.documentForm.value.type as TypeDocument;
    const eventValue = this.documentForm.value.evenementId;
    const evenementId = eventValue ? Number(eventValue) : null;
    if (type !== 'DOCUMENT_ADMINISTRATIF' && evenementId === null) {
      this.uploadError = 'Sélectionnez un événement pour ce type de document.';
      return;
    }

    this.isUploading = true;
    this.uploadError = '';
    this.documentService.uploader(this.selectedFile, type, evenementId).pipe(
      finalize(() => {
        this.isUploading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: () => {
        this.uploadOpen = false;
        this.loadDocuments();
      },
      error: error => this.uploadError = this.errorMessage(error, 'Le dépôt du document a échoué.')
    });
  }

  download(entry: DocumentArchive): void {
    this.actionError = '';
    this.documentService.telecharger(entry.id).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = entry.nomFichier;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: error => {
        console.error('Erreur lors du téléchargement du document.', error);
        this.actionError = this.errorMessage(error, 'Le téléchargement du document a échoué.');
      }
    });
  }

  archive(document: DocumentArchive): void {
    if (!window.confirm(`Archiver « ${document.nomFichier} » ? Le fichier sera conservé et pourra être restauré.`)) return;
    this.documentService.archiver(document.id).subscribe({
      next: () => this.loadDocuments(),
      error: error => this.actionError = this.errorMessage(error, 'L’archivage du document a échoué.')
    });
  }

  restore(document: DocumentArchive): void {
    this.documentService.restaurer(document.id).subscribe({
      next: () => this.loadDocuments(),
      error: error => this.actionError = this.errorMessage(error, 'La restauration du document a échoué.')
    });
  }

  typeLabel(type?: TypeDocument | null): string {
    return this.types.find(item => item.value === type)?.label ?? 'Document';
  }

  typeIcon(type?: TypeDocument | null): string {
    return this.types.find(item => item.value === type)?.icon ?? 'F';
  }

  formatDate(value?: string | null): string {
    const date = this.parseDate(value);
    return date ? new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' }).format(date) : 'Date non précisée';
  }

  formatSize(size?: number | null): string {
    if (size == null || !Number.isFinite(size)) return 'Taille inconnue';
    if (size < 1024 * 1024) return `${Math.max(1, Math.round(size / 1024))} Ko`;
    return `${(size / (1024 * 1024)).toFixed(1)} Mo`;
  }

  eventName(id?: number | null): string {
    if (id == null) return 'Aucun événement';
    return this.evenements.find(event => event.id === id)?.titre ?? 'Événement associé';
  }

  private parseDate(value?: string | null): Date | null {
    if (!value) return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  private errorMessage(error: any, fallback: string): string {
    return error?.error?.message || (typeof error?.error === 'string' ? error.error : '') || fallback;
  }
}
