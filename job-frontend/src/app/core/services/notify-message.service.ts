import { isPlatformBrowser } from '@angular/common';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { HotToastService, ToastOptions } from '@ngxpert/hot-toast';

type NotifyStatus = 'success' | 'error' | 'info' | 'warning' | 'loading' | 'default';

@Injectable({
  providedIn: 'root'
})
export class NotifyMessageService {
  constructor(
    @Inject(PLATFORM_ID) private _platformId: Object,
    private hotToast: HotToastService
  ) {}

  showMessage(
    message: string,
    titleOrStatus?: string,
    statusOrOptions?: NotifyStatus | ToastOptions<unknown>,
    options?: ToastOptions<unknown>
  ) {
    if (!isPlatformBrowser(this._platformId)) {
      return;
    }

    const isTitleActuallyStatus = this.isStatus(titleOrStatus);
    const title = isTitleActuallyStatus ? undefined : titleOrStatus;
    const status = (isTitleActuallyStatus ? titleOrStatus : statusOrOptions) as NotifyStatus | undefined;
    const resolvedOptions = (isTitleActuallyStatus ? statusOrOptions : options) as ToastOptions<unknown> | undefined;

    const content = this.buildContent(message, title);
    this.showByStatus(status ?? 'default', content, resolvedOptions);
  }

  success(message: string, title?: string, options?: ToastOptions<unknown>) {
    this.showByStatus('success', this.buildContent(message, title), options);
  }

  error(message: string, title?: string, options?: ToastOptions<unknown>) {
    this.showByStatus('error', this.buildContent(message, title), options);
  }

  warning(message: string, title?: string, options?: ToastOptions<unknown>) {
    this.showByStatus('warning', this.buildContent(message, title), options);
  }

  info(message: string, title?: string, options?: ToastOptions<unknown>) {
    this.showByStatus('info', this.buildContent(message, title), options);
  }

  loading(message: string, title?: string, options?: ToastOptions<unknown>) {
    this.showByStatus('loading', this.buildContent(message, title), options);
  }

  default(message: string, title?: string, options?: ToastOptions<unknown>) {
    this.showByStatus('default', this.buildContent(message, title), options);
  }

  close(id?: string) {
    if (!isPlatformBrowser(this._platformId)) {
      return;
    }

    this.hotToast.close(id);
  }

  private showByStatus(
    status: NotifyStatus,
    content: string,
    options?: ToastOptions<unknown>
  ) {
    if (!isPlatformBrowser(this._platformId)) {
      return;
    }

    const mergedOptions = this.withDefaultOptions(options);

    switch (status) {
      case 'success':
        this.hotToast.success(content, mergedOptions);
        break;
      case 'error':
        this.hotToast.error(content, mergedOptions);
        break;
      case 'warning':
        this.hotToast.warning(content, mergedOptions);
        break;
      case 'info':
        this.hotToast.info(content, mergedOptions);
        break;
      case 'loading':
        this.hotToast.loading(content, mergedOptions);
        break;
      default:
        this.hotToast.show(content, mergedOptions);
        break;
    }
  }

  private withDefaultOptions(options?: ToastOptions<unknown>): ToastOptions<unknown> {
    return {
      position: 'top-right',
      ...options
    };
  }

  private buildContent(message: string, title?: string): string {
    if (!title) {
      return message;
    }

    return `${title}: ${message}`;
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
