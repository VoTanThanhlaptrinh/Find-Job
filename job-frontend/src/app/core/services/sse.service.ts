import { DestroyRef, inject, Injectable, Injector, signal, Signal, WritableSignal } from '@angular/core';
import { fetchEventSource, EventSourceMessage } from '@microsoft/fetch-event-source';
import { TokenService } from './token.service';
import { UtilitiesService } from './utilities.service';
import { AuthService } from './auth.service';
import { take } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';

// ===== TYPES =====
export type SseStatus = 'idle' | 'connecting' | 'connected' | 'reconnecting' | 'error' | 'disconnected';

// ===== CONSTANTS =====
const MAX_RETRIES = 5;
const INITIAL_RETRY_DELAY_MS = 1_000;
const MAX_RETRY_DELAY_MS = 30_000;

/**
 * Dịch vụ SSE tổng quát cho toàn ứng dụng.
 *
 * - Mỗi user chỉ giữ **1 kết nối SSE duy nhất**.
 * - Server push nhiều event types trên kết nối đó.
 * - Component dùng `fromEvent<T>('event-name')` để nhận Signal chứa data mới nhất.
 *
 * @example
 * ```ts
 * // Trong component:
 * private sseService = inject(SseService);
 * readonly resumeEvent = this.sseService.fromEvent<SseMessagePayload>('resume-process');
 *
 * // Trong template (Angular 17+ signal):
 * @if (resumeEvent()) {
 *   <span>{{ resumeEvent()!.status }}</span>
 * }
 * ```
 */
@Injectable({
  providedIn: 'root',
})
export class SseService {
  private destroyRef = inject(DestroyRef);
  private tokenService = inject(TokenService);
  private utilities = inject(UtilitiesService);
  private injector = inject(Injector);
  private platformId = inject(PLATFORM_ID);

  // ===== PUBLIC STATE =====
  readonly status = signal<SseStatus>('idle');

  // ===== PRIVATE =====
  private abortController?: AbortController;
  private retryCount = 0;

  /**
   * Kho lưu trữ động: Map<eventName, WritableSignal<T>>
   * Component đăng ký trước hoặc server gửi event trước đều hoạt động.
   */
  private readonly eventStore = new Map<string, WritableSignal<any>>();

  constructor() {
    this.destroyRef.onDestroy(() => this.disconnect());
  }

  // ================================================================
  //  PUBLIC API — Dùng bởi Components
  // ================================================================

  fromEvent<T>(eventName: string): Signal<T | null> {
    if (!this.eventStore.has(eventName)) {
      this.eventStore.set(eventName, signal<T | null>(null));
    }
    return this.eventStore.get(eventName)!.asReadonly();
  }

  /**
   * Reset data của một event về null.
   * Dùng khi component cần "dọn dẹp" state cũ.
   */
  clearEvent(eventName: string): void {
    this.eventStore.get(eventName)?.set(null);
  }

  // ================================================================
  //  CONNECTION MANAGEMENT
  // ================================================================

  /**
   * Mở kết nối SSE đến server.
   * Token được inject tự động từ TokenService.
   * Nếu đã có kết nối cũ, sẽ đóng trước khi mở mới.
   */
  connect(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    
    this.disconnect();

    const token = this.tokenService.getToken();
    if (!token) {
      console.warn('[SSE] No token available, skipping connection');
      this.status.set('disconnected');
      return;
    }

    this.retryCount = 0;
    this._doConnect(token);
  }

  /**
   * Ngắt kết nối an toàn.
   */
  disconnect(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    
    if (this.abortController) {
      this.abortController.abort();
      this.abortController = undefined;
    }
    this.retryCount = 0;
    this.status.set('disconnected');
  }

  // ================================================================
  //  INTERNAL — Kết nối & Reconnect
  // ================================================================

