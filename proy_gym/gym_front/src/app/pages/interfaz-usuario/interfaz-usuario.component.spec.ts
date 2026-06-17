import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RouterTestingModule } from '@angular/router/testing';
import { LoginModalService } from '../../services/login-modal.service';

import { InterfazUsuarioComponent } from './interfaz-usuario.component';

describe('InterfazUsuarioComponent', () => {
  let component: InterfazUsuarioComponent;
  let fixture: ComponentFixture<InterfazUsuarioComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InterfazUsuarioComponent, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        LoginModalService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InterfazUsuarioComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => localStorage.clear());

  it('RF06 - esPlanBlack es true con plan Black (acceso exclusivo a rutinas)', () => {
    component.membresia = { id_membresia: 2 };
    expect(component.esPlanBlack).toBeTrue();
  });

});
