import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, timeout } from 'rxjs';
import {
  DemandeAdhesion, EquipeCandidat, StatutCandidat, StatutAdhesion, TraitementAdhesionRequest
} from '../../core/models/adhesion.model';
import { AdhesionService } from '../../core/services/adhesion.service';

function lireMessageErreur(error: HttpErrorResponse | unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const body: unknown = error.error;
    if (typeof body === 'string' && body.trim()) return body;
    if (body && typeof body === 'object' && 'message' in body && typeof (body as { message: unknown }).message === 'string') {
      return (body as { message: string }).message;
    }
  }
  return fallback;
}

type DecisionControls = {
  statutAccorde: FormControl<StatutCandidat>;
  equipeAccordee: FormControl<EquipeCandidat>;
  motif: FormControl<string>;
};

@Component({
  selector: 'app-adhesions-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './adhesions-list.component.html',
  styleUrl: './adhesions-list.component.scss'
})
export class AdhesionsListComponent implements OnInit {
  demandes: DemandeAdhesion[] = [];
  selected: DemandeAdhesion | null = null;
  activeTab: 'attente' | 'historique' = 'attente';
  decision: 'ACCEPTEE' | 'REFUSEE' | null = null;

  isLoading = false;
  isProcessing = false;
  isCvLoading = false;

  errorMessage = '';
  successMessage = '';
  drawerError = '';
  drawerSuccess = '';
  notificationWarning = false;

  searchQuery = '';
  equipeFiltre = 'ALL';

  countAttente = 0;
  countHistorique = 0;
  countAcceptees = 0;
  countRefusees = 0;

  readonly statuts: StatutCandidat[] = ['PERMANENT', 'ASSOCIE', 'DOCTORANT'];
  readonly equipes: EquipeCandidat[] = ['ISDIAC', 'SIMA', 'ILIAS', 'SDTIC'];
  readonly decisionForm: FormGroup<DecisionControls>;

  constructor(
    private adhesionService: AdhesionService,
    private formBuilder: FormBuilder,
    private changeDetector: ChangeDetectorRef
  ) {
    this.decisionForm = this.formBuilder.nonNullable.group({
      statutAccorde: ['PERMANENT' as StatutCandidat, Validators.required],
      equipeAccordee: ['ISDIAC' as EquipeCandidat, Validators.required],
      motif: ['', Validators.maxLength(2000)]
    });
  }

  ngOnInit(): void {
    this.charger();
    this.rafraichirCompteurs();
  }

  get demandesFiltrees(): DemandeAdhesion[] {
    return this.demandes.filter(demande => {
      const matchSearch = !this.searchQuery.trim() ||
        (demande.nom && demande.nom.toLowerCase().includes(this.searchQuery.toLowerCase())) ||
        (demande.prenom && demande.prenom.toLowerCase().includes(this.searchQuery.toLowerCase())) ||
        (demande.email && demande.email.toLowerCase().includes(this.searchQuery.toLowerCase())) ||
        (demande.etablissementOrigine && demande.etablissementOrigine.toLowerCase().includes(this.searchQuery.toLowerCase())) ||
        (demande.laboratoireOrigine && demande.laboratoireOrigine.toLowerCase().includes(this.searchQuery.toLowerCase()));

      const matchEquipe = this.equipeFiltre === 'ALL' || demande.equipeSouhaitee === this.equipeFiltre;

      return matchSearch && matchEquipe;
    });
  }

  rafraichirCompteurs(): void {
    forkJoin({
      attente: this.adhesionService.getEnAttente(),
      historique: this.adhesionService.getHistorique()
    }).subscribe({
      next: ({ attente, historique }) => {
        this.countAttente = attente?.length || 0;
        this.countHistorique = historique?.length || 0;
        this.countAcceptees = historique?.filter(h => h.statut === 'ACCEPTEE').length || 0;
        this.countRefusees = historique?.filter(h => h.statut === 'REFUSEE').length || 0;
        this.changeDetector.detectChanges();
      },
      error: () => {}
    });
  }

  charger(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.demandes = [];

    const demandes$ = this.activeTab === 'attente'
      ? this.adhesionService.getEnAttente()
      : this.adhesionService.getHistorique();

    demandes$.pipe(timeout({ first: 15_000 })).subscribe({
      next: demandes => {
        this.demandes = demandes ?? [];
        if (this.activeTab === 'attente') {
          this.countAttente = this.demandes.length;
        } else {
          this.countHistorique = this.demandes.length;
          this.countAcceptees = this.demandes.filter(h => h.statut === 'ACCEPTEE').length;
          this.countRefusees = this.demandes.filter(h => h.statut === 'REFUSEE').length;
        }
        this.isLoading = false;
        this.changeDetector.detectChanges();
      },
      error: (error: HttpErrorResponse | Error) => {
        this.errorMessage = error.name === 'TimeoutError'
          ? 'Le serveur met trop de temps à répondre. Vérifiez que le backend et la base de données sont disponibles, puis actualisez.'
          : error instanceof HttpErrorResponse
            ? lireMessageErreur(error, 'Impossible de charger les demandes d’adhésion.')
            : 'Impossible de charger les demandes d’adhésion. Vérifiez la connexion au serveur.';
        this.isLoading = false;
        this.changeDetector.detectChanges();
      }
    });
  }

