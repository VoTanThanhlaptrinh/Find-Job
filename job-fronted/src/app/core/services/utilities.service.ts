import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class UtilitiesService {
  private url_dev: string;
  private url_product: string;
  constructor() { 
    this.url_dev = 'http://localhost:8080/api';
    this.url_product = '';
  }
  getURLDev(){
    return this.url_dev
  }
  getURLProduct(){
    return this.url_product
  }
}
