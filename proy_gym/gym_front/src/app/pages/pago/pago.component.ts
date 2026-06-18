import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { PaymentService } from '../../services/payment.service';
import { API_ENDPOINTS } from '../../config/api-config';
import { Navbar } from '../../components/navbar/navbar';

@Component({
  selector: 'app-pago',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Navbar],
  templateUrl: './pago.component.html',
  styleUrls: ['./pago.component.css']
})
export class PagoComponent implements OnInit {

  step: number = 2;
  loading: boolean = false;
  error: string | null = null;
  membresia: any = {};
  subtotal: number = 0;
  igv: number = 0;
  total: number = 0;
  startDate: string = '';
  endDate: string = '';
  cuentaCreada: boolean = false;

  constructor(
    private router: Router,
    private http: HttpClient,
    private paymentService: PaymentService
  ) {}

  ngOnInit() {
    this.cargarDatosMembresia();
  }

  cargarDatosMembresia() {
    const datos = localStorage.getItem('membresiaSeleccionada');

    if (datos) {
      this.membresia = JSON.parse(datos);
      this.calcularTotales();
      this.calcularFechas();
    } else {
      this.router.navigate(['/registrar']);
    }
  }

  get nombrePlan(): string {
    return this.membresia?.name || 'Plan';
  }

  get duracionTexto(): string {
    const months = Number(this.membresia?.months) || 0;
    if (months === 1) return '1 mes';
    if (months === 3) return '3 meses';
    return `${months} meses`;
  }

  calcularTotales() {
    const raw = parseFloat(this.membresia.price);
    if (isNaN(raw)) {
      this.total = 0;
      this.subtotal = 0;
      this.igv = 0;
      return;
    }
    this.total = Math.round(raw * 100) / 100;
    this.subtotal = Math.round((this.total / 1.18) * 100) / 100;
    this.igv = Math.round((this.total - this.subtotal) * 100) / 100;
  }

  calcularFechas() {
    if (this.membresia.startDate && this.membresia.endDate) {
      this.startDate = this.membresia.startDate;
      this.endDate = this.membresia.endDate;
    } else {
      const months = Number(this.membresia.months) || 1;
      const start = new Date();
      const end = new Date(start.getFullYear(), start.getMonth() + months, start.getDate());
      this.startDate = start.toLocaleDateString('es-ES');
      this.endDate = end.toLocaleDateString('es-ES');
    }
  }

  goBack() {
    window.history.back();
  }

  irAPago() {
    this.step = 3;
  }

  private crearCuentaYpagar() {
    const datosCliente = JSON.parse(localStorage.getItem('datosCliente') || '{}');
    const { password: _unused, ...datosSinPassword } = datosCliente;
    const dni = datosCliente.dni;

    if (!dni) {
      this.error = 'Faltan datos del registro. Vuelve al paso 1.';
      this.loading = false;
      return;
    }

    if (this.cuentaCreada) {
      this.redirigirStripe(dni);
      return;
    }

    if (datosCliente.tempToken) {
      this.cuentaCreada = true;
      this.redirigirStripe(dni);
      return;
    }

    this.http.post<any>(API_ENDPOINTS.USUARIO_PRE_REGISTRO, datosCliente).subscribe({
      next: (response) => {
        const datosActualizados = {
          ...datosSinPassword,
          tempToken: response.tempToken
        };
        localStorage.setItem('datosCliente', JSON.stringify(datosActualizados));
        this.cuentaCreada = true;
        this.redirigirStripe(dni);
      },
      error: (err) => {
        const errorMsg = err?.error?.error || err?.message || 'Error al crear la cuenta';
        if (errorMsg.includes('DNI ya') || errorMsg.includes('ya está registrado')) {
          this.http.post<any>(API_ENDPOINTS.USUARIO_RETOMAR_PAGO, { dni }).subscribe({
            next: (response) => {
              const datosActualizados = { ...datosSinPassword, tempToken: response.tempToken };
              localStorage.setItem('datosCliente', JSON.stringify(datosActualizados));
              this.cuentaCreada = true;
              this.redirigirStripe(dni);
            },
            error: () => {
              this.error = 'El DNI ya tiene una cuenta activa. Por favor inicia sesión.';
              this.loading = false;
            }
          });
        } else {
          this.error = 'Error al crear la cuenta: ' + errorMsg;
          this.loading = false;
        }
      }
    });
  }

  private redirigirStripe(dni: number) {
    const planKey = this.membresia?.planKey;
    const months = Number(this.membresia?.months) || 0;

    let stripePlan = '';

    if (planKey === 'fit' && months === 1) {
      stripePlan = 'fit_1m';
    } else if (planKey === 'fit' && months === 3) {
      stripePlan = 'fit_3m';
    } else if (planKey === 'black' && months === 1) {
      stripePlan = 'black_1m';
    } else if (planKey === 'black' && months === 3) {
      stripePlan = 'black_3m';
    } else {
      const priceCents = Math.round(parseFloat(this.membresia.price) * 100);
      if (priceCents === 6990) {
        stripePlan = 'fit_1m';
      } else if (priceCents === 20970) {
        stripePlan = 'fit_3m';
      } else if (priceCents === 8990) {
        stripePlan = 'black_1m';
      } else if (priceCents === 26970) {
        stripePlan = 'black_3m';
      } else {
        this.error = 'Plan no válido. Vuelve al inicio y selecciona tu plan nuevamente.';
        this.loading = false;
        return;
      }
    }

    this.paymentService.createCheckoutSession(stripePlan, dni)
      .subscribe({
        next: (url) => {
          window.location.href = url;
        },
        error: () => {
          this.error = 'Error al conectar con Stripe. Intenta nuevamente.';
          this.loading = false;
        }
      });
  }

  pagar() {
    this.error = null;
    this.loading = true;
    this.crearCuentaYpagar();
  }
}