  private _doConnect(token: string): void {
    this.abortController = new AbortController();
    const url = `${this.utilities.getURLDev()}/sse/connect`;

    this.status.set(this.retryCount === 0 ? 'connecting' : 'reconnecting');

    fetchEventSource(url, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'text/event-stream',
        'Cache-Control': 'no-cache',
      },
      signal: this.abortController.signal,
      openWhenHidden: true,

      onopen: async (response: Response) => {
        if (response.status === 401) {
          console.warn('[SSE] Unauthorized (401), attempting token refresh');
          this._refreshAndReconnect();
          throw new Error('AUTH_401_REFRESHING');
        }
        if (response.status === 403) {
          throw new Error(`AUTH_ERROR:${response.status}`);
        }
        if (!response.ok) {
          throw new Error(`HTTP_ERROR:${response.status}`);
        }
        // Kết nối thành công → reset retry counter
        this.retryCount = 0;
        this.status.set('connected');
        console.log('[SSE] Connected');
      },

      onmessage: (msg: EventSourceMessage) => {
        const eventName = msg.event || 'message';

        // Bỏ qua heartbeat (SSE comments) và messages rỗng
        if (!msg.data) return;
        if (msg.event === 'connected') {
          console.log('[SSE] Server acknowledged connection', msg.data);
        }
        const parsed = this._safeParse(msg.data);
        this._publishEvent(eventName, parsed);
      },

      onerror: (error: any) => {
        if (error instanceof Error && error.message === 'AUTH_401_REFRESHING') {
          // Dừng connection loop hiện tại vì _refreshAndReconnect đang tạo connection mới
          throw error;
        }

        // Lỗi Auth → dừng hẳn, không retry
        if (error instanceof Error && error.message.startsWith('AUTH_ERROR')) {
          console.error('[SSE] Auth error, stopping reconnection');
          this.status.set('error');
          throw error; // Ném ra để fetchEventSource dừng hẳn
        }

        // Lỗi khác → kiểm tra retry budget
        this.retryCount++;

        if (this.retryCount > MAX_RETRIES) {
          console.error(`[SSE] Max retries (${MAX_RETRIES}) exceeded, stopping`);
          this.status.set('error');
          throw new Error('MAX_RETRIES_EXCEEDED');
        }

        const delay = this._calcBackoffDelay();
        console.warn(`[SSE] Connection lost, retry ${this.retryCount}/${MAX_RETRIES} in ${delay}ms`);
        this.status.set('reconnecting');

        // Trả về delay (ms) để fetchEventSource tự retry sau khoảng thời gian này
        return delay;
      },

      onclose: () => {
        console.log('[SSE] Connection closed by server');
        this.status.set('disconnected');

        // Server closed the connection — retry with backoff unless we deliberately disconnected
        if (this.abortController && !this.abortController.signal.aborted) {
          this.retryCount++;
          if (this.retryCount <= MAX_RETRIES) {
            const delay = this._calcBackoffDelay();
            console.warn(`[SSE] Reconnecting after server close, attempt ${this.retryCount}/${MAX_RETRIES} in ${delay}ms`);
            this.status.set('reconnecting');
            setTimeout(() => {
              const freshToken = this.tokenService.getToken();
              if (freshToken) {
                this._doConnect(freshToken);
              }
            }, delay);
          } else {
            console.error('[SSE] Max retries exceeded after server close, giving up');
            this.status.set('error');
          }
        }
      },
    }).catch((err: Error) => {
      if (err.name === 'AbortError') {
        // Người dùng chủ động ngắt → bình thường
        console.log('[SSE] Connection aborted by client');
      } else {
        console.error('[SSE] Fatal error:', err.message);
        this.status.set('error');
      }
    });
  }

  // ================================================================
  //  HELPERS
  // ================================================================

  /**
   * Phân phối data vào đúng Signal trong eventStore.
   * Tự tạo Signal mới nếu event chưa ai đăng ký.
   */
  private _publishEvent(eventName: string, data: any): void {
    if (this.eventStore.has(eventName)) {
      this.eventStore.get(eventName)!.set(data);
    } else {
      this.eventStore.set(eventName, signal(data));
    }
  }

  /**
   * Exponential backoff: 1s → 2s → 4s → 8s → 16s → 30s (max)
   */
  private _calcBackoffDelay(): number {
    const delay = INITIAL_RETRY_DELAY_MS * Math.pow(2, this.retryCount - 1);
    return Math.min(delay, MAX_RETRY_DELAY_MS);
  }

  private _safeParse(data: string): any {
    try {
      return JSON.parse(data);
    } catch {
      return data;
    }
  }

  private _refreshAndReconnect(): void {
    const authService = this.injector.get(AuthService);
    authService.refreshToken$().pipe(take(1)).subscribe({
      next: (res) => {
        let nextToken: string | null = null;
        if (typeof res === 'string') {
          nextToken = res;
        } else if (res && typeof res === 'object' && 'data' in res && typeof res.data === 'string') {
          nextToken = res.data;
        }

        if (nextToken) {
          console.log('[SSE] Token refreshed successfully, reconnecting...');
          this.tokenService.setToken(nextToken);
          authService.setLoggedIn(true);
          this._doConnect(nextToken);
        } else {
          console.error('[SSE] Failed to extract token from refresh response');
          this.tokenService.clearToken();
          authService.setLoggedIn(false);
          this.status.set('error');
        }
      },
      error: (err) => {
        console.error('[SSE] Refresh token request failed', err);
        this.tokenService.clearToken();
        authService.setLoggedIn(false);
        this.status.set('error');
      }
    });
  }
}