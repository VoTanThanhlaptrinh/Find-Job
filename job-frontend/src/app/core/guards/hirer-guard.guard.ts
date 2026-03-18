import {
  CanActivateFn,
  RedirectCommand,
  Router,
  UrlTree,
} from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError, map, of, take } from 'rxjs';
export const hirerGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const protectRoutes: string[] = ['/hirer'];
  const router = inject(Router);
  return auth.checkHirerLogin().pipe(
    take(1),
    map((value: boolean): boolean | UrlTree => {
      return value && protectRoutes.includes(state.url)
        ? true
        : router.createUrlTree(['/login']);
    }),
    catchError(() => of(router.createUrlTree(['/login'])))
  );
};
