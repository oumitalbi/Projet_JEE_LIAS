import { Component, Output, EventEmitter, OnInit, OnDestroy, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { MessageService } from '../../../../core/services/message.service';
import { MembreService } from '../../../../core/services/membre.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss'
})
export class TopbarComponent implements OnInit, OnDestroy {
  @Output() toggleSidebar = new EventEmitter<void>();

  searchQuery = '';
  showProfileMenu = false;
  unreadNotifications = 0;
  unreadMessages = 0;
  private countsSubscription?: Subscription;

  userName = '';
  userEmail = '';
  userPhoto = '';

  constructor(
    private authService: AuthService,
    private messageService: MessageService,
    private membreService: MembreService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    this.refreshUnreadCounts();
    this.countsSubscription = interval(30000).subscribe(() => this.refreshUnreadCounts());
    this.ngZone.run(() => {
      this.userName = this.authService.getUserFullName() || 'Membre LIAS';
      this.userPhoto = this.authService.getUserPhoto();
      this.cdr.detectChanges();
    });

    // Subscribe to auth state changes for reactive updates
    this.authService.currentUser$.subscribe(user => {
      this.ngZone.run(() => {
        if (user) {
          this.userName = `${user.prenom || ''} ${user.nom || ''}`.trim() || 'Membre LIAS';
          this.userEmail = user.email || '';
          this.userPhoto = user.photo || this.authService.getUserPhoto();
          this.loadCurrentUserPhoto();
        } else {
          this.userPhoto = '';
          this.userName = '';
          this.userEmail = '';
        }
        this.cdr.detectChanges();
      });
    });
  }

  ngOnDestroy(): void {
    this.countsSubscription?.unsubscribe();
  }

  onMenuToggle(): void {
    this.toggleSidebar.emit();
  }

  toggleProfileMenu(): void {
    this.showProfileMenu = !this.showProfileMenu;
  }

  closeProfileMenu(): void {
    this.showProfileMenu = false;
  }

  navigateToNotifications(): void {
    this.router.navigate(['/dashboard/notifications']);
  }

  navigateToMessages(): void {
    this.router.navigate(['/dashboard/messagerie']);
  }

  navigateToProfile(): void {
    this.closeProfileMenu();
    this.router.navigate(['/dashboard/mon-profil']);
  }

  logout(): void {
    this.closeProfileMenu();
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private refreshUnreadCounts(): void {
    this.messageService.getUnreadCounts().subscribe({
      next: counts => {
        this.unreadNotifications = counts?.notifications ?? 0;
        this.unreadMessages = counts?.messagesTotal ?? 0;
        this.cdr.detectChanges();
      },
      error: error => console.error('Impossible de mettre à jour les compteurs non lus', error)
    });
  }

  private loadCurrentUserPhoto(): void {
    this.membreService.getMonProfil().subscribe({
      next: membre => this.authService.setCurrentUserPhoto(membre.photo || ''),
      error: error => console.error('Impossible de charger la photo du profil connecté', error)
    });
  }

}
