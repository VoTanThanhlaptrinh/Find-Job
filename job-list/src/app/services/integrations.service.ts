import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from 'express';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class IntegrationsService {
  constructor(private http: HttpClient) { }
  private url = "http://localhost:8080/api/integrations"
  getIntegrations(): Observable<HttpResponse<any>> {
    return this.http.get<any>(this.url, { observe: 'response' });
  }
}
