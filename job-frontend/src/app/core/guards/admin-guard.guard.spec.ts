import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';

import { TokenService } from '../services/token.service';
import { adminGuard } from './admin-guard.guard';

describe('adminGuard', () => {
  let tokenServiceMock: jasmine.SpyObj<TokenService>;
  let routerMock: jasmine.SpyObj<Router>;

  const executeGuard = (url = '/admin/dashboard') =>
    TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot)
    );

  beforeEach(() => {
    tokenServiceMock = jasmine.createSpyObj<TokenService>('TokenService', [
      'getToken',
      'getTokenRoles',
    ]);
    routerMock = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);

    TestBed.configureTestingModule({
      providers: [
        { provide: TokenService, useValue: tokenServiceMock },
        { provide: Router, useValue: routerMock },
      ],
    });

    tokenServiceMock.getToken.and.returnValue('');
    tokenServiceMock.getTokenRoles.and.returnValue([]);
  });

  it('allows navigation when admin token and role are present', () => {
    tokenServiceMock.getToken.and.returnValue('token-value');
    tokenServiceMock.getTokenRoles.and.returnValue(['ROLE_ADMIN']);

    const result = executeGuard('/admin/jobs');

    expect(result).toBeTrue();
    expect(tokenServiceMock.getTokenRoles).toHaveBeenCalledWith('token-value');
  });

  it('redirects to admin login when token is missing', () => {
    const redirectTree = {} as UrlTree;
    routerMock.createUrlTree.and.returnValue(redirectTree);

    const result = executeGuard('/admin/employers');

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/admin/login'], {
      queryParams: { returnUrl: '/admin/employers' },
    });
    expect(result).toBe(redirectTree);
    expect(tokenServiceMock.getTokenRoles).not.toHaveBeenCalled();
  });

  it('redirects to admin login when admin role is missing', () => {
    const redirectTree = {} as UrlTree;

    tokenServiceMock.getToken.and.returnValue('token-value');
    tokenServiceMock.getTokenRoles.and.returnValue(['USER']);
    routerMock.createUrlTree.and.returnValue(redirectTree);

    const result = executeGuard('/admin/billing');

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/admin/login'], {
      queryParams: { returnUrl: '/admin/billing' },
    });
    expect(result).toBe(redirectTree);
  });
});
