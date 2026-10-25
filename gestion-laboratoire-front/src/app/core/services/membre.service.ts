import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Membre } from '../models/membre.model';

@Injectable({
  providedIn: 'root'
})
export class MembreService {
  private apiUrl = 'http://localhost:8080/api/membres';

  constructor(private http: HttpClient) { }

  getMembres(): Observable<Membre[]> {
    return this.http.get<Membre[]>(this.apiUrl);
  }

  envoyerLienReinitialisationMotDePasse(membreId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/${membreId}/reinitialiser-mot-de-passe`, {});
  }

  getMonProfil(): Observable<Membre> {
    return this.http.get<Membre>(`${this.apiUrl}/moi`);
  }

  modifierMonProfil(data: Partial<Membre>): Observable<Membre> {
    return this.http.put<Membre>(`${this.apiUrl}/moi`, data);
  }

  uploaderPhoto(file: File): Observable<Membre> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Membre>(`${this.apiUrl}/moi/photo`, formData);
  }
}
