import { inject } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivateChildFn,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';

import { TokenService } from '../services/token.service';
import { AuthService } from '../services/auth.service';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs';

function buildAdminLoginRedirect(
  router: Router,
  state: RouterStateSnapshot
): UrlTree {
  return router.createUrlTree(['/admin/login'], {
    queryParams: { returnUrl: state.url },
  });
}

function hasAdminRole(tokenService: TokenService): boolean {
  const token = tokenService.getToken();

  if (!token) {
    return false;
  }

  const roles = tokenService.getTokenRoles(token);
  return roles.includes('ADMIN') || roles.includes('ROLE_ADMIN');
}

export const adminGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  void route;
  const tokenService = inject(TokenService);
  const authService = inject(AuthService);
  const router = inject(Router);

  return toObservable(authService.isAuthReady).pipe(
    filter(isReady => isReady === true),
    take(1),
    map(() => {
      return hasAdminRole(tokenService)
        ? true
        : buildAdminLoginRedirect(router, state);
    })
  );
};

export const adminChildGuard: CanActivateChildFn = (
  childRoute: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => adminGuard(childRoute, state);
