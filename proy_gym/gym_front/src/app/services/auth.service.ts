import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, map } from 'rxjs';
import { API_ENDPOINTS } from '../config/api-config';
import { clearAuthStorage } from '../utils/token.helper';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private http: HttpClient) {}

  login(dni: number, password: string): Observable<any> {
    return this.http.post(API_ENDPOINTS.AUTH_LOGIN, { dni, password }).pipe(
      tap((response: any) => {
        if (response.token) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('dni', response.dni.toString());
          localStorage.setItem('nombre', response.nombre);
          localStorage.setItem('apellido', response.apellido || '');
          localStorage.setItem('email', response.email || '');
          localStorage.setItem('rol', response.rol || 'CLIENTE');
          localStorage.setItem('usuario', JSON.stringify(response));
        }
      })
    );
  }

  logout(): void {
    clearAuthStorage();
  }

  isLoggedIn(): boolean {
    const token = localStorage.getItem('token');
    if (!token) return false;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expirationDate = new Date(payload.exp * 1000);
      return expirationDate > new Date();
    } catch {
      return false;
    }
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getUserRole(): string | null {
    return localStorage.getItem('rol');
  }

  getUserDni(): string | null {
    return localStorage.getItem('dni');
  }

  getUserName(): string | null {
    return localStorage.getItem('nombre');
  }

  getUserInfo(): any {
    const userStr = localStorage.getItem('usuario');
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  }

  isAdmin(): boolean {
    const rol = this.getUserRole();
    return rol === 'ADMIN';
  }

  isPersonalOrAdmin(): boolean {
    const rol = this.getUserRole();
    return rol === 'PERSONAL' || rol === 'ADMIN';
  }

  existeDni(dni: number): Observable<{ existe: boolean }> {
    return this.http.get<{ existe: boolean }>(API_ENDPOINTS.USUARIO_EXISTE_DNI(dni));
  }

  existeEmail(email: string): Observable<{ existe: boolean }> {
    return this.http.get<{ existe: boolean }>(API_ENDPOINTS.USUARIO_EXISTE_EMAIL(email));
  }

  existeTelefono(telefono: string): Observable<{ existe: boolean }> {
    return this.http.get<{ existe: boolean }>(API_ENDPOINTS.USUARIO_EXISTE_TELEFONO(telefono));
  }

  retomarPago(usuario: any): Observable<{ tempToken: string; dni: number; nombre: string; mensaje: string }> {
    return this.http.post<any>(API_ENDPOINTS.USUARIO_RETOMAR_PAGO, usuario);
  }

  validateToken(): Observable<boolean> {
    const token = this.getToken();
    if (!token) {
      return new Observable<boolean>(observer => {
        observer.next(false);
        observer.complete();
      });
    }

    return this.http.post<any>(API_ENDPOINTS.AUTH_VALIDATE, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    }).pipe(
      map(response => response.valid === true),
      tap(valid => {
        if (!valid) {
          this.logout();
        }
      })
    );
  }
}