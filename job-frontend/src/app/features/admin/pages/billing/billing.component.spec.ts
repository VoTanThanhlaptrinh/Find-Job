import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideToastr } from 'ngx-toastr';
import { of, throwError } from 'rxjs';
import { BillingComponent } from './billing.component';
import { AdminBillingService } from '../../services/admin-billing.service';
import {
  AdminBillingSummary,
  AdminBillingTier,
  AdminBillingTransactionItem,
} from '../../services/admin-api.models';

describe('BillingComponent', () => {
  let component: BillingComponent;
  let fixture: ComponentFixture<BillingComponent>;
  let billingService: AdminBillingService;
  let router: Router;

  const mockSummary: AdminBillingSummary = {
    monthlyRecurringRevenue: 150000000,
    mrrGrowthPct: 12.5,
    activeSubscriptions: 85,
  };

  const mockTiers: AdminBillingTier[] = [
    {
      id: 'tier-1',
      name: 'Gói Cơ bản',
      badge: 'Starter',
      priceMonthly: 1990000,
      currency: 'VND',
      isPopular: false,
      usagePct: 25,
      features: ['Đăng 5 tin tuyển dụng', 'Xem 50 hồ sơ'],
    },
    {
      id: 'tier-2',
      name: 'Gói Tăng trưởng',
      badge: 'Growth',
      priceMonthly: 4990000,
      currency: 'VND',
      isPopular: true,
      usagePct: 60,
      features: ['Đăng 20 tin tuyển dụng', 'Xem 250 hồ sơ', 'Huy hiệu nổi bật'],
    },
  ];

  const mockTransactions: AdminBillingTransactionItem[] = [
    {
      id: 'tx-1',
      employerId: 'emp-001',
      employerName: 'Công ty Công nghệ ABC',
      packageName: 'Gói Tăng trưởng',
      amount: 4990000,
      currency: 'VND',
      date: '2026-09-24T10:00:00Z',
      status: 'paid',
    },
    {
      id: 'tx-2',
      employerId: 'emp-002',
      employerName: 'Tập đoàn XYZ',
      packageName: 'Gói Cơ bản',
      amount: 1990000,
      currency: 'VND',
      date: '2026-09-23T14:30:00Z',
      status: 'pending',
    },
    {
      id: 'tx-3',
      employerId: 'emp-003',
      employerName: 'Startup DEF',
      packageName: 'Gói Cơ bản',
      amount: 1990000,
      currency: 'VND',
      date: '2026-09-22T08:15:00Z',
      status: 'failed',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BillingComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideToastr(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BillingComponent);
    component = fixture.componentInstance;
    billingService = TestBed.inject(AdminBillingService);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create the component successfully', () => {
    expect(component).toBeTruthy();
  });

  describe('Summary & KPI formatting (BL-02, BL-03)', () => {
    it('should format currency correctly with locale', () => {
      const formattedVnd = component.formatCurrency(1990000, 'VND');
      expect(formattedVnd).toContain('1.990.000');

      const formattedUsd = component.formatCurrency(99, 'USD');
      expect(formattedUsd).toBeTruthy();

      const zeroFormatted = component.formatCurrency(0, 'VND');
      expect(zeroFormatted).toContain('0');
    });

    it('should trigger retrySummary to reload summary metrics', () => {
      spyOn(billingService, 'loadSummary');
      component.retrySummary();
      expect(billingService.loadSummary).toHaveBeenCalled();
    });
  });

  describe('Tiers management & Drawer (BL-04, BL-05, BL-06, BL-07, BL-11, BL-15, BL-16)', () => {
    it('should open edit drawer with the selected tier and initialize form fields', () => {
      const tier = mockTiers[0];
      component.openEditDrawer(tier);

      expect(component.isDrawerOpen).toBeTrue();
      expect(component.editingTier).toEqual(tier);
      expect(component.formPriceMonthly).toBe(tier.priceMonthly);
      expect(component.formFeatures).toEqual([...tier.features]);
      expect(component.isFormDirty).toBeFalse();
    });

    it('should recognize dirty state when price or features change', () => {
      const tier = mockTiers[0];
      component.openEditDrawer(tier);

      component.formPriceMonthly = 2500000;
      expect(component.isFormDirty).toBeTrue();

      component.formPriceMonthly = tier.priceMonthly;
      expect(component.isFormDirty).toBeFalse();

      component.formFeatures.push('Tính năng mới');
      expect(component.isFormDirty).toBeTrue();
    });

    it('should prompt unsaved changes dialog if closing dirty form', () => {
      const tier = mockTiers[0];
      component.openEditDrawer(tier);
      component.formPriceMonthly = 9999999;

      component.attemptCloseDrawer();
      expect(component.showUnsavedDialog).toBeTrue();
      expect(component.isDrawerOpen).toBeTrue();

      component.cancelDiscardChanges();
      expect(component.showUnsavedDialog).toBeFalse();
      expect(component.isDrawerOpen).toBeTrue();

      component.confirmDiscardChanges();
      expect(component.showUnsavedDialog).toBeFalse();
      expect(component.isDrawerOpen).toBeFalse();
    });

    it('should validate form and prevent submit when price <= 0 or features empty', () => {
      const tier = mockTiers[0];
      component.openEditDrawer(tier);
      spyOn(billingService, 'updateTier');

      component.formPriceMonthly = 0;
      expect(component.isFormValid).toBeFalse();
      component.submitSaveTier();
      expect(billingService.updateTier).not.toHaveBeenCalled();

      component.formPriceMonthly = 1000000;
      component.formFeatures = [];
      expect(component.isFormValid).toBeFalse();
      component.submitSaveTier();
      expect(billingService.updateTier).not.toHaveBeenCalled();
    });

    it('should submit updated tier, close drawer on success', fakeAsync(() => {
      const tier = mockTiers[0];
      component.openEditDrawer(tier);
      component.formPriceMonthly = 2500000;

      spyOn(billingService, 'updateTier').and.returnValue(
        of({ id: tier.id, updated: true })
      );

      component.submitSaveTier();
      tick();

      expect(billingService.updateTier).toHaveBeenCalledWith(
        tier.id,
        {
          priceMonthly: 2500000,
          features: tier.features,
        },
        tier.name
      );
      expect(component.isDrawerOpen).toBeFalse();
    }));

    it('should keep drawer open and display inline error on update failure (BL-07)', fakeAsync(() => {
      const tier = mockTiers[0];
      component.openEditDrawer(tier);
      component.formPriceMonthly = 2500000;

      spyOn(billingService, 'updateTier').and.returnValue(
        throwError(() => ({ error: { message: 'Lỗi xác thực dữ liệu' } }))
      );

      component.submitSaveTier();
      tick();

      expect(component.isDrawerOpen).toBeTrue();
      expect(component.drawerError).toBe('Lỗi xác thực dữ liệu');
      expect(component.formPriceMonthly).toBe(2500000); // User input preserved
    }));

    it('should add and remove features in tag mode', () => {
      const tier = mockTiers[0];
      component.openEditDrawer(tier);

      component.formNewFeature = 'Thêm quyền lợi AI';
      component.addFeature();
      expect(component.formFeatures).toContain('Thêm quyền lợi AI');
      expect(component.formNewFeature).toBe('');

      const len = component.formFeatures.length;
      component.removeFeature(len - 1);
      expect(component.formFeatures.length).toBe(len - 1);
    });
  });

  describe('Transactions & Status badges (BL-08, BL-09, BL-12, BL-13, BL-14)', () => {
    it('should return correct semantic status badge classes and labels', () => {
      expect(component.getStatusLabel('paid')).toBe('Đã thanh toán');
      expect(component.getStatusLabel('pending')).toBe('Đang chờ xử lý');
      expect(component.getStatusLabel('failed')).toBe('Thất bại');

      expect(component.getStatusBadgeClass('paid')).toContain('emerald');
      expect(component.getStatusBadgeClass('pending')).toContain('amber');
      expect(component.getStatusBadgeClass('failed')).toContain('rose');
    });

    it('should format date string with timezone and locale (BL-13)', () => {
      const formatted = component.formatDate('2026-09-24T10:00:00Z');
      expect(formatted).toBeTruthy();
      expect(component.formatDate(null)).toBe('—');
    });

    it('should handle status filter change and reset to page 1 (BL-14)', () => {
      spyOn(billingService, 'updateTransactionsQuery');
      const event = { target: { value: 'paid' } } as unknown as Event;

      component.onTransactionsStatusChange(event);
      expect(component.selectedTransactionStatus).toBe('paid');
      expect(component.currentPage).toBe(1);
      expect(billingService.updateTransactionsQuery).toHaveBeenCalledWith({
        page: 1,
        status: 'paid',
      });
    });

    it('should calculate pagination properties accurately (BL-09)', () => {
      component.totalTransactions = 45;
      expect(component.totalPages).toBe(3);
      expect(component.pageStartIndex).toBe(1);
      expect(component.pageEndIndex).toBe(20);

      component.currentPage = 3;
      expect(component.pageStartIndex).toBe(41);
      expect(component.pageEndIndex).toBe(45);
    });

    it('should navigate through pages and update query', () => {
      spyOn(billingService, 'updateTransactionsQuery');
      component.totalTransactions = 50;
      component.currentPage = 1;

      component.goToNextPage();
      expect(component.currentPage).toBe(2);
      expect(billingService.updateTransactionsQuery).toHaveBeenCalledWith({ page: 2 });

      component.goToPrevPage();
      expect(component.currentPage).toBe(1);
      expect(billingService.updateTransactionsQuery).toHaveBeenCalledWith({ page: 1 });
    });

    it('should trigger retryTransactions on failure', () => {
      spyOn(billingService, 'loadTransactions');
      component.retryTransactions();
      expect(billingService.loadTransactions).toHaveBeenCalled();
    });
  });
});
