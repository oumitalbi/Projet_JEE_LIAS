import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { CalendrierItem } from '../../core/models/calendrier.model';
import { CalendrierService } from '../../core/services/calendrier.service';

interface CalendarDay {
  date: Date;
  inCurrentMonth: boolean;
  isToday: boolean;
  items: CalendrierItem[];
}

type CalendarFilter = 'TOUS' | 'EVENEMENT' | 'REUNION' | 'CONFERENCE' | 'SEMINAIRE' | 'WORKSHOP';

@Component({
  selector: 'app-calendrier',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './calendrier.component.html',
  styleUrl: './calendrier.component.scss'
})
export class CalendrierComponent implements OnInit {
  currentMonth = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  items: CalendrierItem[] = [];
  calendarDays: CalendarDay[] = [];
  filter: CalendarFilter = 'TOUS';
  onlyMine = false;
  isLoading = false;
  errorMessage = '';
  readonly weekdays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
  readonly filters: { value: CalendarFilter; label: string }[] = [
    { value: 'TOUS', label: 'Tous les types' },
    { value: 'EVENEMENT', label: 'Tous les événements' },
    { value: 'CONFERENCE', label: 'Conférences' },
    { value: 'SEMINAIRE', label: 'Séminaires' },
    { value: 'WORKSHOP', label: 'Workshops' },
    { value: 'REUNION', label: 'Réunions' }
  ];

  constructor(
    private readonly calendrierService: CalendrierService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadMonth();
  }

  get monthLabel(): string {
    return new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(this.currentMonth);
  }

  get filteredItems(): CalendrierItem[] {
    return this.items.filter(item => {
      const matchesType = this.filter === 'TOUS'
        || (this.filter === 'EVENEMENT' ? item.source === 'EVENEMENT'
          : this.filter === 'REUNION' ? item.source === 'REUNION'
            : item.type === this.filter);
      return matchesType && (!this.onlyMine || item.concerneMembre);
    });
  }

  previousMonth(): void {
    this.shiftMonth(-1);
  }

  nextMonth(): void {
    this.shiftMonth(1);
  }

  goToToday(): void {
    const today = new Date();
    this.currentMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    this.loadMonth();
  }

  openItem(item: CalendrierItem): void {
    if (item.source === 'REUNION') {
      this.router.navigate(['/dashboard/reunions'], { queryParams: { reunionId: item.id } });
      return;
    }
    this.router.navigate(['/dashboard/evenements'], { queryParams: { evenementId: item.id } });
  }

  itemTime(item: CalendrierItem): string {
    const date = this.parseDate(item.dateDebut);
    return date ? new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(date) : '';
  }

  trackByDate(_index: number, day: CalendarDay): string {
    return `${day.date.getFullYear()}-${day.date.getMonth()}-${day.date.getDate()}`;
  }

  trackByItem(_index: number, item: CalendrierItem): string {
    return `${item.source}-${item.id}`;
  }

  itemClass(item: CalendrierItem): string {
    if (item.annule) return 'cancelled';
    if (item.source === 'REUNION') return item.statut === 'ANNULEE' ? 'cancelled' : 'meeting';
    switch (item.type) {
      case 'CONFERENCE': return 'conference';
      case 'SEMINAIRE': return 'seminar';
      case 'WORKSHOP': return 'workshop';
      default: return 'event';
    }
  }

  itemLabel(item: CalendrierItem): string {
    if (item.annule) return 'Annulé';
    if (item.source === 'REUNION') return 'Réunion';
    switch (item.type) {
      case 'CONFERENCE': return 'Conférence';
      case 'SEMINAIRE': return 'Séminaire';
      case 'WORKSHOP': return 'Workshop';
      case 'SOUTENANCE_THESE': return 'Soutenance';
      default: return 'Événement';
    }
  }

  private shiftMonth(amount: number): void {
    this.currentMonth = new Date(this.currentMonth.getFullYear(), this.currentMonth.getMonth() + amount, 1);
    this.loadMonth();
  }

  private loadMonth(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.calendrierService.getMonth(this.currentMonth.getFullYear(), this.currentMonth.getMonth() + 1)
      .pipe(finalize(() => {
        this.isLoading = false;
        this.buildCalendarDays();
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: items => this.items = (items ?? []).filter(item => item && Number.isFinite(item.id)),
        error: error => {
          console.error('Impossible de charger le calendrier du mois', error);
          this.items = [];
          this.errorMessage = error?.error?.message
            || 'Impossible de charger le calendrier. Vérifiez vos droits ou réessayez.';
        }
      });
  }

  buildCalendarDays(): void {
    const year = this.currentMonth.getFullYear();
    const month = this.currentMonth.getMonth();
    const firstDay = new Date(year, month, 1);
    const offsetMondayFirst = (firstDay.getDay() + 6) % 7;
    const dayCount = new Date(year, month + 1, 0).getDate();
    const cellCount = Math.ceil((offsetMondayFirst + dayCount) / 7) * 7;
    const today = new Date();
    const byDay = new Map<number, CalendrierItem[]>();
    for (const item of this.filteredItems) {
      const date = this.parseDate(item.dateDebut);
      if (!date) continue;
      const day = date.getDate();
      byDay.set(day, [...(byDay.get(day) || []), item]);
    }
    this.calendarDays = Array.from({ length: cellCount }, (_unused, index) => {
      const date = new Date(year, month, index - offsetMondayFirst + 1);
      const inCurrentMonth = date.getMonth() === month;
      return {
        date,
        inCurrentMonth,
        isToday: date.toDateString() === today.toDateString(),
        items: inCurrentMonth ? byDay.get(date.getDate()) || [] : []
      };
    });
  }

  private parseDate(value: string | null | undefined): Date | null {
    if (!value) return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }
}
