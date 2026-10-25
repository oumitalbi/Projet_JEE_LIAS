import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ArrivageMateriel,
  ArrivageRequest,
  DemandeMateriel,
  Materiel,
  MaterielRequest,
  DistributionEquipe
} from '../models/logistique.model';

@Injectable({ providedIn: 'root' })
export class LogistiqueService {
  private readonly apiUrl = `${environment.apiUrl}/logistique`;

  constructor(private readonly http: HttpClient) {}

  getMateriel(archives = false): Observable<Materiel[]> {
    const params = archives ? new HttpParams().set('archives', true) : undefined;
    return this.http.get<Materiel[]>(`${this.apiUrl}/materiel`, { params });
  }

  getMaterielDisponible(): Observable<Materiel[]> {
    return this.http.get<Materiel[]>(`${this.apiUrl}/materiel/disponible`);
  }

  ajouterMateriel(request: MaterielRequest): Observable<Materiel> {
    return this.http.post<Materiel>(`${this.apiUrl}/materiel`, request);
  }

  modifierMateriel(id: number, request: MaterielRequest): Observable<Materiel> {
    return this.http.put<Materiel>(`${this.apiUrl}/materiel/${id}`, request);
  }

  archiverMateriel(id: number): Observable<Materiel> {
    return this.http.delete<Materiel>(`${this.apiUrl}/materiel/${id}`);
  }

  restaurerMateriel(id: number): Observable<Materiel> {
    return this.http.put<Materiel>(`${this.apiUrl}/materiel/${id}/restaurer`, {});
  }

  soumettreDemande(request: {
    materielId: number | null;
    nomBesoin: string;
    quantite: number;
    justification: string;
  }): Observable<DemandeMateriel> {
    return this.http.post<DemandeMateriel>(`${this.apiUrl}/demandes`, request);
  }

  getDemandes(miennes: boolean, statut?: string): Observable<DemandeMateriel[]> {
    let params = new HttpParams().set('miennes', miennes);
    if (statut) params = params.set('statut', statut);
    return this.http.get<DemandeMateriel[]>(`${this.apiUrl}/demandes`, { params });
  }

  validerDemande(id: number, materielId?: number): Observable<DemandeMateriel> {
    let params = new HttpParams();
    if (materielId != null) params = params.set('materielId', materielId);
    return this.http.put<DemandeMateriel>(`${this.apiUrl}/demandes/${id}/valider`, {}, { params });
  }

  refuserDemande(id: number, motif: string): Observable<DemandeMateriel> {
    return this.http.put<DemandeMateriel>(`${this.apiUrl}/demandes/${id}/refuser`, { motif });
  }

  retournerMateriel(id: number): Observable<DemandeMateriel> {
    return this.http.put<DemandeMateriel>(`${this.apiUrl}/demandes/${id}/retourner`, {});
  }

  enregistrerArrivage(request: ArrivageRequest): Observable<ArrivageMateriel> {
    return this.http.post<ArrivageMateriel>(`${this.apiUrl}/arrivages`, request);
  }

  getArrivages(materielId?: number): Observable<ArrivageMateriel[]> {
    let params = new HttpParams();
    if (materielId != null) params = params.set('materielId', materielId);
    return this.http.get<ArrivageMateriel[]>(`${this.apiUrl}/arrivages`, { params });
  }

  getDistributionEquite(): Observable<DistributionEquipe[]> {
    return this.http.get<DistributionEquipe[]>(`${this.apiUrl}/distributions/equite`);
  }
}
