import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { RapportAnnuel, RapportArchiveSummary } from '../../core/models/rapport-annuel.model';
import { RapportAnnuelService } from '../../core/services/rapport-annuel.service';

interface BarItem {
  label: string;
  value: number;
  width: number;
}

@Component({
  selector: 'app-rapports-annuels',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rapports-annuels.component.html',
  styleUrl: './rapports-annuels.component.scss'
})
export class RapportsAnnuelsComponent implements OnInit {
  readonly currentYear = new Date().getFullYear();
  year = this.currentYear;
  report: RapportAnnuel | null = null;
  archives: RapportArchiveSummary[] = [];
  isLoading = false;
  isLoadingHistory = false;
  isExporting = false;
  errorMessage = '';

  constructor(
    private readonly rapportService: RapportAnnuelService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadHistory();
  }

  generateReport(): void {
    const year = Number(this.year);
    if (!Number.isInteger(year) || year < 2000 || year > this.currentYear) {
      this.errorMessage = `Choisissez une année comprise entre 2000 et ${this.currentYear}.`;
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.report = null;
    this.rapportService.generer(year).pipe(finalize(() => {
      this.isLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: report => {
        this.report = report;
        this.loadHistory();
      },
      error: error => {
        console.error('Erreur lors de la génération du rapport annuel', error);
        this.errorMessage = error?.error?.message || 'Impossible de générer le rapport annuel.';
      }
    });
  }

  openArchive(archive: RapportArchiveSummary): void {
    if (archive.id === null) return;
    this.isLoading = true;
    this.errorMessage = '';
    this.rapportService.obtenirArchive(archive.id).pipe(finalize(() => {
      this.isLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: report => this.report = report,
      error: error => {
        console.error('Erreur lors du chargement du rapport archivé', error);
        this.errorMessage = error?.error?.message || 'Impossible de charger cet instantané.';
      }
    });
  }

  exportPdf(): void {
    const report = this.report;
    const id = report?.id;
    if (!report || id === null || id === undefined) return;
    this.isExporting = true;
    this.rapportService.exporterPdf(id).pipe(finalize(() => {
      this.isExporting = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `rapport-annuel-${report.annee}-v${report.version}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: error => {
        console.error('Erreur lors de l’export PDF du rapport', error);
        this.errorMessage = error?.error?.message || 'Impossible de télécharger le PDF.';
      }
    });
  }

  bars(values: Record<string, number> | null | undefined): BarItem[] {
    const entries = Object.entries(values ?? {});
    const max = Math.max(1, ...entries.map(([, value]) => value || 0));
    return entries.map(([label, value]) => ({
      label: this.humanize(label),
      value: value || 0,
      width: Math.round(((value || 0) / max) * 100)
    }));
  }

  private loadHistory(): void {
    this.isLoadingHistory = true;
    this.rapportService.obtenirHistorique().pipe(finalize(() => {
      this.isLoadingHistory = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: archives => this.archives = archives ?? [],
      error: error => {
        console.error('Erreur lors du chargement de l’historique des rapports', error);
        this.archives = [];
      }
    });
  }

  private humanize(value: string): string {
    return value.toLowerCase().replaceAll('_', ' ').replace(/(^|\s)\S/g, match => match.toUpperCase());
  }
}
