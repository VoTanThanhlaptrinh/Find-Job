package com.job_web.admin.application;

import com.job_web.admin.api.dto.billing.BillingSummaryResponse;
import com.job_web.admin.api.dto.billing.BillingTierDTO;
import com.job_web.admin.api.dto.billing.TransactionListItem;
import com.job_web.shared.domain.model.PageResponse;

import java.util.List;

public interface AdminBillingService {
    List<BillingTierDTO> getBillingTiers();

    void updateBillingTier(String id, BillingTierDTO request);

    PageResponse<TransactionListItem> getTransactions(int page, int pageSize, String status);

    BillingSummaryResponse getBillingSummary();
}
