import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DocumentArchive, TypeDocument } from '../models/document.model';

interface GlobalSearchResponse {
  documents?: DocumentArchive[] | null;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly apiUrl = `${environment.apiUrl}/documents`;
  private readonly searchUrl = `${environment.apiUrl}/recherche/`;

  constructor(private http: HttpClient) {}

  getDocuments(): Observable<DocumentArchive[]> {
    return this.http.get<DocumentArchive[]>(this.apiUrl);
  }

  getArchives(): Observable<DocumentArchive[]> {
    return this.http.get<DocumentArchive[]>(`${this.apiUrl}/archives`);
  }

  rechercherDocuments(keyword: string): Observable<DocumentArchive[]> {
    return this.http.get<GlobalSearchResponse>(this.searchUrl, {
      params: new HttpParams().set('q', keyword)
    }).pipe(map(response => response.documents ?? []));
  }

  uploader(fichier: File, type: TypeDocument, evenementId: number | null): Observable<DocumentArchive> {
    const formData = new FormData();
    formData.append('fichier', fichier);
    formData.append('type', type);
    if (evenementId !== null) {
      formData.append('evenementId', evenementId.toString());
    }
    return this.http.post<DocumentArchive>(`${this.apiUrl}/upload`, formData);
  }

  telecharger(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/${id}`, { responseType: 'blob' });
  }

  archiver(id: number): Observable<DocumentArchive> {
    return this.http.delete<DocumentArchive>(`${this.apiUrl}/${id}`);
  }

  restaurer(id: number): Observable<DocumentArchive> {
    return this.http.put<DocumentArchive>(`${this.apiUrl}/${id}/restaurer`, {});
  }
}
