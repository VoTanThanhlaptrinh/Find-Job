import { HttpParams } from '@angular/common/http';

export function buildHttpParams<T extends object>(params: T): HttpParams {
  let httpParams = new HttpParams();

  for (const [key, value] of Object.entries(params as Record<string, unknown>)) {
    if (value === undefined || value === null || value === '') {
      continue;
    }
    httpParams = httpParams.set(key, String(value));
  }

  return httpParams;
}
