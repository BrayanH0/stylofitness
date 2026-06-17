import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient()]
    });
    service = TestBed.inject(AuthService);
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('Debe detectar sesion activa con token valido', () => {
    const futureExp = Math.floor(Date.now() / 1000) + 3600;
    const payload = btoa(JSON.stringify({ exp: futureExp }));
    localStorage.setItem('token', `header.${payload}.sig`);

    expect(service.isLoggedIn()).toBeTrue();
  });

  it('Debe detectar sesion inactiva con token expirado', () => {
    const pastExp = Math.floor(Date.now() / 1000) - 3600;
    const payload = btoa(JSON.stringify({ exp: pastExp }));
    localStorage.setItem('token', `header.${payload}.sig`);

    expect(service.isLoggedIn()).toBeFalse();
  });

  it('Debe limpiar localStorage al hacer logout', () => {
    localStorage.setItem('token', 'abc');
    localStorage.setItem('rol', 'CLIENTE');

    service.logout();

    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('rol')).toBeNull();
  });
});
