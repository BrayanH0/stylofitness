import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RouterTestingModule } from '@angular/router/testing';
import { LoginModalService } from '../../services/login-modal.service';

import { Registrar } from './registrar';

describe('Registrar', () => {
  let component: Registrar;
  let fixture: ComponentFixture<Registrar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Registrar, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        LoginModalService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Registrar);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => localStorage.clear());

  it('RF01 - guardarMembresia retorna false si no hay plan ni tipo seleccionado', () => {
    component.planSeleccionado = null;
    component.membershipType = '';
    expect(component.guardarMembresia()).toBeFalse();
  });

  it('RF01 - guardarMembresia guarda en localStorage con plan Black 1 mes', () => {
    component.seleccionarPlan('black');
    component.selectMembership('1-mes', 1);
    const stored = JSON.parse(localStorage.getItem('membresiaSeleccionada') || '{}');
    expect(stored.planKey).toBe('black');
    expect(stored.months).toBe(1);
    expect(stored.price).toBe(89.90);
  });
});
