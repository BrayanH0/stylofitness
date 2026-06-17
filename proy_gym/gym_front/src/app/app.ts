import { Component, signal, inject } from '@angular/core';
import { Router, RouterOutlet, RouterLink, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { Footer } from './components/footer/footer';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, Footer],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App {
  protected readonly title = signal('Stylossfitness');
  private router = inject(Router);
  protected showFooter = signal(true);

  constructor() {
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event) => {
        const navEnd = event as NavigationEnd;
        const hideFooterRoutes = ['/interfaz-usuario', '/personal', '/admin'];
        this.showFooter.set(!hideFooterRoutes.some(route => navEnd.urlAfterRedirects.startsWith(route)));
      });
  }
}
