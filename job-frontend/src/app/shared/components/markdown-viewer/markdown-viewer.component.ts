import { Component, Input, ElementRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MarkdownComponent, MarkdownService } from 'ngx-markdown';
import ClipboardJS from 'clipboard';
import { MarkdownClipboardButtonComponent } from '../markdown-clipboard-button/markdown-clipboard-button.component';

// Đảm bảo ClipboardJS luôn có sẵn cho ngx-markdown
if (typeof window !== 'undefined') {
  (window as any).ClipboardJS = ClipboardJS;
}

@Component({
  selector: 'app-markdown-viewer',
  standalone: true,
  imports: [CommonModule, MarkdownComponent],
  templateUrl: './markdown-viewer.component.html',
  styleUrl: './markdown-viewer.component.css'
})
export class MarkdownViewerComponent {
  private markdownService = inject(MarkdownService);
  private elementRef = inject(ElementRef);

  readonly clipboardButtonComponent = MarkdownClipboardButtonComponent;

  @Input() data = '';
  @Input() proseClass = 'prose prose-slate max-w-none';
  @Input() emptyText = 'Chưa có nội dung để hiển thị.';

  /**
   * Đánh dấu nhãn ngôn ngữ lập trình trên từng khối code sau khi render
   */
  onMarkdownReady(): void {
    if (typeof window === 'undefined') return;
    const host = this.elementRef.nativeElement as HTMLElement;
    const preElements = host.querySelectorAll('pre');
    preElements.forEach((pre: HTMLElement) => {
      const code = pre.querySelector('code');
      const classList = code ? Array.from(code.classList) : Array.from(pre.classList);
      const langClass = classList.find(c => c.startsWith('language-'));
      if (langClass) {
        const lang = langClass.replace('language-', '').toUpperCase();
        pre.setAttribute('data-language', lang);
      } else {
        pre.setAttribute('data-language', 'CODE');
      }
    });
  }

  /**
   * Hàm công khai dùng để trích xuất nội dung HTML đã qua xử lý Markdown & XSS
   */
  public renderToHtml(markdownContent?: string): string | Promise<string> {
    const raw = markdownContent !== undefined ? markdownContent : this.data;
    return this.markdownService.parse(raw || '', { decodeHtml: true });
  }

  /**
   * Lấy số từ của nội dung Markdown
   */
  public getWordCount(): number {
    if (!this.data) return 0;
    return this.data.trim().split(/\s+/).filter(w => w.length > 0).length;
  }
}

