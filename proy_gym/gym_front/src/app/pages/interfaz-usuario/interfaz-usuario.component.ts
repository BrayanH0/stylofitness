import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Navbar } from '../../components/navbar/navbar';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { API_ENDPOINTS } from '../../config/api-config';
import { trapFocus } from '../../utils/modal-focus-trap';

interface ClaseDisponible {
  idClase: number;
  nombre: string;
  descripcion: string;
  fechaClase: string;
  horai: string;
  horaf: string;
  horario: string;
  idTrainer: number;
  estado: string;
  cuposDisponibles: number;
  inscrito: boolean;
}

interface Ejercicio {
  nombre: string;
  grupo: string;
  series: number;
  repeticiones: string;
  descanso: string;
}

interface DiaRutina {
  nombre: string;
  ejercicios: Ejercicio[];
}

interface RutinaForm {
  objetivo: string;
  nivel: string;
  dias: number;
  duracion: number;
}

const EJERCICIOS_POR_GRUPO: { [grupo: string]: { basico: string[]; intermedio: string[]; avanzado: string[] } } = {
  pecho: {
    basico: ['Press de banca con barra', 'Flexiones', 'Press inclinado con mancuernas'],
    intermedio: ['Press de banca', 'Press inclinado', 'Aperturas con mancuernas', 'Flexiones declinadas'],
    avanzado: ['Press de banca', 'Press inclinado', 'Aperturas', 'Cruces en polea', 'Flexiones diamante']
  },
  espalda: {
    basico: ['Jalón al pecho', 'Remo con mancuerna', 'Peso muerto rumano'],
    intermedio: ['Jalón al pecho', 'Remo con barra', 'Peso muerto', 'Remo con mancuerna'],
    avanzado: ['Dominadas', 'Remo con barra', 'Peso muerto', 'Jalón tras nuca', 'Remo T']
  },
  hombros: {
    basico: ['Press militar con mancuernas', 'Elevaciones laterales', 'Face pull'],
    intermedio: ['Press militar', 'Elevaciones laterales', 'Elevaciones frontales', 'Face pull'],
    avanzado: ['Press militar', 'Elevaciones laterales', 'Pájaros', 'Elevaciones frontales', 'Face pull']
  },
  piernas: {
    basico: ['Sentadilla con barra', 'Prensa', 'Zancadas', 'Elevación de pantorrilla'],
    intermedio: ['Sentadilla', 'Prensa', 'Zancadas', 'Curl femoral', 'Extensión de cuádriceps', 'Pantorrilla'],
    avanzado: ['Sentadilla', 'Peso muerto rumano', 'Prensa', 'Zancadas búlgaras', 'Curl femoral', 'Extensión', 'Pantorrilla']
  },
  biceps: {
    basico: ['Curl con barra', 'Curl martillo'],
    intermedio: ['Curl con barra', 'Curl martillo', 'Curl concentrado'],
    avanzado: ['Curl con barra', 'Curl martillo', 'Curl en banco Scott', 'Curl inverso']
  },
  triceps: {
    basico: ['Press francés', 'Extensión en polea'],
    intermedio: ['Press francés', 'Extensión en polea', 'Fondos en paralelas'],
    avanzado: ['Press francés', 'Extensión en polea', 'Fondos', 'Patada trasera', 'Press cerrado']
  },
  core: {
    basico: ['Plancha 45s', 'Crunches', 'Elevación de piernas'],
    intermedio: ['Plancha 60s', 'Crunches', 'Russian twist', 'Elevación de piernas'],
    avanzado: ['Plancha 90s', 'Crunches en polea', 'Russian twist', 'Escalador', 'Elevación de piernas']
  },
  cardio: {
    basico: ['Cinta 10 min', 'Salto de cuerda 5 min'],
    intermedio: ['Cinta 15 min', 'Burpees 3x10', 'Salto de cuerda'],
    avanzado: ['HIIT 20 min', 'Burpees 4x15', 'Salto de cuerda', 'Sprints']
  }
};

