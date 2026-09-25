import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';
import { filter, map, take } from 'rxjs';
import { TokenService } from '../services/token.service';
import { toObservable } from '@angular/core/rxjs-interop';


export const userLoginGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const auth = inject(AuthService);
  const token = inject(TokenService);
  const router: Router = inject(Router);

  return toObservable(auth.isAuthReady).pipe(
    filter(isReady => isReady === true),
    take(1),
    map(() => {
      return token.getToken() !== null && auth.isLoggedIn()
        ? true
        : router.createUrlTree(['/'], { queryParams: { auth: 'login' } });
    })
  );
};
