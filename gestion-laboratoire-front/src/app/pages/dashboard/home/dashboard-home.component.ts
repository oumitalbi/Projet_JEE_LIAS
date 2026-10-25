import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, finalize, timeout } from 'rxjs/operators';
import { DocumentArchive } from '../../../core/models/document.model';
import { Evenement } from '../../../core/models/evenement.model';
import { Membre } from '../../../core/models/membre.model';
import { Publication } from '../../../core/models/publication.model';
import { Reunion } from '../../../core/models/reunion.model';
import { DocumentService } from '../../../core/services/document.service';
import { EvenementService } from '../../../core/services/evenement.service';
import { MembreService } from '../../../core/services/membre.service';
import { PublicationService } from '../../../core/services/publication.service';
import { ReunionService } from '../../../core/services/reunion.service';

interface DashboardStat {
  label: string;
  value: number | string;
  icon: string;
  trend: string;
  route: string;
}

interface ActivityItem {
  label: string;
  title: string;
  date: string | null;
  route: string;
}

interface DashboardCalendarItem {
  id: number;
  source: 'EVENEMENT' | 'REUNION';
  titre: string;
  dateDebut: string;
  type: string;
}

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard-home.component.html',
  styleUrl: './dashboard-home.component.scss'
})
export class DashboardHomeComponent implements OnInit {
  currentDate = new Date();
  isLoading = true;
  hasLoadError = false;
  authenticationError = false;
  stats: DashboardStat[] = [
    { label: 'Publications', value: '…', icon: 'file', trend: 'publications actives', route: '/dashboard/publications' },
    { label: 'Doctorants', value: '…', icon: 'graduation', trend: 'membres actifs', route: '/dashboard/membres' },
    { label: 'Événements à venir', value: '…', icon: 'calendar', trend: 'événements actifs', route: '/dashboard/evenements' },
    { label: 'Documents actifs', value: '…', icon: 'folder', trend: 'documents non archivés', route: '/dashboard/documents' },
    { label: 'Membres actifs', value: '…', icon: 'users', trend: 'comptes actifs', route: '/dashboard/membres' },
    { label: 'Réunions à venir', value: '…', icon: 'meeting', trend: 'réunions visibles', route: '/dashboard/reunions' }
  ];
  calendarItems: DashboardCalendarItem[] = [];
  recentActivity: ActivityItem[] = [];

