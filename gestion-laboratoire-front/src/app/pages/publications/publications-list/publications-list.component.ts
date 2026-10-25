import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PublicationService } from '../../../core/services/publication.service';
import { MembreService } from '../../../core/services/membre.service';
import { AuthService } from '../../../core/services/auth.service';
import { Observable } from 'rxjs';
import { Publication, PublicationCreateRequest, TypePublication, getTypePublicationLabel, getTypePublicationBadgeClass } from '../../../core/models/publication.model';
import { Membre } from '../../../core/models/membre.model';

@Component({
  selector: 'app-publications-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './publications-list.component.html',
  styleUrls: ['./publications-list.component.scss']
})
export class PublicationsListComponent implements OnInit {
  publications: Publication[] = [];
  allPublications: Publication[] = [];
  membres: Membre[] = [];
  isLoading = false;
  activeFilter = 'TOUTES';
  selectedYear = 'TOUTES';
  selectedAuteurId = 'TOUS';
  showArchived = false;
  currentUserId: number | null = null;

  // Detail panel
  selectedPublication: Publication | null = null;
  isDetailOpen = false;

  // Add form
  isFormOpen = false;
  isSaving = false;
  pubForm!: FormGroup;
  selectedAuteurIds: number[] = [];
  auteurSearch = '';
  showSuccessToast = false;

  // Confirm archive
  showConfirmArchive = false;
  archiveTargetId: number | null = null;

  typeOptions: { value: TypePublication; label: string }[] = [
    { value: 'ARTICLE_JOURNAL', label: 'Article de journal' },
    { value: 'CONFERENCE', label: 'Conférence' },
    { value: 'CHAPITRE_LIVRE', label: 'Chapitre de livre' },
    { value: 'THESE', label: 'Thèse' },
    { value: 'BREVET', label: 'Brevet' }
  ];

  equipes = ['TOUTES', 'ISDIAC', 'SIMA', 'SDTIC', 'ILIAS'];

  constructor(
    private publicationService: PublicationService,
    private membreService: MembreService,
    private authService: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = localStorage.getItem('user_id');
    if (id) this.currentUserId = +id;
    this.initForm();
    this.loadPublications();
    this.loadMembres();
  }

  initForm(): void {
    this.pubForm = this.fb.group({
      titre: ['', Validators.required],
      resume: ['', Validators.required],
      datePublication: ['', Validators.required],
      lienDoi: ['', [Validators.required, Validators.pattern(/^https?:\/\/.+/)]],
      type: ['', Validators.required]
    });
  }

  loadPublications(): void {
    this.isLoading = true;
    const obs = this.showArchived
      ? this.publicationService.getPublicationsArchivees()
      : this.publicationService.getPublications();

    obs.subscribe({
      next: (data) => {
        this.allPublications = data;
        this.applyFilters();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur chargement publications', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadMembres(): void {
    this.membreService.getMembres().subscribe({
      next: (data) => this.membres = data,
      error: (err) => console.error('Erreur chargement membres', err)
    });
  }

  filterByEquipe(equipe: string): void {
    this.activeFilter = equipe;
    this.applyFilters();
    this.cdr.detectChanges();
  }

  filterByYear(year: string): void {
    this.selectedYear = year;
    this.applyFilters();
    this.cdr.detectChanges();
  }

  filterByAuteur(auteurId: string): void {
    this.selectedAuteurId = auteurId;
    this.applyFilters();
    this.cdr.detectChanges();
  }

  get years(): string[] {
    return [...new Set(this.allPublications
      .map(publication => this.getYear(publication.datePublication))
      .filter(Boolean))].sort((first, second) => Number(second) - Number(first));
  }

  canViewArchives(): boolean {
    const role = localStorage.getItem('user_role');
    return role === 'ADMIN' || role === 'DIRECTEUR';
  }

  toggleArchived(): void {
    this.showArchived = !this.showArchived;
    this.activeFilter = 'TOUTES';
    this.selectedYear = 'TOUTES';
    this.selectedAuteurId = 'TOUS';
    this.loadPublications();
  }

  private applyFilters(): void {
    this.publications = this.allPublications.filter(publication => {
      const matchesTeam = this.activeFilter === 'TOUTES' ||
        publication.auteurs?.some(auteur => auteur.equipeLaboratoire === this.activeFilter);
      const matchesYear = this.selectedYear === 'TOUTES' ||
        this.getYear(publication.datePublication) === this.selectedYear;
      const matchesAuteur = this.selectedAuteurId === 'TOUS' ||
        publication.auteurs?.some(auteur => auteur.id?.toString() === this.selectedAuteurId);
      return matchesTeam && matchesYear && matchesAuteur;
    });
  }

  // ─── Helpers ─────────────────────────────────────────────────
  getLabel(type: TypePublication): string {
    return getTypePublicationLabel(type);
  }

  getBadgeClass(type: TypePublication): string {
    return getTypePublicationBadgeClass(type);
  }

  getYear(date: string): string {
    return date ? new Date(date).getFullYear().toString() : '';
  }

  getInitials(membre: Membre): string {
    return (membre.prenom?.charAt(0) || '') + (membre.nom?.charAt(0) || '');
  }

  getFullName(membre: Membre): string {
    return `${membre.prenom || ''} ${membre.nom || ''}`.trim();
  }

  truncate(text: string | null | undefined, max: number): string {
    if (!text) return '';
    return text.length > max ? text.substring(0, max) + '…' : text;
  }

  // ─── Detail panel ────────────────────────────────────────────
  openDetail(pub: Publication): void {
    this.selectedPublication = pub;
    this.isDetailOpen = true;
  }

  closeDetail(): void {
    this.isDetailOpen = false;
    this.selectedPublication = null;
  }

  formatDateLong(date: string): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: 'numeric', month: 'long', year: 'numeric'
    });
  }

