import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdhesionService } from '../../core/services/adhesion.service';
import { DemandeAdhesionReceipt, EquipeCandidat, StatutCandidat } from '../../core/models/adhesion.model';

function lireMessageErreur(error: HttpErrorResponse, fallback: string): string {
  const body: unknown = error.error;
  if (typeof body === 'string' && body.trim()) return body;
  if (body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') {
    return body.message;
  }
  return fallback;
}

type CandidatureControls = {
  nom: FormControl<string>;
  prenom: FormControl<string>;
  email: FormControl<string>;
  statutSouhaite: FormControl<StatutCandidat>;
  etablissementOrigine: FormControl<string>;
  laboratoireOrigine: FormControl<string>;
  equipeSouhaitee: FormControl<EquipeCandidat>;
  lettreMotivation: FormControl<string>;
  centresInteret: FormControl<string>;
  biographie: FormControl<string>;
  consentementDonnees: FormControl<boolean>;
  website: FormControl<string>;
};

@Component({
  selector: 'app-candidature',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './candidature.component.html',
  styleUrl: './candidature.component.scss'
})
export class CandidatureComponent {
  readonly statuts: { value: StatutCandidat; label: string }[] = [
    { value: 'PERMANENT', label: 'Membre permanent' },
    { value: 'ASSOCIE', label: 'Membre associé' },
    { value: 'DOCTORANT', label: 'Doctorant' }
  ];
  readonly equipes: EquipeCandidat[] = ['ISDIAC', 'SIMA', 'SDTIC', 'ILIAS'];
  readonly form: FormGroup<CandidatureControls>;

  cv: File | null = null;
  isLoading = false;
  errorMessage = '';
  receipt: DemandeAdhesionReceipt | null = null;

  constructor(private formBuilder: FormBuilder, private adhesionService: AdhesionService) {
    this.form = this.formBuilder.nonNullable.group({
      nom: ['', [Validators.required, Validators.maxLength(100)]],
      prenom: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      statutSouhaite: ['PERMANENT' as StatutCandidat, Validators.required],
      etablissementOrigine: ['', [Validators.required, Validators.maxLength(200)]],
      laboratoireOrigine: ['', Validators.maxLength(200)],
      equipeSouhaitee: ['ISDIAC' as EquipeCandidat, Validators.required],
      lettreMotivation: ['', [Validators.required, Validators.minLength(50), Validators.maxLength(10000)]],
      centresInteret: ['', Validators.maxLength(1000)],
      biographie: ['', Validators.maxLength(3000)],
      consentementDonnees: [false, Validators.requiredTrue],
      website: ['']
    });
  }

  onCvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.errorMessage = '';
    if (!file) {
      this.cv = null;
      return;
    }
    if ((file.type && file.type !== 'application/pdf') || !file.name.toLowerCase().endsWith('.pdf')
        || file.size > 5 * 1024 * 1024) {
      this.cv = null;
      input.value = '';
      this.errorMessage = 'Le CV doit être un PDF de 5 Mo maximum.';
      return;
    }
    this.cv = file;
  }

  soumettre(): void {
    this.form.markAllAsTouched();
    this.errorMessage = '';
    if (this.form.invalid || !this.cv) {
      if (!this.cv) this.errorMessage = 'Veuillez joindre votre CV au format PDF (5 Mo maximum).';
      return;
    }

    const data = new FormData();
    Object.entries(this.form.getRawValue()).forEach(([key, value]) => {
      data.append(key, String(value ?? ''));
    });
    data.append('cv', this.cv);
    this.isLoading = true;
    this.adhesionService.soumettre(data).subscribe({
      next: receipt => {
        this.receipt = receipt;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = lireMessageErreur(error,
          'La candidature n’a pas pu être transmise. Veuillez réessayer plus tard.');
        this.isLoading = false;
      }
    });
  }
}
