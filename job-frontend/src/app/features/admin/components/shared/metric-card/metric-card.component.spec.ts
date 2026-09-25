import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { AdminMetricCardComponent, AdminMetricTrend } from './metric-card.component';

@Component({
  standalone: true,
  imports: [AdminMetricCardComponent],
  template: `
    <app-admin-metric-card
      [label]="label"
      [displayValue]="displayValue"
      [trend]="trend"
      [loading]="loading"
      [subtext]="subtext"
    >
      <span metric-icon class="test-icon">icon_user</span>
      <span metric-footer class="test-footer">Custom footer</span>
    </app-admin-metric-card>
  `,
})
class TestHostComponent {
  label = 'Tổng người dùng';
  displayValue: string | number | null = '12.450';
  trend?: AdminMetricTrend;
  loading = false;
  subtext?: string;
}

describe('AdminMetricCardComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let hostComponent: TestHostComponent;
  let cardElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, AdminMetricCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
    cardElement = fixture.nativeElement.querySelector('article');
  });

  it('should render semantic article root with accessible label', () => {
    expect(cardElement).toBeTruthy();
    expect(cardElement.getAttribute('aria-label')).toBe('Tổng người dùng');
  });

  it('should render label and formatted display value correctly', () => {
    const labelEl = cardElement.querySelector('h3');
    expect(labelEl?.textContent?.trim()).toBe('Tổng người dùng');

    const valueEl = cardElement.querySelector('p.tabular-nums');
    expect(valueEl?.textContent?.trim()).toBe('12.450');
  });

  it('should display em dash for unknown/null/empty value without defaulting to 0', () => {
    hostComponent.displayValue = null;
    fixture.detectChanges();

    const valueEl = cardElement.querySelector('p.tabular-nums');
    expect(valueEl?.textContent?.trim()).toBe('—');
    expect(valueEl?.classList.contains('italic')).toBeTrue();
  });

  it('should render trend with correct direction, display value, tone, and comparison label', () => {
    hostComponent.trend = {
      displayValue: '+15.2%',
      direction: 'up',
      tone: 'positive',
      comparisonLabel: 'so với tháng trước',
    };
    fixture.detectChanges();

    const trendText = cardElement.textContent;
    expect(trendText).toContain('+15.2%');
    expect(trendText).toContain('so với tháng trước');

    // Direction SVG is present
    const svgIcon = cardElement.querySelector('svg');
    expect(svgIcon).toBeTruthy();

    // Check positive tone class
    const trendBadge = cardElement.querySelector('span.border');
    expect(trendBadge?.className).toContain('bg-emerald-50');
  });

  it('should decouple trend tone from direction (e.g. pending jobs down with positive tone)', () => {
    hostComponent.trend = {
      displayValue: '-20%',
      direction: 'down',
      tone: 'positive',
      comparisonLabel: 'giảm tồn đọng',
    };
    fixture.detectChanges();

    const trendBadge = cardElement.querySelector('span.border');
    expect(trendBadge?.className).toContain('bg-emerald-50');
    expect(trendBadge?.textContent).toContain('-20%');
  });

  it('should render fallback subtext when no trend is provided', () => {
    hostComponent.trend = undefined;
    hostComponent.subtext = 'Chỉ số 30 ngày qua';
    fixture.detectChanges();

    expect(cardElement.textContent).toContain('Chỉ số 30 ngày qua');
  });

  it('should render skeleton loading and aria-busy when loading is true', () => {
    hostComponent.loading = true;
    fixture.detectChanges();

    expect(cardElement.getAttribute('aria-busy')).toBe('true');
    const skeleton = cardElement.querySelector('[role="status"]');
    expect(skeleton).toBeTruthy();

    const srOnly = skeleton?.querySelector('.sr-only');
    expect(srOnly?.textContent).toContain('Đang tải chỉ số');
  });

  it('should project metric-icon and metric-footer properly', () => {
    const iconEl = cardElement.querySelector('.test-icon');
    expect(iconEl?.textContent).toBe('icon_user');

    const footerEl = cardElement.querySelector('.test-footer');
    expect(footerEl?.textContent).toBe('Custom footer');
  });
});
