import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Navbar } from '../../components/navbar/navbar';
import { API_ENDPOINTS } from '../../config/api-config';
import { AuthService } from '../../services/auth.service';
import { LoginModalService } from '../../services/login-modal.service';
import { clearSessionData } from '../../utils/token.helper';

@Component({
  selector: 'app-exito',
  templateUrl: './exito.component.html',
  styleUrls: ['./exito.component.css'],
  standalone: true,
  imports: [CommonModule, Navbar, RouterLink]
})
export class ExitoComponent implements OnInit {

  estado: 'cargando' | 'confirmado' | 'login_auto' | 'listo' | 'error' = 'cargando';
  errorMessage: string = '';
  loginErrorMessage: string = '';
  private datosCliente: any = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private authService: AuthService,
    private loginModalService: LoginModalService
  ) {}

  ngOnInit() {
    const sessionId = this.route.snapshot.queryParamMap.get('session_id');

    if (!sessionId) {
      this.router.navigate(['/registrar']);
      return;
    }

    this.datosCliente = JSON.parse(localStorage.getItem('datosCliente') || '{}');

    if (!this.datosCliente || !this.datosCliente.dni) {
      this.estado = 'listo';
      return;
    }

    this.http.post(`${API_ENDPOINTS.PAYMENT_CONFIRM}?sessionId=${sessionId}`, {})
      .subscribe({
        next: () => {
          this.estado = 'confirmado';
          this.intentarLoginAutomatico();
        },
        error: (err) => {
          this.estado = 'error';
          this.errorMessage = 'Hubo un problema confirmando tu pago. Intenta nuevamente o contacta soporte.';
        }
      });
  }

  private intentarLoginAutomatico() {
    if (!this.datosCliente?.dni) {
      this.estado = 'listo';
      this.limpiarDatosRegistro();
      this.datosCliente = null;
      return;
    }

    const dni = Number(this.datosCliente.dni);
    const tempToken = this.datosCliente.tempToken;

    if (!tempToken) {
      this.estado = 'listo';
      this.limpiarDatosRegistro();
      this.datosCliente = null;
      return;
    }

    this.estado = 'login_auto';

    this.http.post(`${API_ENDPOINTS.AUTH_LOGIN.replace('/login', '/post-pago-login')}`, {
      dni: dni,
      tempToken: tempToken
    }).subscribe({
      next: (response: any) => {
        if (response.token) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('dni', response.dni.toString());
          localStorage.setItem('nombre', response.nombre);
          localStorage.setItem('apellido', response.apellido || '');
          localStorage.setItem('email', response.email || '');
          localStorage.setItem('rol', response.rol || 'CLIENTE');
          localStorage.setItem('usuario', JSON.stringify(response));
        }
        this.limpiarDatosRegistro();
        this.datosCliente = null;
        this.router.navigate(['/interfaz-usuario']);
      },
      error: () => {
        this.estado = 'listo';
      }
    });
  }

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  irALogin() {
    this.limpiarDatosRegistro();
    this.datosCliente = null;
    this.router.navigate(['/']).then(() => {
      this.loginModalService.open();
    });
  }

  irAPortal() {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/interfaz-usuario']);
      return;
    }

    if (this.datosCliente?.dni && this.datosCliente?.tempToken) {
      this.estado = 'login_auto';
      const dni = Number(this.datosCliente.dni);
      const tempToken = this.datosCliente.tempToken;

      this.http.post(`${API_ENDPOINTS.AUTH_LOGIN.replace('/login', '/post-pago-login')}`, {
        dni: dni,
        tempToken: tempToken
      }).subscribe({
        next: (response: any) => {
          if (response.token) {
            localStorage.setItem('token', response.token);
            localStorage.setItem('dni', response.dni.toString());
            localStorage.setItem('nombre', response.nombre);
            localStorage.setItem('apellido', response.apellido || '');
            localStorage.setItem('email', response.email || '');
            localStorage.setItem('rol', response.rol || 'CLIENTE');
            localStorage.setItem('usuario', JSON.stringify(response));
          }
          this.limpiarDatosRegistro();
          this.datosCliente = null;
          this.router.navigate(['/interfaz-usuario']);
        },
        error: (err: any) => {
          this.estado = 'listo';
          const backendMsg = err?.error?.error || err?.message || 'Error desconocido';
          this.loginErrorMessage = `Login fallo: ${backendMsg}`;
        }
      });
      return;
    }

    this.limpiarDatosRegistro();
    this.datosCliente = null;
    this.router.navigate(['/']).then(() => {
      this.loginModalService.open();
    });
  }

  private limpiarDatosRegistro(): void {
    clearSessionData();
  }
}
