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
  const router = inject(Router);

  return hasAdminRole(tokenService)
    ? true
    : buildAdminLoginRedirect(router, state);
};

export const adminChildGuard: CanActivateChildFn = (
  childRoute: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => adminGuard(childRoute, state);
