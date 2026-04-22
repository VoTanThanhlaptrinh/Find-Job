import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-admin-page-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './page-header.component.html',
})
export class AdminPageHeaderComponent {
  @Input() title = '';
  @Input() description = '';
  @Input() actionLabel = '';
  @Input() actionIcon = '';

  @Input() wrapperClass = 'flex justify-between items-end mb-8';
  @Input() titleClass = 'text-3xl font-bold tracking-tight text-slate-900';
  @Input() descriptionClass = 'text-slate-500 mt-1';

  @Input()
  actionClass =
    'flex items-center gap-2 bg-blue-600 text-white px-5 py-2.5 ad-rounded-xl font-bold text-sm shadow-md shadow-blue-600/20 hover:bg-blue-700 active:scale-95 transition-all';
}
