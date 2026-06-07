import { ApplicationRef, inject, Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SseService {
  private appRef = inject(ApplicationRef);
  private eventSource?: EventSource;

  // ===== SIGNAL STATE =====
  readonly latestEvents = signal<Record<string, any>>({});
  readonly status = signal<'idle' | 'connected' | 'error' | 'disconnected'>('idle');

  // ===== INTERNAL BUFFER =====
  private buffer: any[] = [];
  private flushTimer?: number;

  // ===== TICK CONTROL (CRITICAL) =====
  private tickScheduled = false;

  connect(url: string, eventNames: string[] = ['message'], options?: { withCredentials?: boolean }) {
    this.disconnect();

    this.eventSource = new EventSource(url, {
      withCredentials: options?.withCredentials ?? true,
    });

    this.eventSource.onopen = () => {
      this.status.set('connected');
      this.scheduleTick();
    };

    eventNames.forEach(eventName => {
      this.eventSource?.addEventListener(eventName, (event: MessageEvent) => {
        const parsedData = this.safeParse(event.data);

        this.buffer.push({
          eventName: eventName,
          status: parsedData?.status || 'success', 
          message: parsedData?.message || '',
          data: parsedData,
        });
      });
    });

    this.eventSource.onerror = () => {
      this.status.set('error');
      this.scheduleTick();
    };

    this.startFlushLoop();
  }

  // ===== FLUSH BUFFER (BATCHED) =====
  private startFlushLoop() {
    this.flushTimer = window.setInterval(() => {
      if (!this.buffer.length) return;

      const batch = [...this.buffer];
      this.buffer.length = 0;

      this.latestEvents.update((currentObj) => {
        const newObj = { ...currentObj };
        for (const item of batch) {
          // Ghi đè dữ liệu mới nhất vào đúng Key là tên của event
          newObj[item.eventName] = item;
        }
        return newObj;
      });

      this.scheduleTick(); // ✅ SAFE
    }, 100);
  }

  // ===== SAFE MANUAL CHANGE DETECTION =====
  private scheduleTick() {
    if (this.tickScheduled) return;

    this.tickScheduled = true;

    queueMicrotask(() => {
      this.tickScheduled = false;
      this.appRef.tick(); // ✅ NEVER recursive
    });
  }

  // ===== DISCONNECT =====
  disconnect() {
    this.eventSource?.close();
    this.eventSource = undefined;

    if (this.flushTimer) {
      clearInterval(this.flushTimer);
      this.flushTimer = undefined;
    }

    this.status.set('disconnected');
    this.scheduleTick();
  }

  ngOnDestroy() {
    this.disconnect();
  }

  private safeParse(data: string) {
    try {
      return JSON.parse(data);
    } catch {
      return data;
    }
  }
}