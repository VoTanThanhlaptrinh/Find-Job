import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';

import { AuthService } from '../services/auth.service';
import { TokenService } from '../services/token.service';
import { hirerGuard } from './hirer-guard.guard';

describe('hirerGuard', () => {
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let tokenServiceMock: jasmine.SpyObj<TokenService>;
  let routerMock: jasmine.SpyObj<Router>;

  const executeGuard = (url = '/recruiter/dashboard') =>
    TestBed.runInInjectionContext(() =>
      hirerGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot)
    );

  beforeEach(() => {
    authServiceMock = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthReady']);
    tokenServiceMock = jasmine.createSpyObj<TokenService>('TokenService', [
      'hasAnyRole',
      'getTokenRoles',
    ]);
    routerMock = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: TokenService, useValue: tokenServiceMock },
        { provide: Router, useValue: routerMock },
      ],
    });

    tokenServiceMock.getTokenRoles.and.returnValue([]);
  });

  it('allows navigation while auth is still restoring', () => {
    authServiceMock.isAuthReady.and.returnValue(false);

    const result = executeGuard();

    expect(result).toBeTrue();
    expect(tokenServiceMock.hasAnyRole).not.toHaveBeenCalled();
  });

  it('allows navigation for recruiter roles', () => {
    authServiceMock.isAuthReady.and.returnValue(true);
    tokenServiceMock.hasAnyRole.and.returnValue(true);

    const result = executeGuard('/recruiter/jobs');

    expect(result).toBeTrue();
  });

  it('redirects to recruiter login when recruiter role is missing', () => {
    const redirectTree = {} as UrlTree;

    authServiceMock.isAuthReady.and.returnValue(true);
    tokenServiceMock.hasAnyRole.and.returnValue(false);
    routerMock.createUrlTree.and.returnValue(redirectTree);

    const result = executeGuard('/recruiter/company-address');

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/recruiter/login'], {
      queryParams: { returnUrl: '/recruiter/company-address' },
    });
    expect(result).toBe(redirectTree);
  });
});
