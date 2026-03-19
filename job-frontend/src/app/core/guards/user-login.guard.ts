import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';


export const userLoginGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const auth = inject(AuthService);
  const router: Router = inject(Router);
  const protectRoutes: string[] = ['/infor']; // for check the url is protect by guard
  return true;
  // return auth.checkLogin().pipe(
  //   // đảm bảo Guard kết thúc sau emit đầu tiên
  //   take(1),
  //   // map sang GuardResult
  //   map((isLoggedIn: boolean): boolean | UrlTree => {
  //     return isLoggedIn && protectRoutes.includes(state.url)
  //       ? true
  //       : router.createUrlTree(['/login']);
  //   }),
  //   // nếu API lỗi → coi như không đăng nhập
  //   catchError(() =>
  //     of(
  //       router.createUrlTree(['/login'], {
  //         queryParams: { redirectUrl: state.url },
  //       })
  //     )
  //   )
  // );
};
