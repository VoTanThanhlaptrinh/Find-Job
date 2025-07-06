import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  GuardResult,
  MaybeAsync,
  Router,
  RouterStateSnapshot
} from '@angular/router';
import {AuthService} from './auth.service';
import {jwtDecode, JwtPayload} from 'jwt-decode';

@Injectable({
  providedIn: 'root'
})

export class RoleGuardService implements CanActivate {

  constructor(
    private authService:AuthService
    ,private router: Router) {
  }
  canActivate(route: ActivatedRouteSnapshot): boolean {
    const expectedRole = route.data['expectedRole'];
    let token = null;
    if (typeof window !== 'undefined' && window.localStorage) {
      token = localStorage.getItem('jwtToken');
    }
    if(token === null){
      this.router.navigate(['login']);
      return false;
    }
    const tokenPayload = jwtDecode<MyJwtPayload>(token);
    if (this.authService.isAuthenticated()  &&
      this.authService.hasAnyRole(tokenPayload.roles, expectedRole)) {
      return true;
    }else{
      this.router.navigate(['login']);
      return false;
    }
  }
}
interface MyJwtPayload extends JwtPayload {
  roles: string[];
}
