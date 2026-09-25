import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type MetricTrendDirection = 'up' | 'down' | 'flat';
export type MetricTrendTone = 'positive' | 'negative' | 'warning' | 'neutral';

export interface AdminMetricTrend {
  displayValue: string;
  direction: MetricTrendDirection;
  tone: MetricTrendTone;
  comparisonLabel: string;
  ariaLabel?: string;
}

export interface AdminMetricCardViewModel {
  label: string;
  displayValue: string | number;
  valueAriaLabel?: string;
  trend?: AdminMetricTrend;
  subtext?: string;
}

@Component({
  selector: 'app-admin-metric-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './metric-card.component.html',
})
export class AdminMetricCardComponent {
  @Input({ required: true }) label = '';
  @Input() displayValue: string | number | null = null;
  @Input() valueAriaLabel?: string;
  @Input() trend?: AdminMetricTrend;
  @Input() subtext?: string;
  @Input() loading = false;

  // Compatibility getters/setters for legacy 'value' and 'hint' inputs
  @Input()
  set value(val: string | number | null) {
    if (this.displayValue === null || this.displayValue === undefined) {
      this.displayValue = val;
    }
  }
  get value(): string | number | null {
    return this.displayValue;
  }

  @Input()
  set hint(val: string) {
    if (val && !this.trend && !this.subtext) {
      this.subtext = val;
    }
  }
  get hint(): string {
    return this.subtext || '';
  }

  get formattedDisplayValue(): string {
    if (this.displayValue === null || this.displayValue === undefined || this.displayValue === '') {
      return '—';
    }
    return String(this.displayValue);
  }

  get isUnknownValue(): boolean {
    return this.displayValue === null || this.displayValue === undefined || this.displayValue === '';
  }

  getTrendToneClass(tone?: MetricTrendTone): string {
    switch (tone) {
      case 'positive':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200/60';
      case 'negative':
        return 'bg-rose-50 text-rose-700 border-rose-200/60';
      case 'warning':
        return 'bg-amber-50 text-amber-700 border-amber-200/60';
      case 'neutral':
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  }

  getTrendTextClass(tone?: MetricTrendTone): string {
    switch (tone) {
      case 'positive':
        return 'text-emerald-700';
      case 'negative':
        return 'text-rose-700';
      case 'warning':
        return 'text-amber-700';
      case 'neutral':
      default:
        return 'text-slate-600';
    }
  }
}
