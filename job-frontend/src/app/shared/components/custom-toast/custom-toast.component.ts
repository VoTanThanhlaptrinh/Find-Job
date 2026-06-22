import {
  animate,
  state,
  style,
  transition,
  trigger,
} from '@angular/animations';
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Toast } from 'ngx-toastr';

@Component({
  selector: 'app-custom-toast',
  standalone: true,
  imports: [CommonModule],
  styleUrl: './custom-toast.component.css',
  templateUrl: './custom-toast.component.html',
  animations: [
    trigger('flyInOut', [
      state('inactive', style({
        opacity: 0,
      })),
      state('active', style({
        opacity: 1,
      })),
      state('removed', style({
        opacity: 0,
      })),
      transition('inactive => active', animate('300ms ease-in')),
      transition('active => removed', animate('300ms ease-out')),
    ]),
  ],
  preserveWhitespaces: false,
})
export class CustomToastComponent extends Toast {

  get iconSvg(): string {
    // In ngx-toastr v20, toastPackage might be a signal or regular prop, let's assume it's still available via this.toastPackage() or this.toastPackage
    // The toast type is usually in this.toastPackage.toastType. Let's see if it's a signal. 
    // Wait, the error didn't complain about toastPackage being a function. 
    // So it's probably just a normal property or we can access it from options.
    const type = typeof this.toastPackage === 'function' ? (this.toastPackage as any)().toastType : this.toastPackage.toastType;
    switch (type) {
      case 'toast-success':
        return `<svg class="w-6 h-6 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>`;
      case 'toast-error':
        return `<svg class="w-6 h-6 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>`;
      case 'toast-warning':
        return `<svg class="w-6 h-6 text-orange-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>`;
      case 'toast-info':
      default:
        return `<svg class="w-6 h-6 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>`;
    }
  }

  get toastTypeClass(): string {
    const type = typeof this.toastPackage === 'function' ? (this.toastPackage as any)().toastType : this.toastPackage.toastType;
    return type;
  }
}
