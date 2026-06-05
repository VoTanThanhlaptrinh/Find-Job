import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { take } from 'rxjs';
import { ApiResponse } from '../../../shared/models/api-response.model';
import { UtilitiesService } from '../../../core/services/utilities.service';

export interface RecruiterLoginPayload {
  username: string;
  password: string;
}

export interface RecruiterRegisterPayload {
  fullName: string;
  companyName: string;
  email: string;
  phone: string;
  password: string;
}

@Injectable({
  providedIn: 'root'
})
export class RecruiterAuthService {
  private readonly url: string;
  private readonly isSubmittingAuth = signal<boolean>(false);
  private readonly authError = signal<string | null>(null);
  private readonly loginToken = signal<string | null>(null);
  private readonly registerSuccess = signal<boolean>(false);
  private readonly isLoadingHirerRoles = signal<boolean>(false);
  private readonly hirerRoles = signal<string[]>([]);

  readonly isSubmittingAuth$ = computed(() => this.isSubmittingAuth());
  readonly authError$ = computed(() => this.authError());
  readonly loginToken$ = computed(() => this.loginToken());
  readonly registerSuccess$ = computed(() => this.registerSuccess());
  readonly isLoadingHirerRoles$ = computed(() => this.isLoadingHirerRoles());
  readonly hirerRoles$ = computed(() => this.hirerRoles());

  constructor(
    private readonly http: HttpClient,
    private readonly utilities: UtilitiesService
  ) {
    this.url = this.utilities.getURLDev();
  }

  login(payload: RecruiterLoginPayload): void {
    this.isSubmittingAuth.set(true);
    this.authError.set(null);
    this.loginToken.set(null);

    this.http.post<ApiResponse<string>>(
      `${this.url}/auth/hirer/login`,
      payload,
      { withCredentials: true }
    ).pipe(take(1)).subscribe({
      next: (response) => {
        this.loginToken.set(response?.data ?? null);
      },
      error: (error: { error?: { message?: string } }) => {
        this.authError.set(error?.error?.message ?? 'Đăng nhập thất bại. Vui lòng thử lại.');
      },
      complete: () => {
        this.isSubmittingAuth.set(false);
      }
    });
  }

  register(payload: RecruiterRegisterPayload): void {
    this.isSubmittingAuth.set(true);
    this.authError.set(null);
    this.registerSuccess.set(false);

    this.http.post<ApiResponse<unknown>>(
      `${this.url}/auth/hirer/register`,
      payload,
      { withCredentials: true }
    ).pipe(take(1)).subscribe({
      next: () => {
        this.registerSuccess.set(true);
      },
      error: (error: { error?: { message?: string } }) => {
        this.authError.set(error?.error?.message ?? 'Đăng ký thất bại. Vui lòng thử lại.');
      },
      complete: () => {
        this.isSubmittingAuth.set(false);
      }
    });
  }

  loadHirerRoles(): void {
    this.isLoadingHirerRoles.set(true);

    this.http.get<ApiResponse<string[]>>(
      `${this.url}/account/roles/hirer`,
      { withCredentials: true }
    ).pipe(take(1)).subscribe({
      next: (response) => {
        this.hirerRoles.set(response?.data ?? []);
      },
      error: () => {
        this.hirerRoles.set([]);
      },
      complete: () => {
        this.isLoadingHirerRoles.set(false);
      }
    });
  }

  resetRegisterState(): void {
    this.registerSuccess.set(false);
  }

  clearAuthError(): void {
    this.authError.set(null);
  }
}