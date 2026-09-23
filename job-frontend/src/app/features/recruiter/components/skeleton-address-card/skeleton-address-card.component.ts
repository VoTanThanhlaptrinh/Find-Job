import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton-address-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 w-full">
      <div *ngFor="let i of countArray" class="bg-white border border-slate-200/80 p-5 rounded-2xl relative flex flex-col justify-between shadow-sm animate-pulse">
        <div class="flex items-start justify-between gap-3">
          <div class="flex items-center gap-3 w-full">
            <div class="w-10 h-10 rounded-xl bg-slate-100 shrink-0"></div>
            <div class="space-y-1.5 w-2/3">
              <div class="h-4 bg-slate-100 rounded-md w-3/4"></div>
              <div class="h-3 bg-slate-100 rounded-md w-1/2"></div>
            </div>
          </div>
        </div>
        <div class="mt-4 pt-4 border-t border-slate-100 space-y-2">
          <div class="h-3.5 bg-slate-100 rounded-md w-full"></div>
          <div class="h-3.5 bg-slate-100 rounded-md w-4/5"></div>
        </div>
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
