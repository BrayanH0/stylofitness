import { Component, EventEmitter, Input, Output, ViewChild, AfterViewInit, OnDestroy, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';
import { trapFocus } from 'src/app/utils/modal-focus-trap';

@Component({
  selector: 'app-login-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login-modal.component.html',
  styleUrls: ['./login-modal.component.css']
})
export class LoginModalComponent implements AfterViewInit, OnDestroy {

  loading = false;
  errorMsg: string | null = null;
  showPassword = false;
  private previousFocusElement: HTMLElement | null = null;

  @Input() role: 'usuario' | 'personal' = 'usuario';
  @Output() closed = new EventEmitter<void>();

  @ViewChild('dniInput') dniInput!: ElementRef<HTMLInputElement>;

  form = this.fb.group({
    dni: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
    password: ['', [Validators.required]]
  });

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {}

  ngAfterViewInit(): void {
    this.previousFocusElement = document.activeElement as HTMLElement;
    setTimeout(() => {
      this.dniInput?.nativeElement?.focus();
    }, 100);
  }

  ngOnDestroy(): void {
    if (this.previousFocusElement) {
      this.previousFocusElement.focus();
    }
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.close();
      return;
    }
    const container = (event.target as HTMLElement).closest('.modal-centered');
    if (container) {
      trapFocus(event, container as HTMLElement);
    }
  }

  submit() {
    if (this.form.invalid) return;

    this.loading = true;
    this.errorMsg = null;

    const dni = Number(this.form.value.dni);
    const password = this.form.value.password!;

    this.auth.login(dni, password).subscribe({
      next: () => {
        this.loading = false;
        this.close();

        const rol = this.auth.getUserRole();
        if (rol === 'ADMIN') {
          this.router.navigate(['/admin']);
        } else if (rol === 'PERSONAL') {
          this.router.navigate(['/personal']);
        } else {
          this.router.navigate(['/interfaz-usuario']);
        }
      },
      error: (err: any) => {
        this.loading = false;
        const backendMsg = err?.error?.error || err?.error?.message || err?.error;
        this.errorMsg = backendMsg || 'Usuario o contrasena incorrectos';
      }
    });
  }

  close() {
    this.form.reset();
    this.errorMsg = null;
    this.loading = false;
    this.closed.emit();
  }
}