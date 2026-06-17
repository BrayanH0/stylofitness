import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Navbar } from '../../components/navbar/navbar';
import { AuthService } from '../../services/auth.service';
import { AdminService } from '../../services/admin.service';
import { API_ENDPOINTS } from '../../config/api-config';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, Navbar],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {

  totalUsuarios: number = 0;
  usuariosActivos: number = 0;
  usuariosInactivos: number = 0;
  totalPersonal: number = 0;

  totalClases: number = 0;
  clasesActivas: number = 0;
  totalInscripciones: number = 0;
  inscripcionesActivas: number = 0;
  membresiasActivas: number = 0;
  membresiasPorVencer: number = 0;
  ingresosTotales: number = 0;
  ingresosMesActual: number = 0;
  pagosCompletadosCount: number = 0;
  planBasico: number = 0;
  planPremium: number = 0;
  nuevosUsuariosMes: number = 0;

  ingresosMensuales: any[] = [];
  clasesPopulares: any[] = [];
  membresiasList: any[] = [];
  statsLoaded: boolean = false;

  usuarios: any[] = [];
  personalList: any[] = [];
  personalFiltrado: any[] = [];
  pagos: any[] = [];
  userNamesMap: Map<number, string> = new Map();

  filtroEstado: string = 'todos';
  filtroBusqueda: string = '';

  showEditModal: boolean = false;
  editingUser: any = null;
  editingPersonal: boolean = false;
  isCreatingPersonal: boolean = false;

  mensajeExito: string | null = null;
  mensajeError: string | null = null;
  modalError: string | null = null;
  showPasswordModal: boolean = false;

  activeTab: 'usuarios' | 'personal' | 'estadisticas' | 'suscripciones' = 'usuarios';

  loading: boolean = false;
  error: string | null = null;

  @ViewChild('pagosChart') pagosChart!: ElementRef<HTMLCanvasElement>;
  @ViewChild('ingresosChart') ingresosChart!: ElementRef<HTMLCanvasElement>;
  chart: any = null;
  ingresosBarChart: any = null;
  showBarChart: boolean = false;

  filtroPagoEstado: string = 'todos';

  constructor(
    private authService: AuthService,
    private adminService: AdminService,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit() {
    if (!this.authService.isLoggedIn() || !this.authService.isAdmin()) {
      this.router.navigate(['/']);
      return;
    }

    this.cargarDatos();
  }

  get usuario(): any {
    return this.authService.getUserInfo();
  }

  get esAdmin(): boolean {
    return this.authService.getUserRole() === 'ADMIN';
  }

  private mostrarExito(msg: string) {
    this.mensajeExito = msg;
    this.mensajeError = null;
    setTimeout(() => { this.mensajeExito = null; }, 3000);
  }

  private mostrarError(msg: string) {
    this.mensajeError = msg;
    this.mensajeExito = null;
    setTimeout(() => { this.mensajeError = null; }, 4000);
  }

  cargarDatos() {
    this.loading = true;
    this.error = null;

    this.adminService.getEstadisticasUsuarios().subscribe({
      next: (stats) => {
        this.totalUsuarios = stats.total;
        this.usuariosActivos = stats.activos;
        this.usuariosInactivos = stats.inactivos;
      },
      error: (err) => { console.error('Error cargando estadísticas', err); }
    });

    this.adminService.getUsuarios().subscribe({
      next: (data) => {
        this.usuarios = data;
        this.userNamesMap = new Map();
        for (const u of data) {
          if (u.dni) {
            this.userNamesMap.set(Number(u.dni), `${u.nombre || ''} ${u.apellido || ''}`.trim() || `Usuario ${u.dni}`);
          }
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error al cargar usuarios';
        this.loading = false;
        console.error('Error cargando usuarios', err);
      }
    });

    this.adminService.getPersonal().subscribe({
      next: (data) => {
        this.personalList = data;
        this.personalFiltrado = data;
        this.totalPersonal = data.length;
      },
      error: (err) => { console.error('Error cargando personal', err); }
    });
  }

  get usuariosFiltrados(): any[] {
    let filtrados = this.usuarios.filter(u => u.rol === 'CLIENTE');

    if (this.filtroEstado !== 'todos') {
      filtrados = filtrados.filter(u => u.estado?.toLowerCase() === this.filtroEstado.toLowerCase());
    }

    if (this.filtroBusqueda.trim()) {
      const busqueda = this.filtroBusqueda.toLowerCase();
      filtrados = filtrados.filter(u =>
        u.nombre?.toLowerCase().includes(busqueda) ||
        u.apellido?.toLowerCase().includes(busqueda) ||
        u.dni?.toString().includes(busqueda) ||
        u.email?.toLowerCase().includes(busqueda)
      );
    }

    return filtrados;
  }

  get pagosFiltrados(): any[] {
    if (this.filtroPagoEstado === 'todos') return this.pagos;
    return this.pagos.filter(p => {
      const est = (p.estado || '').toLowerCase();
      return est === this.filtroPagoEstado.toLowerCase();
    });
  }

  get totalIngresos(): number {
    return this.pagosFiltrados.reduce((sum, p) => sum + (Number(p.monto ?? p.amount ?? 0) || 0), 0);
  }

  get pagosCompletados(): number {
    return this.pagos.filter(p => {
      const est = (p.estado || '').toLowerCase();
      return est === 'pagado' || est === 'completed';
    }).length;
  }

  setTab(tab: 'usuarios' | 'personal' | 'estadisticas' | 'suscripciones') {
    this.activeTab = tab;
    this.filtroBusqueda = '';
    this.filtroEstado = 'todos';
    this.filtroPagoEstado = 'todos';

    if (tab === 'suscripciones') {
      if (this.pagos.length === 0) {
        this.cargarPagos();
      } else {
        setTimeout(() => this.renderChart(this.showBarChart ? 'bar' : 'doughnut'), 300);
      }
    }

    if (tab === 'estadisticas') {
      if (!this.statsLoaded) {
        this.cargarEstadisticas();
      } else {
        setTimeout(() => this.renderIngresosChart(), 300);
      }
    }
  }

  cargarPagos() {
    this.http.get<any[]>(API_ENDPOINTS.PAYMENT_ALL).subscribe({
      next: (data) => {
        const all = Array.isArray(data) ? data : [];
        this.pagos = all.filter(p => {
          const s = p?.sessionId || p?.session_id || p?.session || '';
          return s !== null && s !== undefined && String(s).trim().length > 0;
        });
        setTimeout(() => this.renderChart(), 350);
      },
      error: () => { this.pagos = []; }
    });
  }

  private planDisplayName(plan: string | null | undefined, monto?: number): string {
    if (plan && String(plan).trim().length > 0) {
      const p = String(plan).toLowerCase().trim();
      if (p.includes('fit')) return 'Fit';
      if (p.includes('black')) return 'Black';
      if (p === 'basic') return 'Fit';
      if (p === 'premium') return 'Black';
    }
    if (monto != null && monto > 0) {
      if (monto < 150) {
        return monto <= 85 ? 'Fit' : 'Black';
      } else {
        return monto <= 255 ? 'Fit' : 'Black';
      }
    }
    return 'Sin plan';
  }

  async renderChart(type: 'doughnut' | 'bar' = 'doughnut') {
    try {
      const ChartModule: any = await import('chart.js/auto');
      if (this.chart) { try { this.chart.destroy(); } catch {} this.chart = null; }
      const ctx = this.pagosChart?.nativeElement?.getContext('2d');
      if (!ctx) return;

      const bgColors = ['#3498db', '#2ecc71', '#f39c12', '#e74c3c', '#9b59b6', '#1abc9c', '#e67e22', '#34495e'];

      if (type === 'doughnut') {
        const totals: Record<string, number> = {};
        for (const p of this.pagos) {
          const amt = Number(p?.monto ?? p?.amount ?? 0) || 0;
          const plan = this.planDisplayName(p?.plan, amt);
          totals[plan] = (totals[plan] || 0) + amt;
        }
        const labels = Object.keys(totals);
        const data = labels.map(l => totals[l]);
        this.chart = new ChartModule.Chart(ctx, {
          type: 'doughnut',
          data: { labels, datasets: [{ data, backgroundColor: labels.map((_, i) => bgColors[i % bgColors.length]), borderColor: '#ffffff', borderWidth: 2 }] },
          options: { responsive: true, plugins: { legend: { position: 'bottom', labels: { padding: 20, font: { size: 13 } } }, tooltip: { enabled: true } } }
        });
      } else {
        const counts: Record<string, number> = {};
        for (const p of this.pagos) {
          const amt = Number(p?.monto ?? p?.amount ?? 0) || 0;
          const plan = this.planDisplayName(p?.plan, amt);
          counts[plan] = (counts[plan] || 0) + 1;
        }
        const labels = Object.keys(counts);
        const data = labels.map(l => counts[l]);
        this.chart = new ChartModule.Chart(ctx, {
          type: 'bar',
          data: { labels, datasets: [{ label: 'Suscripciones', data, backgroundColor: labels.map((_, i) => bgColors[i % bgColors.length]), borderColor: '#ffffff', borderWidth: 1, borderRadius: 6 }] },
          options: { responsive: true, plugins: { legend: { display: false }, tooltip: { enabled: true } }, scales: { x: { ticks: { autoSkip: false, font: { size: 12 } }, grid: { display: false } }, y: { beginAtZero: true, precision: 0, ticks: { font: { size: 12 } } } } }
        });
      }
    } catch (e) { console.warn('Could not load Chart.js', e); }
  }

  toggleBarChart() {
    this.showBarChart = !this.showBarChart;
    setTimeout(() => this.renderChart(this.showBarChart ? 'bar' : 'doughnut'), 50);
  }

  cargarEstadisticas() {
    this.adminService.getEstadisticasCompletas().subscribe({
      next: (stats) => {
        this.totalUsuarios = stats.totalUsuarios ?? 0;
        this.usuariosActivos = stats.usuariosActivos ?? 0;
        this.usuariosInactivos = stats.usuariosInactivos ?? 0;
        this.totalPersonal = stats.totalPersonal ?? 0;
        this.totalClases = stats.totalClases ?? 0;
        this.clasesActivas = stats.clasesActivas ?? 0;
        this.totalInscripciones = stats.totalInscripciones ?? 0;
        this.inscripcionesActivas = stats.inscripcionesActivas ?? 0;
        this.membresiasActivas = stats.membresiasActivas ?? 0;
        this.membresiasPorVencer = stats.membresiasPorVencer ?? 0;
        this.ingresosTotales = stats.ingresosTotales ?? 0;
        this.ingresosMesActual = stats.ingresosMesActual ?? 0;
        this.pagosCompletadosCount = stats.pagosCompletados ?? 0;
        this.planBasico = stats.planBasico ?? 0;
        this.planPremium = stats.planPremium ?? 0;
        this.nuevosUsuariosMes = stats.nuevosUsuariosMes ?? 0;
        this.statsLoaded = true;
      },
      error: (err) => { console.error('Error cargando estadisticas completas', err); }
    });

    this.adminService.getIngresosMensuales().subscribe({
      next: (data) => {
        this.ingresosMensuales = data;
        setTimeout(() => this.renderIngresosChart(), 350);
      },
      error: (err) => { console.error('Error cargando ingresos mensuales', err); }
    });

    this.adminService.getClasesPopulares().subscribe({
      next: (data) => {
        this.clasesPopulares = data;
      },
      error: (err) => { console.error('Error cargando clases populares', err); }
    });

    this.adminService.getMembresias().subscribe({
      next: (data) => {
        this.membresiasList = data;
      },
      error: (err) => { console.error('Error cargando membresias', err); }
    });
  }

  async renderIngresosChart() {
    try {
      const ChartModule: any = await import('chart.js/auto');
      if (this.ingresosBarChart) { try { this.ingresosBarChart.destroy(); } catch {} this.ingresosBarChart = null; }
      const ctx = this.ingresosChart?.nativeElement?.getContext('2d');
      if (!ctx || this.ingresosMensuales.length === 0) return;

      const labels = this.ingresosMensuales.map((m: any) => m.mes).reverse();
      const data = this.ingresosMensuales.map((m: any) => m.ingresos).reverse();

      this.ingresosBarChart = new ChartModule.Chart(ctx, {
        type: 'bar',
        data: {
          labels,
          datasets: [{
            label: 'Ingresos (S/)',
            data,
            backgroundColor: 'rgba(52, 152, 219, 0.7)',
            borderColor: '#3498db',
            borderWidth: 1,
            borderRadius: 6
          }]
        },
        options: {
          responsive: true,
          plugins: { legend: { display: false }, tooltip: { enabled: true, callbacks: { label: (ctx: any) => `S/ ${ctx.parsed.y.toFixed(2)}` } } },
          scales: {
            x: { ticks: { font: { size: 12 } }, grid: { display: false } },
            y: { beginAtZero: true, ticks: { font: { size: 12 }, callback: (v: any) => `S/ ${v}` } }
          }
        }
      });
    } catch (e) { console.warn('Could not load ingresos chart', e); }
  }

  openCrearPersonalModal() {
    this.editingUser = { dni: null, nombre: '', apellido: '', email: '', telefono: '', direccion: '', passwordHash: '', estado: 'ACTIVO', rol: 'PERSONAL' };
    this.editingPersonal = true;
    this.isCreatingPersonal = true;
    this.showEditModal = true;
  }

  openEditModal(usuario: any) {
    this.editingUser = { ...usuario };
    this.editingPersonal = false;
    this.isCreatingPersonal = false;
    this.showEditModal = true;
  }

  openEditPersonalModal(personal: any) {
    this.editingUser = { ...personal };
    this.editingPersonal = true;
    this.isCreatingPersonal = false;
    this.showEditModal = true;
  }

  closeEditModal() {
    this.showEditModal = false;
    this.editingUser = null;
    this.editingPersonal = false;
    this.isCreatingPersonal = false;
    this.modalError = null;
    this.showPasswordModal = false;
  }

  async saveUserEdit() {
    if (!this.editingUser) return;

    if (this.isCreatingPersonal) {
      this.modalError = null;
      if (!this.editingUser.dni || !this.editingUser.nombre || !this.editingUser.apellido || !this.editingUser.passwordHash) {
        this.modalError = 'DNI, nombre, apellido y contraseña son obligatorios';
        return;
      }
      if (!this.editingUser.email || !this.editingUser.telefono || !this.editingUser.direccion) {
        this.modalError = 'Email, teléfono y dirección son obligatorios';
        return;
      }
      const telefonoRegex = /^9[0-9]{8}$/;
      if (!telefonoRegex.test(this.editingUser.telefono)) {
        this.modalError = 'El teléfono debe empezar con 9 y tener 9 dígitos';
        return;
      }
      try {
        const dniRes = await firstValueFrom(this.authService.existeDni(this.editingUser.dni));
        if (dniRes.existe) {
          this.modalError = 'El DNI ya está registrado';
          return;
        }
        const emailRes = await firstValueFrom(this.authService.existeEmail(this.editingUser.email));
        if (emailRes.existe) {
          this.modalError = 'El email ya está registrado';
          return;
        }
        const telRes = await firstValueFrom(this.authService.existeTelefono(this.editingUser.telefono));
        if (telRes.existe) {
          this.modalError = 'El teléfono ya está registrado';
          return;
        }
      } catch (e) {
        this.modalError = 'No se pudo verificar disponibilidad. Intente nuevamente.';
        return;
      }
      this.adminService.crearPersonal(this.editingUser).subscribe({
        next: (nuevo) => {
          this.personalList.push(nuevo);
          this.personalFiltrado = [...this.personalList];
          this.totalPersonal = this.personalList.length;
          this.closeEditModal();
          this.mostrarExito('Personal creado correctamente');
        },
        error: (err) => {
          console.error('Error creando personal', err);
          this.modalError = 'Error al crear personal: ' + (err.error?.error || err.message);
        }
      });
      return;
    }

    const observable = this.editingPersonal
      ? this.adminService.actualizarPersonal(this.editingUser.dni, this.editingUser)
      : this.adminService.actualizarUsuario(this.editingUser.dni, this.editingUser);

    observable.subscribe({
      next: (updated) => {
        if (this.editingPersonal) {
          const index = this.personalList.findIndex(p => p.dni === this.editingUser.dni);
          if (index !== -1) {
            this.personalList[index] = updated;
            this.personalFiltrado = [...this.personalList];
          }
        } else {
          const index = this.usuarios.findIndex(u => u.dni === this.editingUser.dni);
          if (index !== -1) {
            this.usuarios[index] = updated;
          }
        }
        this.closeEditModal();
        this.mostrarExito((this.editingPersonal ? 'Personal' : 'Usuario') + ' actualizado correctamente');
      },
      error: (err) => {
        console.error('Error actualizando', err);
        this.mostrarError('Error al actualizar: ' + (err.error?.error || err.message));
      }
    });
  }

  desactivarUsuario(dni: number) {
    if (!confirm('¿Está seguro de desactivar este usuario?')) return;
    this.adminService.desactivarUsuario(dni).subscribe({
      next: () => {
        const usuario = this.usuarios.find(u => u.dni === dni);
        if (usuario) usuario.estado = 'INACTIVO';
        this.mostrarExito('Usuario desactivado correctamente');
      },
      error: (err) => {
        console.error('Error desactivando usuario', err);
        this.mostrarError('Error al desactivar usuario');
      }
    });
  }

  activarUsuario(dni: number) {
    this.adminService.activarUsuario(dni).subscribe({
      next: () => {
        const usuario = this.usuarios.find(u => u.dni === dni);
        if (usuario) usuario.estado = 'ACTIVO';
        this.mostrarExito('Usuario activado correctamente');
      },
      error: (err) => {
        console.error('Error activando usuario', err);
        this.mostrarError('Error al activar usuario');
      }
    });
  }

  eliminarUsuario(dni: number) {
    if (!confirm('¿Está seguro de eliminar este cliente? Esta acción no se puede deshacer.')) return;
    this.adminService.eliminarUsuario(dni).subscribe({
      next: () => {
        this.usuarios = this.usuarios.filter(u => u.dni !== dni);
        this.mostrarExito('Cliente eliminado correctamente');
      },
      error: (err) => {
        console.error('Error eliminando usuario', err);
        this.mostrarError('Error al eliminar: ' + (err.error?.error || err.message));
      }
    });
  }

  desactivarPersonal(dni: number) {
    if (!confirm('¿Está seguro de desactivar este personal?')) return;
    this.adminService.desactivarPersonal(dni).subscribe({
      next: () => {
        const personal = this.personalList.find(p => p.dni === dni);
        if (personal) personal.estado = 'INACTIVO';
        this.personalFiltrado = [...this.personalList];
        this.mostrarExito('Personal desactivado correctamente');
      },
      error: (err) => {
        console.error('Error desactivando personal', err);
        this.mostrarError('Error al desactivar personal');
      }
    });
  }

  activarPersonal(dni: number) {
    const existente = this.personalList.find(p => p.dni === dni);
    if (!existente) return;
    this.adminService.actualizarPersonal(dni, { estado: 'ACTIVO' }).subscribe({
      next: () => {
        const personal = this.personalList.find(p => p.dni === dni);
        if (personal) personal.estado = 'ACTIVO';
        this.personalFiltrado = [...this.personalList];
        this.mostrarExito('Personal activado correctamente');
      },
      error: (err) => {
        console.error('Error activando personal', err);
        this.mostrarError('Error al activar personal');
      }
    });
  }

  getUserName(dni: any): string {
    if (!dni) return '-';
    const num = Number(dni);
    if (this.userNamesMap.has(num)) return this.userNamesMap.get(num)!;
    return `Usuario ${dni}`;
  }

  filtrarPersonal() {
    if (!this.filtroBusqueda.trim()) {
      this.personalFiltrado = this.personalList;
      return;
    }
    const busqueda = this.filtroBusqueda.toLowerCase();
    this.personalFiltrado = this.personalList.filter(p =>
      p.nombre?.toLowerCase().includes(busqueda) ||
      p.apellido?.toLowerCase().includes(busqueda) ||
      p.dni?.toString().includes(busqueda) ||
      p.email?.toLowerCase().includes(busqueda)
    );
  }

  getPlanDisplayName(plan: string | null | undefined, monto?: number): string {
    if (plan && String(plan).trim().length > 0) {
      const p = String(plan).toLowerCase().trim();
      if (p.includes('fit')) return 'Fit';
      if (p.includes('black')) return 'Black';
      if (p === 'basic') return 'Fit';
      if (p === 'premium') return 'Black';
    }
    if (monto != null && monto > 0) {
      if (monto < 150) {
        return monto <= 85 ? 'Fit' : 'Black';
      } else {
        return monto <= 255 ? 'Fit' : 'Black';
      }
    }
    return 'Sin plan';
  }

  planBadgeClass(plan: string | null | undefined, monto?: number): string {
    const name = this.getPlanDisplayName(plan, monto);
    if (name === 'Fit') return 'bg-secondary text-white';
    if (name === 'Black') return 'bg-error text-white';
    return 'bg-surface-variant text-on-surface-variant';
  }

  getEstadoBadgeClass(estado: string): string {
    const e = (estado || '').toLowerCase();
    if (e === 'activo' || e === 'completed' || e === 'pagado') return 'badge-success';
    if (e === 'inactivo' || e === 'fallido') return 'badge-danger';
    if (e === 'pendiente') return 'badge-warning';
    return 'badge-secondary';
  }

  get maxInscripciones(): number {
    if (this.clasesPopulares.length === 0) return 1;
    return Math.max(...this.clasesPopulares.map((c: any) => c.inscripciones), 1);
  }

  logout() { this.authService.logout(); this.router.navigate(['/']); }
}