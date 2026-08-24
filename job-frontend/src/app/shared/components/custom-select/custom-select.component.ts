import { CommonModule, isPlatformBrowser } from '@angular/common';
import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  inject,
  Input,
  OnChanges,
  Output,
  PLATFORM_ID,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';

export interface SelectOption<T = any> {
  value: T;
  label: string;
  count?: number;
  icon?: string;
  disabled?: boolean;
}

@Component({
  selector: 'app-custom-select',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './custom-select.component.html',
  styleUrl: './custom-select.component.css',
})
export class CustomSelectComponent implements OnChanges {
  @Input() options: SelectOption[] = [];
  @Input() value: any = '';
  @Input() placeholder = 'Chọn...';
  @Input() icon = '';
  @Input() searchable = false;
  @Input() searchPlaceholder = 'Tìm kiếm...';
  @Input() variant: 'search-bar' | 'pill' | 'form' = 'search-bar';
  @Input() prefixLabel = '';
  @Input() disabled = false;
  @Input() id = `custom-select-${Math.random().toString(36).substring(2, 9)}`;
  @Input() ariaLabel = '';
  @Input() panelClass = '';

  @Output() valueChange = new EventEmitter<any>();

  @ViewChild('triggerButton') triggerButton?: ElementRef<HTMLButtonElement>;
  @ViewChild('searchInput') searchInputElement?: ElementRef<HTMLInputElement>;
  @ViewChild('optionsList') optionsListElement?: ElementRef<HTMLUListElement>;

  private readonly elementRef = inject(ElementRef);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  isOpen = false;
  searchTerm = '';
  highlightedIndex = -1;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['options'] || changes['value']) {
      this.updateHighlightedIndex();
    }
  }

  get selectedOption(): SelectOption | undefined {
    return this.options.find((opt) => opt.value === this.value);
  }

  get displayLabel(): string {
    const selected = this.selectedOption;
    if (selected) {
      return selected.label;
    }
    return this.placeholder;
  }

  get isPlaceholder(): boolean {
    return !this.selectedOption && this.value === '';
  }

  get filteredOptions(): SelectOption[] {
    if (!this.searchable || !this.searchTerm.trim()) {
      return this.options;
    }
    const term = this.normalizeVietnamese(this.searchTerm.toLowerCase().trim());
    return this.options.filter((opt) => {
      const label = this.normalizeVietnamese(opt.label.toLowerCase());
      return label.includes(term);
    });
  }

  toggleOpen(): void {
    if (this.disabled) return;
    if (this.isOpen) {
      this.close();
    } else {
      this.open();
    }
  }

  open(): void {
    if (this.disabled) return;
    this.isOpen = true;
    this.searchTerm = '';
    this.updateHighlightedIndex();

    if (this.searchable && this.isBrowser) {
      setTimeout(() => {
        this.searchInputElement?.nativeElement.focus();
      }, 50);
    }
  }

  close(): void {
    this.isOpen = false;
    this.searchTerm = '';
    this.highlightedIndex = -1;
  }

  selectOption(option: SelectOption): void {
    if (option.disabled) return;
    this.value = option.value;
    this.valueChange.emit(option.value);
    this.close();
    if (this.isBrowser) {
      this.triggerButton?.nativeElement.focus();
    }
  }

  clearSelection(event: MouseEvent): void {
    event.stopPropagation();
    event.preventDefault();
    this.value = '';
    this.valueChange.emit('');
    this.close();
  }

  private updateHighlightedIndex(): void {
    const list = this.filteredOptions;
    const index = list.findIndex((opt) => opt.value === this.value);
    this.highlightedIndex = index >= 0 ? index : list.length > 0 ? 0 : -1;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.close();
    }
  }

  onTriggerKeyDown(event: KeyboardEvent): void {
    if (this.disabled) return;

    switch (event.key) {
      case 'Enter':
      case ' ':
        event.preventDefault();
        event.stopPropagation();
        this.toggleOpen();
        break;
      case 'ArrowDown':
        event.preventDefault();
        event.stopPropagation();
        if (!this.isOpen) {
          this.open();
        } else {
          this.moveHighlight(1);
        }
        break;
      case 'ArrowUp':
        event.preventDefault();
        event.stopPropagation();
        if (!this.isOpen) {
          this.open();
        } else {
          this.moveHighlight(-1);
        }
        break;
      case 'Escape':
        if (this.isOpen) {
          event.preventDefault();
          event.stopPropagation();
          this.close();
        }
        break;
      case 'Tab':
        this.close();
        break;
    }
  }

  onSearchKeyDown(event: KeyboardEvent): void {
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        event.stopPropagation();
        this.moveHighlight(1);
        break;
      case 'ArrowUp':
        event.preventDefault();
        event.stopPropagation();
        this.moveHighlight(-1);
        break;
      case 'Enter':
        event.preventDefault();
        event.stopPropagation();
        if (this.highlightedIndex >= 0 && this.highlightedIndex < this.filteredOptions.length) {
          const opt = this.filteredOptions[this.highlightedIndex];
          if (!opt.disabled) {
            this.selectOption(opt);
          }
        }
        break;
      case 'Escape':
        event.preventDefault();
        event.stopPropagation();
        this.close();
        this.triggerButton?.nativeElement.focus();
        break;
    }
  }

  private moveHighlight(step: number): void {
    const list = this.filteredOptions;
    if (list.length === 0) return;

    let nextIndex = this.highlightedIndex + step;
    if (nextIndex < 0) nextIndex = list.length - 1;
    if (nextIndex >= list.length) nextIndex = 0;

    // Skip disabled
    if (list[nextIndex]?.disabled) {
      nextIndex += step;
      if (nextIndex < 0) nextIndex = list.length - 1;
      if (nextIndex >= list.length) nextIndex = 0;
    }

    this.highlightedIndex = nextIndex;
    this.scrollToHighlighted();
  }

  private scrollToHighlighted(): void {
    if (!this.isBrowser || !this.optionsListElement) return;
    const items = this.optionsListElement.nativeElement.querySelectorAll('li');
    if (items[this.highlightedIndex]) {
      items[this.highlightedIndex].scrollIntoView({ block: 'nearest' });
    }
  }

  private normalizeVietnamese(str: string): string {
    return str
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd')
      .replace(/Đ/g, 'D');
  }
}
