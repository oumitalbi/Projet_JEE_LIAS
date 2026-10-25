import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { LogistiqueService } from '../../../core/services/logistique.service';
import { MembreService } from '../../../core/services/membre.service';
import {
  ArrivageMateriel,
  DemandeMateriel,
  DistributionEquipe,
  Materiel
} from '../../../core/models/logistique.model';

@Component({
  selector: 'app-logistique-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DatePipe],
  templateUrl: './logistique-list.component.html',
  styleUrl: './logistique-list.component.scss'
})
export class LogistiqueListComponent implements OnInit {
  materials: Materiel[] = [];
  myRequests: DemandeMateriel[] = [];
  queue: DemandeMateriel[] = [];
  arrivals: ArrivageMateriel[] = [];
  distribution: DistributionEquipe[] = [];
  activeTab: 'catalogue' | 'demandes' | 'validation' | 'historique' = 'catalogue';
  isDirector = false;
  canRequest = false;
  canAccess = false;
  isLoading = false;
  isSaving = false;
  errorMessage = '';
  successMessage = '';
  requestOpen = false;
  materialOpen = false;
  arrivalOpen = false;
  selectedMaterial: Materiel | null = null;
  rejectId: number | null = null;
  rejectReason = '';
  approvalMaterial: Record<number, number | null> = {};
  filterStatus = 'TOUS';
  showArchived = false;
  editingMaterialId: number | null = null;
  requestForm: FormGroup;
  materialForm: FormGroup;
  arrivalForm: FormGroup;

  constructor(
    private readonly logisticService: LogistiqueService,
    private readonly memberService: MembreService,
    private readonly authService: AuthService,
    private readonly fb: FormBuilder,
    private readonly cdr: ChangeDetectorRef
  ) {
    this.requestForm = this.fb.group({
      materielId: [null],
      nomBesoin: [''],
      quantite: [1, [Validators.required, Validators.min(1)]],
      justification: ['', [Validators.required, Validators.maxLength(2000)]]
    });
    this.materialForm = this.fb.group({
      nom: ['', [Validators.required, Validators.maxLength(200)]],
      description: [''],
      numeroInventaire: ['', [Validators.required, Validators.maxLength(100)]],
      categorie: ['', [Validators.required, Validators.maxLength(80)]],
      etat: ['BON_ETAT', Validators.required],
      dateAcquisition: [''],
      quantiteInitiale: [1, [Validators.required, Validators.min(1)]],
      consommable: [false]
    });
    this.arrivalForm = this.fb.group({
      materielId: [null, Validators.required],
      quantite: [1, [Validators.required, Validators.min(1)]],
      dateArrivage: [new Date().toISOString().slice(0, 10), Validators.required],
      fournisseur: [''],
      bonLivraison: ['']
    });
  }

  ngOnInit(): void {
    this.isLoading = true;
    this.memberService.getMonProfil().subscribe({
      next: member => {
        const role = member?.role || '';
        const systemRole = member?.roleSysteme || this.authService.getUserRole();
        const status = member?.statut || '';
        this.isDirector = role === 'DIRECTEUR' || systemRole === 'DIRECTEUR';
        this.canRequest = ['PERMANENT', 'ASSOCIE'].includes(status) && systemRole !== 'DOCTORANT';
        this.canAccess = this.canRequest || this.isDirector;
        this.loadData();
      },
      error: error => {
        console.error('Impossible de charger le profil pour la logistique', error);
        this.errorMessage = 'Impossible de vérifier les droits d’accès au module Logistique.';
        this.isLoading = false;
      }
    });
  }

  get visibleRequests(): DemandeMateriel[] {
    return this.myRequests.filter(request => this.filterStatus === 'TOUS' || request.statut === this.filterStatus);
  }

  get pendingCount(): number {
    return this.queue.filter(request => request.statut === 'EN_ATTENTE').length;
  }

  setTab(tab: 'catalogue' | 'demandes' | 'validation' | 'historique'): void {
    this.activeTab = tab;
    this.errorMessage = '';
  }