interface SplitConfig { nombre: string; dias: { nombre: string; grupos: string[] }[] }

const SPLITS: { [dias: number]: SplitConfig } = {
  3: {
    nombre: 'Full Body',
    dias: [
      { nombre: 'Día 1', grupos: ['pecho', 'espalda', 'piernas', 'core'] },
      { nombre: 'Día 2', grupos: ['hombros', 'biceps', 'triceps', 'cardio'] },
      { nombre: 'Día 3', grupos: ['pecho', 'espalda', 'piernas', 'core'] }
    ]
  },
  4: {
    nombre: 'Upper / Lower',
    dias: [
      { nombre: 'Día 1 – Torso', grupos: ['pecho', 'espalda', 'hombros', 'biceps', 'triceps'] },
      { nombre: 'Día 2 – Piernas', grupos: ['piernas', 'core', 'cardio'] },
      { nombre: 'Día 3 – Torso', grupos: ['pecho', 'espalda', 'hombros', 'biceps', 'triceps'] },
      { nombre: 'Día 4 – Piernas', grupos: ['piernas', 'core', 'cardio'] }
    ]
  },
  5: {
    nombre: 'Push / Pull / Legs',
    dias: [
      { nombre: 'Día 1 – Push', grupos: ['pecho', 'hombros', 'triceps'] },
      { nombre: 'Día 2 – Pull', grupos: ['espalda', 'biceps'] },
      { nombre: 'Día 3 – Piernas', grupos: ['piernas', 'core'] },
      { nombre: 'Día 4 – Push', grupos: ['pecho', 'hombros', 'triceps'] },
      { nombre: 'Día 5 – Pull + Cardio', grupos: ['espalda', 'biceps', 'cardio'] }
    ]
  },
  6: {
    nombre: 'Push / Pull / Legs x2',
    dias: [
      { nombre: 'Día 1 – Push', grupos: ['pecho', 'hombros', 'triceps'] },
      { nombre: 'Día 2 – Pull', grupos: ['espalda', 'biceps'] },
      { nombre: 'Día 3 – Piernas', grupos: ['piernas', 'core'] },
      { nombre: 'Día 4 – Push', grupos: ['pecho', 'hombros', 'triceps'] },
      { nombre: 'Día 5 – Pull', grupos: ['espalda', 'biceps'] },
      { nombre: 'Día 6 – Piernas + Cardio', grupos: ['piernas', 'core', 'cardio'] }
    ]
  }
};

interface ObjetivoConfig { series: number; repeticiones: string; descanso: string; extraCardio: boolean }

const OBJETIVOS: { [key: string]: ObjetivoConfig } = {
  masa:      { series: 4, repeticiones: '8-12', descanso: '90s', extraCardio: false },
  perder:    { series: 3, repeticiones: '12-15', descanso: '45s', extraCardio: true },
  resistencia: { series: 3, repeticiones: '15-20', descanso: '30s', extraCardio: true },
  tonificar: { series: 3, repeticiones: '12-15', descanso: '60s', extraCardio: false }
};

@Component({
  selector: 'app-interfaz-usuario',
  standalone: true,
  imports: [CommonModule, FormsModule, Navbar],
  templateUrl: './interfaz-usuario.component.html',
  styleUrls: ['./interfaz-usuario.component.css']
})
export class InterfazUsuarioComponent implements OnInit {

  usuario: any;
  membresia: any = null;
  subscripcionActual: string = 'Sin membresía';
  diasTranscurridos: number = 0;
  diasRestantes: number = 0;
  fechaRenovacion: string = '';
  fechaActual: string = '';

  clasesDisponibles: ClaseDisponible[] = [];
  inscripcionesActivas: any[] = [];
  loadingClases: boolean = false;
  loadingInscripcion: number | null = null;
  mensajeExito: string = '';
  mensajeError: string = '';
  mensajeUpgrade: string = '';