  changerOnglet(tab: 'attente' | 'historique'): void {
    if (this.activeTab === tab) return;
    this.activeTab = tab;
    this.selected = null;
    this.decision = null;
    this.errorMessage = '';
    this.successMessage = '';
    this.drawerError = '';
    this.drawerSuccess = '';
    this.notificationWarning = false;
    this.charger();
  }

  ouvrir(demande: DemandeAdhesion): void {
    this.selected = demande;
    this.decision = null;
    this.drawerError = '';
    this.drawerSuccess = '';

    this.decisionForm.controls.motif.clearValidators();
    this.decisionForm.controls.motif.setValidators([Validators.maxLength(2000)]);
    this.decisionForm.reset({
      statutAccorde: demande.statutSouhaite || 'PERMANENT',
      equipeAccordee: demande.equipeSouhaitee || 'ISDIAC',
      motif: ''
    });
    this.decisionForm.updateValueAndValidity();
  }

  fermerDetail(): void {
    this.selected = null;
    this.decision = null;
    this.drawerError = '';
    this.drawerSuccess = '';
  }

  demanderDecision(decision: 'ACCEPTEE' | 'REFUSEE'): void {
    this.decision = decision;
    this.drawerError = '';
    this.drawerSuccess = '';

    if (decision === 'REFUSEE') {
      this.decisionForm.controls.motif.setValidators([
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(2000)
      ]);
    } else {
      this.decisionForm.controls.motif.setValidators([Validators.maxLength(2000)]);
    }
    this.decisionForm.controls.motif.updateValueAndValidity();
  }

  confirmerDecision(): void {
    if (!this.selected || !this.decision) return;

    this.drawerError = '';
    this.drawerSuccess = '';
    this.decisionForm.markAllAsTouched();

    if (this.decisionForm.invalid) {
      this.drawerError = this.decision === 'REFUSEE'
        ? 'Veuillez saisir un motif valide justifiant le refus (minimum 3 caractères).'
        : 'Veuillez vérifier les informations renseignées.';
      return;
    }

    const formValue = this.decisionForm.getRawValue();
    const request: TraitementAdhesionRequest = this.decision === 'ACCEPTEE'
      ? {
          estAcceptee: true,
          statutAccorde: formValue.statutAccorde || this.selected.statutSouhaite || 'PERMANENT',
          equipeAccordee: formValue.equipeAccordee || this.selected.equipeSouhaitee || 'ISDIAC',
          roleInitial: 'MEMBRE_EQUIPE',
          motif: formValue.motif ? formValue.motif.trim() : undefined
        }
      : {
          estAcceptee: false,
          motif: formValue.motif ? formValue.motif.trim() : undefined
        };

    this.isProcessing = true;
    this.errorMessage = '';
    this.drawerError = '';

    const demandeId = this.selected.id;
    const decisionPrise = this.decision;

    this.adhesionService.traiter(demandeId, request).subscribe({
      next: result => {
        const msg = result.message || (decisionPrise === 'ACCEPTEE'
          ? 'Candidature acceptée avec succès ! Le membre a été créé et notifié.'
          : 'Candidature refusée avec succès.');

        this.successMessage = msg;
        this.drawerSuccess = msg;
        this.notificationWarning = !result.notificationEnvoyee;
        this.isProcessing = false;

        // Mise à jour immédiate de l'objet affiché dans le panneau
        if (this.selected && this.selected.id === demandeId) {
          const nouveauStatut: StatutAdhesion = result.demande?.statut || decisionPrise;
          this.selected.statut = nouveauStatut;
          this.selected.dateTraitement = result.demande?.dateTraitement || new Date().toISOString();
          this.selected.traitePar = result.demande?.traitePar || this.selected.traitePar;
          this.selected.motifDecision = result.demande?.motifDecision || (formValue.motif || null);
          this.selected.membreCree = result.demande?.membreCree || null;
        }

        // Mise à jour de la liste en mémoire
        if (this.activeTab === 'attente') {
          this.demandes = this.demandes.filter(d => d.id !== demandeId);
          this.countAttente = Math.max(0, this.countAttente - 1);
          this.countHistorique += 1;
          if (decisionPrise === 'ACCEPTEE') this.countAcceptees += 1;
          else this.countRefusees += 1;
        } else {
          const index = this.demandes.findIndex(d => d.id === demandeId);
          if (index !== -1 && result.demande) {
            this.demandes[index] = result.demande;
          }
        }

        this.decision = null;
        this.changeDetector.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        const err = lireMessageErreur(error, 'La décision n’a pas pu être enregistrée. Veuillez réessayer.');
        this.errorMessage = err;
        this.drawerError = err;
        this.isProcessing = false;
        this.changeDetector.detectChanges();
      }
    });
  }

  telechargerCv(demande: DemandeAdhesion): void {
    this.isCvLoading = true;
    this.drawerError = '';
    this.adhesionService.telechargerCv(demande.id).subscribe({
      next: blob => {
        const objectUrl = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = objectUrl;
        anchor.download = `cv-adhesion-${demande.id}.pdf`;
        anchor.click();
        window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
        this.isCvLoading = false;
        this.changeDetector.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        const err = lireMessageErreur(error, 'Le CV n’a pas pu être téléchargé.');
        this.drawerError = err;
        this.isCvLoading = false;
        this.changeDetector.detectChanges();
      }
    });
  }

  statutLabel(statut: StatutAdhesion): string {
    return statut === 'EN_ATTENTE' ? 'En attente' : statut === 'ACCEPTEE' ? 'Acceptée' : 'Refusée';
  }
}
