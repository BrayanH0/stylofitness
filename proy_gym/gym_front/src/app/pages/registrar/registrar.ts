import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Navbar } from '../../components/navbar/navbar';
import { AuthService } from '../../services/auth.service';

interface Usuario {
  dni?: number | null;
  nombre: string;
  apellido: string;
  telefono: string;
  email: string;
  fecha_nacimiento: string;
  direccion: string;
  fecha_registro: string;
  estado: string;
  password: string;
}

interface PlanConfig {
  key: string;
  nombre: string;
  precio1m: number;
  precio3m: number;
}

const PLANES: Record<string, PlanConfig> = {
  black: { key: 'black', nombre: 'PLAN Black', precio1m: 89.90, precio3m: 269.70 },
  fit: { key: 'fit', nombre: 'PLAN Fit', precio1m: 69.90, precio3m: 209.70 }
};

@Component({
  selector: 'app-registrar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Navbar],
  templateUrl: './registrar.html',
  styleUrls: ['./registrar.css']
})
export class Registrar implements OnInit {
  usuario: Usuario = {
    dni: null,
    nombre: '',
    apellido: '',
    telefono: '',
    email: '',
    fecha_nacimiento: '',
    direccion: '',
    fecha_registro: new Date().toISOString().split('T')[0],
    estado: 'INACTIVO',
    password: ''
  };

  membershipType: string = '';
  planSeleccionado: PlanConfig | null = null;
  startDate: string = '-';
  endDate: string = '-';
  durationText: string = '-';
  showMembershipError: boolean = false;
  showFormError: boolean = false;
  showSuccess: boolean = false;
  showPassword: boolean = false;
  showDateError: boolean = false;
  dniError: string = '';
  emailError: string = '';
  telefonoError: string = '';
  maxDate: string = '';
  minDate: string = '';

  constructor(private router: Router, private route: ActivatedRoute, private authService: AuthService) {}

  ngOnInit() {
    const hoy = new Date();
    const max = new Date(hoy.getFullYear() - 10, hoy.getMonth(), hoy.getDate());
    const min = new Date(hoy.getFullYear() - 100, hoy.getMonth(), hoy.getDate());
    this.maxDate = max.toISOString().split('T')[0];
    this.minDate = min.toISOString().split('T')[0];

    this.startDate = this.formatDate(hoy);

    this.route.queryParams.subscribe(params => {
      const planKey = params['plan'];
      if (planKey && PLANES[planKey]) {
        this.planSeleccionado = PLANES[planKey];
      } else {
        this.planSeleccionado = PLANES['fit'];
      }
    });
  }

  get precio1m(): string {
    return this.planSeleccionado ? this.planSeleccionado.precio1m.toFixed(2) : '0.00';
  }

  get precio3m(): string {
    return this.planSeleccionado ? (this.planSeleccionado.precio3m / 3).toFixed(2) : '0.00';
  }

  get precio3mTotal(): string {
    return this.planSeleccionado ? this.planSeleccionado.precio3m.toFixed(2) : '0.00';
  }

  get nombrePlan(): string {
    return this.planSeleccionado ? this.planSeleccionado.nombre : 'Plan';
  }

  seleccionarPlan(planKey: string) {
    if (PLANES[planKey]) {
      this.planSeleccionado = PLANES[planKey];
      this.membershipType = '';
      this.endDate = '-';
      this.durationText = '-';
    }
  }

