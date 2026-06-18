import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton-job-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col items-center gap-6 w-full">
      <div *ngFor="let i of countArray" class="flex w-full lg:w-[70vw] bg-white border border-slate-200 rounded-xl px-4 py-3 shadow-md flex-col gap-4">
        <div class="flex items-start gap-4">
          <!-- Logo skeleton -->
          <div class="w-20 h-20 rounded-xl animate-shimmer shrink-0"></div>
          
          <!-- Content skeleton -->
          <div class="grow min-w-0 space-y-3 mt-1">
            <div class="flex items-start justify-between gap-2">
              <div class="h-6 animate-shimmer rounded-md w-2/3"></div>
              <div class="h-6 animate-shimmer rounded-full w-24 shrink-0"></div>
            </div>
            
            <div class="flex items-center gap-3">
              <div class="h-4 animate-shimmer rounded-md w-32"></div>
              <div class="h-4 animate-shimmer rounded-md w-24"></div>
              <div class="h-4 animate-shimmer rounded-md w-20"></div>
            </div>
          </div>
        </div>
        
        <!-- Progress bar skeleton -->
        <div class="border-t border-slate-100 pt-3 mt-1">
          <div class="flex justify-between items-center mb-1.5">
            <div class="h-3 animate-shimmer rounded w-16"></div>
            <div class="h-3 animate-shimmer rounded w-12"></div>
          </div>
          <div class="w-full h-2 animate-shimmer rounded-full"></div>
        </div>
        
        <!-- Actions skeleton -->
        <div class="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
          <div class="h-8 animate-shimmer rounded-full w-20"></div>
          <div class="h-8 animate-shimmer rounded-full w-32"></div>
          <div class="h-8 animate-shimmer rounded-full w-24"></div>
          <div class="h-8 animate-shimmer rounded-full w-16"></div>
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
