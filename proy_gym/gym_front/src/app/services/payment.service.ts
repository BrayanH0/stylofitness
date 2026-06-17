import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_ENDPOINTS } from '../config/api-config';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {

  constructor(private http: HttpClient) {}

  createCheckoutSession(plan: string, dni?: number) {
    const url = dni
      ? `${API_ENDPOINTS.PAYMENT_CREATE(plan)}?dni=${dni}`
      : API_ENDPOINTS.PAYMENT_CREATE(plan);
    return this.http.post(url, {}, { responseType: 'text' });
  }
}