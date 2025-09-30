import { isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private _platformID = inject(PLATFORM_ID);
  constructor() { }

  public setToken(token: string): void {
    if(isPlatformBrowser(this._platformID))
    sessionStorage.setItem('jwtToken', token);
  }

  public getToken(): string {
     if(isPlatformBrowser(this._platformID)){
      return sessionStorage.getItem('jwtToken') || '';
     }
    return  '';
  }
  public clearToken(): void {
     if(isPlatformBrowser(this._platformID))
    sessionStorage.removeItem('jwtToken');
  }
}
