import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
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

  trackByTier(_: number, item: AdminBillingTier): string {
    return item.id;
  }

  trackByTransaction(_: number, item: AdminBillingTransactionItem): string {
    return item.id;
  }
}
