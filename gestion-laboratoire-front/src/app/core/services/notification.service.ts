import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationItem } from '../models/collaboration.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly apiUrl = `${environment.apiUrl}/notifications`;

  constructor(private http: HttpClient) {}

  getMine(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(`${this.apiUrl}/moi`);
  }

  getUnread(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(`${this.apiUrl}/moi/non-lues`);
  }

  getUnreadCount(): Observable<{ nonLues: number }> {
    return this.http.get<{ nonLues: number }>(`${this.apiUrl}/moi/compteur`);
  }

  markAllRead(): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/moi/marquer-lues`, {});
  }

  markRead(id: number): Observable<NotificationItem> {
    return this.http.put<NotificationItem>(`${this.apiUrl}/${id}/marquer-lue`, {});
  }
}
