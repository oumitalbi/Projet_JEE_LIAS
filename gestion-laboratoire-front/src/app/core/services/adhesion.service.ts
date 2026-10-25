import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DemandeAdhesion, DemandeAdhesionReceipt, TraitementAdhesionRequest, TraitementAdhesionResponse
} from '../models/adhesion.model';

@Injectable({ providedIn: 'root' })
export class AdhesionService {
  private readonly apiUrl = `${environment.apiUrl.replace(/\/+$/, '')}/adhesions`;

  constructor(private http: HttpClient) {}

  soumettre(form: FormData): Observable<DemandeAdhesionReceipt> {
    return this.http.post<DemandeAdhesionReceipt>(`${this.apiUrl}/soumettre`, form);
  }

  getEnAttente(): Observable<DemandeAdhesion[]> {
    return this.http.get<DemandeAdhesion[]>(`${this.apiUrl}/en-attente`);
  }

  getHistorique(): Observable<DemandeAdhesion[]> {
    return this.http.get<DemandeAdhesion[]>(`${this.apiUrl}/historique`);
  }

  getDemande(id: number): Observable<DemandeAdhesion> {
    return this.http.get<DemandeAdhesion>(`${this.apiUrl}/${id}`);
  }

  traiter(id: number, request: TraitementAdhesionRequest): Observable<TraitementAdhesionResponse> {
    return this.http.put<TraitementAdhesionResponse>(`${this.apiUrl}/${id}/traiter`, request);
  }

  telechargerCv(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/cv`, { responseType: 'blob' });
  }
}
