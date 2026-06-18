import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/models/api-response.model';
import { AddressFormData } from '../components/add-address-modal/add-address-modal.component';
import { CompanyAddress } from '../pages/company-address/company-address.component';

@Injectable({
  providedIn: 'root'
})
export class RecruiterAddressService {
  private readonly apiUrl = `${environment.apiBaseUrl}/addresses`;

  constructor(private readonly http: HttpClient) {}

  getAddresses(): Observable<ApiResponse<CompanyAddress[]>> {
    return this.http.get<ApiResponse<CompanyAddress[]>>(this.apiUrl);
  }

  createAddress(data: AddressFormData): Observable<ApiResponse<CompanyAddress>> {
    return this.http.post<ApiResponse<CompanyAddress>>(this.apiUrl, data);
  }

  updateAddress(id: number, data: AddressFormData): Observable<ApiResponse<CompanyAddress>> {
    return this.http.put<ApiResponse<CompanyAddress>>(`${this.apiUrl}/${id}`, data);
  }

  deleteAddress(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`);
  }
}
