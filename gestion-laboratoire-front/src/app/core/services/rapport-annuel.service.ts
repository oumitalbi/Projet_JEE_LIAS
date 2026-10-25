import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RapportAnnuel, RapportArchiveSummary } from '../models/rapport-annuel.model';

@Injectable({ providedIn: 'root' })
export class RapportAnnuelService {
  private readonly apiUrl = `${environment.apiUrl}/rapports`;

  constructor(private readonly http: HttpClient) {}

  generer(annee: number): Observable<RapportAnnuel> {
    return this.http.post<RapportAnnuel>(`${this.apiUrl}/annuel/${annee}`, {});
  }

  obtenirHistorique(): Observable<RapportArchiveSummary[]> {
    return this.http.get<RapportArchiveSummary[]>(`${this.apiUrl}/historique`);
  }

  obtenirArchive(id: number): Observable<RapportAnnuel> {
    return this.http.get<RapportAnnuel>(`${this.apiUrl}/historique/${id}`);
  }

  exporterPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/historique/${id}/pdf`, { responseType: 'blob' });
  }
}
