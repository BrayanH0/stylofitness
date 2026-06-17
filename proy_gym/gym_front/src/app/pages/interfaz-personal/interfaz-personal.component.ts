import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Navbar } from 'src/app/components/navbar/navbar';
import { AuthService } from 'src/app/services/auth.service';
import { API_ENDPOINTS } from 'src/app/config/api-config';

interface CalendarDay {
  date: string;
  dayNumber: number;
  isCurrentMonth: boolean;
  isToday: boolean;
  clases: any[];
}

const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
const DIAS_SEMANA = ['Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab', 'Dom'];

@Component({
  selector: 'app-interfaz-personal',
  standalone: true,
  imports: [FormsModule, CommonModule, Navbar],
  templateUrl: './interfaz-personal.component.html',
  styleUrls: ['./interfaz-personal.component.css']
})
export class InterfazPersonalComponent implements OnInit {

  usuario: any = null;
  loading: boolean = false;
  clases: any[] = [];
  selectedClase: any = null;
  showCreateModal: boolean = false;
  deletingIds: Set<string> = new Set<string>();
  fechaActual: string = '';

  currentMonth: number = new Date().getMonth();
  currentYear: number = new Date().getFullYear();
  calendarDays: CalendarDay[] = [];
  selectedDate: string | null = null;
  diasSemana = DIAS_SEMANA;

  newClase: any = {
    nombre: '', fechaClase: '',
    horai: '', horaf: '', horario: '',
    idTrainer: null, estado: 'ACTIVO', cupo: 20
  };
  createError: string = '';

  showInscritos: boolean = false;
  claseInscritos: any = null;
  inscritos: any[] = [];
  loadingInscritos: boolean = false;

  get esAdmin(): boolean {
    return this.authService.getUserRole() === 'ADMIN';
  }

  get clasesActivas(): number {
    return this.clases.filter(c => {
      const est = (c.estado || '').toUpperCase();
      return est === 'ACTIVO' || est === 'ACTIVA';
    }).length;
  }

  get monthLabel(): string {
    return `${MESES[this.currentMonth]} ${this.currentYear}`;
  }

  get selectedDateClases(): any[] {
    if (!this.selectedDate) return [];
    return this.clases.filter(c => this.normalizeDate(c.fechaClase) === this.selectedDate);
  }

  constructor(
    private http: HttpClient,
    private router: Router,
    private cd: ChangeDetectorRef,
    private authService: AuthService
  ) {}

  ngOnInit() {
    if (!this.authService.isLoggedIn() || !this.authService.isPersonalOrAdmin()) {
      this.router.navigate(['/']);
      return;
    }
    this.usuario = this.authService.getUserInfo();
    this.fechaActual = new Date().toLocaleDateString('es-ES', { day: '2-digit', month: 'short' });
    this.getClases();
  }

  generateCalendarDays() {
    const days: CalendarDay[] = [];
    const firstDay = new Date(this.currentYear, this.currentMonth, 1);
    const lastDay = new Date(this.currentYear, this.currentMonth + 1, 0);

    let startDow = firstDay.getDay() - 1;
    if (startDow < 0) startDow = 6;

    const prevMonthLastDay = new Date(this.currentYear, this.currentMonth, 0).getDate();
    for (let i = startDow - 1; i >= 0; i--) {
      const dayNum = prevMonthLastDay - i;
      const d = new Date(this.currentYear, this.currentMonth - 1, dayNum);
      const dateStr = this.formatDateStr(d);
      days.push({
        date: dateStr,
        dayNumber: dayNum,
        isCurrentMonth: false,
        isToday: false,
        clases: this.clasesForDate(dateStr)
      });
    }

    const today = new Date();
    const todayStr = this.formatDateStr(today);
    for (let day = 1; day <= lastDay.getDate(); day++) {
      const d = new Date(this.currentYear, this.currentMonth, day);
      const dateStr = this.formatDateStr(d);
      days.push({
        date: dateStr,
        dayNumber: day,
        isCurrentMonth: true,
        isToday: dateStr === todayStr,
        clases: this.clasesForDate(dateStr)
      });
    }

    const remaining = 42 - days.length;
    for (let i = 1; i <= remaining; i++) {
      const d = new Date(this.currentYear, this.currentMonth + 1, i);
      const dateStr = this.formatDateStr(d);
      days.push({
        date: dateStr,
        dayNumber: i,
        isCurrentMonth: false,
        isToday: false,
        clases: this.clasesForDate(dateStr)
      });
    }

    this.calendarDays = days;
  }