  constructor(
    private readonly changeDetectorRef: ChangeDetectorRef,
    private readonly publicationService: PublicationService,
    private readonly membreService: MembreService,
    private readonly evenementService: EvenementService,
    private readonly documentService: DocumentService,
    private readonly reunionService: ReunionService
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  private loadDashboard(): void {
    forkJoin({
      publications: this.loadOrEmpty(this.publicationService.getPublications()),
      members: this.loadOrEmpty(this.membreService.getMembres()),
      events: this.loadOrEmpty(this.evenementService.getEvenementsAVenir()),
      allEvents: this.loadOrEmpty(this.evenementService.getEvenements()),
      documents: this.loadOrEmpty(this.documentService.getDocuments()),
      meetings: this.loadOrEmpty(this.reunionService.getReunions())
    }).pipe(
      timeout({ first: 15000 }),
      catchError(error => {
        console.error('Impossible de charger les données du tableau de bord', error);
        this.hasLoadError = true;
        return of({
          publications: [],
          members: [],
          events: [],
          allEvents: [],
          documents: [],
          meetings: []
        });
      }),
      finalize(() => {
        this.isLoading = false;
        if (this.hasLoadError) {
          this.stats = this.stats.map(stat => ({ ...stat, value: '—' }));
        }
        this.changeDetectorRef.markForCheck();
      })
    ).subscribe({
      next: data => {
        const events = data.events.filter(event => event?.statut !== 'ANNULE');
        const members = data.members.filter(member => member?.compteActif !== false
          && member?.compteGele !== true
          && !['ANCIEN', 'RETRAITE'].includes(member?.statut ?? ''));
        const meetings = data.meetings.filter(meeting => !meeting?.archivee
          && meeting?.statut !== 'ANNULEE'
          && new Date(meeting?.dateDebut ?? 0).getTime() >= Date.now());
        const documents = data.documents.filter(document => !document?.estArchive);
        this.stats = [
          { label: 'Publications', value: data.publications.length, icon: 'file', trend: 'publications actives', route: '/dashboard/publications' },
          { label: 'Doctorants', value: members.filter(member => member?.statut === 'DOCTORANT').length, icon: 'graduation', trend: 'membres actifs', route: '/dashboard/membres' },
          { label: 'Événements à venir', value: events.length, icon: 'calendar', trend: 'événements actifs', route: '/dashboard/evenements' },
          { label: 'Documents actifs', value: documents.length, icon: 'folder', trend: 'documents non archivés', route: '/dashboard/documents' },
          { label: 'Membres actifs', value: members.length, icon: 'users', trend: 'comptes actifs', route: '/dashboard/membres' },
          { label: 'Réunions à venir', value: meetings.length, icon: 'meeting', trend: 'réunions visibles', route: '/dashboard/reunions' }
        ];
        this.calendarItems = [
          ...events.map(event => ({
            id: event.id,
            source: 'EVENEMENT' as const,
            titre: event.titre,
            dateDebut: event.dateDebut ?? '',
            type: event.type ?? 'EVENEMENT'
          })),
          ...meetings.map(meeting => ({
            id: meeting.id,
            source: 'REUNION' as const,
            titre: meeting.titre,
            dateDebut: meeting.dateDebut,
            type: 'REUNION'
          }))
        ]
          .filter(item => item.dateDebut && !Number.isNaN(new Date(item.dateDebut).getTime()))
          .sort((left, right) => new Date(left.dateDebut).getTime() - new Date(right.dateDebut).getTime())
          .slice(0, 6);
        this.recentActivity = this.buildActivity(data.allEvents, data.publications, data.documents, data.meetings);
      },
      error: error => {
        console.error('Erreur lors du chargement des données du tableau de bord', error);
        this.hasLoadError = true;
      }
    });
  }

  private loadOrEmpty<T>(request: Observable<T[]>): Observable<T[]> {
    return request.pipe(catchError(error => {
      console.error('Une source de données du tableau de bord n’a pas pu être chargée', error);
      this.hasLoadError = true;
      this.authenticationError ||= error?.status === 401 || error?.status === 403;
      return of([]);
    }));
  }

  private buildActivity(
    events: Evenement[],
    publications: Publication[],
    documents: DocumentArchive[],
    meetings: Reunion[]
  ): ActivityItem[] {
    const activity: ActivityItem[] = [
      ...events.map(event => ({
        label: 'Événement',
        title: event?.titre ?? 'Événement sans titre',
        date: event?.dateCreation ?? event?.dateDebut ?? null,
        route: '/dashboard/evenements'
      })),
      ...publications.map(publication => ({
        label: 'Publication',
        title: publication?.titre ?? 'Publication sans titre',
        date: publication?.dateMiseAJour ?? publication?.datePublication ?? null,
        route: '/dashboard/publications'
      })),
      ...documents.map(document => ({
        label: 'Document',
        title: document?.nomFichier ?? 'Document sans nom',
        date: document?.dateUpload ?? null,
        route: '/dashboard/documents'
      })),
      ...meetings.map(meeting => ({
        label: 'Réunion',
        title: meeting?.titre ?? 'Réunion sans titre',
        date: meeting?.dateCreation ?? meeting?.dateDebut ?? null,
        route: '/dashboard/reunions'
      }))
    ];
    return activity
      .filter(item => item.date && !Number.isNaN(new Date(item.date).getTime()))
      .sort((left, right) => new Date(right.date!).getTime() - new Date(left.date!).getTime())
      .slice(0, 6);
  }
}
