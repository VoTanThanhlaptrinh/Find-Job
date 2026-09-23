import {
  Component,
  ElementRef,
  forwardRef,
  Input,
  ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'app-markdown-editor',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './markdown-editor.component.html',
  styleUrl: './markdown-editor.component.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MarkdownEditorComponent),
      multi: true
    }
  ]
})
export class MarkdownEditorComponent implements ControlValueAccessor {
  @ViewChild('editorTextarea') textareaRef?: ElementRef<HTMLTextAreaElement>;

  @Input() placeholder = 'Nhập nội dung định dạng Markdown tại đây...';
  @Input() maxLength?: number;
  @Input() showToolbar = true;
  @Input() showFooter = true;

  value = '';
  disabled = false;
  isFullscreen = false;

  private onChange: (val: string) => void = () => {};
  private onTouched: () => void = () => {};

  // ControlValueAccessor implementations
  writeValue(value: any): void {
    let normalizedValue = value ?? '';
    if (this.maxLength && normalizedValue.length > this.maxLength) {
      normalizedValue = normalizedValue.substring(0, this.maxLength);
    }
    this.value = normalizedValue;
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onContentChange(val: string): void {
    if (this.maxLength && val && val.length > this.maxLength) {
      val = val.substring(0, this.maxLength);
    }
    this.value = val;
    this.onChange(this.value);
    this.onTouched();
  }

  onBlur(): void {
    this.onTouched();
  }

  toggleFullscreen(): void {
    this.isFullscreen = !this.isFullscreen;
  }

  // Public utility methods to retrieve or control content outside
  public getContent(): string {
    return this.value;
  }

  public getWordCount(): number {
    return this.wordCount;
  }

  public getCharCount(): number {
    return this.charCount;
  }

  public focus(): void {
    this.focusTextarea();
  }

  public clear(): void {
    this.value = '';
    this.onContentChange('');
  }

  // Keyboard Shortcuts Handler
  onKeyDown(event: KeyboardEvent): void {
    if (this.disabled) return;

    // Handle Tab key (indent 2 spaces instead of changing focus)
    if (event.key === 'Tab') {
      event.preventDefault();
      this.insertTextAtCursor('  ');
      return;
    }

    // Handle Ctrl/Cmd combinations
    if (event.ctrlKey || event.metaKey) {
      const key = event.key.toLowerCase();
      if (key === 'b') {
        event.preventDefault();
        this.insertFormat('**', '**', 'văn bản in đậm');
      } else if (key === 'i') {
        event.preventDefault();
        this.insertFormat('*', '*', 'văn bản in nghiêng');
      } else if (key === 'k') {
        event.preventDefault();
        this.insertLink();
      }
    }
  }

  // Formatting actions
  insertFormat(prefix: string, suffix = '', defaultText = ''): void {
    const textarea = this.getTextarea();
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;
    const selectedText = text.substring(start, end) || defaultText;

    let replacement = `${prefix}${selectedText}${suffix}`;
    if (this.maxLength) {
      const netLengthAfter = text.length - (end - start) + replacement.length;
      if (netLengthAfter > this.maxLength) {
        const allowedLength = Math.max(0, this.maxLength - (text.length - (end - start)));
        replacement = replacement.substring(0, allowedLength);
      }
    }
    if (!replacement) return;

    this.value = text.substring(0, start) + replacement + text.substring(end);
    this.onContentChange(this.value);

    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(
        start + prefix.length,
        start + prefix.length + selectedText.length
      );
    }, 0);
  }

  insertLinePrefix(prefix: string): void {
    const textarea = this.getTextarea();
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;

    // Find the start of the current line
    const lineStart = text.lastIndexOf('\n', start - 1) + 1;
    const lineEnd = text.indexOf('\n', end);
    const actualLineEnd = lineEnd === -1 ? text.length : lineEnd;

    const selectedLines = text.substring(lineStart, actualLineEnd);
    let updatedLines = selectedLines
      .split('\n')
      .map(line => `${prefix}${line}`)
      .join('\n');

    if (this.maxLength) {
      const netLengthAfter = text.length - (actualLineEnd - lineStart) + updatedLines.length;
      if (netLengthAfter > this.maxLength) {
        const allowedLength = Math.max(0, this.maxLength - (text.length - (actualLineEnd - lineStart)));
        updatedLines = updatedLines.substring(0, allowedLength);
      }
    }

    this.value = text.substring(0, lineStart) + updatedLines + text.substring(actualLineEnd);
    this.onContentChange(this.value);

    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(lineStart, lineStart + updatedLines.length);
    }, 0);
  }

  insertHeading(level: 1 | 2 | 3): void {
    const prefix = '#'.repeat(level) + ' ';
    this.insertLinePrefix(prefix);
  }

  insertCodeBlock(): void {
    this.insertFormat('```typescript\n', '\n```', '// Nhập mã nguồn của bạn tại đây');
  }

  insertLink(): void {
    this.insertFormat('[', '](https://example.com)', 'Tên liên kết');
  }

  insertImage(): void {
    this.insertFormat('![', '](https://images.unsplash.com/photo-1486312338219-ce68d2c6f44d?w=800)', 'Mô tả hình ảnh');
  }

  insertTable(): void {
    const tableTemplate = `\n| Tiêu đề 1 | Tiêu đề 2 | Tiêu đề 3 |\n| :--- | :---: | ---: |\n| Dữ liệu 1 | Dữ liệu 2 | Dữ liệu 3 |\n| Dữ liệu 4 | Dữ liệu 5 | Dữ liệu 6 |\n\n`;
    this.insertTextAtCursor(tableTemplate);
  }

  insertHorizontalRule(): void {
    this.insertTextAtCursor('\n\n---\n\n');
  }

  // Helper methods
  private insertTextAtCursor(insertText: string): void {
    const textarea = this.getTextarea();
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;

    if (this.maxLength) {
      const netLengthAfter = text.length - (end - start) + insertText.length;
      if (netLengthAfter > this.maxLength) {
        const allowedLength = Math.max(0, this.maxLength - (text.length - (end - start)));
        insertText = insertText.substring(0, allowedLength);
      }
    }
    if (!insertText) return;

    this.value = text.substring(0, start) + insertText + text.substring(end);
    this.onContentChange(this.value);

    setTimeout(() => {
      textarea.focus();
      const newPos = start + insertText.length;
      textarea.setSelectionRange(newPos, newPos);
    }, 0);
  }

  private getTextarea(): HTMLTextAreaElement | null {
    return this.textareaRef?.nativeElement || null;
  }

  private focusTextarea(): void {
    this.getTextarea()?.focus();
  }

  // Statistics getters
  get wordCount(): number {
    if (!this.value) return 0;
    return this.value.trim().split(/\s+/).filter(w => w.length > 0).length;
  }

  get charCount(): number {
    return this.value ? this.value.length : 0;
  }

  get isAtLimit(): boolean {
    return !!(this.maxLength && this.charCount >= this.maxLength);
  }

  get isNearLimit(): boolean {
    return !!(this.maxLength && this.charCount >= this.maxLength * 0.9);
  }

  get readingTimeMinutes(): number {
    const words = this.wordCount;
    return Math.max(1, Math.ceil(words / 200));
  }
}
