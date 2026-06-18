import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton-address-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col gap-4 w-full">
      <div *ngFor="let i of countArray" class="bg-slate-50 border border-slate-200 px-4 py-3 rounded-2xl relative flex justify-between gap-3">
        <div class="space-y-2 w-full">
          <div class="flex items-center gap-2">
            <div class="h-6 animate-shimmer rounded-md w-1/3"></div>
            <div class="h-5 animate-shimmer rounded-full w-16"></div>
          </div>
          <div class="h-4 animate-shimmer rounded-md w-2/3 mt-1"></div>
        </div>
        <div class="w-9 h-9 rounded-full animate-shimmer shrink-0 mt-1"></div>
      </div>
    </div>
  `
})
export class SkeletonAddressCardComponent {
  @Input() count: number = 1;

  get countArray(): number[] {
    return Array.from({ length: this.count }, (_, i) => i);
  }
}
