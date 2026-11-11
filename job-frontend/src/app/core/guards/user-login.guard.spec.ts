import { PLATFORM_ID, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { isObservable, firstValueFrom } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { TokenService } from '../services/token.service';

import { userLoginGuard } from './user-login.guard';

describe('userLoginGuard', () => {
  let isAuthReadySignal: ReturnType<typeof signal<boolean>>;
  let isLoggedInSignal: ReturnType<typeof signal<boolean>>;
  let tokenServiceMock: jasmine.SpyObj<TokenService>;
  let routerMock: jasmine.SpyObj<Router>;
  let redirectTree: UrlTree;

  const executeGuard: CanActivateFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => userLoginGuard(...guardParameters));

  const setupTestBed = (platformId: string = 'browser') => {
    isAuthReadySignal = signal(false);
    isLoggedInSignal = signal(false);
    tokenServiceMock = jasmine.createSpyObj<TokenService>('TokenService', ['getToken']);
    routerMock = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);
    redirectTree = {} as UrlTree;

    tokenServiceMock.getToken.and.returnValue('');
    routerMock.createUrlTree.and.returnValue(redirectTree);

    TestBed.configureTestingModule({
      providers: [
        { provide: PLATFORM_ID, useValue: platformId },
        {
          provide: AuthService,
          useValue: {
            isAuthReady: isAuthReadySignal.asReadonly(),
            isLoggedIn: isLoggedInSignal.asReadonly(),
          },
        },
        { provide: TokenService, useValue: tokenServiceMock },
        { provide: Router, useValue: routerMock },
      ],
    });
  };

  it('waits until auth is ready and allows authenticated browser navigation', async () => {
    setupTestBed('browser');

    const result$ = executeGuard(
      {} as ActivatedRouteSnapshot,
      { url: '/infor/cv' } as RouterStateSnapshot
    );
    expect(isObservable(result$)).toBeTrue();
    if (!isObservable(result$)) {
      return;
    }

    const decisionPromise = firstValueFrom(result$);
    tokenServiceMock.getToken.and.returnValue('valid-jwt');
    isLoggedInSignal.set(true);
    isAuthReadySignal.set(true);

    expect(await decisionPromise).toBeTrue();
  });

  it('redirects to login modal query param when auth finishes unauthenticated', async () => {
    setupTestBed('browser');

    const result$ = executeGuard(
      {} as ActivatedRouteSnapshot,
      { url: '/infor/cv' } as RouterStateSnapshot
    );
    if (!isObservable(result$)) {
      fail('Expected observable guard result');
      return;
    }

    const decisionPromise = firstValueFrom(result$);
    isAuthReadySignal.set(true);

    expect(await decisionPromise).toBe(redirectTree);
    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/'], {
      queryParams: { auth: 'login' },
    });
  });

  it('passes through during SSR so server does not redirect before browser cookie refresh', () => {
    setupTestBed('server');

    const result = executeGuard(
      {} as ActivatedRouteSnapshot,
      { url: '/infor/cv' } as RouterStateSnapshot
    );

    expect(result).toBeTrue();
  });
});
