import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';
import {map, take} from 'rxjs';
import { TokenService } from '../services/token.service';


export const userLoginGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const auth = inject(AuthService);
  const token = inject(TokenService);
  const router: Router = inject(Router);
  const protectRoutes: string[] = ['/infor']; 
  return token.getToken() !== null && auth.isLoggedIn() ? true : router.parseUrl('/login');
};
