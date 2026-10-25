import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, finalize } from 'rxjs';
import { ConversationSummary, MessageItem } from '../../core/models/collaboration.model';
import { Evenement } from '../../core/models/evenement.model';
import { Membre } from '../../core/models/membre.model';
import { AuthService } from '../../core/services/auth.service';
import { EvenementService } from '../../core/services/evenement.service';
import { MembreService } from '../../core/services/membre.service';
import { MessageService } from '../../core/services/message.service';

type Channel = 'DIRECT' | 'EQUIPE' | 'EVENEMENT';

@Component({
  selector: 'app-messagerie',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messagerie.component.html',
  styleUrl: './messagerie.component.scss'
})
export class MessagerieComponent implements OnInit, OnDestroy {
  readonly teams = ['ISDIAC', 'SIMA', 'ILIAS', 'SDTIC', 'DIRECTION'];
  channel: Channel = 'DIRECT';
  conversations: ConversationSummary[] = [];
  members: Membre[] = [];
  events: Evenement[] = [];
  messages: MessageItem[] = [];
  currentMember: Membre | null = null;
  canAccessMessaging = false;
  selectedContactId: number | null = null;
  selectedTeam = '';
  selectedEventId: number | null = null;
  newRecipientId: number | null = null;
  draft = '';
  isLoading = false;
  isSending = false;
  errorMessage = '';
  intervalId: number | null = null;
  private currentUserId: number | null = null;

