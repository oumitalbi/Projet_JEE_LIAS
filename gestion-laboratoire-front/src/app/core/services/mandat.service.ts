import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DateCreationLaboratoire, HistoriqueProfil, HistoriqueRole, Mandat, MandatRequest, PeriodeAffiliation
} from '../models/mandat.model';
import { Membre } from '../models/membre.model';

@Injectable({ providedIn: 'root' })
export class MandatService {
  private readonly apiUrl = `${environment.apiUrl}/mandats`;
  private readonly membresUrl = `${environment.apiUrl}/membres`;

  constructor(private http: HttpClient) {}

  getTous(): Observable<Mandat[]> {
    return this.http.get<Mandat[]>(this.apiUrl);
  }

  getActifs(): Observable<Mandat[]> {
    return this.http.get<Mandat[]>(`${this.apiUrl}/actifs`);
  }

  getParMembre(id: number): Observable<Mandat[]> {
    return this.http.get<Mandat[]>(`${this.apiUrl}/membre/${id}`);
  }

  getParEquipe(equipe: string): Observable<Mandat[]> {
    return this.http.get<Mandat[]>(`${this.apiUrl}/equipe/${equipe}`);
  }

  getHistoriqueRoles(id: number): Observable<HistoriqueRole[]> {
    return this.http.get<HistoriqueRole[]>(`${this.apiUrl}/historique-roles/${id}`);
  }

  getAffiliations(id: number): Observable<PeriodeAffiliation[]> {
    return this.http.get<PeriodeAffiliation[]>(`${environment.apiUrl}/affiliations/${id}`);
  }

  getHistoriqueProfil(id: number): Observable<HistoriqueProfil[]> {
    return this.http.get<HistoriqueProfil[]>(`${this.membresUrl}/${id}/historique-profil`);
  }

  getDateCreationLaboratoire(): Observable<DateCreationLaboratoire> {
    return this.http.get<DateCreationLaboratoire>(`${this.apiUrl}/date-creation-laboratoire`);
  }

  getMembres(): Observable<Membre[]> {
    return this.http.get<Membre[]>(this.membresUrl);
  }

  creer(request: MandatRequest): Observable<Mandat> {
    return this.http.post<Mandat>(this.apiUrl, request);
  }

  modifier(id: number, request: Pick<MandatRequest, 'dateDebut' | 'equipe' | 'description'>): Observable<Mandat> {
    return this.http.put<Mandat>(`${this.apiUrl}/${id}`, request);
  }

  cloturer(id: number, dateFin: string, motif: string): Observable<Mandat> {
    return this.http.put<Mandat>(`${this.apiUrl}/${id}/cloturer`, { dateFin, motif });
  }

  mettreEnRetraite(id: number): Observable<Membre> {
    return this.http.put<Membre>(`${this.membresUrl}/${id}/retraite`, {});
  }

  desactiver(id: number): Observable<Membre> {
    return this.http.put<Membre>(`${this.membresUrl}/${id}/desactiver`, {});
  }

  reactiver(id: number, statut: string): Observable<Membre> {
    return this.http.put<Membre>(`${this.membresUrl}/${id}/reactiver`, { statut });
  }
}
