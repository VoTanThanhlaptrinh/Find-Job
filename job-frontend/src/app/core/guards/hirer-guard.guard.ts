import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  CanActivateChildFn,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { inject } from '@angular/core';
import { TokenService } from '../services/token.service';
import { AuthService } from '../services/auth.service';

function buildUnauthorizedRedirect(
  router: Router,
  state: RouterStateSnapshot
): UrlTree {
  return router.createUrlTree(['/recruiter/login'], {
    queryParams: { returnUrl: state.url }
  });
}

function hasHirerRole(tokenService: TokenService): boolean {
  return tokenService.hasAnyRole(['HIRER', 'ROLE_HIRER']);
}

export const hirerGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  void route;
  const tokenService = inject(TokenService);
  const authService = inject(AuthService);
  const router = inject(Router);

  // Allow navigation while auth state is still restoring from refresh token.
  if (!authService.isAuthReady()) {
    return true;
  }

  return hasHirerRole(tokenService)
    ? true
    : buildUnauthorizedRedirect(router, state);
};

export const hirerChildGuard: CanActivateChildFn = (
  childRoute: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => hirerGuard(childRoute, state);
