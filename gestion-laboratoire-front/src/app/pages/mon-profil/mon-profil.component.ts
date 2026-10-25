import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MembreService } from '../../core/services/membre.service';
import { Membre } from '../../core/models/membre.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-mon-profil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './mon-profil.component.html',
  styleUrls: ['./mon-profil.component.scss']
})
export class MonProfilComponent implements OnInit {
  profilForm!: FormGroup;
  membreActuel: Membre | null = null;
  isLoading = false;
  isSaving = false;
  showSuccessToast = false;

  constructor(
    private fb: FormBuilder,
    private membreService: MembreService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadProfil();
  }

  initForm(): void {
    this.profilForm = this.fb.group({
      titreAcademique: [''],
      nom: [''],
      prenom: [''],
      biographie: [''],
      etablissementOrigine: ['']
    });
  }

  loadProfil(): void {
    this.isLoading = true;
    this.cdr.detectChanges();
    this.membreService.getMonProfil().subscribe({
      next: (membre) => {
        if (membre) {
          this.membreActuel = membre;
          this.profilForm.patchValue({
            titreAcademique: membre.titreAcademique || '',
            nom: membre.nom || '',
            prenom: membre.prenom || '',
            biographie: membre.biographie || '',
            etablissementOrigine: membre.etablissementOrigine || ''
          });
        }
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur lors du chargement du profil', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onSubmit(): void {
    if (this.profilForm.invalid || !this.profilForm.dirty) return;

    this.isSaving = true;
    this.showSuccessToast = false;
    
    this.membreService.modifierMonProfil(this.profilForm.value).subscribe({
      next: (membreMisAJour) => {
        this.membreActuel = membreMisAJour;
        this.profilForm.markAsPristine();
        this.isSaving = false;
        this.showSuccessToast = true;
        
        // Hide toast after 3 seconds
        setTimeout(() => {
          this.showSuccessToast = false;
        }, 3000);
      },
      error: (err) => {
        console.error('Erreur lors de la sauvegarde du profil', err);
        this.isSaving = false;
      }
    });
  }

  onPhotoSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.isLoading = true;
      this.cdr.detectChanges();
      this.membreService.uploaderPhoto(file).subscribe({
        next: (membreMisAJour) => {
          this.membreActuel = membreMisAJour;
          this.authService.setCurrentUserPhoto(membreMisAJour.photo || '');
          
          this.isLoading = false;
          this.showSuccessToast = true;
          this.cdr.detectChanges();
          
          setTimeout(() => {
            this.showSuccessToast = false;
            this.cdr.detectChanges();
          }, 3000);
        },
        error: (err) => {
          console.error('Erreur lors de l\'upload de la photo', err);
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }
}