  prevMonth() {
    if (this.currentMonth === 0) {
      this.currentMonth = 11;
      this.currentYear--;
    } else {
      this.currentMonth--;
    }
    this.generateCalendarDays();
  }

  nextMonth() {
    if (this.currentMonth === 11) {
      this.currentMonth = 0;
      this.currentYear++;
    } else {
      this.currentMonth++;
    }
    this.generateCalendarDays();
  }

  goToday() {
    const today = new Date();
    this.currentMonth = today.getMonth();
    this.currentYear = today.getFullYear();
    this.selectedDate = this.formatDateStr(today);
    this.generateCalendarDays();
  }

  onDayClick(day: CalendarDay) {
    this.selectedDate = day.date;
    this.selectedClase = null;
  }

  selectClaseFromCalendar(c: any) {
    this.selectedClase = c;
  }

  private clasesForDate(dateStr: string): any[] {
    return this.clases.filter(c =>
      this.normalizeDate(c.fechaClase) === dateStr &&
      c.estado !== 'FINALIZADA'
    );
  }

  private formatDateStr(d: Date): string {
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  getClases() {
    this.loading = true;
    const dni = this.usuario?.dni;
    const url = dni ? API_ENDPOINTS.CLASES_TRAINER(dni) : API_ENDPOINTS.CLASES;
    this.http.get(url).subscribe({
      next: (data: any) => {
        let arr: any[] = [];
        if (Array.isArray(data)) arr = data;
        else if (data && Array.isArray(data.value)) arr = data.value;
        else if (data && Array.isArray(data.clases)) arr = data.clases;
        else if (data && typeof data === 'object') {
          const possible = Object.keys(data).map(k => data[k]).find(v => Array.isArray(v));
          if (possible) arr = possible;
        }
        const hoyStr = this.formatDateStr(new Date());
        const ahora = new Date();
        this.clases = (arr || []).map((c: any) => {
          const n = this.normalizeDate(c.fechaClase);
          if (n) c.fechaClase = n;
          return c;
        }).filter((c: any) => {
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
        this.generateCalendarDays();
        this.loading = false;
      },
      error: () => {
        this.clases = [];
        this.generateCalendarDays();
        this.loading = false;
      }
    });
  }

  createClase() {
    this.createError = '';

    if (!this.newClase.nombre) {
      this.createError = 'El nombre de la clase es obligatorio.';
      return;
    }

    if (this.newClase.fechaClase) {
      const fechaClase = new Date(this.newClase.fechaClase + 'T00:00:00');
      const hoy = new Date();
      hoy.setHours(0, 0, 0, 0);
      if (fechaClase < hoy) {
        this.createError = 'No se pueden crear clases en fechas pasadas.';
        return;
      }
      if (fechaClase.getTime() === hoy.getTime() && this.newClase.horai) {
        const [h, m] = this.newClase.horai.split(':').map(Number);
        const horaInicio = new Date();
        horaInicio.setHours(h, m, 0, 0);
        const ahora = new Date();
        if (horaInicio <= ahora) {
          this.createError = 'La hora de inicio debe ser posterior a la hora actual.';
          return;
        }
      }
    }

    if (this.newClase.horai && this.newClase.horai < '06:00') {
      this.createError = 'Las clases no pueden comenzar antes de las 06:00.';
      return;
    }

    if (this.newClase.horai && this.newClase.horaf) {
      if (this.newClase.horaf <= this.newClase.horai) {
        this.createError = 'La hora de fin debe ser posterior a la hora de inicio.';
        return;
      }
      const [h1, m1] = this.newClase.horai.split(':').map(Number);
      const [h2, m2] = this.newClase.horaf.split(':').map(Number);
      const duracionMin = (h2 * 60 + m2) - (h1 * 60 + m1);
      if (duracionMin < 30) {
        this.createError = 'La clase debe durar al menos 30 minutos.';
        return;
      }
    }

    if (!this.newClase.cupo || this.newClase.cupo < 10) {
      this.createError = 'El cupo debe ser al menos 10.';
      return;
    }
    if (this.newClase.cupo > 30) {
      this.createError = 'El cupo no puede superar 30 personas.';
      return;
    }

    const datePart = this.normalizeDate(this.newClase.fechaClase);
    const fechaToSend = datePart ? `${datePart}T12:00:00` : null;

    const body: any = {
      nombre: this.newClase.nombre,
      fechaClase: fechaToSend,
      horai: this.newClase.horai || '',
      horaf: this.newClase.horaf || '',
      horario: this.newClase.horai && this.newClase.horaf ? `${this.newClase.horai} - ${this.newClase.horaf}` : '',
      idTrainer: this.usuario?.dni || null,
      estado: 'activo',
      cupo: this.newClase.cupo
    };

    this.loading = true;
    this.http.post(API_ENDPOINTS.CLASES, body).subscribe({
      next: (created: any) => {
        const added = {
          ...created,
          fechaClase: this.normalizeDate(created?.fechaClase ?? body.fechaClase)
        };
        if (!this.clases) this.clases = [];
        this.clases.unshift(added);
        this.generateCalendarDays();
        this.showCreateModal = false;
        this.loading = false;
        this.resetNewClase();
      },
      error: (err) => {
        this.createError = err?.error?.message || err?.error?.error || 'Error al crear la clase.';
        this.loading = false;
      }
    });
  }

  deleteClase(id: number) {
    if (!id) return;
    if (!confirm('¿Eliminar esta clase?')) return;
    const sid = String(id);
    this.deletingIds.add(sid);
    this.cd.detectChanges();
    this.http.delete(API_ENDPOINTS.CLASE(id)).subscribe({
      next: () => {
        this.clases = this.clases.filter(c => String(c.idClase || c.id) !== sid);
        this.generateCalendarDays();
        if (this.selectedClase && String(this.selectedClase.idClase || this.selectedClase.id) === sid) {
          this.selectedClase = null;
        }
        if (this.selectedDate) {
          const remaining = this.clases.filter(c => this.normalizeDate(c.fechaClase) === this.selectedDate);
          if (remaining.length === 0) {
            this.selectedDate = null;
          }
        }
        this.deletingIds.delete(sid);
        this.cd.detectChanges();
      },
      error: () => {
        this.deletingIds.delete(sid);
        this.cd.detectChanges();
      }
    });
  }

  selectClase(c: any) { this.selectedClase = c; }

  verInscritos(clase: any) {
    this.claseInscritos = clase;
    this.inscritos = [];
    this.loadingInscritos = true;
    this.showInscritos = true;
    const id = clase.idClase || clase.id;
    this.http.get<any[]>(API_ENDPOINTS.INSCRIPCIONES_CLASE(id)).subscribe({
      next: (data) => {
        this.inscritos = Array.isArray(data) ? data : [];
        this.loadingInscritos = false;
      },
      error: () => {
        this.inscritos = [];
        this.loadingInscritos = false;
      }
    });
  }

  closeInscritos() {
    this.showInscritos = false;
    this.claseInscritos = null;
    this.inscritos = [];
  }

  openCreateModal() {
    this.resetNewClase();
    this.newClase.fechaClase = new Date().toISOString().split('T')[0];
    this.showCreateModal = true;
  }

  closeCreateModal() { this.showCreateModal = false; }

  onAction(text: string) {
    if (text === 'Calculo de ventas') this.router.navigate(['/admin']);
  }

  logout() { this.authService.logout(); this.usuario = null; this.router.navigate(['/']); }

  private resetNewClase() {
    this.newClase = { nombre: '', fechaClase: '', horai: '', horaf: '', horario: '', idTrainer: null, estado: 'activo', cupo: 20 };
    this.createError = '';
  }

  private normalizeDate(d: any): string | null {
    if (!d) return null;
    if (typeof d === 'string') return d.split('T')[0];
    if (typeof d === 'number') return new Date(d).toISOString().split('T')[0];
    if (d instanceof Date) return d.toISOString().split('T')[0];
    try { const dt = new Date(d); if (!isNaN(dt.getTime())) return dt.toISOString().split('T')[0]; } catch {}
    return null;
  }

  getHorario(c: any): string {
    if (c.horai) return c.horai + (c.horaf ? ' - ' + c.horaf : '');
    return c.horario || '—';
  }
}