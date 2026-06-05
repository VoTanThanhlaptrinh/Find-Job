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

function logGuardDecision(
  message: string,
  state: RouterStateSnapshot,
  authReady: boolean,
  roles: string[]
): void {
  console.info('[HirerGuard]', {
    message,
    url: state.url,
    authReady,
    roles,
  });
}

export const hirerGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  void route;
  const tokenService = inject(TokenService);
  const authService = inject(AuthService);
  const router = inject(Router);
  const authReady = authService.isAuthReady();
  const roles = tokenService.getTokenRoles();

  // Allow navigation while auth state is still restoring from refresh token.
  if (!authReady) {
    logGuardDecision('Auth is not ready yet, allowing temporary navigation.', state, authReady, roles);
    return true;
  }

  const isAuthorized = hasHirerRole(tokenService);

  logGuardDecision(
    isAuthorized
      ? 'Recruiter role detected, allowing navigation.'
      : 'Recruiter role missing, redirecting to recruiter login.',
    state,
    authReady,
    roles
  );

  return isAuthorized ? true : buildUnauthorizedRedirect(router, state);
};

export const hirerChildGuard: CanActivateChildFn = (
  childRoute: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => hirerGuard(childRoute, state);
