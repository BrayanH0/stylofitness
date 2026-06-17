import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Navbar } from '../../components/navbar/navbar';

@Component({
  selector: 'app-cancel',
  standalone: true,
  imports: [CommonModule, Navbar],
  templateUrl: './cancel.component.html',
  styleUrls: ['./cancel.component.css']
})
export class CancelComponent {

  constructor(private router: Router) {}

  goToHome() {
    this.router.navigate(['/']);
  }

  goToPayment() {
    this.router.navigate(['/pago']);
  }

  goToPlans() {
    this.router.navigate(['/registrar']);
  }
}