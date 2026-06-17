import { HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

export function AuthInterceptor(request: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
  const token = localStorage.getItem('token');

  const publicEndpoints = [
    '/api/auth/login',
    '/api/usuario/pre-registro',
    '/api/usuario/existe-dni',
    '/api/payment/create-checkout-session',
    '/api/payment/confirm'
  ];

  const url = new URL(request.url, window.location.origin);
  const isPublicEndpoint = publicEndpoints.some(endpoint => url.pathname === endpoint);

  if (token && !isPublicEndpoint) {
    const authRequest = request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(authRequest);
  }

  return next(request);
}