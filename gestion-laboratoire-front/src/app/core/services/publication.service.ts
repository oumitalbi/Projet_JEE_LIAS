import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Publication, PublicationCreateRequest } from '../models/publication.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PublicationService {
  private apiUrl = `${environment.apiUrl}/publications`;

  constructor(private http: HttpClient) {}

  getPublications(): Observable<Publication[]> {
    return this.http.get<Publication[]>(this.apiUrl);
  }

  getPublicationsParEquipe(equipe: string): Observable<Publication[]> {
    return this.http.get<Publication[]>(`${this.apiUrl}/equipe/${equipe}`);
  }

  getPublicationsParAuteur(membreId: number): Observable<Publication[]> {
    return this.http.get<Publication[]>(`${this.apiUrl}/auteur/${membreId}`);
  }

  ajouterPublication(data: PublicationCreateRequest): Observable<Publication> {
    return this.http.post<Publication>(this.apiUrl, data);
  }

  archiverPublication(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getPublicationsArchivees(): Observable<Publication[]> {
    return this.http.get<Publication[]>(`${this.apiUrl}/archivees`);
  }

  desarchiverPublication(id: number): Observable<Publication> {
    return this.http.put<Publication>(`${this.apiUrl}/${id}/desarchiver`, {});
  }
}
