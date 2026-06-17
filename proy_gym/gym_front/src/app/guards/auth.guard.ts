import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { isTokenValid, clearAuthStorage } from '../utils/token.helper';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private router: Router) {}

  canActivate(): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    const token = localStorage.getItem('token');

    if (isTokenValid(token)) {
      return true;
    }

    clearAuthStorage();
    this.router.navigate(['/']);
    return false;
  }
}