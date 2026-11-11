import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';
import { filter, map, take } from 'rxjs';
import { TokenService } from '../services/token.service';
import { toObservable } from '@angular/core/rxjs-interop';

export const userLoginGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  void route;
  void state;

  const platformId = inject(PLATFORM_ID);
  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  const auth = inject(AuthService);
  const token = inject(TokenService);
  const router: Router = inject(Router);

  return toObservable(auth.isAuthReady).pipe(
    filter(isReady => isReady === true),
    take(1),
    map(() => {
      return Boolean(token.getToken()) && auth.isLoggedIn()
        ? true
        : router.createUrlTree(['/'], { queryParams: { auth: 'login' } });
    })
  );
};

export const userLoginChildGuard: CanActivateChildFn = (
  childRoute: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => userLoginGuard(childRoute, state);
