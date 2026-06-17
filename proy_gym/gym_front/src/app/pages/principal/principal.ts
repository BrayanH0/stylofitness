import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Navbar } from '../../components/navbar/navbar';
import { LoginModalService } from '../../services/login-modal.service';

@Component({
  selector: 'app-principal',
  standalone: true,
  imports: [CommonModule, RouterLink, Navbar],
  templateUrl: './principal.html',
  styleUrls: ['./principal.css']
})
export class Principal {
  constructor(private loginModalService: LoginModalService, private router: Router) {}

  openLogin() {
    this.loginModalService.open();
  }

  elegirPlan(planKey: string) {
    this.router.navigate(['/registrar'], { queryParams: { plan: planKey } });
  }

  planes = [
    {
      key: 'black',
      nombre: 'PLAN Black',
      descripcion: 'Acceso total a todas nuestras sedes y beneficios exclusivos',
      precio: {
        promocional: 'S/ 89,90*',
        regular: 'S/ 109,90/mes',
        periodo: 'Entrena todo un mes',
        fidelidad: '12 meses de fidelidad'
      },
      destacado: true
    },
    {
      key: 'fit',
      nombre: 'PLAN Fit',
      descripcion: 'Acceso a una sede con los beneficios esenciales',
      precio: {
        promocional: 'S/ 69,90*',
        regular: 'S/ 79,90/mes',
        periodo: 'Entrena todo un mes',
        fidelidad: '12 meses de fidelidad'
      },
      destacado: false
    }
  ];

  beneficios = [
    {
      nombre: 'Entrena en todas nuestras clases grupales – Más de 50 clases semanales',
      planBlack: true,
      planFit: true,
      esTitulo: true
    },
    {
      nombre: 'Acceso ilimitado a todas las áreas de peso libre e integrado – Máquinas, pesas, discos y barras',
      planBlack: true,
      planFit: true,
      esTitulo: false
    },
    {
      nombre: 'Clases grupales con profesores – Activate, baila y relájate',
      planBlack: true,
      planFit: true,
      esTitulo: false
    },
    {
      nombre: 'Evaluación detallada – Tu plan de entrenamiento personalizado',
      planBlack: true,
      planFit: false,
      esTitulo: false
    },
    {
      nombre: 'Stylo Fitness Go – Entrena donde y cuando quieras',
      planBlack: true,
      planFit: false,
      esTitulo: false
    },
    {
      nombre: 'Acceso a todas nuestras sedes',
      planBlack: true,
      planFit: false,
      esTitulo: false
    },
    {
      nombre: 'Relájate en los sillones de masajes',
      planBlack: true,
      planFit: false,
      esTitulo: false
    },
    {
      nombre: '5 invitados al mes en el gimnasio que quieras',
      planBlack: true,
      planFit: false,
      esTitulo: false
    },
    {
      nombre: 'Casillero compartido',
      planBlack: true,
      planFit: true,
      esTitulo: false
    }
  ];
}