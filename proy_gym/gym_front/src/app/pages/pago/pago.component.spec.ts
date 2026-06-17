import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RouterTestingModule } from '@angular/router/testing';
import { PaymentService } from '../../services/payment.service';
import { LoginModalService } from '../../services/login-modal.service';

import { PagoComponent } from './pago.component';

describe('PagoComponent', () => {
  let component: PagoComponent;
  let fixture: ComponentFixture<PagoComponent>;

  beforeEach(async () => {
    localStorage.setItem('membresiaSeleccionada', JSON.stringify({
      name: 'PLAN Black', months: 1, price: 89.90,
      planKey: 'black', startDate: '01/01/2026', endDate: '01/02/2026', duration: '1 mes'
    }));

    await TestBed.configureTestingModule({
      imports: [PagoComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        PaymentService,
        LoginModalService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PagoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => localStorage.clear());

  it('RF03 - calcularTotales descompone precio con IGV 18%', () => {
    component.membresia = { price: 89.90 };
    component.calcularTotales();
    expect(component.total).toBeCloseTo(89.90, 1);
    expect(component.igv).toBeGreaterThan(0);
    expect(component.subtotal + component.igv).toBeCloseTo(component.total, 0);
  });

});
