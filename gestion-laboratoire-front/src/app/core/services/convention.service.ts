import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Convention, ConventionRequest, Partenaire, PartenaireRequest } from '../models/convention.model';

@Injectable({ providedIn: 'root' })
export class ConventionService {
  private readonly apiUrl = `${environment.apiUrl}/conventions`;

  constructor(private readonly http: HttpClient) {}

  getConventions(filters: {
    statut?: string;
    partenaireId?: number | null;
    annee?: number | null;
    archives?: boolean;
  } = {}): Observable<Convention[]> {
    let params = new HttpParams();
    if (filters.statut) params = params.set('statut', filters.statut);
    if (filters.partenaireId) params = params.set('partenaireId', filters.partenaireId);
    if (filters.annee) params = params.set('annee', filters.annee);
    if (filters.archives) params = params.set('archives', true);
    return this.http.get<Convention[]>(this.apiUrl, { params });
  }

  getActiveConventions(): Observable<Convention[]> {
    return this.http.get<Convention[]>(`${this.apiUrl}/actives`);
  }

  getConvention(id: number): Observable<Convention> {
    return this.http.get<Convention>(`${this.apiUrl}/${id}`);
  }

  createConvention(request: ConventionRequest): Observable<Convention> {
    return this.http.post<Convention>(this.apiUrl, request);
  }

  updateConvention(id: number, request: ConventionRequest): Observable<Convention> {
    return this.http.put<Convention>(`${this.apiUrl}/${id}`, request);
  }

  attachDocument(id: number, documentId: number): Observable<Convention> {
    return this.http.put<Convention>(`${this.apiUrl}/${id}/document/${documentId}`, {});
  }

  archive(id: number): Observable<Convention> {
    return this.http.delete<Convention>(`${this.apiUrl}/${id}`);
  }

  restore(id: number): Observable<Convention> {
    return this.http.put<Convention>(`${this.apiUrl}/${id}/desarchiver`, {});
  }

  addActivity(id: number, evenementId: number): Observable<Convention> {
    return this.http.put<Convention>(`${this.apiUrl}/${id}/activites/${evenementId}`, {});
  }

  removeActivity(id: number, evenementId: number): Observable<Convention> {
    return this.http.delete<Convention>(`${this.apiUrl}/${id}/activites/${evenementId}`);
  }

  getPartners(archives = false): Observable<Partenaire[]> {
    const params = archives ? new HttpParams().set('archives', true) : undefined;
    return this.http.get<Partenaire[]>(`${this.apiUrl}/partenaires`, { params });
  }

  addPartner(request: PartenaireRequest): Observable<Partenaire> {
    return this.http.post<Partenaire>(`${this.apiUrl}/partenaires`, request);
  }

  updatePartner(id: number, request: PartenaireRequest): Observable<Partenaire> {
    return this.http.put<Partenaire>(`${this.apiUrl}/partenaires/${id}`, request);
  }

  archivePartner(id: number): Observable<Partenaire> {
    return this.http.delete<Partenaire>(`${this.apiUrl}/partenaires/${id}`);
  }
}
