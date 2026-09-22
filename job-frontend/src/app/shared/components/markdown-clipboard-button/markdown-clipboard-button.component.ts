import { Component, ElementRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-markdown-clipboard-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      type="button"
      class="markdown-clipboard-button inline-flex items-center gap-1.5 px-2.5 py-1 text-[11px] font-semibold tracking-wide rounded-lg transition-all duration-150 cursor-pointer select-none"
      [class.copied]="copied"
      (click)="onCopy($event)"
      [title]="copied ? 'Đã sao chép vào bộ nhớ tạm' : 'Sao chép đoạn mã này'"
      aria-label="Sao chép mã nguồn"
    >
      @if (copied) {
        <span class="material-symbols-outlined text-[14px] text-emerald-400">check</span>
        <span class="text-emerald-300">Đã chép!</span>
      } @else {
        <span class="material-symbols-outlined text-[14px] text-slate-300">content_copy</span>
        <span class="text-slate-200">Sao chép</span>
      }
    </button>
  `,
  styles: [`
    :host {
      display: inline-block;
    }
  `]
})
export class MarkdownClipboardButtonComponent {
  private elementRef = inject(ElementRef);
  copied = false;
  private timeoutId: any;

  onCopy(event?: MouseEvent): void {
    this.copied = true;

    // Cơ chế fallback: Tự động trích xuất nội dung từ thẻ <pre> liền kề nếu ClipboardJS chưa bắt sự kiện
    try {
      if (typeof navigator !== 'undefined' && navigator.clipboard) {
        const hostEl = this.elementRef.nativeElement as HTMLElement;
        const toolbar = hostEl.closest('.markdown-clipboard-toolbar');
        const preWrapper = toolbar?.parentElement;
        const pre = preWrapper?.querySelector('pre');
        if (pre) {
          const textToCopy = pre.innerText || pre.textContent || '';
          if (textToCopy) {
            navigator.clipboard.writeText(textToCopy).catch(() => {});
          }
        }
      }
    } catch {
      // Bỏ qua nếu ClipboardJS đã đảm nhiệm việc sao chép
    }

    clearTimeout(this.timeoutId);
    this.timeoutId = setTimeout(() => {
      this.copied = false;
    }, 2000);
  }
}
