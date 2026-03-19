import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private token = signal<string>('');
  constructor() { }

  public setToken(token: string): void {
    this.token.set(token);
  }

  public getToken(): string {
    return this.token();
  }
  public clearToken(): void {
    this.token.set('');
  }

  public getTokenSubject(token: string = this.getToken()): string | null {
    if (!token) {
      return null;
    }

    const parts = token.split('.');
    if (parts.length < 2) {
      return null;
    }

    const payload = this.decodeBase64Url(parts[1]);
    if (!payload) {
      return null;
    }

    try {
      const parsedPayload = JSON.parse(payload) as { sub?: unknown };
      return typeof parsedPayload.sub === 'string' ? parsedPayload.sub : null;
    } catch {
      return null;
    }
  }

  private decodeBase64Url(value: string): string | null {
    try {
      const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');

      if (typeof atob !== 'function') {
        return null;
      }

      const binary = atob(padded);
      const percentEncoded = Array.from(binary)
        .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join('');

      return decodeURIComponent(percentEncoded);
    } catch {
      return null;
    }
  }
}
