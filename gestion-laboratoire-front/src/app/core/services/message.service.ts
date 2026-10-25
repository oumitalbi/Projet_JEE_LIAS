import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ConversationSummary,
  MessageItem,
  UnreadCounts
} from '../models/collaboration.model';

@Injectable({ providedIn: 'root' })
export class MessageService {
  private readonly apiUrl = `${environment.apiUrl}/messages`;

  constructor(private http: HttpClient) {}

  getUnreadCounts(): Observable<UnreadCounts> {
    return this.http.get<UnreadCounts>(`${this.apiUrl}/moi/compteur`);
  }

  getConversations(): Observable<ConversationSummary[]> {
    return this.http.get<ConversationSummary[]>(`${this.apiUrl}/moi/conversations`);
  }

  getDirectThread(memberId: number): Observable<MessageItem[]> {
    return this.http.get<MessageItem[]>(`${this.apiUrl}/conversation/${memberId}`);
  }

  getTeamThread(team: string): Observable<MessageItem[]> {
    return this.http.get<MessageItem[]>(`${this.apiUrl}/equipe/${encodeURIComponent(team)}`);
  }

  getEventThread(eventId: number): Observable<MessageItem[]> {
    return this.http.get<MessageItem[]>(`${this.apiUrl}/evenement/${eventId}`);
  }

  sendDirect(recipientId: number, contenu: string): Observable<MessageItem> {
    return this.http.post<MessageItem>(`${this.apiUrl}/envoyer/direct`, { destinataireId: recipientId, contenu });
  }

  sendToTeam(equipe: string, contenu: string): Observable<MessageItem> {
    return this.http.post<MessageItem>(`${this.apiUrl}/envoyer/equipe`, { equipe, contenu });
  }

  sendToEvent(evenementId: number, contenu: string): Observable<MessageItem> {
    return this.http.post<MessageItem>(`${this.apiUrl}/envoyer/evenement`, { evenementId, contenu });
  }
}