  constructor(
    private readonly messageService: MessageService,
    private readonly membreService: MembreService,
    private readonly evenementService: EvenementService,
    private readonly authService: AuthService,
    private readonly route: ActivatedRoute,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.authService.getUserId();
    this.isLoading = true;
    this.membreService.getMonProfil().pipe(finalize(() => {
      this.isLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: member => {
        this.currentMember = member;
        if (member?.statut === 'DOCTORANT' || member?.roleSysteme === 'DOCTORANT') {
          this.errorMessage = 'La messagerie interne est réservée aux membres non doctorants.';
          return;
        }
        this.canAccessMessaging = true;
        this.loadConversationsAndMembers();
        this.loadEvents();
        const contactId = Number(this.route.snapshot.queryParamMap.get('membreId'));
        if (Number.isInteger(contactId) && contactId > 0) {
          this.channel = 'DIRECT';
          this.selectedContactId = contactId;
          this.loadThread();
        }
        const eventId = Number(this.route.snapshot.queryParamMap.get('evenementId'));
        if (Number.isInteger(eventId) && eventId > 0) {
          this.channel = 'EVENEMENT';
          this.selectedEventId = eventId;
          this.loadThread();
        }
      },
      error: error => {
        console.error('Impossible de vérifier les droits de messagerie', error);
        this.errorMessage = 'Impossible de charger votre profil et vos droits de messagerie.';
      }
    });
    this.intervalId = window.setInterval(() => this.refreshSelectedThread(), 30000);
  }

  ngOnDestroy(): void {
    if (this.intervalId !== null) window.clearInterval(this.intervalId);
  }

  get availableTeams(): string[] {
    const member = this.currentMember;
    if (!member) return [];
    const isSupervisor = ['ADMIN', 'DIRECTEUR', 'VICE_DIRECTEUR'].includes(member.role || '')
      || member.roleSysteme === 'ADMIN';
    if (isSupervisor) return this.teams;
    return member.statut === 'PERMANENT' && member.equipeLaboratoire
      && member.equipeLaboratoire !== 'AUCUNE' ? [member.equipeLaboratoire] : [];
  }

  get availableRecipients(): Membre[] {
    return this.members.filter(member =>
      member?.id !== this.currentUserId
      && member?.compteActif === true
      && member?.compteGele !== true
      && !['DOCTORANT', 'ANCIEN', 'RETRAITE'].includes(member?.statut || '')
      && member?.roleSysteme !== 'DOCTORANT'
    );
  }

  get availableEvents(): Evenement[] {
    const currentId = this.currentUserId;
    return this.events.filter(event =>
      event?.organisateurs?.some(member => member?.id === currentId)
      || event?.participants?.some(member => member?.id === currentId)
      || ['ADMIN', 'DIRECTEUR', 'VICE_DIRECTEUR'].includes(this.currentMember?.role || '')
    );
  }

  selectChannel(channel: Channel): void {
    this.channel = channel;
    this.messages = [];
    this.errorMessage = '';
    if (channel === 'DIRECT') this.refreshConversations();
  }

  openConversation(conversation: ConversationSummary): void {
    this.selectedContactId = conversation.autreMembreId;
    this.selectedTeam = '';
    this.selectedEventId = null;
    this.loadThread();
  }

  startConversation(): void {
    if (!this.newRecipientId) return;
    const recipient = this.availableRecipients.find(member => member.id === this.newRecipientId);
    if (!recipient) return;
    this.selectedContactId = recipient.id;
    this.selectedTeam = '';
    this.selectedEventId = null;
    this.loadThread();
  }

  openTeam(team: string): void {
    this.channel = 'EQUIPE';
    this.selectedTeam = team;
    this.selectedContactId = null;
    this.selectedEventId = null;
    this.loadThread();
  }

  openEvent(event: Evenement): void {
    this.channel = 'EVENEMENT';
    this.selectedEventId = event.id;
    this.selectedTeam = '';
    this.selectedContactId = null;
    this.loadThread();
  }

  send(): void {
    const contenu = this.draft.trim();
    if (!contenu || contenu.length > 5000 || this.isSending) return;
    let request;
    if (this.channel === 'DIRECT' && this.selectedContactId) {
      request = this.messageService.sendDirect(this.selectedContactId, contenu);
    } else if (this.channel === 'EQUIPE' && this.selectedTeam) {
      request = this.messageService.sendToTeam(this.selectedTeam, contenu);
    } else if (this.channel === 'EVENEMENT' && this.selectedEventId) {
      request = this.messageService.sendToEvent(this.selectedEventId, contenu);
    } else {
      this.errorMessage = 'Sélectionnez une conversation avant d’envoyer un message.';
      return;
    }

    this.isSending = true;
    this.errorMessage = '';
    request.pipe(finalize(() => {
      this.isSending = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: message => {
        this.messages = [...this.messages, message];
        this.draft = '';
        if (this.channel === 'DIRECT') this.refreshConversations();
      },
      error: error => {
        console.error('Échec de l’envoi du message', error);
        this.errorMessage = error?.error?.message || 'Le message n’a pas pu être envoyé.';
      }
    });
  }

  get threadTitle(): string {
    if (this.channel === 'DIRECT') {
      const contact = this.availableRecipients.find(member => member.id === this.selectedContactId);
      return contact ? `${contact.prenom} ${contact.nom}` : 'Conversation directe';
    }
    if (this.channel === 'EQUIPE') return `Équipe ${this.selectedTeam}`;
    return this.availableEvents.find(event => event.id === this.selectedEventId)?.titre || 'Discussion d’événement';
  }

  trackMessage(_index: number, message: MessageItem): number {
    return message.id;
  }

  isMine(message: MessageItem): boolean {
    return message.expediteur?.id === this.currentUserId;
  }

  private loadConversationsAndMembers(): void {
    this.isLoading = true;
    this.errorMessage = '';
    forkJoin({
      conversations: this.messageService.getConversations(),
      members: this.membreService.getMembres()
    }).pipe(finalize(() => {
      this.isLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: result => {
        this.conversations = result.conversations ?? [];
        this.members = result.members ?? [];
      },
      error: error => {
        console.error('Impossible de charger les conversations ou les membres', error);
        this.errorMessage = error?.error?.message || 'Les conversations ne peuvent pas être chargées.';
      }
    });
  }

  private loadEvents(): void {
    this.evenementService.getEvenements().subscribe({
      next: events => this.events = events ?? [],
      error: error => {
        console.error('Impossible de charger les événements accessibles', error);
        this.errorMessage = 'La liste des événements pour les discussions n’a pas pu être chargée.';
        this.cdr.detectChanges();
      }
    });
  }

  private refreshConversations(): void {
    this.messageService.getConversations().subscribe({
      next: conversations => {
        this.conversations = conversations ?? [];
        this.cdr.detectChanges();
      },
      error: error => console.error('Impossible d’actualiser les conversations', error)
    });
  }

  private loadThread(): void {
    this.isLoading = true;
    this.errorMessage = '';
    let request;
    if (this.channel === 'DIRECT' && this.selectedContactId) {
      request = this.messageService.getDirectThread(this.selectedContactId);
    } else if (this.channel === 'EQUIPE' && this.selectedTeam) {
      request = this.messageService.getTeamThread(this.selectedTeam);
    } else if (this.channel === 'EVENEMENT' && this.selectedEventId) {
      request = this.messageService.getEventThread(this.selectedEventId);
    } else {
      this.isLoading = false;
      this.messages = [];
      return;
    }

    request.pipe(finalize(() => {
      this.isLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: messages => this.messages = messages ?? [],
      error: error => {
        console.error('Impossible de charger cette discussion', error);
        this.errorMessage = error?.error?.message || 'Cette discussion est inaccessible.';
      }
    });
  }

  private refreshSelectedThread(): void {
    if (this.channel === 'DIRECT' && this.selectedContactId) {
      this.refreshConversations();
      this.loadThread();
    } else if (this.channel === 'EQUIPE' && this.selectedTeam) {
      this.loadThread();
    } else if (this.channel === 'EVENEMENT' && this.selectedEventId) {
      this.loadThread();
    }
  }
}
