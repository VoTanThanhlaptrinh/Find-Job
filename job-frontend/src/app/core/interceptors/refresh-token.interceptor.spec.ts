import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { TokenService } from '../services/token.service';
import { loggerInterceptor } from './logger.interceptor';
import { refreshTokenInterceptor } from './refresh-token.interceptor';

describe('refreshTokenInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let tokenService: TokenService;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [
      'refreshToken$',
      'logout',
      'getLoginUrl',
      'setLoggedIn',
    ]);
    authServiceSpy.getLoginUrl.and.returnValue('/login');

    TestBed.configureTestingModule({
      providers: [
        TokenService,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: PLATFORM_ID, useValue: 'browser' },
        provideHttpClient(withInterceptors([loggerInterceptor, refreshTokenInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    tokenService = TestBed.inject(TokenService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('retries directly without calling refreshToken$ when TokenService already received a newer token', () => {
    let responseBody: unknown;
    http.get('/api/user/resumes').subscribe((res) => (responseBody = res));

    const firstReq = httpMock.expectOne('/api/user/resumes');
    expect(firstReq.request.headers.has('Authorization')).toBeFalse();

    // Simulate startup refresh completing just before the 401 arrives
    tokenService.setToken('fresh-startup-jwt');
    firstReq.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.refreshToken$).not.toHaveBeenCalled();

    const retryReq = httpMock.expectOne('/api/user/resumes');
    expect(retryReq.request.headers.get('Authorization')).toBe('Bearer fresh-startup-jwt');
    retryReq.flush({ data: [] });

    expect(responseBody).toEqual({ data: [] });
  });

  it('does not call logout when refreshToken$ fails with a transient 503 error', () => {
    tokenService.setToken('expired-jwt');
    authServiceSpy.refreshToken$.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 503, statusText: 'Service Unavailable' }))
    );

    let receivedStatus: number | undefined;
    http.get('/api/user/resumes').subscribe({
      error: (err: HttpErrorResponse) => (receivedStatus = err.status),
    });

    httpMock.expectOne('/api/user/resumes').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(receivedStatus).toBe(503);
    expect(authServiceSpy.logout).not.toHaveBeenCalled();
  });

  it('calls logout when refreshToken$ rejects with 404 refresh token missing', () => {
    tokenService.setToken('expired-jwt');
    authServiceSpy.refreshToken$.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 404, statusText: 'Not Found' }))
    );

    let receivedStatus: number | undefined;
    http.get('/api/user/resumes').subscribe({
      error: (err: HttpErrorResponse) => (receivedStatus = err.status),
    });

    httpMock.expectOne('/api/user/resumes').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(receivedStatus).toBe(404);
    expect(authServiceSpy.logout).toHaveBeenCalledWith('/login');
  });

  it('refreshes once via refreshToken$ and replays the request with the new token', () => {
    tokenService.setToken('expired-jwt');
    authServiceSpy.refreshToken$.and.returnValue(of('rotated-jwt'));

    let responseBody: unknown;
    http.get('/api/user/resumes').subscribe((res) => (responseBody = res));

    httpMock.expectOne('/api/user/resumes').flush(null, { status: 401, statusText: 'Unauthorized' });

    const retryReq = httpMock.expectOne('/api/user/resumes');
    expect(retryReq.request.headers.get('Authorization')).toBe('Bearer rotated-jwt');
    retryReq.flush({ ok: true });

    expect(responseBody).toEqual({ ok: true });
    expect(tokenService.getToken()).toBe('rotated-jwt');
  });
});

