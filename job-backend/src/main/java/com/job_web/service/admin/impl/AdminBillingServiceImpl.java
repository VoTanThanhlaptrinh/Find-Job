package com.job_web.service.admin.impl;

import com.job_web.dto.admin.billing.BillingSummaryResponse;
import com.job_web.dto.admin.billing.BillingTierDTO;
import com.job_web.dto.admin.billing.TransactionListItem;
import com.job_web.dto.common.PageResponse;
import com.job_web.service.admin.AdminBillingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AdminBillingServiceImpl implements AdminBillingService {

    @Override
    public List<BillingTierDTO> getBillingTiers() {
        List<BillingTierDTO> tiers = new ArrayList<>();
        tiers.add(BillingTierDTO.builder().id("basic").name("Basic").badge("Standard").priceMonthly(499).currency("USD").isPopular(false).usagePct(15).features(List.of("Up to 10 Job Postings", "Standard Talent Pool", "Email Support")).build());
        tiers.add(BillingTierDTO.builder().id("pro").name("Pro").badge("Advanced").priceMonthly(1299).currency("USD").isPopular(true).usagePct(65).features(List.of("Unlimited Job Postings", "AI Talent Matching", "Dedicated Account Manager", "Analytics Dashboard")).build());
        return tiers;
    }

    @Override
    public void updateBillingTier(String id, BillingTierDTO request) {
        log.info("Billing tier {} updated", id);
    }

    @Override
    public PageResponse<TransactionListItem> getTransactions(int page, int pageSize, String status) {
        List<TransactionListItem> items = List.of(TransactionListItem.builder()
                .id("TX-90234")
                .employerId("emp_001")
                .employerName("Nexus Core Inc.")
                .packageName("pro")
                .amount(1299)
                .currency("USD")
                .date(LocalDateTime.now())
                .status("paid")
                .build());
        return PageResponse.<TransactionListItem>builder()
                .items(items)
                .pagination(PageResponse.Pagination.builder().page(page).pageSize(pageSize).totalItems(1).totalPages(1).build())
                .build();
    }

    @Override
    public BillingSummaryResponse getBillingSummary() {
        return BillingSummaryResponse.builder()
                .monthlyRecurringRevenue(442890.0)
                .mrrGrowthPct(12.4)
                .activeSubscriptions(2841)
                .build();
    }
}
