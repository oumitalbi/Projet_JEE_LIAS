import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CalendrierItem } from '../models/calendrier.model';

@Injectable({ providedIn: 'root' })
export class CalendrierService {
  private readonly apiUrl = `${environment.apiUrl}/calendrier`;

  constructor(private readonly http: HttpClient) {}

  getMonth(year: number, month: number): Observable<CalendrierItem[]> {
    const params = new HttpParams().set('annee', year).set('mois', month);
    return this.http.get<CalendrierItem[]>(`${this.apiUrl}/mois`, { params });
  }
}
