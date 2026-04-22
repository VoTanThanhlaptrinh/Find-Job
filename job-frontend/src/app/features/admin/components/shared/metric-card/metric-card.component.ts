import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-admin-metric-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './metric-card.component.html',
})
export class AdminMetricCardComponent {
  @Input() label = '';
  @Input() value = '';
  @Input() hint = '';

  @Input()
  containerClass =
    'bg-white p-6 ad-rounded-xl shadow-sm border border-slate-100 flex flex-col justify-between';
  @Input() labelClass = 'text-xs font-bold uppercase tracking-widest text-slate-500';
  @Input() valueClass = 'text-3xl font-bold tracking-tight text-slate-900';
  @Input() valueRowClass = 'flex items-baseline space-x-2';
  @Input() hintClass = 'text-xs font-bold text-emerald-500';
}
