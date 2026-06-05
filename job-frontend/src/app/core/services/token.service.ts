import { Injectable, signal } from '@angular/core';

interface JwtPayload {
  sub?: unknown;
  roles?: unknown;
  role?: unknown;
  authorities?: unknown;
  authority?: unknown;
}

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
    const payload = this.parseJwtPayload(token);
    return typeof payload?.sub === 'string' ? payload.sub : null;
  }

  public getTokenRoles(token: string = this.getToken()): string[] {
    const payload = this.parseJwtPayload(token);
    if (!payload) {
      return [];
    }

    const roleCandidates = [
      ...this.toRolesArray(payload.roles),
      ...this.toRolesArray(payload.role),
      ...this.toRolesArray(payload.authorities),
      ...this.toRolesArray(payload.authority)
    ];

    return Array.from(new Set(roleCandidates.map((role) => role.toUpperCase())));
  }

  public hasAnyRole(expectedRoles: string[], token: string = this.getToken()): boolean {
    const currentRoles = this.getTokenRoles(token);
    if (currentRoles.length === 0) {
      return false;
    }

    const normalizedExpectedRoles = expectedRoles.map((role) => role.toUpperCase());
    return normalizedExpectedRoles.some((role) => currentRoles.includes(role));
  }

  private parseJwtPayload(token: string): JwtPayload | null {
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
      return JSON.parse(payload) as JwtPayload;
    } catch {
      return null;
    }
  }

  private toRolesArray(value: unknown): string[] {
    if (Array.isArray(value)) {
      return value.flatMap((item) => {
        if (typeof item === 'string') {
          return [item];
        }

        if (item && typeof item === 'object') {
          const roleValue = (item as { role?: unknown }).role;
          const authorityValue = (item as { authority?: unknown }).authority;
          const nameValue = (item as { name?: unknown }).name;

          return [roleValue, authorityValue, nameValue]
            .filter((entry): entry is string => typeof entry === 'string');
        }

        return [];
      });
    }

    if (typeof value === 'string') {
      return value
        .split(/[\s,]+/)
        .map((item) => item.trim())
        .filter((item) => item.length > 0);
    }

    return [];
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
