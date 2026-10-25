import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private ngZone: NgZone) {
    const token = this.getToken();
    if (token) {
      const userInfo = this.getStoredUserInfo();
      this.ngZone.run(() => {
        this.currentUserSubject.next({ token, ...userInfo });
      });
    }
  }

  login(credentials: any): Observable<any> {
    // Map frontend form fields to backend DTO fields
    const body = {
      email: credentials.email,
      motDePasse: credentials.password
    };

    return this.http.post<any>(`${this.apiUrl}/login`, body).pipe(
      tap(response => {
        if (response && response.token) {
          localStorage.removeItem('user_photo');
          localStorage.setItem('jwt_token', response.token);
          localStorage.setItem('user_nom', response.nom || '');
          localStorage.setItem('user_prenom', response.prenom || '');
          localStorage.setItem('user_role', response.role || '');
          localStorage.setItem('user_id', response.id?.toString() || '');

          this.ngZone.run(() => {
            this.currentUserSubject.next({
              token: response.token,
              nom: response.nom,
              prenom: response.prenom,
              role: response.role,
              id: response.id
            });
          });
        }
      })
    );
  }

  demanderReinitialisationMotDePasse(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/mot-de-passe-oublie`, { email });
  }

  reinitialiserMotDePasse(
    token: string,
    motDePasse: string,
    confirmationMotDePasse: string
  ): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/reinitialiser-mot-de-passe`, {
      token,
      motDePasse,
      confirmationMotDePasse
    });
  }

  logout(): void {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_nom');
    localStorage.removeItem('user_prenom');
    localStorage.removeItem('user_role');
    localStorage.removeItem('user_id');
    localStorage.removeItem('user_photo');
    this.currentUserSubject.next(null);
  }

  getUserPhoto(): string {
    const userId = this.getUserId();
    return userId === null ? '' : localStorage.getItem(`user_photo_${userId}`) || '';
  }

  setCurrentUserPhoto(photo: string): void {
    const userId = this.getUserId();
    if (userId === null) return;

    const key = `user_photo_${userId}`;
    if (photo) localStorage.setItem(key, photo);
    else localStorage.removeItem(key);

    const currentUser = this.currentUserSubject.value;
    if (currentUser && currentUser.photo !== photo) {
      this.currentUserSubject.next({ ...currentUser, photo });
    }
  }

  getToken(): string | null {
    return localStorage.getItem('jwt_token');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getUserRole(): string {
    return localStorage.getItem('user_role') || '';
  }

  getUserId(): number | null {
    const id = localStorage.getItem('user_id');
    return id ? Number(id) : null;
  }

  getUserFullName(): string {
    const prenom = localStorage.getItem('user_prenom') || '';
    const nom = localStorage.getItem('user_nom') || '';
    return `${prenom} ${nom}`.trim();
  }

  private getStoredUserInfo(): any {
    return {
      nom: localStorage.getItem('user_nom') || '',
      prenom: localStorage.getItem('user_prenom') || '',
      role: localStorage.getItem('user_role') || '',
      id: localStorage.getItem('user_id') || ''
    };
  }
}
