import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

export interface AdminBreadcrumbItem {
  label: string;
  routerLink?: string | readonly unknown[];
}

@Component({
  selector: 'app-admin-page-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './page-header.component.html',
})
export class AdminPageHeaderComponent {
  @Input({ required: true }) title = '';
  @Input() description = '';
  @Input() breadcrumbs: AdminBreadcrumbItem[] = [];

  // Direct action inputs for backward compatibility / simple usage
  @Input() actionLabel = '';
  @Input() actionIcon = '';
  @Input() actionDisabled = false;
  @Input() actionLoading = false;
  @Output() actionClick = new EventEmitter<void>();

  onActionClick(): void {
    if (!this.actionDisabled && !this.actionLoading) {
      this.actionClick.emit();
    }
  }
}
