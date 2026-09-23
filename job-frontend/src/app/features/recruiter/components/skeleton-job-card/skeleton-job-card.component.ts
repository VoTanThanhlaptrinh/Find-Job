import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton-job-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col items-center gap-6 w-full">
      <div *ngFor="let i of countArray" class="flex w-full lg:w-[70vw] bg-white border border-slate-200/90 rounded-xl px-4 py-3.5 sm:px-5 shadow-xs flex-col gap-3">
        <div class="flex items-start gap-4">
          <!-- Logo skeleton -->
          <div class="w-16 h-16 rounded-xl animate-shimmer shrink-0"></div>
          
          <!-- Content skeleton -->
          <div class="grow min-w-0 space-y-2.5 mt-0.5">
            <div class="flex items-start justify-between gap-2">
              <div class="h-5 animate-shimmer rounded-md w-1/2"></div>
              <div class="h-5 animate-shimmer rounded-full w-20 shrink-0"></div>
            </div>
            
            <div class="flex items-center gap-2.5">
              <div class="h-4 animate-shimmer rounded-md w-28"></div>
              <div class="h-4 animate-shimmer rounded-md w-20"></div>
              <div class="h-4 animate-shimmer rounded-md w-16"></div>
              <div class="h-5 animate-shimmer rounded-full w-28"></div>
            </div>
          </div>
        </div>
        
        <!-- Actions skeleton -->
        <div class="flex items-center justify-end gap-2 pt-2.5 border-t border-slate-100">
          <div class="h-7 animate-shimmer rounded-lg w-16"></div>
          <div class="h-7 animate-shimmer rounded-lg w-28"></div>
          <div class="h-7 animate-shimmer rounded-lg w-20"></div>
          <div class="h-7 animate-shimmer rounded-lg w-14"></div>
        </div>
      </div>
    </div>
  `
})
export class SkeletonJobCardComponent {
  @Input() count: number = 1;

  get countArray(): number[] {
    return Array.from({ length: this.count }, (_, i) => i);
  }
}