  showRutinaModal: boolean = false;
  showRutinaResult: boolean = false;
  rutinaGenerada: DiaRutina[] = [];
  rutinaSplit: string = '';
  rutinaForm: RutinaForm = { objetivo: '', nivel: '', dias: 3, duracion: 45 };

  showEditModal: boolean = false;
  editingUsuario: any = {};

  constructor(
    private authService: AuthService,
    private router: Router,
    private http: HttpClient
  ) {}

  openEditModal() {
    this.editingUsuario = this.usuario ? {
      nombre: this.usuario.nombre, apellido: this.usuario.apellido,
      email: this.usuario.email, telefono: this.usuario.telefono,
      direccion: this.usuario.direccion
    } : { nombre: '', apellido: '', email: '', telefono: '', direccion: '' };
    this.showEditModal = true;
  }

  closeEditModal() { this.showEditModal = false; }

  openRutinaModal() {
    this.rutinaForm = { objetivo: '', nivel: '', dias: 3, duracion: 45 };
    this.showRutinaModal = true;
    this.showRutinaResult = false;
  }

  closeRutinaModal() {
    this.showRutinaModal = false;
    this.showRutinaResult = false;
  }

  generarRutina() {
    const { objetivo, nivel, dias, duracion } = this.rutinaForm;
    if (!objetivo || !nivel) return;

    const config = OBJETIVOS[objetivo];
    const split = SPLITS[dias] || SPLITS[3];
    this.rutinaSplit = split.nombre;

    const nivelKey = nivel === 'principiante' ? 'basico' : nivel === 'intermedio' ? 'intermedio' : 'avanzado';

    const ejPorSesion = duracion <= 30 ? 4 : duracion <= 45 ? 5 : 7;

    this.rutinaGenerada = split.dias.map(dia => {
      const ejercicios: Ejercicio[] = [];

      const grupos = dia.grupos;
      const ejPorGrupo = Math.max(1, Math.floor(ejPorSesion / grupos.length));
      let restantes = ejPorSesion;

      for (const grupo of grupos) {
        if (restantes <= 0) break;
        const pool = EJERCICIOS_POR_GRUPO[grupo]?.[nivelKey] || EJERCICIOS_POR_GRUPO[grupo]?.basico || [];
        const cantidad = Math.min(restantes <= ejPorGrupo ? restantes : ejPorGrupo, pool.length);
        for (let i = 0; i < cantidad && restantes > 0; i++) {
          ejercicios.push({
            nombre: pool[i],
            grupo: grupo.charAt(0).toUpperCase() + grupo.slice(1),
            series: config.series,
            repeticiones: config.repeticiones,
            descanso: config.descanso
          });
          restantes--;
        }
      }

      if (restantes > 0 && grupos.length > 0) {
        const grupo = grupos[0];
        const pool = EJERCICIOS_POR_GRUPO[grupo]?.[nivelKey] || EJERCICIOS_POR_GRUPO[grupo]?.basico || [];
        for (let i = 0; restantes > 0 && i < pool.length; i++) {
          if (!ejercicios.find(e => e.nombre === pool[i])) {
            ejercicios.push({
              nombre: pool[i],
              grupo: grupo.charAt(0).toUpperCase() + grupo.slice(1),
              series: config.series,
              repeticiones: config.repeticiones,
              descanso: config.descanso
            });
            restantes--;
          }
        }
      }

      if (config.extraCardio) {
        const cardioPool = EJERCICIOS_POR_GRUPO['cardio']?.[nivelKey] || EJERCICIOS_POR_GRUPO['cardio'].basico;
        if (cardioPool.length > 0 && !ejercicios.find(e => e.grupo === 'Cardio')) {
          ejercicios.push({
            nombre: cardioPool[0],
            grupo: 'Cardio',
            series: 1,
            repeticiones: objetivo === 'resistencia' ? '20 min' : '10 min',
            descanso: '-'
          });
        }
      }

      return { nombre: dia.nombre, ejercicios };
    });

    this.guardarRutina();

    this.showRutinaResult = true;
    this.showRutinaModal = false;
  }

  onModalKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape') { this.closeEditModal(); return; }
    const container = (event.target as HTMLElement).closest('.modal-content');
    if (container) { trapFocus(event, container as HTMLElement); }
  }

  saveUserEdits() {
    const dni = this.usuario?.dni;
    if (!dni) return;
    this.http.put(API_ENDPOINTS.USUARIO_PERFIL(dni), this.editingUsuario).subscribe({
      next: () => {
        this.usuario = { ...this.usuario, ...this.editingUsuario };
        try { localStorage.setItem('usuario', JSON.stringify(this.usuario)); } catch { }
        this.showEditModal = false;
      },
      error: (err) => { console.error('Error guardando cambios:', err); this.showEditModal = false; }
    });
  }

  ngOnInit() {
    if (!this.authService.isLoggedIn()) { this.router.navigate(['/']); return; }
    this.usuario = this.authService.getUserInfo();
    if (!this.usuario) {
      const datos = localStorage.getItem('datosCliente');
      if (datos) this.usuario = JSON.parse(datos);
    }
    if (this.usuario?.dni) this.loadMembresia(this.usuario.dni);
    const currentDate = new Date();
    this.fechaActual = currentDate.toLocaleDateString('es-ES', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
    this.loadClasesEInscripciones();
    this.cargarRutina();
  }

  loadClasesEInscripciones() {
    this.loadingClases = true;
    const dni = this.usuario?.dni;
    if (!dni) { this.loadingClases = false; return; }
    this.http.get(API_ENDPOINTS.INSCRIPCIONES_USUARIO_ACTIVAS(dni)).subscribe({
      next: (inscripciones: any) => {
        let arr: any[] = [];
        if (Array.isArray(inscripciones)) arr = inscripciones;
        else if (inscripciones && Array.isArray((inscripciones as any).value)) arr = (inscripciones as any).value;
        this.inscripcionesActivas = arr;
        this.loadClases();
      },
      error: () => { this.inscripcionesActivas = []; this.loadClases(); }
    });
  }

  loadClases() {
    this.http.get(API_ENDPOINTS.CLASES).subscribe({
      next: (data: any) => {
        let arr: any[] = [];
        if (Array.isArray(data)) arr = data;
        else if (data && Array.isArray(data.value)) arr = data.value;
        else if (data && Array.isArray(data.clases)) arr = data.clases;
        else if (data && typeof data === 'object') {
          const possible = Object.keys(data).map(k => data[k]).find(v => Array.isArray(v));
          if (possible) arr = possible;
        }
        const hoyStr = new Date().toISOString().split('T')[0];
        const ahora = new Date();
        const activas = arr.filter((c: any) => {
          const est = (c.estado || '').toUpperCase();
          if (est !== 'ACTIVO') return false;
          const f = this.normalizeDate(c.fechaClase);
          if (!f) return false;
          if (f < hoyStr) return false;
          if (f === hoyStr && c.horaf) {
            const [h, m] = c.horaf.split(':').map(Number);
            if (h < ahora.getHours() || (h === ahora.getHours() && m <= ahora.getMinutes())) return false;
          }
          return true;
        });
        const inscripcionesIds = new Set(this.inscripcionesActivas.map((i: any) => Number(i.idClase)));
        this.clasesDisponibles = activas.map((c: any) => ({
          idClase: c.idClase, nombre: c.nombre, descripcion: c.descripcion || '',
          fechaClase: c.fechaClase, horai: c.horai, horaf: c.horaf, horario: c.horario,
          idTrainer: c.idTrainer, estado: c.estado, cuposDisponibles: 20,
          inscrito: inscripcionesIds.has(Number(c.idClase))
        }));
        this.loadCupos();
      },
      error: () => { this.clasesDisponibles = []; this.loadingClases = false; }
    });
  }

  loadCupos() {
    const requests = this.clasesDisponibles.map((clase) =>
      this.http.get(API_ENDPOINTS.INSCRIPCIONES_CLASE_CUPOS(clase.idClase)).toPromise().catch(() => null)
    );
    Promise.all(requests).then((results) => {
      results.forEach((res: any, index: number) => {
        if (res !== null) {
          const cupos = typeof res === 'number' ? res : (res?.cuposDisponibles ?? res?.cupos ?? 20);
          this.clasesDisponibles[index].cuposDisponibles = cupos;
        }
      });
      this.loadingClases = false;
    });
  }

  inscribirse(clase: ClaseDisponible) {
    const dni = this.usuario?.dni;
    if (!dni) return;
    this.loadingInscripcion = clase.idClase;
    this.limpiarMensajes();
    this.http.post(API_ENDPOINTS.INSCRIPCIONES_INSCRIBIR(dni, clase.idClase), {}).subscribe({
      next: () => {
        clase.inscrito = true;
        this.loadCupos();
        this.mensajeExito = `Te inscribiste a "${clase.nombre}" correctamente`;
        this.loadingInscripcion = null;
        setTimeout(() => this.mensajeExito = '', 4000);
      },
      error: (err) => {
        this.mensajeError = err?.error?.message || err?.error?.error || (typeof err?.error === 'string' ? err.error : null) || 'No se pudo completar la inscripción';
        this.loadingInscripcion = null;
        setTimeout(() => this.mensajeError = '', 4000);
      }
    });
  }

  cancelarInscripcion(clase: ClaseDisponible) {
    const dni = this.usuario?.dni;
    if (!dni) return;
    this.loadingInscripcion = clase.idClase;
    this.limpiarMensajes();
    this.http.put(API_ENDPOINTS.INSCRIPCIONES_CANCELAR(dni, clase.idClase), {}).subscribe({
      next: () => {
        clase.inscrito = false;
        this.loadCupos();
        this.mensajeExito = `Cancelaste tu inscripción a "${clase.nombre}"`;
        this.loadingInscripcion = null;
        setTimeout(() => this.mensajeExito = '', 4000);
      },
      error: (err) => {
        this.mensajeError = err?.error?.message || err?.error?.error || (typeof err?.error === 'string' ? err.error : null) || 'No se pudo cancelar la inscripción';
        this.loadingInscripcion = null;
        setTimeout(() => this.mensajeError = '', 4000);
      }
    });
  }

  limpiarMensajes() { this.mensajeExito = ''; this.mensajeError = ''; this.mensajeUpgrade = ''; }

  formatearFecha(fecha: string): string {
    if (!fecha) return '';
    const [year, month, day] = fecha.split('-').map(Number);
    const d = new Date(year, month - 1, day);
    return d.toLocaleDateString('es-ES', { weekday: 'short', day: 'numeric', month: 'short' });
  }

  private normalizeDate(d: any): string | null {
    if (!d) return null;
    if (typeof d === 'string') return d.split('T')[0];
    if (typeof d === 'number') return new Date(d).toISOString().split('T')[0];
    if (d instanceof Date) return d.toISOString().split('T')[0];
    try { const dt = new Date(d); if (!isNaN(dt.getTime())) return dt.toISOString().split('T')[0]; } catch {}
    return null;
  }

  private logoBase64: string | null = null;

  async downloadUltimaBoleta() {
    try {
      const cliente = this.usuario || (localStorage.getItem('datosCliente') ? JSON.parse(localStorage.getItem('datosCliente')!) : null);

      let pago: any = null;
      if (cliente?.dni) {
        try {
          pago = await this.http.get(`${API_ENDPOINTS.PAYMENT_LAST}?dni=${cliente.dni}`).toPromise();
        } catch {
          pago = null;
        }
      }
      if (!pago) pago = this.membresia || (localStorage.getItem('ultimoPago') ? JSON.parse(localStorage.getItem('ultimoPago')!) : null);

      if (this.logoBase64 === null) {
        try {
          const logoResp = await fetch('/gym.jpg');
          if (logoResp.ok) {
            const logoBlob = await logoResp.blob();
            this.logoBase64 = await new Promise<string>((resolve) => {
              const reader = new FileReader();
              reader.onload = () => resolve(reader.result as string);
              reader.onerror = () => resolve('');
              reader.readAsDataURL(logoBlob);
            });
          } else {
            this.logoBase64 = '';
          }
        } catch {
          this.logoBase64 = '';
        }
      }
      const logoSrc = this.logoBase64 ?? '';
      const logoImg = logoSrc
        ? `<img src="${logoSrc}" style="width: 60px; height: 60px; border-radius: 12px; object-fit: cover; border: 2px solid rgba(255,255,255,0.3);" />`
        : `<div style="width: 60px; height: 60px; border-radius: 12px; background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; font-size: 1.5rem; font-weight: 800; color: #fff;">S</div>`;

      const now = new Date();
      const fechaStr = now.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' });
      const horaStr = now.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
      const boletaNum = `BOL-${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}-${String(cliente?.dni ?? '0000').slice(-4)}`;

      const cNombre = cliente?.nombre ?? 'Cliente';
      const cApellido = cliente?.apellido ?? '';
      const cDni = cliente?.dni ?? 'N/A';
      const cEmail = cliente?.email ?? 'N/A';
      const cTelefono = cliente?.telefono ?? 'N/A';
      const pPlan = pago?.plan ?? 'Suscripción Mensual';
      const pMonto = pago?.monto ? 'S/ ' + pago.monto : 'N/A';
      const pEstado = pago?.estado ?? 'Pendiente';
      const estadoColor = (pEstado === 'Activo' || pEstado === 'activo' || pEstado === 'COMPLETED') ? '#198754' : '#dc3545';

      const receiptHtml = `
        <div style="font-family: 'Segoe UI', Arial, sans-serif; width: 760px; background: #fff; color: #2c3e50;">
          <div style="background: #2c3e50; padding: 30px 40px; display: flex; align-items: center; justify-content: space-between;">
            <div style="display: flex; align-items: center; gap: 16px;">
              ${logoImg}
              <div>
                <div style="color: #fff; font-size: 1.6rem; font-weight: 800; letter-spacing: -0.5px;">Stylossfitness</div>
                <div style="color: rgba(255,255,255,0.75); font-size: 0.85rem; margin-top: 2px;">Centro de Entrenamiento</div>
              </div>
            </div>
            <div style="text-align: right;">
              <div style="color: rgba(255,255,255,0.7); font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1px;">Boleta de Pago</div>
              <div style="color: #fff; font-size: 1.1rem; font-weight: 700; margin-top: 4px;">${boletaNum}</div>
            </div>
          </div>
          <div style="background: #f8f9fa; padding: 12px 40px; display: flex; justify-content: space-between; border-bottom: 1px solid #e9ecef;">
            <div style="font-size: 0.85rem; color: #6c757d;">Fecha: <strong style="color: #2c3e50;">${fechaStr}</strong></div>
            <div style="font-size: 0.85rem; color: #6c757d;">Hora: <strong style="color: #2c3e50;">${horaStr}</strong></div>
          </div>
          <div style="padding: 28px 40px;">
            <div style="font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1.5px; color: #3498db; font-weight: 700; margin-bottom: 12px;">Datos del Cliente</div>
            <div style="display: flex; gap: 40px;">
              <div style="flex: 1;">
                <div style="margin-bottom: 8px;">
                  <span style="font-size: 0.78rem; color: #95a5a6; display: block;">Nombre completo</span>
                  <span style="font-size: 1rem; font-weight: 600; color: #2c3e50;">${cNombre} ${cApellido}</span>
                </div>
                <div>
                  <span style="font-size: 0.78rem; color: #95a5a6; display: block;">Email</span>
                  <span style="font-size: 1rem; color: #2c3e50;">${cEmail}</span>
                </div>
              </div>
              <div style="flex: 1;">
                <div style="margin-bottom: 8px;">
                  <span style="font-size: 0.78rem; color: #95a5a6; display: block;">DNI</span>
                  <span style="font-size: 1rem; font-weight: 600; color: #2c3e50;">${cDni}</span>
                </div>
                <div>
                  <span style="font-size: 0.78rem; color: #95a5a6; display: block;">Telefono</span>
                  <span style="font-size: 1rem; color: #2c3e50;">${cTelefono}</span>
                </div>
              </div>
            </div>
          </div>
          <div style="height: 1px; background: #dee2e6; margin: 0 40px;"></div>
          <div style="padding: 28px 40px;">
            <div style="font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1.5px; color: #3498db; font-weight: 700; margin-bottom: 16px;">Detalle del Pago</div>
            <table style="width: 100%; border-collapse: collapse;">
              <thead>
                <tr style="background: #2c3e50; color: #fff;">
                  <th style="padding: 12px 16px; text-align: left; font-size: 0.82rem; font-weight: 600; border-radius: 8px 0 0 0;">Concepto</th>
                  <th style="padding: 12px 16px; text-align: center; font-size: 0.82rem; font-weight: 600;">Estado</th>
                  <th style="padding: 12px 16px; text-align: right; font-size: 0.82rem; font-weight: 600; border-radius: 0 8px 0 0;">Monto</th>
                </tr>
              </thead>
              <tbody>
                <tr style="border-bottom: 1px solid #f0f0f0;">
                  <td style="padding: 14px 16px; font-size: 0.95rem; font-weight: 500;">${pPlan}</td>
                  <td style="padding: 14px 16px; text-align: center;">
                    <span style="background: ${estadoColor}; color: #fff; padding: 4px 14px; border-radius: 20px; font-size: 0.78rem; font-weight: 600;">${pEstado}</span>
                  </td>
                  <td style="padding: 14px 16px; text-align: right; font-size: 1.1rem; font-weight: 700; color: #2c3e50;">${pMonto}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div style="margin: 0 40px; background: #2c3e50; border-radius: 12px; padding: 20px 24px; display: flex; justify-content: space-between; align-items: center;">
            <span style="color: rgba(255,255,255,0.8); font-size: 0.9rem; font-weight: 500;">Total a pagar</span>
            <span style="color: #fff; font-size: 1.5rem; font-weight: 800;">${pMonto}</span>
          </div>
          <div style="padding: 24px 40px; text-align: center;">
            <div style="font-size: 0.85rem; color: #2c3e50; font-weight: 600; margin-bottom: 4px;">Gracias por tu preferencia</div>
            <div style="font-size: 0.78rem; color: #95a5a6;">Av. Universitaria 7451, Comas 15314 | contacto@stylossfitness.com</div>
            <div style="margin-top: 12px; font-size: 0.7rem; color: #bdc3c7;">Este documento no tiene valor tributario. Es solo de referencia informativa.</div>
          </div>
        </div>`;

      const wrapper = document.createElement('div');
      wrapper.style.position = 'absolute';
      wrapper.style.left = '-9999px';
      wrapper.style.top = '0';
      wrapper.style.width = '760px';
      wrapper.innerHTML = receiptHtml;
      document.body.appendChild(wrapper);
      const html2canvasModule: any = await import('html2canvas');
      const html2canvas = html2canvasModule?.default ?? html2canvasModule;

      const jspdfModule: any = await import('jspdf');
      const jsPDF = jspdfModule?.jsPDF ?? jspdfModule?.default ?? jspdfModule;

      if (!html2canvas || !jsPDF) {
        document.body.removeChild(wrapper);
        this.mensajeError = 'No se pudo generar el PDF. Intente de nuevo.';
        setTimeout(() => this.mensajeError = '', 4000);
        return;
      }

      const canvas: HTMLCanvasElement = await html2canvas(wrapper, { scale: 2, backgroundColor: '#ffffff' });

      const imgData = canvas.toDataURL('image/png');
      document.body.removeChild(wrapper);

      const pdf = new jsPDF('portrait', 'pt', 'a4');
      const pw = pdf.internal.pageSize.getWidth();
      const ph = pdf.internal.pageSize.getHeight();
      const ratio = Math.min((pw - 40) / canvas.width, (ph - 60) / canvas.height);
      const rw = canvas.width * ratio;
      const rh = canvas.height * ratio;
      pdf.addImage(imgData, 'PNG', (pw - rw) / 2, 30, rw, rh);
      pdf.save(`boleta-${(cliente?.dni ?? 'cliente')}-${new Date().toISOString().split('T')[0]}.pdf`);

      this.mensajeExito = 'Boleta descargada correctamente';
      setTimeout(() => this.mensajeExito = '', 3000);

    } catch (err) {
      console.error('[Boleta] Error completo:', err);
      this.mensajeError = 'Error al generar la boleta: ' + (err instanceof Error ? err.message : String(err));
      setTimeout(() => this.mensajeError = '', 6000);
    }
  }

  loadMembresia(dni: number) {
    this.http.get(API_ENDPOINTS.MEMBRESIA_ACTIVA(dni)).subscribe(
      (response: any) => { this.membresia = response; this.calcularEstadisticas(); },
      () => { this.membresia = null; }
    );
  }

  calcularEstadisticas() {
    if (!this.membresia) return;
    const planesMap: { [key: number]: string } = { 1: 'Fit', 2: 'Black' };
    this.subscripcionActual = planesMap[this.membresia.id_membresia] || 'Plan desconocido';

    const parseLocalDate = (d: string): Date => {
      if (!d) return new Date();
      const [year, month, day] = d.split('-').map(Number);
      return new Date(year, month - 1, day);
    };

    const fi = parseLocalDate(this.membresia.fecha_inicio);
    const ff = parseLocalDate(this.membresia.fecha_fin);
    const hoy = new Date();
    this.diasTranscurridos = Math.floor((hoy.getTime() - fi.getTime()) / (86400000));
    this.diasRestantes = Math.max(0, Math.floor((ff.getTime() - hoy.getTime()) / (86400000)));
    this.fechaRenovacion = ff.toLocaleDateString('es-ES');
  }

  private guardarRutina() {
    const dni = this.usuario?.dni;
    if (!dni || this.rutinaGenerada.length === 0) return;
    try {
      localStorage.setItem(`rutina_${dni}`, JSON.stringify({
        rutina: this.rutinaGenerada,
        split: this.rutinaSplit,
        form: this.rutinaForm
      }));
    } catch { }
  }

  private cargarRutina() {
    const dni = this.usuario?.dni;
    if (!dni) return;
    try {
      const data = localStorage.getItem(`rutina_${dni}`);
      if (data) {
        const parsed = JSON.parse(data);
        this.rutinaGenerada = parsed.rutina || [];
        this.rutinaSplit = parsed.split || '';
        this.rutinaForm = parsed.form || { objetivo: '', nivel: '', dias: 3, duracion: 45 };
        if (this.rutinaGenerada.length > 0) {
          this.showRutinaResult = true;
        }
      }
    } catch { }
  }

  quitarRutina() {
    const dni = this.usuario?.dni;
    this.rutinaGenerada = [];
    this.rutinaSplit = '';
    if (dni) {
      try { localStorage.removeItem(`rutina_${dni}`); } catch { }
    }
  }

  get esPlanBlack(): boolean {
    return this.membresia?.id_membresia === 2;
  }

  mostrarUpgradeRutina() {
    this.mensajeError = '';
    this.mensajeExito = '';
    this.mensajeUpgrade = 'La rutina personalizada esta disponible solo con el PLAN Black. ¡Actualiza tu membresia para desbloquearla!';
    setTimeout(() => { this.mensajeUpgrade = ''; }, 6000);
  }

  logout() { this.authService.logout(); this.usuario = null; this.router.navigate(['/']); }
}