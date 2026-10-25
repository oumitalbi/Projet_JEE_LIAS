import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { NotificationItem, TypeNotification } from '../../core/models/collaboration.model';
import { NotificationService } from '../../core/services/notification.service';

type NotificationFilter = 'TOUTES' | 'NON_LUES' | TypeNotification;

@Component({
  selector: 'app-notifications-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './notifications-list.component.html',
  styleUrl: './notifications-list.component.scss'
})
export class NotificationsListComponent implements OnInit {
  notifications: NotificationItem[] = [];
  filter: NotificationFilter = 'TOUTES';
  isLoading = false;
  isUpdating = false;
  errorMessage = '';

  readonly types: { value: TypeNotification; label: string }[] = [
    { value: 'EVENEMENT', label: 'Événements' },
    { value: 'DOCUMENT', label: 'Documents' },
    { value: 'REUNION', label: 'Réunions' },
    { value: 'CONVENTION', label: 'Conventions' },
    { value: 'LOGISTIQUE', label: 'Logistique' },
    { value: 'MANDAT', label: 'Mandats' },
    { value: 'ADHESION', label: 'Adhésions' },
    { value: 'PUBLICATION', label: 'Publications' },
    { value: 'MESSAGE', label: 'Messages' },
    { value: 'SYSTEME', label: 'Système' }
  ];

  constructor(
    private readonly notificationService: NotificationService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadNotifications();
  }

  get filteredNotifications(): NotificationItem[] {
    if (this.filter === 'NON_LUES') return this.notifications.filter(item => !item?.lue);
    if (this.filter === 'TOUTES') return this.notifications;
    return this.notifications.filter(item => item?.typeNotification === this.filter);
  }

  setFilter(value: string): void {
    this.filter = value as NotificationFilter;
  }

  loadNotifications(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.notificationService.getMine().pipe(finalize(() => {
      this.isLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: items => this.notifications = (items ?? []).map(item => this.normalizeNotification(item)),
      error: error => {
        console.error('Impossible de charger les notifications', error);
        this.errorMessage = 'Les notifications ne peuvent pas être chargées. Réessayez plus tard.';
      }
    });
  }

  markAllRead(): void {
    this.isUpdating = true;
    this.errorMessage = '';
    this.notificationService.markAllRead().pipe(finalize(() => {
      this.isUpdating = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: () => this.notifications = this.notifications.map(item => ({ ...item, lue: true })),
      error: error => {
        console.error('Impossible de marquer les notifications comme lues', error);
        this.errorMessage = 'La mise à jour des notifications a échoué.';
      }
    });
  }

  openNotification(notification: NotificationItem): void {
    if (!notification?.lue) {
      this.notificationService.markRead(notification.id).subscribe({
        next: updated => {
          this.notifications = this.notifications.map(item => item.id === updated.id ? updated : item);
          this.cdr.detectChanges();
        },
        error: error => console.error('Impossible de marquer cette notification comme lue', error)
      });
    }
    this.navigateToTarget(notification);
  }

  targetRoute(notification: NotificationItem): string {
    switch ((notification?.cibleType || notification?.typeNotification || '').toUpperCase()) {
      case 'EVENEMENT': return '/dashboard/evenements';
      case 'DOCUMENT': return '/dashboard/documents';
      case 'REUNION': return '/dashboard/reunions';
      case 'CONVENTION': return '/dashboard/conventions';
      case 'PUBLICATION': return '/dashboard/publications';
      case 'MESSAGE': return '/dashboard/messagerie';
      case 'ADHESION': return '/dashboard/adhesions';
      case 'LOGISTIQUE': return '/dashboard/logistique';
      case 'MANDAT': return '/dashboard/gouvernance';
      default: return '/dashboard/notifications';
    }
  }

  trackById(_index: number, item: NotificationItem): number {
    return item.id;
  }

  relativeDate(value: string): string {
    const minutes = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 60000));
    if (minutes < 1) return 'À l’instant';
    if (minutes < 60) return `Il y a ${minutes} min`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `Il y a ${hours} h`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `Il y a ${days} j`;
    return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium' }).format(new Date(value));
  }

  iconFor(type: TypeNotification): string {
    const icons: Record<TypeNotification, string> = {
      EVENEMENT: 'EV',
      DOCUMENT: 'DOC',
      REUNION: 'R',
      CONVENTION: 'C',
      LOGISTIQUE: 'L',
      MANDAT: 'M',
      ADHESION: 'A',
      PUBLICATION: 'P',
      MESSAGE: '✉',
      SYSTEME: 'i'
    };
    return icons[type] || 'i';
  }

  private navigateToTarget(notification: NotificationItem): void {
    const route = this.targetRoute(notification);
    if (notification?.cibleId) {
      const queryKey = notification.typeNotification === 'MESSAGE'
        ? 'membreId'
        : notification.typeNotification === 'EVENEMENT' ? 'evenementId' : 'id';
      this.router.navigate([route], { queryParams: { [queryKey]: notification.cibleId } });
      return;
    }
    this.router.navigateByUrl(route);
  }

  private normalizeNotification(value: NotificationItem): NotificationItem {
    const item = value as NotificationItem & Record<string, unknown>;
    const type = String(item.typeNotification ?? item['type'] ?? 'SYSTEME').toUpperCase() as TypeNotification;
    return {
      ...item,
      id: Number(item.id),
      titre: String(item.titre ?? item['title'] ?? item['nom'] ?? 'Notification'),
      contenu: String(item.contenu ?? item['message'] ?? item['description'] ?? ''),
      typeNotification: this.types.some(option => option.value === type) ? type : 'SYSTEME',
      cibleType: item.cibleType == null ? null : String(item.cibleType),
      cibleId: item.cibleId == null ? null : Number(item.cibleId),
      lue: Boolean(item.lue),
      archivee: Boolean(item.archivee),
      dateCreation: String(item.dateCreation ?? item['createdAt'] ?? new Date().toISOString())
    };
  }
}
