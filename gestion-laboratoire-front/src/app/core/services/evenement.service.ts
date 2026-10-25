import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Evenement, EvenementPublic, EvenementRequest } from '../models/evenement.model';

@Injectable({
  providedIn: 'root'
})
export class EvenementService {
  private readonly apiUrl = `${environment.apiUrl}/evenements`;

  constructor(private http: HttpClient) {}

  getEvenements(): Observable<Evenement[]> {
    return this.http.get<Evenement[]>(this.apiUrl);
  }

  getEvenementsAVenir(): Observable<Evenement[]> {
    return this.http.get<Evenement[]>(`${this.apiUrl}/a-venir`);
  }

  getEvenementsPublicsAVenir(): Observable<EvenementPublic[]> {
    return this.http.get<EvenementPublic[]>(`${this.apiUrl}/public/a-venir`);
  }

  getEvenementsAnnules(): Observable<Evenement[]> {
    return this.http.get<Evenement[]>(`${this.apiUrl}/annules`);
  }

  getEvenementsParParticipant(membreId: number): Observable<Evenement[]> {
    return this.http.get<Evenement[]>(`${this.apiUrl}/participant/${membreId}`);
  }

  getEvenement(id: number): Observable<Evenement> {
    return this.http.get<Evenement>(`${this.apiUrl}/${id}`);
  }

  creerEvenement(request: EvenementRequest): Observable<Evenement> {
    return this.http.post<Evenement>(this.apiUrl, request);
  }

  modifierEvenement(id: number, request: EvenementRequest): Observable<Evenement> {
    return this.http.put<Evenement>(`${this.apiUrl}/${id}`, request);
  }

  affecterOrganisateur(evenementId: number, membreId: number): Observable<Evenement> {
    return this.http.put<Evenement>(`${this.apiUrl}/${evenementId}/organisateurs/${membreId}`, {});
  }

  retirerOrganisateur(evenementId: number, membreId: number): Observable<Evenement> {
    return this.http.delete<Evenement>(`${this.apiUrl}/${evenementId}/organisateurs/${membreId}`);
  }

  inscrire(id: number): Observable<Evenement> {
    return this.http.post<Evenement>(`${this.apiUrl}/${id}/inscription`, {});
  }

  desinscrire(id: number): Observable<Evenement> {
    return this.http.delete<Evenement>(`${this.apiUrl}/${id}/inscription`);
  }

  annulerEvenement(id: number, motif: string): Observable<Evenement> {
    return this.http.delete<Evenement>(`${this.apiUrl}/${id}`, { body: { motif } });
  }
}
