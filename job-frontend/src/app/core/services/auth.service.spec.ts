import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { SseService } from './sse.service';
import { TokenService } from './token.service';
import { UtilitiesService } from './utilities.service';

describe('AuthService Refresh Deduplication', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let tokenService: TokenService;
  let sseServiceSpy: jasmine.SpyObj<SseService>;

  beforeEach(() => {
    sseServiceSpy = jasmine.createSpyObj<SseService>('SseService', ['connect', 'disconnect']);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        TokenService,
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SseService, useValue: sseServiceSpy },
        {
          provide: UtilitiesService,
          useValue: { getURLDev: () => 'http://localhost:8080/api' },
        },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    tokenService = TestBed.inject(TokenService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('shares one in-flight /auth/refreshToken request across concurrent startup and refreshToken$ callers', fakeAsync(() => {
    const startupResults: Array<string | null> = [];
    const interceptorResults: string[] = [];
    const sseResults: string[] = [];

    service.refreshToken().subscribe((token) => startupResults.push(token));
    service.refreshToken$().subscribe((token) => interceptorResults.push(token));
    service.refreshToken$().subscribe((token) => sseResults.push(token));

    const requests = httpMock.match('http://localhost:8080/api/auth/refreshToken');
    expect(requests.length).toBe(1);
    expect(requests[0].request.withCredentials).toBeTrue();

    requests[0].flush({ data: 'new-access-jwt' });
    tick();

    expect(startupResults).toEqual(['new-access-jwt']);
    expect(interceptorResults).toEqual(['new-access-jwt']);
    expect(sseResults).toEqual(['new-access-jwt']);
    expect(tokenService.getToken()).toBe('new-access-jwt');
    expect(service.isLoggedIn()).toBeTrue();
    expect(service.isAuthReady()).toBeTrue();

    // Clears setTimeout(3000) scheduled inside setLoggedIn(true)
    tick(3000);
  }));

  it('starts a fresh HTTP call once the previous in-flight refresh completes', fakeAsync(() => {
    service.refreshToken$().subscribe();
    const first = httpMock.expectOne('http://localhost:8080/api/auth/refreshToken');
    first.flush({ data: 'jwt-1' });
    tick();

    let secondResult = '';
    service.refreshToken$().subscribe((token) => (secondResult = token));
    const second = httpMock.expectOne('http://localhost:8080/api/auth/refreshToken');
    second.flush({ data: 'jwt-2' });
    tick();

    expect(secondResult).toBe('jwt-2');
    tick(3000);
  }));
});

