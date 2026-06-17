import { Component, OnInit, OnDestroy } from '@angular/core';
import { RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { LoginModalComponent } from '../login-modal/login-modal.component';
import { AuthService } from '../../services/auth.service';
import { LoginModalService } from '../../services/login-modal.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule, CommonModule, LoginModalComponent],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css']
})
export class Navbar implements OnInit, OnDestroy {

  loginOpen: boolean = false;
  menuOpen: boolean = false;
  private sub!: Subscription;

  constructor(
    private authService: AuthService,
    private router: Router,
    private loginModalService: LoginModalService
  ) {}

  ngOnInit() {
    this.sub = this.loginModalService.open$.subscribe(() => {
      this.loginOpen = true;
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  openLogin() {
    this.loginOpen = true;
  }

  closeLoginModal() {
    this.loginOpen = false;
  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  isPersonal(): boolean {
    return this.authService.getUserRole() === 'PERSONAL';
  }

  isPersonalOrAdmin(): boolean {
    return this.authService.isPersonalOrAdmin();
  }

  getUserName(): string {
    return this.authService.getUserName() || 'Usuario';
  }

  getUserRole(): string {
    return this.authService.getUserRole() || '';
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}