  async onSubmit(form: NgForm) {
    this.dniError = '';
    this.emailError = '';
    this.telefonoError = '';

    if (form && form.controls) {
      Object.keys(form.controls).forEach(name => {
        try { form.controls[name].markAsTouched(); } catch { }
      });
    }

    if (!this.membershipType) {
      this.showMembershipError = true;
      return;
    }

    this.showDateError = false;
    const fechaNac = this.usuario.fecha_nacimiento;
    if (fechaNac) {
      const fechaIngresada = new Date(fechaNac + 'T00:00:00');
      const max = new Date(this.maxDate + 'T00:00:00');
      const min = new Date(this.minDate + 'T00:00:00');
      if (fechaIngresada > max || fechaIngresada < min) {
        this.showDateError = true;
        if (form && form.controls['fecha_nacimiento']) {
          form.controls['fecha_nacimiento'].setErrors({ invalidDate: true });
        }
        return;
      }
    }

    if (!form || !form.valid) {
      return;
    }

    try {
      const dniRes = await firstValueFrom(this.authService.existeDni(this.usuario.dni!));
      if (dniRes.existe) {
        // Intentar retomar pago si el usuario quedó INACTIVO por pago abandonado
        try {
          const retomarRes = await firstValueFrom(this.authService.retomarPago(this.usuario));
          localStorage.setItem('datosCliente', JSON.stringify({ dni: this.usuario.dni, tempToken: retomarRes.tempToken }));
          this.guardarMembresia();
          this.router.navigate(['/pago']);
        } catch (err: any) {
          const msg: string = err?.error?.error || err?.message || '';
          if (msg.includes('email')) {
            this.emailError = 'El email ya está registrado. Use otro email.';
          } else if (msg.includes('teléfono') || msg.includes('telefono')) {
            this.telefonoError = 'El teléfono ya está registrado. Use otro número.';
          } else {
            this.dniError = 'El DNI ya tiene una cuenta activa. Inicia sesión o usa otro DNI.';
          }
        }
        return;
      }
      const emailRes = await firstValueFrom(this.authService.existeEmail(this.usuario.email));
      if (emailRes.existe) {
        this.emailError = 'El email ya está registrado. Use otro email o inicie sesión.';
        return;
      }
      const telRes = await firstValueFrom(this.authService.existeTelefono(this.usuario.telefono));
      if (telRes.existe) {
        this.telefonoError = 'El teléfono ya está registrado. Use otro número o inicie sesión.';
        return;
      }
    } catch (e) {
      this.dniError = 'No se pudo verificar disponibilidad. Intente nuevamente.';
      return;
    }

    this.usuario.fecha_registro = new Date().toISOString().split('T')[0];

    try {
      localStorage.setItem('datosCliente', JSON.stringify(this.usuario));
    } catch (e) {
      console.warn('No se pudo almacenar datosCliente en localStorage', e);
    }

    this.guardarMembresia();

    this.router.navigate(['/pago']);
  }

  selectMembership(value: string, months: number) {
    this.membershipType = value;
    this.showMembershipError = false;

    const startDate = new Date();
    const endDate = new Date(startDate);
    endDate.setMonth(endDate.getMonth() + months);

    this.endDate = this.formatDate(endDate);
    this.durationText = `${months} mes${months > 1 ? 'es' : ''}`;

    try {
      this.guardarMembresia();
    } catch (e) {
      console.warn('No se pudo guardar la membresía', e);
    }
  }

  formatDate(date: Date): string {
    return date.toLocaleDateString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  onReset(form: NgForm) {
    form.resetForm();
    this.membershipType = '';
    this.showMembershipError = false;
    this.showFormError = false;
    this.dniError = '';
    this.emailError = '';
    this.telefonoError = '';
    this.showSuccess = false;
    this.endDate = '-';
    this.durationText = '-';

    this.usuario = {
      dni: null, nombre: '', apellido: '', telefono: '', email: '',
      fecha_nacimiento: '', direccion: '',
      fecha_registro: new Date().toISOString().split('T')[0],
      estado: 'INACTIVO', password: ''
    };
  }

  guardarMembresia(): boolean {
    if (!this.membershipType || !this.planSeleccionado) {
      return false;
    }

    const months = this.membershipType === '3-meses' ? 3 : 1;
    const price = months === 1 ? this.planSeleccionado.precio1m : this.planSeleccionado.precio3m;
    const startDate = new Date();
    const endDate = new Date();
    endDate.setMonth(startDate.getMonth() + months);

    const membresia = {
      value: this.membershipType,
      months: months,
      price: price,
      name: this.planSeleccionado.nombre,
      planKey: this.planSeleccionado.key,
      startDate: startDate.toLocaleDateString('es-ES'),
      endDate: endDate.toLocaleDateString('es-ES'),
      duration: `${months} mes${months > 1 ? 'es' : ''}`
    };

    localStorage.setItem('membresiaSeleccionada', JSON.stringify(membresia));
    return true;
  }
}