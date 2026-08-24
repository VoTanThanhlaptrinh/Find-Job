import { isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID, signal } from '@angular/core';

const SAVED_JOBS_STORAGE_KEY = 'job_listing_saved_jobs';

@Injectable({
  providedIn: 'root',
})
export class SavedJobsService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  private readonly savedJobsSignal = signal<Set<string | number>>(new Set());
  readonly savedJobIds = this.savedJobsSignal.asReadonly();

  constructor() {
    this.loadSavedJobs();
  }

  private loadSavedJobs(): void {
    if (!this.isBrowser) {
      return;
    }

    try {
      const stored = localStorage.getItem(SAVED_JOBS_STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (Array.isArray(parsed)) {
          this.savedJobsSignal.set(new Set(parsed));
        }
      }
    } catch {
      // Ignore local storage parse error
    }
  }

  private persistSavedJobs(ids: Set<string | number>): void {
    if (!this.isBrowser) {
      return;
    }

    try {
      localStorage.setItem(SAVED_JOBS_STORAGE_KEY, JSON.stringify(Array.from(ids)));
    } catch {
      // Ignore local storage write error
    }
  }

  isSaved(jobId: string | number): boolean {
    const set = this.savedJobsSignal();
    return set.has(jobId) || set.has(Number(jobId)) || set.has(String(jobId));
  }

  toggleSave(jobId: string | number): boolean {
    const current = new Set(this.savedJobsSignal());
    const numericId = typeof jobId === 'string' && !isNaN(Number(jobId)) ? Number(jobId) : jobId;
    const isCurrentlySaved = current.has(jobId) || current.has(numericId) || current.has(String(jobId));

    if (isCurrentlySaved) {
      current.delete(jobId);
      current.delete(numericId);
      current.delete(String(jobId));
    } else {
      current.add(numericId);
    }

    this.savedJobsSignal.set(current);
    this.persistSavedJobs(current);
    return !isCurrentlySaved;
  }

  count(): number {
    return this.savedJobsSignal().size;
  }
}
