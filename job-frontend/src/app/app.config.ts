import {ApplicationConfig, importProvidersFrom, provideZoneChangeDetection} from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { loggerInterceptor } from './core/interceptors/logger.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { refreshTokenInterceptor } from './core/interceptors/refresh-token.interceptor';
import {provideNativeDateAdapter} from '@angular/material/core';
import {JwtModule} from '@auth0/angular-jwt';
import { provideHotToastConfig } from '@ngxpert/hot-toast';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideClientHydration(),
    provideHttpClient(
      withFetch(),
      withInterceptors([loggerInterceptor, errorInterceptor, refreshTokenInterceptor])
    ),
    importProvidersFrom(JwtModule.forRoot({})),
    provideNativeDateAdapter(),
    provideHotToastConfig(),
    provideAnimations(),
  ]
};
