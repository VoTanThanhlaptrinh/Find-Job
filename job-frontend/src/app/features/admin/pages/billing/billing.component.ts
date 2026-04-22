import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { take } from 'rxjs';
import { AdminBillingService } from '../../services/admin-billing.service';
import { AdminBillingTier, AdminBillingTransactionItem } from '../../services/admin-api.models';

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './billing.component.html'
})
export class BillingComponent implements OnInit {
  private readonly billingService = inject(AdminBillingService);

  ngOnInit(): void {
    this.billingService.loadSummary();
    this.billingService.loadTiers();
    this.billingService.loadTransactions();
  }

  get summary() {
    return this.billingService.summary();
  }

  get tiers() {
    return this.billingService.tiers();
  }

  get transactions() {
    return this.billingService.transactions();
  }

  get totalTransactions(): number {
    return this.billingService.totalTransactions();
  }

  get isLoadingSummary(): boolean {
    return this.billingService.isLoadingSummary();
  }

  get isLoadingTiers(): boolean {
    return this.billingService.isLoadingTiers();
  }

  get isLoadingTransactions(): boolean {
    return this.billingService.isLoadingTransactions();
  }

  get updatingTierId(): string | null {
    return this.billingService.updatingTierId();
  }

  get selectedTransactionStatus(): string {
    return this.billingService.transactionsQuery().status ?? '';
  }

  onTransactionsStatusChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.billingService.updateTransactionsQuery({
      page: 1,
      status: value.length > 0 ? value : undefined,
    });
  }

  saveTier(
    tier: AdminBillingTier,
    priceValue: string,
    featuresValue: string
  ): void {
    const priceMonthly = Number(priceValue);
    if (Number.isNaN(priceMonthly) || priceMonthly <= 0) {
      return;
    }

    const features = featuresValue
      .split(',')
      .map((feature) => feature.trim())
      .filter((feature) => feature.length > 0);

    this.billingService.updateTier(tier.id, {
      priceMonthly,
      features,
    }).pipe(take(1)).subscribe();
  }

  trackByTier(_: number, item: AdminBillingTier): string {
    return item.id;
  }

  trackByTransaction(_: number, item: AdminBillingTransactionItem): string {
    return item.id;
  }
}
