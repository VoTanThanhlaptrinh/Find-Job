import { Component, OnInit, inject, effect } from '@angular/core';
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

  summary: any = null;
  tiers: AdminBillingTier[] = [];
  transactions: AdminBillingTransactionItem[] = [];
  totalTransactions = 0;
  isLoadingSummary = false;
  isLoadingTiers = false;
  isLoadingTransactions = false;
  updatingTierId: string | null = null;
  selectedTransactionStatus = '';

  constructor() {
    effect(() => {
      this.summary = this.billingService.summary();
      this.tiers = this.billingService.tiers();
      this.transactions = this.billingService.transactions();
      this.totalTransactions = this.billingService.totalTransactions();
      this.isLoadingSummary = this.billingService.isLoadingSummary();
      this.isLoadingTiers = this.billingService.isLoadingTiers();
      this.isLoadingTransactions = this.billingService.isLoadingTransactions();
      this.updatingTierId = this.billingService.updatingTierId();
      
      const query = this.billingService.transactionsQuery();
      this.selectedTransactionStatus = query.status ?? '';
    });
  }

  ngOnInit(): void {
    this.billingService.loadSummary();
    this.billingService.loadTiers();
    this.billingService.loadTransactions();
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