  openRequest(material: Materiel | null): void {
    this.selectedMaterial = material;
    const nameControl = this.requestForm.get('nomBesoin');
    if (material) nameControl?.clearValidators();
    else nameControl?.setValidators([Validators.required, Validators.maxLength(200)]);
    nameControl?.updateValueAndValidity();
    this.requestForm.reset({
      materielId: material?.id ?? null,
      nomBesoin: material ? '' : '',
      quantite: 1,
      justification: ''
    });
    this.requestOpen = true;
  }

  submitRequest(): void {
    if (this.requestForm.invalid || this.isSaving) {
      this.requestForm.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    this.errorMessage = '';
    const value = this.requestForm.getRawValue();
    this.logisticService.soumettreDemande({
      materielId: value.materielId ? Number(value.materielId) : null,
      nomBesoin: value.nomBesoin || '',
      quantite: Number(value.quantite),
      justification: value.justification.trim()
    }).pipe(finalize(() => this.isSaving = false)).subscribe({
      next: () => {
        this.requestOpen = false;
        this.successMessage = 'Votre demande a été envoyée à la direction.';
        this.loadData();
      },
      error: error => this.showError(error, 'La demande n’a pas pu être envoyée.')
    });
  }

  openMaterialForm(): void {
    this.editingMaterialId = null;
    this.materialForm.reset({
      nom: '', description: '', numeroInventaire: '', categorie: '',
      etat: 'BON_ETAT', dateAcquisition: '', quantiteInitiale: 1, consommable: false
    });
    this.materialOpen = true;
  }

  openEditMaterial(material: Materiel): void {
    this.editingMaterialId = material.id;
    this.materialForm.reset({
      nom: material.nom,
      description: material.description || '',
      numeroInventaire: material.numeroInventaire,
      categorie: material.categorie,
      etat: material.etat,
      dateAcquisition: material.dateAcquisition || '',
      quantiteInitiale: material.quantiteTotale,
      consommable: material.consommable
    });
    this.materialOpen = true;
  }

  addMaterial(): void {
    if (this.materialForm.invalid || this.isSaving) {
      this.materialForm.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    const value = this.materialForm.getRawValue();
    const payload = {
      ...value,
      quantiteInitiale: Number(value.quantiteInitiale),
      dateAcquisition: value.dateAcquisition || null
    };
    const action = this.editingMaterialId == null
      ? this.logisticService.ajouterMateriel(payload)
      : this.logisticService.modifierMateriel(this.editingMaterialId, payload);
    action.pipe(finalize(() => this.isSaving = false)).subscribe({
      next: () => {
        this.materialOpen = false;
        this.successMessage = this.editingMaterialId == null
          ? 'Le matériel a été ajouté à l’inventaire.'
          : 'Le matériel a été mis à jour.';
        this.loadData();
      },
      error: error => this.showError(error, 'Impossible d’ajouter ce matériel.')
    });
  }

  archiveMaterial(material: Materiel): void {
    if (!window.confirm(`Archiver « ${material.nom} » ? Le matériel restera dans l’historique.`)) return;
    this.logisticService.archiverMateriel(material.id).subscribe({
      next: () => { this.successMessage = 'Matériel archivé sans suppression.'; this.loadData(); },
      error: error => this.showError(error, 'Impossible d’archiver ce matériel.')
    });
  }

  restoreMaterial(material: Materiel): void {
    this.logisticService.restaurerMateriel(material.id).subscribe({
      next: () => { this.successMessage = 'Matériel restauré dans le catalogue.'; this.loadData(); },
      error: error => this.showError(error, 'Impossible de restaurer ce matériel.')
    });
  }

  toggleArchived(): void {
    this.showArchived = !this.showArchived;
    this.loadData();
  }

  openArrivalForm(): void {
    this.arrivalForm.reset({
      materielId: null, quantite: 1, dateArrivage: new Date().toISOString().slice(0, 10),
      fournisseur: '', bonLivraison: ''
    });
    this.arrivalOpen = true;
  }

  addArrival(): void {
    if (this.arrivalForm.invalid || this.isSaving) {
      this.arrivalForm.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    const value = this.arrivalForm.getRawValue();
    this.logisticService.enregistrerArrivage({
      ...value, materielId: Number(value.materielId), quantite: Number(value.quantite)
    }).pipe(finalize(() => this.isSaving = false)).subscribe({
      next: () => {
        this.arrivalOpen = false;
        this.successMessage = 'L’arrivage est enregistré et le stock a été mis à jour.';
        this.loadData();
      },
      error: error => this.showError(error, 'Impossible d’enregistrer cet arrivage.')
    });
  }

  validate(request: DemandeMateriel): void {
    const selected = this.approvalMaterial[request.id] ?? request.materiel?.id;
    this.isSaving = true;
    this.logisticService.validerDemande(request.id, selected).pipe(
      finalize(() => this.isSaving = false)
    ).subscribe({
      next: () => {
        this.successMessage = 'La demande a été validée et le stock actualisé.';
        this.loadData();
      },
      error: error => this.showError(error, 'La demande ne peut pas être validée.')
    });
  }

  startReject(request: DemandeMateriel): void {
    this.rejectId = request.id;
    this.rejectReason = '';
  }

  refuse(): void {
    if (this.rejectId == null || !this.rejectReason.trim() || this.isSaving) return;
    this.isSaving = true;
    this.logisticService.refuserDemande(this.rejectId, this.rejectReason.trim()).pipe(
      finalize(() => this.isSaving = false)
    ).subscribe({
      next: () => {
        this.rejectId = null;
        this.successMessage = 'La demande a été refusée et le demandeur a été notifié.';
        this.loadData();
      },
      error: error => this.showError(error, 'Impossible de refuser cette demande.')
    });
  }

  returnMaterial(request: DemandeMateriel): void {
    this.isSaving = true;
    this.logisticService.retournerMateriel(request.id).pipe(
      finalize(() => this.isSaving = false)
    ).subscribe({
      next: () => {
        this.successMessage = 'Le retour est enregistré et le stock a été réapprovisionné.';
        this.loadData();
      },
      error: error => this.showError(error, 'Impossible d’enregistrer le retour.')
    });
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      EN_ATTENTE: 'En attente', VALIDEE: 'Validée', LIVREE: 'Distribuée',
      REFUSEE: 'Refusée', RETOURNEE: 'Retournée'
    };
    return labels[status] || status;
  }

  private loadData(): void {
    if (!this.canAccess) {
      this.isLoading = false;
      return;
    }
    this.isLoading = true;
    this.logisticService.getMateriel(this.showArchived).subscribe({
      next: materials => { this.materials = materials || []; this.cdr.detectChanges(); },
      error: error => this.showError(error, 'Impossible de charger le catalogue.')
    });
    this.logisticService.getDemandes(true).subscribe({
      next: requests => { this.myRequests = requests || []; this.cdr.detectChanges(); },
      error: error => this.showError(error, 'Impossible de charger vos demandes.')
    });
    if (this.isDirector) {
      this.logisticService.getDemandes(false).subscribe({
        next: requests => { this.queue = requests || []; this.cdr.detectChanges(); },
        error: error => this.showError(error, 'Impossible de charger la file de validation.')
      });
      this.logisticService.getArrivages().subscribe({
        next: arrivals => { this.arrivals = arrivals || []; this.cdr.detectChanges(); },
        error: error => this.showError(error, 'Impossible de charger l’historique des arrivages.')
      });
      this.logisticService.getDistributionEquite().subscribe({
        next: distribution => { this.distribution = distribution || []; this.isLoading = false; this.cdr.detectChanges(); },
        error: error => { this.showError(error, 'Impossible de charger les indicateurs de distribution.'); this.isLoading = false; }
      });
    } else {
      this.isLoading = false;
    }
  }

  private showError(error: any, fallback: string): void {
    console.error(fallback, error);
    this.errorMessage = error?.error?.detail || error?.error?.message || fallback;
    this.cdr.detectChanges();
  }
}
