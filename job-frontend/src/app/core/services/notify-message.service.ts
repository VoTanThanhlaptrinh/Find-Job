import { isPlatformBrowser } from '@angular/common';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { ToastrService, IndividualConfig } from 'ngx-toastr';

export type NotifyStatus = 'success' | 'error' | 'info' | 'warning' | 'loading' | 'default';

@Injectable({
  providedIn: 'root'
})
export class NotifyMessageService {
  constructor(
    @Inject(PLATFORM_ID) private _platformId: Object,
    private toastr: ToastrService
  ) {}

  showMessage(
    message: string,
    titleOrStatus?: string,
    statusOrOptions?: NotifyStatus | Partial<IndividualConfig>,
    options?: Partial<IndividualConfig>
  ) {
    if (!isPlatformBrowser(this._platformId)) {
      return;
    }

    const isTitleActuallyStatus = this.isStatus(titleOrStatus);
    const title = isTitleActuallyStatus ? undefined : titleOrStatus;
    const status = (isTitleActuallyStatus ? titleOrStatus : statusOrOptions) as NotifyStatus | undefined;
    const resolvedOptions = (isTitleActuallyStatus ? statusOrOptions : options) as Partial<IndividualConfig> | undefined;

    this.showByStatus(status ?? 'default', message, title, resolvedOptions);
  }

  success(message: string, title?: string, options?: Partial<IndividualConfig>) {
    this.showByStatus('success', message, title, options);
  }

  error(message: string, title?: string, options?: Partial<IndividualConfig>) {
    this.showByStatus('error', message, title, options);
  }

  warning(message: string, title?: string, options?: Partial<IndividualConfig>) {
    this.showByStatus('warning', message, title, options);
  }

  info(message: string, title?: string, options?: Partial<IndividualConfig>) {
    this.showByStatus('info', message, title, options);
  }

  loading(message: string, title?: string, options?: Partial<IndividualConfig>) {
    this.showByStatus('loading', message, title, options);
  }

  default(message: string, title?: string, options?: Partial<IndividualConfig>) {
    this.showByStatus('default', message, title, options);
  }

  close(id?: number) {
    if (!isPlatformBrowser(this._platformId)) {
      return;
    }

    if (id !== undefined) {
      this.toastr.clear(id);
    } else {
      this.toastr.clear();
    }
  }

  private showByStatus(
    status: NotifyStatus,
    message: string,
    title?: string,
    options?: Partial<IndividualConfig>
  ) {
    if (!isPlatformBrowser(this._platformId)) {
      return;
    }

    // Pass title to ToastrService separately, as Toastr treats title as the second argument
    switch (status) {
      case 'success':
        this.toastr.success(message, title, options);
        break;
      case 'error':
        this.toastr.error(message, title, options);
        break;
      case 'warning':
        this.toastr.warning(message, title, options);
        break;
      case 'info':
      case 'loading':
      case 'default':
      default:
        this.toastr.info(message, title, options);
        break;
    }
  }

  private isStatus(value?: string): value is NotifyStatus {
    return value === 'success'
      || value === 'error'
      || value === 'info'
      || value === 'warning'
      || value === 'loading'
      || value === 'default';
  }
}
