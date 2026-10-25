import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Reunion, ReunionRequest, StatutReunion } from '../models/reunion.model';

@Injectable({ providedIn: 'root' })
export class ReunionService {
  private readonly apiUrl = `${environment.apiUrl}/reunions`;

  constructor(private readonly http: HttpClient) {}

  getReunions(archivees = false): Observable<Reunion[]> {
    return this.http.get<Reunion[]>(this.apiUrl, {
      params: new HttpParams().set('archivees', archivees)
    });
  }

  getReunionsDuMembre(membreId: number): Observable<Reunion[]> {
    return this.http.get<Reunion[]>(`${this.apiUrl}/membre/${membreId}`);
  }

  getReunion(id: number): Observable<Reunion> {
    return this.http.get<Reunion>(`${this.apiUrl}/${id}`);
  }

  planifier(request: ReunionRequest): Observable<Reunion> {
    return this.http.post<Reunion>(this.apiUrl, request);
  }

  modifier(id: number, request: ReunionRequest): Observable<Reunion> {
    return this.http.put<Reunion>(`${this.apiUrl}/${id}`, request);
  }

  ajouterParticipant(id: number, membreId: number): Observable<Reunion> {
    return this.http.put<Reunion>(`${this.apiUrl}/${id}/participants/${membreId}`, {});
  }

  retirerParticipant(id: number, membreId: number): Observable<Reunion> {
    return this.http.delete<Reunion>(`${this.apiUrl}/${id}/participants/${membreId}`);
  }

  changerStatut(id: number, statut: StatutReunion): Observable<Reunion> {
    return this.http.put<Reunion>(`${this.apiUrl}/${id}/statut`, {}, {
      params: new HttpParams().set('statut', statut)
    });
  }

  attacherPv(id: number, documentId: number): Observable<Reunion> {
    return this.http.put<Reunion>(`${this.apiUrl}/${id}/pv/${documentId}`, {});
  }

  archiver(id: number): Observable<Reunion> {
    return this.http.put<Reunion>(`${this.apiUrl}/${id}/archiver`, {});
  }

  desarchiver(id: number): Observable<Reunion> {
    return this.http.put<Reunion>(`${this.apiUrl}/${id}/desarchiver`, {});
  }
}