  canArchive(pub: Publication): boolean {
    const role = this.authService.getUserRole();
    const userId = this.authService.getUserId();
    return role === 'ADMIN'
      || role === 'DIRECTEUR'
      || role === 'VICE_DIRECTEUR'
      || (!!userId && pub.auteurs?.some(auteur => auteur.id === userId) === true);
  }

  confirmArchive(id: number): void {
    this.archiveTargetId = id;
    this.showConfirmArchive = true;
  }

  cancelArchive(): void {
    this.showConfirmArchive = false;
    this.archiveTargetId = null;
  }

  doArchive(): void {
    if (!this.archiveTargetId) return;
    const request: Observable<unknown> = this.showArchived
      ? this.publicationService.desarchiverPublication(this.archiveTargetId)
      : this.publicationService.archiverPublication(this.archiveTargetId);
    request.subscribe({
      next: () => {
        this.showConfirmArchive = false;
        this.archiveTargetId = null;
        this.closeDetail();
        this.loadPublications();
      },
      error: (err) => console.error('Erreur archivage', err)
    });
  }

  // ─── Add form ────────────────────────────────────────────────
  openForm(): void {
    this.pubForm.reset();
    this.selectedAuteurIds = [];
    // Pre-select connected user
    const myId = localStorage.getItem('user_id');
    if (myId) this.selectedAuteurIds.push(+myId);
    this.isFormOpen = true;
  }

  closeForm(): void {
    this.isFormOpen = false;
  }

  get filteredMembres(): Membre[] {
    if (!this.auteurSearch) return this.membres;
    const q = this.auteurSearch.toLowerCase();
    return this.membres.filter(m =>
      (m.nom?.toLowerCase().includes(q) || m.prenom?.toLowerCase().includes(q))
    );
  }

  isAuteurSelected(id: number): boolean {
    return this.selectedAuteurIds.includes(id);
  }

  toggleAuteur(id: number): void {
    if (this.isAuteurSelected(id)) {
      this.selectedAuteurIds = this.selectedAuteurIds.filter(aid => aid !== id);
    } else {
      this.selectedAuteurIds.push(id);
    }
  }

  getSelectedAuteurs(): Membre[] {
    return this.membres.filter(m => this.selectedAuteurIds.includes(m.id));
  }

  removeAuteur(id: number): void {
    this.selectedAuteurIds = this.selectedAuteurIds.filter(aid => aid !== id);
  }

  submitPublication(): void {
    if (this.pubForm.invalid || this.selectedAuteurIds.length === 0) return;

    this.isSaving = true;
    const formVal = this.pubForm.value;
    const request: PublicationCreateRequest = {
      publication: {
        titre: formVal.titre,
        resume: formVal.resume,
        datePublication: formVal.datePublication,
        lienDoi: formVal.lienDoi,
        type: formVal.type
      },
      auteursIds: this.selectedAuteurIds
    };

    this.publicationService.ajouterPublication(request).subscribe({
      next: () => {
        this.isSaving = false;
        this.closeForm();
        this.showSuccessToast = true;
        this.loadPublications();
        setTimeout(() => this.showSuccessToast = false, 3000);
      },
      error: (err) => {
        console.error('Erreur ajout publication', err);
        this.isSaving = false;
      }
    });
  }
}
