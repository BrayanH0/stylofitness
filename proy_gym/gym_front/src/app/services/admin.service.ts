import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../config/api-config';

@Injectable({
  providedIn: 'root'
})
export class AdminService {

  constructor(private http: HttpClient) {}

  getUsuarios(): Observable<any[]> {
    return this.http.get<any[]>(API_ENDPOINTS.ADMIN_USUARIOS);
  }

  getUsuario(dni: number): Observable<any> {
    return this.http.get<any>(API_ENDPOINTS.ADMIN_USUARIO(dni));
  }

  actualizarUsuario(dni: number, usuario: any): Observable<any> {
    return this.http.put<any>(API_ENDPOINTS.ADMIN_USUARIO(dni), usuario);
  }

  desactivarUsuario(dni: number): Observable<any> {
    return this.http.delete<any>(API_ENDPOINTS.ADMIN_USUARIO(dni));
  }

  activarUsuario(dni: number): Observable<any> {
    return this.http.put<any>(API_ENDPOINTS.ADMIN_ACTIVAR(dni), {});
  }

  eliminarUsuario(dni: number): Observable<any> {
    return this.http.delete<any>(API_ENDPOINTS.ADMIN_ELIMINAR(dni));
  }

  getUsuariosPorEstado(estado: string): Observable<any[]> {
    return this.http.get<any[]>(API_ENDPOINTS.ADMIN_POR_ESTADO(estado));
  }

  getEstadisticasUsuarios(): Observable<any> {
    return this.http.get<any>(API_ENDPOINTS.ADMIN_ESTADISTICAS);
  }

  getEstadisticasCompletas(): Observable<any> {
    return this.http.get<any>(API_ENDPOINTS.ADMIN_ESTADISTICAS_COMPLETAS);
  }

  getIngresosMensuales(): Observable<any[]> {
    return this.http.get<any[]>(API_ENDPOINTS.ADMIN_INGRESOS_MENSUALES);
  }

  getClasesPopulares(): Observable<any[]> {
    return this.http.get<any[]>(API_ENDPOINTS.ADMIN_CLASES_POPULARES);
  }

  getMembresias(): Observable<any[]> {
    return this.http.get<any[]>(API_ENDPOINTS.ADMIN_MEMBRESIAS);
  }

  getPersonal(): Observable<any[]> {
    return this.http.get<any[]>(API_ENDPOINTS.ADMIN_PERSONAL);
  }

  getPersonalPorDni(dni: number): Observable<any> {
    return this.http.get<any>(API_ENDPOINTS.ADMIN_PERSONAL_DNI(dni));
  }

  crearPersonal(personal: any): Observable<any> {
    return this.http.post<any>(API_ENDPOINTS.ADMIN_PERSONAL, personal);
  }

  actualizarPersonal(dni: number, personal: any): Observable<any> {
    return this.http.put<any>(API_ENDPOINTS.ADMIN_PERSONAL_DNI(dni), personal);
  }

  desactivarPersonal(dni: number): Observable<any> {
    return this.http.put<any>(API_ENDPOINTS.ADMIN_PERSONAL_DESACTIVAR(dni), {});
  }

  /** @deprecated Usar desactivarPersonal */
  eliminarPersonal(dni: number): Observable<any> {
    return this.http.delete<any>(API_ENDPOINTS.ADMIN_PERSONAL_DNI(dni));
  }
}